package com.enterprise.openfinance.uc12.domain.model;

import java.time.Duration;

public record OnboardingSettings(
        Duration idempotencyTtl,
        Duration cacheTtl,
        String accountPrefix
) {

    public OnboardingSettings {
        if (idempotencyTtl == null || idempotencyTtl.isNegative() || idempotencyTtl.isZero()) {
            throw new IllegalArgumentException("idempotencyTtl must be positive");
        }
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
        if (accountPrefix == null || accountPrefix.isBlank()) {
            throw new IllegalArgumentException("accountPrefix is required");
        }

        accountPrefix = accountPrefix.trim().toUpperCase();
    }
}
