package com.bank.loanmanagement.loan.domain.application;

/**
 * Application priority enumeration
 */
public enum ApplicationPriority {
    LOW("Low"),
    STANDARD("Standard"),
    HIGH("High"),
    URGENT("Urgent");
    
    private final String displayName;
    
    ApplicationPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}