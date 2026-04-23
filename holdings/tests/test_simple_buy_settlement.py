from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_simple_buy_then_settlement():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))

    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])
    bucket = query.by_account_isin("ACC", "X")[0]

    assert bucket["place_of_trade"] == "SG"
    assert bucket["settled_qty"] == 100
    assert bucket["pending_buy_qty"] == 0
    assert bucket["projected_qty"] == 100


def test_t2_expected_settlement_modeled():
    _, alloc_service, _, _, _, rule_service = build_services()
    event = alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 10, date(2026, 1, 1), 1))
    assert event.expected_settlement_date == date(2026, 1, 3)
    assert rule_service.cycle_days_for("SG", date(2026, 1, 1)) == 2
