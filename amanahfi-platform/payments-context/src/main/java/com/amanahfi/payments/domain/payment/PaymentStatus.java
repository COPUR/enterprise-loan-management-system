package com.amanahfi.payments.domain.payment;

/**
 * Payment status enumeration
 * Following payment lifecycle from creation to settlement
 */
public enum PaymentStatus {
    /**
     * Payment created but not yet processed
     */
    PENDING,
    
    /**
     * Payment is being processed
     */
    PROCESSING,
    
    /**
     * Payment completed successfully
     */
    COMPLETED,
    
    /**
     * Payment failed
     */
    FAILED,
    
    /**
     * Payment cancelled by user
     */
    CANCELLED,
    
    /**
     * Payment requires manual review
     */
    UNDER_REVIEW
}