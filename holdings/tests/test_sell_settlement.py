from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_sell_settlement_reduces_settled_and_pending_sell():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("B1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])

    alloc_service.ingest(alloc("S1", "ACC", "X", "SG", Side.SELL, 40, date(2026, 1, 4), 1))
    custody.ingest([row("S2", date(2026, 1, 6), "ACC", "X", 60, "L1")])

    bucket = query.by_account_isin("ACC", "X")[0]
    assert bucket["settled_qty"] == 60
    assert bucket["pending_sell_qty"] == 0
