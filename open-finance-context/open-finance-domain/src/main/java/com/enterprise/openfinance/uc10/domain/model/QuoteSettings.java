package com.enterprise.openfinance.uc10.domain.model;

import java.math.BigDecimal;
import java.time.Duration;

public record QuoteSettings(
        Duration quoteTtl,
        Duration idempotencyTtl,
        Duration cacheTtl,
        String currency,
        BigDecimal basePremium
) {

    public QuoteSettings {
        if (quoteTtl == null || quoteTtl.isZero() || quoteTtl.isNegative()) {
            throw new IllegalArgumentException("quoteTtl must be positive");
        }
        if (idempotencyTtl == null || idempotencyTtl.isZero() || idempotencyTtl.isNegative()) {
            throw new IllegalArgumentException("idempotencyTtl must be positive");
        }
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (basePremium == null || basePremium.signum() <= 0) {
            throw new IllegalArgumentException("basePremium must be positive");
        }

        currency = currency.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
