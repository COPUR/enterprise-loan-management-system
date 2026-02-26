package com.enterprise.openfinance.corporatetreasury.domain.model;

import java.time.Duration;

public record CorporateTreasurySettings(
        Duration cacheTtl,
        int defaultPageSize,
        int maxPageSize
) {

    public CorporateTreasurySettings {
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            throw new IllegalArgumentException("cacheTtl must be positive");
        }
        if (defaultPageSize < 1) {
            throw new IllegalArgumentException("defaultPageSize must be >= 1");
        }
        if (maxPageSize < defaultPageSize) {
            throw new IllegalArgumentException("maxPageSize must be >= defaultPageSize");
        }
    }
}
