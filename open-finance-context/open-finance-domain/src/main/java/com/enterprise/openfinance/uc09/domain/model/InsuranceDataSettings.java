package com.enterprise.openfinance.uc09.domain.model;

import java.time.Duration;

public record InsuranceDataSettings(
        Duration cacheTtl,
        int defaultPageSize,
        int maxPageSize
) {

    public InsuranceDataSettings {
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
        if (defaultPageSize <= 0) {
            throw new IllegalArgumentException("defaultPageSize must be positive");
        }
        if (maxPageSize <= 0 || maxPageSize < defaultPageSize) {
            throw new IllegalArgumentException("maxPageSize must be >= defaultPageSize and positive");
        }
    }
}
