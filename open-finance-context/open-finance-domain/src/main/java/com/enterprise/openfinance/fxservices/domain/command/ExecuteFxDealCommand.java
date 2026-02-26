package com.enterprise.openfinance.fxservices.domain.command;

public record ExecuteFxDealCommand(
        String tppId,
        String quoteId,
        String idempotencyKey,
        String interactionId
) {

    public ExecuteFxDealCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        tppId = tppId.trim();
        quoteId = quoteId.trim();
        idempotencyKey = idempotencyKey.trim();
        interactionId = interactionId.trim();
    }

    public String requestFingerprint() {
        return quoteId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
