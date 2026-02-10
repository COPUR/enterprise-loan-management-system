package com.enterprise.openfinance.uc15.domain.model;

import java.time.Duration;

public record AtmDataSettings(Duration cacheTtl) {

    public AtmDataSettings {
        if (cacheTtl == null || cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
    }
}
