package com.enterprise.openfinance.fxservices.domain.model;

public record FxQuoteItemResult(
        FxQuote quote,
        boolean cacheHit
) {

    public FxQuoteItemResult {
        if (quote == null) {
            throw new IllegalArgumentException("quote is required");
        }
    }

    public FxQuoteItemResult withCacheHit(boolean nextCacheHit) {
        return new FxQuoteItemResult(quote, nextCacheHit);
    }
}
