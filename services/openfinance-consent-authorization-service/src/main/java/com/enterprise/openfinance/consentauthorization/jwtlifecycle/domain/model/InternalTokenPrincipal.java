package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import java.time.Instant;

public record InternalTokenPrincipal(
        String subject,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
    public InternalTokenPrincipal {
        if (isBlank(subject)) {
            throw new IllegalArgumentException("subject is required");
        }
        if (isBlank(jti)) {
            throw new IllegalArgumentException("jti is required");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("issuedAt and expiresAt are required");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

