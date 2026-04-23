from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.scenarios.sample_data import alloc, row

from conftest import build_services


def test_reconciliation_exception_created_for_mismatch():
    repo, alloc_service, custody, _, recon, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 100, date(2026, 1, 1), 1))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])

    # Force mismatch to validate control.
    bucket = repo.get_bucket("ACC", "X", "SG")
    bucket.settled_qty = 90
    repo.upsert_bucket(bucket)

    exc = recon.reconcile(date(2026, 1, 3), "ACC", "X")
    assert exc is not None
    assert exc.difference_qty == -10


def test_multiple_snapshots_time_series_deltas():
    _, alloc_service, custody, query, _, _ = build_services()
    alloc_service.ingest(alloc("A1", "ACC", "X", "SG", Side.BUY, 200, date(2026, 1, 1), 1))
    alloc_service.ingest(alloc("A2", "ACC", "X", "SG", Side.SELL, 60, date(2026, 1, 2), 2))
    custody.ingest([row("S1", date(2026, 1, 3), "ACC", "X", 100, "L1")])  # +100
    custody.ingest([row("S2", date(2026, 1, 4), "ACC", "X", 140, "L1")])  # +40
    custody.ingest([row("S3", date(2026, 1, 5), "ACC", "X", 90, "L1")])   # -50

    bucket = query.by_account_isin("ACC", "X")[0]
    assert bucket["settled_qty"] == 90
    assert bucket["pending_buy_qty"] == 60
    assert bucket["pending_sell_qty"] == 10
