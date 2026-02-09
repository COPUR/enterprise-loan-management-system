package com.enterprise.openfinance.uc10.domain.model;

public record MotorQuoteItemResult(
        MotorInsuranceQuote quote,
        boolean cacheHit
) {

    public MotorQuoteItemResult {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
    }

    public MotorQuoteItemResult withCacheHit(boolean cacheHitValue) {
        return new MotorQuoteItemResult(quote, cacheHitValue);
    }
}
