package com.enterprise.openfinance.consent.domain.model;

public enum ConsentStatus {
    PENDING,
    AUTHORIZED,
    REVOKED,
    EXPIRED;

    public boolean allowsDataAccess() {
        return this == AUTHORIZED;
    }
}
