package com.enterprise.openfinance.uc10.domain.model;

import java.time.Instant;

public record MotorQuoteIdempotencyRecord(
        String idempotencyKey,
        String tppId,
        String requestHash,
        String quoteId,
        String policyId,
        Instant expiresAt
) {

    public MotorQuoteIdempotencyRecord {
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(requestHash)) {
            throw new IllegalArgumentException("requestHash is required");
        }
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }

        idempotencyKey = idempotencyKey.trim();
        tppId = tppId.trim();
        requestHash = requestHash.trim();
        quoteId = quoteId.trim();
        policyId = isBlank(policyId) ? null : policyId.trim();
    }

    public boolean isActiveAt(Instant now) {
        return expiresAt.isAfter(now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
