from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_custody_rows_are_aggregated_not_directly_mapped():
    repo, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    alloc_service.ingest(alloc("A2", "ACC", "X", "HK", Side.BUY, 100, date(2026, 1, 1), 2))

    result = custody.ingest([
        row("S1", date(2026, 1, 3), "ACC", "X", 50, "L1"),
        row("S1", date(2026, 1, 3), "ACC", "X", 100, "L2"),
    ])

    assert result["aggregates"][0].custody_settled_qty == 150
    assert len(repo.custody_raw_rows) == 2
    assert len(result["attributions"]) >= 1
    by_place = {x["place_of_trade"]: x for x in query.by_account_isin("ACC", "X")}
    assert by_place["SG"]["settled_qty"] == 100
    assert by_place["HK"]["settled_qty"] == 50


def test_zero_delta_creates_no_attribution():
    _, alloc_service, custody, _, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])
    result = custody.ingest([row("S2", date(2026, 1, 4), "ACC", "X", 100, "L1")])
    assert result["attributions"] == []
