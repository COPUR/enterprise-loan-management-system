package com.enterprise.openfinance.uc14.domain.model;

import java.time.Duration;

public record ProductDataSettings(Duration cacheTtl) {

    public ProductDataSettings {
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
    }
}
