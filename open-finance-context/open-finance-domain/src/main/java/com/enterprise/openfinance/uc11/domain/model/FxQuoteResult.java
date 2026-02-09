package com.enterprise.openfinance.uc11.domain.model;

public record FxQuoteResult(
        FxQuote quote
) {

    public FxQuoteResult {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
    }
}
