package com.enterprise.openfinance.paymentinitiation.domain.model;

import java.time.Instant;

public record IdempotencyRecord(
        String idempotencyKey,
        String tppId,
        String requestHash,
        PaymentResult result,
        Instant createdAt,
        Instant expiresAt
) {
    public IdempotencyRecord {
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(requestHash)) {
            throw new IllegalArgumentException("requestHash is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
        if (createdAt == null || expiresAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
