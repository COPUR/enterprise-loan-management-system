package com.enterprise.openfinance.uc11.domain.model;

public record FxDealResult(
        FxDeal deal,
        boolean idempotencyReplay
) {

    public FxDealResult {
        if (deal == null) {
            throw new IllegalArgumentException("deal is required");
        }
    }
}
