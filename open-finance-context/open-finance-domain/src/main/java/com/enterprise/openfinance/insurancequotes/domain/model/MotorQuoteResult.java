package com.enterprise.openfinance.insurancequotes.domain.model;

public record MotorQuoteResult(
        MotorInsuranceQuote quote,
        boolean idempotencyReplay
) {

    public MotorQuoteResult {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
    }
}
