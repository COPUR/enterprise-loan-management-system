package com.enterprise.openfinance.payeeverification.domain.model;

import java.time.Duration;

public record ConfirmationSettings(
        int closeMatchThreshold,
        Duration cacheTtl
) {
    public ConfirmationSettings {
        if (closeMatchThreshold < 1 || closeMatchThreshold > 99) {
            throw new IllegalArgumentException("closeMatchThreshold must be between 1 and 99");
        }
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
    }
}
