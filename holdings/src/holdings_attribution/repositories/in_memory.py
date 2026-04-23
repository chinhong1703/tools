from __future__ import annotations

from collections import defaultdict
from dataclasses import asdict
from datetime import date
from itertools import count
from typing import Iterable

from holdings_attribution.models import (
    CustodyPositionRaw,
    CustodyPositionSnapshot,
    HoldingBucket,
    HoldingEvent,
    ReconciliationException,
    SettlementAttribution,
)


class InMemoryRepository:
    def __init__(self) -> None:
        self._id_seq = count(1)
        self.holding_buckets: dict[tuple[str, str, str], HoldingBucket] = {}
        self.holding_events: list[HoldingEvent] = []
        self.custody_raw_rows: list[CustodyPositionRaw] = []
        self.custody_snapshots: list[CustodyPositionSnapshot] = []
        self.attributions: list[SettlementAttribution] = []
        self.reconciliation_exceptions: list[ReconciliationException] = []

    def next_id(self, prefix: str) -> str:
        return f"{prefix}-{next(self._id_seq):05d}"

    def upsert_bucket(self, bucket: HoldingBucket) -> HoldingBucket:
        key = (bucket.customer_account_id, bucket.isin, bucket.place_of_trade)
        existing = self.holding_buckets.get(key)
        bucket.version = (existing.version + 1) if existing else 1
        self.holding_buckets[key] = bucket
        return bucket

    def get_bucket(self, account: str, isin: str, place: str) -> HoldingBucket | None:
        return self.holding_buckets.get((account, isin, place))

    def list_buckets(self, account: str, isin: str) -> list[HoldingBucket]:
        return [
            b
            for (acc, cur_isin, _), b in self.holding_buckets.items()
            if acc == account and cur_isin == isin
        ]

    def append_event(self, event: HoldingEvent) -> None:
        self.holding_events.append(event)

    def append_raw_row(self, row: CustodyPositionRaw) -> None:
        self.custody_raw_rows.append(row)

    def append_snapshot(self, snapshot: CustodyPositionSnapshot) -> None:
        self.custody_snapshots.append(snapshot)

    def append_attribution(self, attribution: SettlementAttribution) -> None:
        self.attributions.append(attribution)

    def append_recon_exception(self, exception: ReconciliationException) -> None:
        self.reconciliation_exceptions.append(exception)

    def latest_custody_total(self, account: str, isin: str) -> int:
        candidates = [
            s.custody_settled_qty
            for s in self.custody_snapshots
            if s.customer_account_id == account and s.isin == isin
        ]
        return candidates[-1] if candidates else 0

    def pending_events(self, account: str, isin: str, side: str) -> list[HoldingEvent]:
        return [
            ev
            for ev in self.holding_events
            if ev.customer_account_id == account
            and ev.isin == isin
            and ev.status.value == "OPEN"
            and ev.side
            and ev.side.value == side
            and ev.event_type.value.startswith("ALLOCATED")
        ]

    def to_dict(self) -> dict:
        return {
            "buckets": [asdict(v) for v in self.holding_buckets.values()],
            "events": [asdict(v) for v in self.holding_events],
            "snapshots": [asdict(v) for v in self.custody_snapshots],
            "attributions": [asdict(v) for v in self.attributions],
            "exceptions": [asdict(v) for v in self.reconciliation_exceptions],
        }
