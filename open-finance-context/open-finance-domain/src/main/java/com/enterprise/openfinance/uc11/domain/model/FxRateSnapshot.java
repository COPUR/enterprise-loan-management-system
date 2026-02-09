package com.enterprise.openfinance.uc11.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FxRateSnapshot(
        String pair,
        BigDecimal rate,
        Instant timestamp,
        String provider
) {

    public FxRateSnapshot {
        if (isBlank(pair)) {
            throw new IllegalArgumentException("pair is required");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (isBlank(provider)) {
            throw new IllegalArgumentException("provider is required");
        }

        pair = pair.trim().toUpperCase();
        provider = provider.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
