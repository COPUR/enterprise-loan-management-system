package com.enterprise.openfinance.fxservices.domain.model;

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
