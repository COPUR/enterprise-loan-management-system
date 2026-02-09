package com.enterprise.openfinance.uc01.domain.model;

public enum ConsentStatus {
    PENDING,
    AUTHORIZED,
    REVOKED,
    EXPIRED;

    public boolean allowsDataAccess() {
        return this == AUTHORIZED;
    }
}
