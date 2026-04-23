from __future__ import annotations

from holdings_attribution.enums import ExceptionType
from holdings_attribution.models import ReconciliationException
from holdings_attribution.repositories.in_memory import InMemoryRepository


class ReconciliationService:
    def __init__(self, repo: InMemoryRepository) -> None:
        self.repo = repo

    def reconcile(self, business_date, customer_account_id: str, isin: str) -> ReconciliationException | None:
        custody_total = self.repo.latest_custody_total(customer_account_id, isin)
        internal_total = sum(
            b.settled_qty for b in self.repo.list_buckets(customer_account_id, isin)
        )
        diff = internal_total - custody_total
        if diff == 0:
            return None

        exc = ReconciliationException(
            id=self.repo.next_id("rex"),
            business_date=business_date,
            customer_account_id=customer_account_id,
            isin=isin,
            exception_type=ExceptionType.RECON_MISMATCH,
            internal_settled_qty=internal_total,
            custody_settled_qty=custody_total,
            difference_qty=diff,
            status="OPEN",
            details="Internal sum of place-of-trade settled buckets differs from custody control total.",
        )
        self.repo.append_recon_exception(exc)
        return exc
