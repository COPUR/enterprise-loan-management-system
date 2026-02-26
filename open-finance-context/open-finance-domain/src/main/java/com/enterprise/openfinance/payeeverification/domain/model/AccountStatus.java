package com.enterprise.openfinance.payeeverification.domain.model;

public enum AccountStatus {
    ACTIVE,
    CLOSED,
    DECEASED,
    UNKNOWN;

    public boolean canReceivePayments() {
        return this == ACTIVE;
    }
}
