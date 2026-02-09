package com.enterprise.openfinance.uc03.domain.model;

public enum AccountStatus {
    ACTIVE,
    CLOSED,
    DECEASED,
    UNKNOWN;

    public boolean canReceivePayments() {
        return this == ACTIVE;
    }
}
