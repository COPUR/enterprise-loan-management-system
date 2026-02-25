package com.enterprise.openfinance.consentauthorization.domain.model;

import java.time.Instant;

public record AuthorizationRedirect(
        String code,
        String state,
        String redirectUri,
        Instant expiresAt
) {

    public AuthorizationRedirect {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalArgumentException("redirectUri is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
    }
}

