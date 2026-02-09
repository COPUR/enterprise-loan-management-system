package com.enterprise.openfinance.uc11.domain.model;

import java.time.Instant;

public record FxIdempotencyRecord(
        String idempotencyKey,
        String tppId,
        String requestHash,
        String dealId,
        Instant expiresAt
) {

    public FxIdempotencyRecord {
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(requestHash)) {
            throw new IllegalArgumentException("requestHash is required");
        }
        if (isBlank(dealId)) {
            throw new IllegalArgumentException("dealId is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }

        idempotencyKey = idempotencyKey.trim();
        tppId = tppId.trim();
        requestHash = requestHash.trim();
        dealId = dealId.trim();
    }

    public boolean isActiveAt(Instant now) {
        return expiresAt.isAfter(now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
