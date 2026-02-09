package com.enterprise.openfinance.uc02.domain.model;

import java.util.List;

public record BalanceListResult(
        List<BalanceSnapshot> balances,
        boolean cacheHit
) {

    public BalanceListResult {
        if (balances == null) {
            throw new IllegalArgumentException("balances is required");
        }
        balances = List.copyOf(balances);
    }
}
