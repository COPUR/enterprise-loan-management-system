package com.bank.loan.domain.model;

/**
 * Enumeration representing the status of a payment in its lifecycle
 */
public enum PaymentStatus {
    PENDING("Pending", false, false),
    PROCESSING("Processing", false, false),
    PROCESSED("Processed", true, false),
    FAILED("Failed", false, true),
    CANCELLED("Cancelled", false, true),
    REVERSED("Reversed", false, true),
    REFUNDED("Refunded", false, true),
    EXPIRED("Expired", false, true);
    
    private final String displayName;
    private final boolean successful;
    private final boolean terminal;
    
    PaymentStatus(String displayName, boolean successful, boolean terminal) {
        this.displayName = displayName;
        this.successful = successful;
        this.terminal = terminal;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public boolean isTerminal() {
        return terminal;
    }
    
    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean canBeModified() {
        return !terminal && !successful;
    }
    
    public boolean canBeReversed() {
        return this == PROCESSED;
    }
}