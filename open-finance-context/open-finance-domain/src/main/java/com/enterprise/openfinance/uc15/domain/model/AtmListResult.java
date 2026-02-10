package com.enterprise.openfinance.uc15.domain.model;

import java.util.List;

public record AtmListResult(
        List<AtmLocation> atms,
        boolean cacheHit
) {

    public AtmListResult {
        if (atms == null) {
            throw new IllegalArgumentException("atms is required");
        }
        atms = List.copyOf(atms);
    }

    public AtmListResult withCacheHit(boolean cacheHitValue) {
        return new AtmListResult(atms, cacheHitValue);
    }
}
