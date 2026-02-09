package com.enterprise.openfinance.uc06.domain.model;

import java.time.Duration;

public record PaymentSettings(Duration idempotencyTtl) {
    public PaymentSettings {
        if (idempotencyTtl == null || idempotencyTtl.isNegative() || idempotencyTtl.isZero()) {
            throw new IllegalArgumentException("idempotencyTtl must be positive");
        }
    }
}
