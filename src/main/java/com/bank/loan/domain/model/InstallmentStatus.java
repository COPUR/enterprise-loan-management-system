package com.bank.loan.domain.model;

/**
 * Enumeration representing the status of a loan installment
 */
public enum InstallmentStatus {
    PENDING("Pending", false),
    DUE("Due", false),
    OVERDUE("Overdue", false),
    PAID("Paid", true),
    PARTIALLY_PAID("Partially Paid", false),
    WAIVED("Waived", true),
    RESTRUCTURED("Restructured", false),
    WRITTEN_OFF("Written Off", true);
    
    private final String displayName;
    private final boolean settled;
    
    InstallmentStatus(String displayName, boolean settled) {
        this.displayName = displayName;
        this.settled = settled;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSettled() {
        return settled;
    }
    
    public boolean isPaid() {
        return this == PAID;
    }
    
    public boolean requiresPayment() {
        return !settled && this != RESTRUCTURED;
    }
    
    public boolean isOverdue() {
        return this == OVERDUE;
    }
    
    public boolean canAcceptPayment() {
        return this == PENDING || this == DUE || this == OVERDUE || this == PARTIALLY_PAID;
    }
}