package com.enterprise.openfinance.fxservices.domain.query;

public record GetFxQuoteQuery(
        String quoteId,
        String tppId,
        String interactionId
) {

    public GetFxQuoteQuery {
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        quoteId = quoteId.trim();
        tppId = tppId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
