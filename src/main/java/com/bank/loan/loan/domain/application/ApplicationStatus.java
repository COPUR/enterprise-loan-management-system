package com.bank.loanmanagement.loan.domain.application;

/**
 * Application status enumeration
 */
public enum ApplicationStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PENDING_DOCUMENTS("Pending Documents"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}