package com.enterprise.openfinance.consentauthorization.domain.model;

public enum ConsentStatus {
    PENDING,
    AUTHORIZED,
    REVOKED,
    EXPIRED;

    public boolean allowsDataAccess() {
        return this == AUTHORIZED;
    }
}
