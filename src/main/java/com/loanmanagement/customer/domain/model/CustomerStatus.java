package com.loanmanagement.customer.domain.model;

public enum CustomerStatus {
    ACTIVE("Active customer with full privileges"),
    INACTIVE("Inactive customer - no new loans"),
    SUSPENDED("Suspended customer - under review"),
    BLOCKED("Blocked customer - compliance issues");
    
    private final String description;
    
    CustomerStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean allowsNewLoans() {
        return this == ACTIVE;
    }
}