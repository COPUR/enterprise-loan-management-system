package com.bank.loan.domain.model;

/**
 * Enumeration representing the status of a loan application
 */
public enum ApplicationStatus {
    PENDING("Pending", false),
    UNDER_REVIEW("Under Review", false),
    PENDING_DOCUMENTS("Pending Documents", false),
    APPROVED("Approved", true),
    REJECTED("Rejected", true),
    CANCELLED("Cancelled", true),
    EXPIRED("Expired", true),
    WITHDRAWN("Withdrawn", true);
    
    private final String displayName;
    private final boolean terminal;
    
    ApplicationStatus(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isTerminal() {
        return terminal;
    }
    
    public boolean isActive() {
        return !terminal;
    }
    
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    public boolean canBeModified() {
        return !terminal;
    }
    
    public boolean requiresUnderwriterAction() {
        return this == UNDER_REVIEW || this == PENDING_DOCUMENTS;
    }
}