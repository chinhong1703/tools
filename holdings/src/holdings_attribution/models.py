from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime

from .enums import EventStatus, EventType, ExceptionType, Side


@dataclass
class HoldingBucket:
    customer_account_id: str
    isin: str
    place_of_trade: str
    settled_qty: int = 0
    pending_buy_qty: int = 0
    pending_sell_qty: int = 0
    attributed_source: str = "OMS_ALLOCATIONS_AND_CUSTODY_CONTROL_TOTALS"
    last_updated_at: datetime = field(default_factory=datetime.utcnow)
    version: int = 0

    @property
    def projected_qty(self) -> int:
        return self.settled_qty + self.pending_buy_qty - self.pending_sell_qty


@dataclass
class HoldingEvent:
    id: str
    customer_account_id: str
    isin: str
    place_of_trade: str | None
    event_type: EventType
    side: Side | None
    quantity: int
    trade_date: date | None
    expected_settlement_date: date | None
    actual_settlement_date: date | None
    business_date: date | None
    source_system: str
    source_ref: str
    linked_ref: str | None
    status: EventStatus
    created_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class CustodyPositionRaw:
    id: str
    snapshot_id: str
    business_date: date
    customer_account_id: str
    isin: str
    quantity: int
    raw_line_ref: str
    received_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class CustodyPositionSnapshot:
    snapshot_id: str
    business_date: date
    customer_account_id: str
    isin: str
    custody_settled_qty: int
    raw_line_count: int
    received_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class SettlementAttribution:
    id: str
    customer_account_id: str
    isin: str
    place_of_trade: str
    attributed_qty: int
    attribution_method: str
    custody_snapshot_id: str
    attributed_business_date: date
    source_pending_event_id: str | None
    created_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class ReconciliationException:
    id: str
    business_date: date
    customer_account_id: str
    isin: str
    exception_type: ExceptionType
    internal_settled_qty: int
    custody_settled_qty: int
    difference_qty: int
    status: str
    details: str
    created_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class SettlementRule:
    place_of_trade: str
    instrument_type: str | None
    effective_from: date
    effective_to: date | None
    settlement_cycle_days: int


@dataclass
class OMSAllocation:
    allocation_id: str
    customer_account_id: str
    isin: str
    place_of_trade: str
    side: Side
    quantity: int
    trade_date: date
    allocation_timestamp: datetime


@dataclass
class CustodyRowInput:
    snapshot_id: str
    business_date: date
    customer_account_id: str
    isin: str
    quantity: int
    raw_line_ref: str
