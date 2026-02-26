package com.enterprise.openfinance.fxservices.domain.model;

public record FxQuoteResult(
        FxQuote quote
) {

    public FxQuoteResult {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
    }
}
