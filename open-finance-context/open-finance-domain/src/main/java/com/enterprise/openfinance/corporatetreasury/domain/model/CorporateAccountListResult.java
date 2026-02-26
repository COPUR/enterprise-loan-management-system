package com.enterprise.openfinance.corporatetreasury.domain.model;

import java.util.List;

public record CorporateAccountListResult(
        List<CorporateAccountSnapshot> accounts,
        boolean cacheHit
) {

    public CorporateAccountListResult {
        if (accounts == null) {
            throw new IllegalArgumentException("accounts is required");
        }
        accounts = List.copyOf(accounts);
    }

    public CorporateAccountListResult withCacheHit(boolean value) {
        return new CorporateAccountListResult(accounts, value);
    }
}
