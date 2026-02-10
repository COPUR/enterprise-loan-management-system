package com.enterprise.openfinance.uc12.domain.model;

import java.time.Instant;

public record OnboardingIdempotencyRecord(
        String idempotencyKey,
        String tppId,
        String requestHash,
        String accountId,
        Instant expiresAt
) {

    public OnboardingIdempotencyRecord {
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(requestHash)) {
            throw new IllegalArgumentException("requestHash is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }

        idempotencyKey = idempotencyKey.trim();
        tppId = tppId.trim();
        requestHash = requestHash.trim();
        accountId = accountId.trim();
    }

    public boolean isActiveAt(Instant now) {
        return expiresAt.isAfter(now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
