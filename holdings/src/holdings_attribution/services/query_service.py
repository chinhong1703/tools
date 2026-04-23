from __future__ import annotations

from holdings_attribution.repositories.in_memory import InMemoryRepository


class HoldingsQueryService:
    def __init__(self, repo: InMemoryRepository) -> None:
        self.repo = repo

    def by_account_isin(self, customer_account_id: str, isin: str) -> list[dict]:
        buckets = sorted(
            self.repo.list_buckets(customer_account_id, isin),
            key=lambda b: b.place_of_trade,
        )
        return [
            {
                "customer_account_id": b.customer_account_id,
                "isin": b.isin,
                "place_of_trade": b.place_of_trade,
                "settled_qty": b.settled_qty,
                "pending_buy_qty": b.pending_buy_qty,
                "pending_sell_qty": b.pending_sell_qty,
                "projected_qty": b.projected_qty,
            }
            for b in buckets
        ]
