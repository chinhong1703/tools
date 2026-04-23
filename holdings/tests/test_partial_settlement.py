from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_partial_settlement_leaves_pending_balance():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 60, "L1")])

    bucket = query.by_account_isin("ACC", "X")[0]
    assert bucket["settled_qty"] == 60
    assert bucket["pending_buy_qty"] == 40
    assert bucket["projected_qty"] == 100
