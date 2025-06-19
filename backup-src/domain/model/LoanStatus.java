
package com.bank.loanmanagement.domain.model;

/**
 * Enum representing the various states a loan can be in throughout its lifecycle.
 */
public enum LoanStatus {
    PENDING("Pending approval"),
    APPROVED("Approved - awaiting disbursement"),
    ACTIVE("Active - disbursed and being repaid"),
    PAID_OFF("Paid off - loan completed"),
    REJECTED("Rejected - loan application denied"),
    DEFAULTED("Defaulted - payment obligations not met");
    
    private final String description;
    
    LoanStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean canAcceptPayments() {
        return this == ACTIVE;
    }
    
    public boolean isFinalState() {
        return this == PAID_OFF || this == REJECTED || this == DEFAULTED;
    }
    
    public boolean canBeModified() {
        return this == PENDING;
    }
}
