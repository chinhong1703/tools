from __future__ import annotations

from datetime import date, timedelta

from holdings_attribution.models import SettlementRule


class SettlementRuleService:
    """Resolves settlement cycle and computes expected settlement dates."""

    def __init__(self, rules: list[SettlementRule]) -> None:
        self.rules = rules

    def cycle_days_for(self, place_of_trade: str, on_date: date) -> int:
        for rule in self.rules:
            if rule.place_of_trade != place_of_trade:
                continue
            if on_date < rule.effective_from:
                continue
            if rule.effective_to and on_date > rule.effective_to:
                continue
            return rule.settlement_cycle_days
        return 2

    def expected_settlement_date(self, place_of_trade: str, trade_date: date) -> date:
        return trade_date + timedelta(days=self.cycle_days_for(place_of_trade, trade_date))
