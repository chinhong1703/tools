from __future__ import annotations

from collections import defaultdict

from holdings_attribution.enums import EventStatus, EventType
from holdings_attribution.models import CustodyPositionRaw, CustodyPositionSnapshot, CustodyRowInput, HoldingEvent
from holdings_attribution.repositories.in_memory import InMemoryRepository
from holdings_attribution.services.settlement_attribution import SettlementAttributionService


class CustodySnapshotIngestionService:
    def __init__(self, repo: InMemoryRepository, attribution: SettlementAttributionService) -> None:
        self.repo = repo
        self.attribution = attribution

    def ingest(self, rows: list[CustodyRowInput]) -> dict:
        if not rows:
            return {"aggregates": [], "attributions": []}

        aggregates: dict[tuple[str, str, str], dict] = defaultdict(lambda: {"qty": 0, "rows": 0, "date": None})
        for row in rows:
            self.repo.append_raw_row(
                CustodyPositionRaw(
                    id=self.repo.next_id("raw"),
                    snapshot_id=row.snapshot_id,
                    business_date=row.business_date,
                    customer_account_id=row.customer_account_id,
                    isin=row.isin,
                    quantity=row.quantity,
                    raw_line_ref=row.raw_line_ref,
                )
            )
            key = (row.snapshot_id, row.customer_account_id, row.isin)
            aggregates[key]["qty"] += row.quantity
            aggregates[key]["rows"] += 1
            aggregates[key]["date"] = row.business_date

        all_attributions = []
        snapshots = []
        for (snapshot_id, account, isin), value in aggregates.items():
            new_total = value["qty"]
            business_date = value["date"]
            prev_total = self.repo.latest_custody_total(account, isin)
            delta = new_total - prev_total

            snapshot = CustodyPositionSnapshot(
                snapshot_id=snapshot_id,
                business_date=business_date,
                customer_account_id=account,
                isin=isin,
                custody_settled_qty=new_total,
                raw_line_count=value["rows"],
            )
            self.repo.append_snapshot(snapshot)
            snapshots.append(snapshot)
            self.repo.append_event(
                HoldingEvent(
                    id=self.repo.next_id("hev"),
                    customer_account_id=account,
                    isin=isin,
                    place_of_trade=None,
                    event_type=EventType.CUSTODY_CONTROL_TOTAL_RECEIVED,
                    side=None,
                    quantity=new_total,
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
            all_attributions.extend(
                self.attribution.attribute_delta(snapshot_id, business_date, account, isin, delta)
            )

        return {"aggregates": snapshots, "attributions": all_attributions}
