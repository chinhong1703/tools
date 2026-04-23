from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_fifo_across_sg_then_hk():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 50, date(2026, 1, 1), 1))
    alloc_service.ingest(alloc("A2", "ACC", "X", "HK", Side.BUY, 70, date(2026, 1, 1), 2))

    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 80, "L1")])
    by_place = {x["place_of_trade"]: x for x in query.by_account_isin("ACC", "X")}

    assert by_place["SG"]["settled_qty"] == 50
    assert by_place["SG"]["pending_buy_qty"] == 0
    assert by_place["HK"]["settled_qty"] == 30
    assert by_place["HK"]["pending_buy_qty"] == 40


def test_mixed_buy_sell_deltas_hit_correct_side_only():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("B1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])
    alloc_service.ingest(alloc("B2", "ACC", "X", "SG", Side.BUY, 50, date(2026, 1, 4), 2))
    alloc_service.ingest(alloc("B3", "ACC", "X", "HK", Side.SELL, 30, date(2026, 1, 4), 3))

    custody.ingest([row("S2", date(2026, 1, 6), "ACC", "X", 120, "L1")])  # +20
    b = {x["place_of_trade"]: x for x in query.by_account_isin("ACC", "X")}
    assert b["SG"]["pending_buy_qty"] == 30
    assert b["HK"]["pending_sell_qty"] == 30

    custody.ingest([row("S3", date(2026, 1, 7), "ACC", "X", 100, "L1")])  # -20
    b = {x["place_of_trade"]: x for x in query.by_account_isin("ACC", "X")}
    assert b["SG"]["pending_buy_qty"] == 30
    assert b["HK"]["pending_sell_qty"] == 10
