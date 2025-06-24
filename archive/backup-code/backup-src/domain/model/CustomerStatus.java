
package com.bank.loanmanagement.domain.model;

/**
 * Enum representing the various states a customer can be in.
 * Used to control business operations and access rights.
 */
public enum CustomerStatus {
    ACTIVE("Active - Full access to services"),
    INACTIVE("Inactive - Limited access"),
    SUSPENDED("Suspended - No access due to policy violation"),
    PENDING_VERIFICATION("Pending verification - Account setup in progress"),
    CLOSED("Closed - Account permanently closed");
    
    private final String description;
    
    CustomerStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canTakeLoan() {
        return this == ACTIVE;
    }
    
    public boolean canMakePayment() {
        return this == ACTIVE || this == INACTIVE;
    }
    
    public boolean isOperational() {
        return this == ACTIVE || this == INACTIVE;
    }
}
