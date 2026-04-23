from __future__ import annotations

from datetime import datetime

from holdings_attribution.enums import EventStatus, EventType, Side, UNKNOWN_PLACE_OF_TRADE
from holdings_attribution.models import HoldingBucket, HoldingEvent, ReconciliationException, SettlementAttribution
from holdings_attribution.repositories.in_memory import InMemoryRepository


class SettlementAttributionService:
    """Attributes custody aggregate deltas back to OMS place-of-trade pending queues."""

    def __init__(self, repo: InMemoryRepository) -> None:
        self.repo = repo

    def attribute_delta(
        self,
        snapshot_id: str,
        business_date,
        customer_account_id: str,
        isin: str,
        delta_qty: int,
    ) -> list[SettlementAttribution]:
        if delta_qty == 0:
            return []

        side = Side.BUY if delta_qty > 0 else Side.SELL
        remaining = abs(delta_qty)
        pending = self.repo.pending_events(customer_account_id, isin, side.value)
        pending_sorted = sorted(
            pending,
            key=lambda ev: (
                ev.expected_settlement_date,
                ev.trade_date,
                ev.created_at,
                ev.source_ref,
                ev.id,
            ),
        )

        results: list[SettlementAttribution] = []
        for ev in pending_sorted:
            if remaining <= 0:
                break
            take = min(remaining, ev.quantity)
            self._apply_settlement(ev, take, snapshot_id, business_date)
            results.append(
                SettlementAttribution(
                    id=self.repo.next_id("att"),
                    customer_account_id=customer_account_id,
                    isin=isin,
                    place_of_trade=ev.place_of_trade or UNKNOWN_PLACE_OF_TRADE,
                    attributed_qty=take if side == Side.BUY else -take,
                    attribution_method="FIFO(expected_settlement,trade_date,allocation_ts,allocation_id)",
                    custody_snapshot_id=snapshot_id,
                    attributed_business_date=business_date,
                    source_pending_event_id=ev.id,
                )
            )
            remaining -= take

        if remaining > 0:
            # Honest handling of unmatched movement: place in UNKNOWN and record external adjustment.
            unknown_bucket = self.repo.get_bucket(customer_account_id, isin, UNKNOWN_PLACE_OF_TRADE) or HoldingBucket(
                customer_account_id=customer_account_id, isin=isin, place_of_trade=UNKNOWN_PLACE_OF_TRADE
            )
            if side == Side.BUY:
                unknown_bucket.settled_qty += remaining
                adjustment_type = EventType.EXTERNAL_ADJUSTMENT
                qty = remaining
            else:
                unknown_bucket.settled_qty -= remaining
                adjustment_type = EventType.EXTERNAL_ADJUSTMENT
                qty = -remaining
            unknown_bucket.last_updated_at = datetime.utcnow()
            self.repo.upsert_bucket(unknown_bucket)
            self.repo.append_event(
                HoldingEvent(
                    id=self.repo.next_id("hev"),
                    customer_account_id=customer_account_id,
                    isin=isin,
                    place_of_trade=UNKNOWN_PLACE_OF_TRADE,
                    event_type=adjustment_type,
                    side=side,
                    quantity=abs(qty),
                    trade_date=None,
                    expected_settlement_date=None,
                    actual_settlement_date=business_date,
                    business_date=business_date,
                    source_system="CUSTODY",
                    source_ref=snapshot_id,
                    linked_ref=None,
                    status=EventStatus.SETTLED,
                )
            )
            results.append(
                SettlementAttribution(
                    id=self.repo.next_id("att"),
                    customer_account_id=customer_account_id,
                    isin=isin,
                    place_of_trade=UNKNOWN_PLACE_OF_TRADE,
                    attributed_qty=remaining if side == Side.BUY else -remaining,
                    attribution_method="UNMATCHED_TO_UNKNOWN",
                    custody_snapshot_id=snapshot_id,
                    attributed_business_date=business_date,
                    source_pending_event_id=None,
                )
            )

        for att in results:
            self.repo.append_attribution(att)
        return results

    def _apply_settlement(self, pending_event: HoldingEvent, qty: int, snapshot_id: str, business_date) -> None:
        bucket = self.repo.get_bucket(
            pending_event.customer_account_id,
            pending_event.isin,
            pending_event.place_of_trade or UNKNOWN_PLACE_OF_TRADE,
        )
        if not bucket:
            raise ValueError("Expected bucket for pending event")

        if pending_event.side == Side.BUY:
            bucket.pending_buy_qty -= qty
            bucket.settled_qty += qty
            settled_type = EventType.SETTLED_BUY_ATTRIBUTED
        else:
            bucket.pending_sell_qty -= qty
            bucket.settled_qty -= qty
            settled_type = EventType.SETTLED_SELL_ATTRIBUTED
        bucket.last_updated_at = datetime.utcnow()
        self.repo.upsert_bucket(bucket)

        pending_event.quantity -= qty
        if pending_event.quantity == 0:
            pending_event.status = EventStatus.SETTLED
        self.repo.append_event(
            HoldingEvent(
                id=self.repo.next_id("hev"),
                customer_account_id=pending_event.customer_account_id,
                isin=pending_event.isin,
                place_of_trade=pending_event.place_of_trade,
                event_type=settled_type,
                side=pending_event.side,
                quantity=qty,
                trade_date=pending_event.trade_date,
                expected_settlement_date=pending_event.expected_settlement_date,
                actual_settlement_date=business_date,
                business_date=business_date,
                source_system="CUSTODY",
                source_ref=snapshot_id,
                linked_ref=pending_event.id,
                status=EventStatus.SETTLED,
            )
        )
