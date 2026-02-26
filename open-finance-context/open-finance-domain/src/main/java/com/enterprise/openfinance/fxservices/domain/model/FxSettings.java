package com.enterprise.openfinance.fxservices.domain.model;

import java.time.Duration;

public record FxSettings(
        Duration quoteTtl,
        Duration idempotencyTtl,
        Duration cacheTtl,
        int rateScale
) {

    public FxSettings {
        if (quoteTtl == null || quoteTtl.isZero() || quoteTtl.isNegative()) {
            throw new IllegalArgumentException("quoteTtl must be positive");
        }
        if (idempotencyTtl == null || idempotencyTtl.isZero() || idempotencyTtl.isNegative()) {
            throw new IllegalArgumentException("idempotencyTtl must be positive");
        }
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
        if (rateScale <= 0) {
            throw new IllegalArgumentException("rateScale must be positive");
        }
    }
}
