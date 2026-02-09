package com.enterprise.openfinance.uc02.domain.model;

import java.util.List;

public record AccountListResult(
        List<AccountSnapshot> accounts,
        boolean cacheHit
) {

    public AccountListResult {
        if (accounts == null) {
            throw new IllegalArgumentException("accounts is required");
        }
        accounts = List.copyOf(accounts);
    }
}
