from holdings_attribution.repositories.in_memory import InMemoryRepository
from holdings_attribution.scenarios.sample_data import default_rules
from holdings_attribution.services.allocation_ingestion import AllocationIngestionService
from holdings_attribution.services.custody_ingestion import CustodySnapshotIngestionService
from holdings_attribution.services.query_service import HoldingsQueryService
from holdings_attribution.services.reconciliation import ReconciliationService
from holdings_attribution.services.settlement_attribution import SettlementAttributionService
from holdings_attribution.services.settlement_rules import SettlementRuleService


def build_services():
    repo = InMemoryRepository()
    rule_service = SettlementRuleService(default_rules())
    alloc_service = AllocationIngestionService(repo, rule_service)
    attribution = SettlementAttributionService(repo)
    custody = CustodySnapshotIngestionService(repo, attribution)
    query = HoldingsQueryService(repo)
    recon = ReconciliationService(repo)
    return repo, alloc_service, custody, query, recon, rule_service
