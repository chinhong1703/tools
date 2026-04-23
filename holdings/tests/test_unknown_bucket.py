from datetime import date

from holdings_attribution.enums import Side, UNKNOWN_PLACE_OF_TRADE
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_opening_legacy_holdings_land_in_unknown():
    _, _, custody, query, _, _ = build_services()
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 500, "L1")])

    rows = query.by_account_isin("ACC", "X")
    assert rows[0]["place_of_trade"] == UNKNOWN_PLACE_OF_TRADE
    assert rows[0]["settled_qty"] == 500


def test_external_movement_without_pending_goes_unknown():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])
    custody.ingest([row("S2", date(2026, 1, 4), "ACC", "X", 150, "L1")])

    by_place = {x["place_of_trade"]: x for x in query.by_account_isin("ACC", "X")}
    assert by_place[UNKNOWN_PLACE_OF_TRADE]["settled_qty"] == 50
