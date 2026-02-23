package com.enterprise.openfinance.consentauthorization.domain.model;

public record TokenResult(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        String scope
) {

    public TokenResult {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("tokenType is required");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("expiresIn must be > 0");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope is required");
        }
    }
}

