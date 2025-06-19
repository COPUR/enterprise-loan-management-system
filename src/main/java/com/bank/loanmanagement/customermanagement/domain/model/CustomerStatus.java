package com.bank.loanmanagement.customermanagement.domain.model;

/**
 * Enumeration representing the possible states of a customer account.
 */
public enum CustomerStatus {
    PENDING("Account is pending activation"),
    ACTIVE("Account is active and operational"),
    SUSPENDED("Account is temporarily suspended"),
    CLOSED("Account is permanently closed"),
    BLOCKED("Account is blocked due to security concerns");
    
    private final String description;
    
    CustomerStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean canPerformTransactions() {
        return this == ACTIVE;
    }
    
    public boolean canBeActivated() {
        return this == PENDING;
    }
    
    public boolean canBeSuspended() {
        return this == ACTIVE;
    }
}