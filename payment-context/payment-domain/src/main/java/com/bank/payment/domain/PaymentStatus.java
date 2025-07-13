package com.bank.payment.domain;

/**
 * Enumeration representing the status of a payment
 */
public enum PaymentStatus {
    PENDING("Pending Processing"),
    PROCESSING("Processing"),
    COMPLETED("Completed Successfully"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded"),
    DISPUTED("Under Dispute"),
    SETTLED("Settled");
    
    private final String displayName;
    
    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isTerminalStatus() {
        return this == COMPLETED || this == FAILED || 
               this == CANCELLED || this == REFUNDED || this == SETTLED;
    }
    
    public boolean canBeConfirmed() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean canBeFailed() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean canBeCancelled() {
        return this == PENDING;
    }
    
    public boolean canBeRefunded() {
        return this == COMPLETED;
    }
}