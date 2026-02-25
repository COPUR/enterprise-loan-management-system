package com.loanmanagement.loan.domain.model;

/**
 * Payment Status Enumeration
 * Defines the various states a payment can be in
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but not yet processed
     */
    PENDING("Pending", "Payment initiated, awaiting processing"),
    
    /**
     * Payment is currently being processed
     */
    PROCESSING("Processing", "Payment is being processed"),
    
    /**
     * Payment has been successfully completed
     */
    COMPLETED("Completed", "Payment successfully processed"),
    
    /**
     * Payment failed to process
     */
    FAILED("Failed", "Payment processing failed"),
    
    /**
     * Payment was cancelled before processing
     */
    CANCELLED("Cancelled", "Payment was cancelled"),
    
    /**
     * Payment was reversed after completion
     */
    REVERSED("Reversed", "Payment was reversed"),
    
    /**
     * Payment is on hold for review
     */
    HELD("Held", "Payment held for review"),
    
    /**
     * Payment was declined by bank/processor
     */
    DECLINED("Declined", "Payment declined by processor"),
    
    /**
     * Payment has been fully paid
     */
    PAID("Paid", "Payment has been fully paid"),
    
    /**
     * Payment has been partially paid
     */
    PARTIAL("Partial", "Payment has been partially paid"),
    
    /**
     * Payment is in default
     */
    DEFAULTED("Defaulted", "Payment has defaulted");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
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
     * Check if payment is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REVERSED || this == DECLINED;
    }

    /**
     * Check if payment was successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Check if payment failed
     */
    public boolean isFailed() {
        return this == FAILED || this == DECLINED;
    }

    /**
     * Check if payment can be cancelled
     */
    public boolean canBeCancelled() {
        return this == PENDING || this == HELD;
    }

    /**
     * Check if payment can be reversed
     */
    public boolean canBeReversed() {
        return this == COMPLETED;
    }

    /**
     * Check if payment is still in progress
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING || this == HELD;
    }

    @Override
    public String toString() {
        return displayName;
    }
}