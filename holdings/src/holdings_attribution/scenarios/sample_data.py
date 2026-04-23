from __future__ import annotations

from datetime import date, datetime, timedelta

from holdings_attribution.enums import Side
from holdings_attribution.models import CustodyRowInput, OMSAllocation, SettlementRule


def default_rules() -> list[SettlementRule]:
    return [
        SettlementRule("SG", None, date(2020, 1, 1), None, 2),
        SettlementRule("HK", None, date(2020, 1, 1), None, 2),
    ]


def alloc(allocation_id: str, account: str, isin: str, pot: str, side: Side, qty: int, d: date, seconds: int = 0) -> OMSAllocation:
    return OMSAllocation(
        allocation_id=allocation_id,
        customer_account_id=account,
        isin=isin,
        place_of_trade=pot,
        side=side,
        quantity=qty,
        trade_date=d,
        allocation_timestamp=datetime.combine(d, datetime.min.time()) + timedelta(seconds=seconds),
    )


def row(snapshot_id: str, business_date: date, account: str, isin: str, qty: int, line: str) -> CustodyRowInput:
    return CustodyRowInput(snapshot_id, business_date, account, isin, qty, line)
