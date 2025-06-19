package com.bank.loanmanagement.customermanagement.infrastructure.adapter.out.persistence;

/**
 * JPA enumeration for customer status.
 * Mirrors domain CustomerStatus but used only for persistence.
 */
public enum CustomerStatusJpa {
    PENDING,
    ACTIVE,
    SUSPENDED,
    CLOSED,
    BLOCKED
}