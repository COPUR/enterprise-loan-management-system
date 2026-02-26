package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import java.time.Instant;

public record InternalTokenIssueResult(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
    public InternalTokenIssueResult {
        if (isBlank(accessToken)) {
            throw new IllegalArgumentException("accessToken is required");
        }
        if (isBlank(tokenType)) {
            throw new IllegalArgumentException("tokenType is required");
        }
        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException("expiresInSeconds must be greater than zero");
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

