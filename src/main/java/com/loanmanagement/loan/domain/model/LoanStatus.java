package com.loanmanagement.loan.domain.model;

/**
 * Loan Status Enumeration
 * Defines the various states a loan can be in during its lifecycle
 */
public enum LoanStatus {
    /**
     * Initial state when loan application is submitted
     */
    PENDING("Pending Review", "Application is under review"),
    
    /**
     * Loan has been approved but not yet disbursed
     */
    APPROVED("Approved", "Loan approved, awaiting disbursement"),
    
    /**
     * Loan application has been rejected
     */
    REJECTED("Rejected", "Loan application rejected"),
    
    /**
     * Loan has been disbursed and is actively being paid
     */
    ACTIVE("Active", "Loan is active and payments are being made"),
    
    /**
     * Loan terms have been modified/restructured
     */
    RESTRUCTURED("Restructured", "Loan terms have been modified"),
    
    /**
     * Loan is in default due to missed payments
     */
    DEFAULTED("Defaulted", "Loan is in default"),
    
    /**
     * Loan has been fully paid off
     */
    PAID_OFF("Paid Off", "Loan has been fully paid"),
    
    /**
     * Loan has been written off as uncollectible
     */
    WRITTEN_OFF("Written Off", "Loan written off as uncollectible"),
    
    /**
     * Loan has been transferred to collections
     */
    IN_COLLECTIONS("In Collections", "Loan transferred to collections");

    private final String displayName;
    private final String description;

    LoanStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this status represents an active loan
     */
    public boolean isActive() {
        return this == ACTIVE || this == RESTRUCTURED;
    }

    /**
     * Check if this status represents a closed loan
     */
    public boolean isClosed() {
        return this == PAID_OFF || this == WRITTEN_OFF;
    }

    /**
     * Check if this status represents a problematic loan
     */
    public boolean isProblematic() {
        return this == DEFAULTED || this == IN_COLLECTIONS || this == WRITTEN_OFF;
    }

    /**
     * Check if loan can be disbursed from this status
     */
    public boolean canDisburse() {
        return this == APPROVED;
    }

    /**
     * Check if payments can be made on loan in this status
     */
    public boolean canMakePayments() {
        return this == ACTIVE || this == RESTRUCTURED || this == DEFAULTED;
    }

    /**
     * Check if loan can be restructured from this status
     */
    public boolean canRestructure() {
        return this == ACTIVE || this == DEFAULTED;
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    // Legacy methods for backward compatibility
    public boolean isTerminal() {
        return this == PAID_OFF || this == REJECTED || this == DEFAULTED || this == WRITTEN_OFF;
    }
    
    public boolean allowsPayments() {
        return canMakePayments();
    }
}