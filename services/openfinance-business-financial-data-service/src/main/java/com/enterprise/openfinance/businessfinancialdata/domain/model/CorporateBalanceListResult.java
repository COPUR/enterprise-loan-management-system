package com.enterprise.openfinance.businessfinancialdata.domain.model;

import java.util.List;

public record CorporateBalanceListResult(
        List<CorporateBalanceSnapshot> balances,
        boolean cacheHit,
        boolean masked
) {

    public CorporateBalanceListResult {
        if (balances == null) {
            throw new IllegalArgumentException("balances is required");
        }
        balances = List.copyOf(balances);
    }

    public CorporateBalanceListResult withCacheHit(boolean value) {
        return new CorporateBalanceListResult(balances, value, masked);
    }
}
