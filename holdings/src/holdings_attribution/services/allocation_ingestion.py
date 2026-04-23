from __future__ import annotations

from datetime import datetime

from holdings_attribution.enums import EventStatus, EventType, Side
from holdings_attribution.models import HoldingBucket, HoldingEvent, OMSAllocation
from holdings_attribution.repositories.in_memory import InMemoryRepository
from holdings_attribution.services.settlement_rules import SettlementRuleService


class AllocationIngestionService:
    def __init__(self, repo: InMemoryRepository, rules: SettlementRuleService) -> None:
        self.repo = repo
        self.rules = rules

    def ingest(self, allocation: OMSAllocation) -> HoldingEvent:
        if allocation.quantity <= 0:
            raise ValueError("Allocation quantity must be positive")

        expected_settlement = self.rules.expected_settlement_date(
            allocation.place_of_trade, allocation.trade_date
        )
        event_type = (
            EventType.ALLOCATED_BUY_PENDING
            if allocation.side == Side.BUY
            else EventType.ALLOCATED_SELL_PENDING
        )
        event = HoldingEvent(
            id=self.repo.next_id("hev"),
            customer_account_id=allocation.customer_account_id,
            isin=allocation.isin,
            place_of_trade=allocation.place_of_trade,
            event_type=event_type,
            side=allocation.side,
            quantity=allocation.quantity,
            trade_date=allocation.trade_date,
            expected_settlement_date=expected_settlement,
            actual_settlement_date=None,
            business_date=allocation.trade_date,
            source_system="OMS",
            source_ref=allocation.allocation_id,
            linked_ref=None,
            status=EventStatus.OPEN,
        )
        self.repo.append_event(event)

        bucket = self.repo.get_bucket(
            allocation.customer_account_id, allocation.isin, allocation.place_of_trade
        ) or HoldingBucket(
            customer_account_id=allocation.customer_account_id,
            isin=allocation.isin,
            place_of_trade=allocation.place_of_trade,
        )
        if allocation.side == Side.BUY:
            bucket.pending_buy_qty += allocation.quantity
        else:
            bucket.pending_sell_qty += allocation.quantity
        bucket.last_updated_at = datetime.utcnow()
        self.repo.upsert_bucket(bucket)
        return event
