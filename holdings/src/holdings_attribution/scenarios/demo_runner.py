from __future__ import annotations

from datetime import date

from holdings_attribution.enums import Side
from holdings_attribution.repositories.in_memory import InMemoryRepository
from holdings_attribution.scenarios.sample_data import alloc, default_rules, row
from holdings_attribution.services.allocation_ingestion import AllocationIngestionService
from holdings_attribution.services.custody_ingestion import CustodySnapshotIngestionService
from holdings_attribution.services.query_service import HoldingsQueryService
from holdings_attribution.services.reconciliation import ReconciliationService
from holdings_attribution.services.settlement_attribution import SettlementAttributionService
from holdings_attribution.services.settlement_rules import SettlementRuleService
from holdings_attribution.utils.formatting import print_table


def run_demo() -> None:
    repo = InMemoryRepository()
    rule_service = SettlementRuleService(default_rules())
    alloc_service = AllocationIngestionService(repo, rule_service)
    attribution_service = SettlementAttributionService(repo)
    custody_service = CustodySnapshotIngestionService(repo, attribution_service)
    query_service = HoldingsQueryService(repo)
    recon_service = ReconciliationService(repo)

    account = "A1"
    isin = "ISIN-X"
    print("Scenario: mixed SG/HK buys, partial settlement, sell settlement, unknown handling")

    alloc_service.ingest(alloc("AL1", account, isin, "SG", Side.BUY, 100, date(2026, 4, 1), 1))
    alloc_service.ingest(alloc("AL2", account, isin, "HK", Side.BUY, 200, date(2026, 4, 1), 2))
    print_table("Holdings after allocations", query_service.by_account_isin(account, isin))

    result = custody_service.ingest([
        row("S1", date(2026, 4, 3), account, isin, 50, "L1"),
        row("S1", date(2026, 4, 3), account, isin, 100, "L2"),
    ])
    print_table("Attributions after custody snapshot S1", [a.__dict__ for a in result["attributions"]])
    print_table("Holdings after S1", query_service.by_account_isin(account, isin))

    alloc_service.ingest(alloc("AL3", account, isin, "SG", Side.SELL, 40, date(2026, 4, 4), 3))
    result = custody_service.ingest([row("S2", date(2026, 4, 6), account, isin, 110, "L1")])
    print_table("Attributions after custody snapshot S2", [a.__dict__ for a in result["attributions"]])
    print_table("Holdings after S2", query_service.by_account_isin(account, isin))

    # External movement unmatched to pending queue -> UNKNOWN
    result = custody_service.ingest([row("S3", date(2026, 4, 7), account, isin, 300, "L1")])
    print_table("Attributions after custody snapshot S3", [a.__dict__ for a in result["attributions"]])
    print_table("Holdings after S3", query_service.by_account_isin(account, isin))

    exc = recon_service.reconcile(date(2026, 4, 7), account, isin)
    if exc:
        print("Reconciliation exception:", exc)
    else:
        print("Reconciliation status: matched")


if __name__ == "__main__":
    run_demo()
