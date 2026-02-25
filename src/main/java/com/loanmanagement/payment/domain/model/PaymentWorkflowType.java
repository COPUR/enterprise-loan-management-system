package com.loanmanagement.payment.domain.model;

import lombok.Getter;

import java.util.Set;

/**
 * Enum representing different types of payment workflows.
 * Each type has specific processing rules and requirements.
 */
@Getter
public enum PaymentWorkflowType {
    
    STANDARD_PAYMENT(
            "Standard Payment",
            "Regular one-time payment processing",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.APPROVAL),
            300000L), // 5 minutes
    
    RECURRING_PAYMENT(
            "Recurring Payment",
            "Scheduled recurring payment processing",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.SCHEDULING, PaymentWorkflowFeature.NOTIFICATION),
            600000L), // 10 minutes
    
    BATCH_PAYMENT(
            "Batch Payment",
            "Multiple payments processed as a batch",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.PARALLEL_PROCESSING, PaymentWorkflowFeature.PROGRESS_TRACKING),
            1800000L), // 30 minutes
    
    REVERSAL(
            "Payment Reversal",
            "Reversal of a previous payment",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.COMPENSATION, PaymentWorkflowFeature.AUDIT_TRAIL),
            180000L), // 3 minutes
    
    REFUND(
            "Payment Refund",
            "Refund processing for returned payments",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.APPROVAL, PaymentWorkflowFeature.NOTIFICATION),
            300000L), // 5 minutes
    
    INSTANT_PAYMENT(
            "Instant Payment",
            "Real-time payment processing",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.REAL_TIME, PaymentWorkflowFeature.PRIORITY_PROCESSING),
            30000L), // 30 seconds
    
    INTERNATIONAL_PAYMENT(
            "International Payment",
            "Cross-border payment processing",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.COMPLIANCE_CHECK, PaymentWorkflowFeature.FX_CONVERSION),
            900000L), // 15 minutes
    
    BULK_TRANSFER(
            "Bulk Transfer",
            "High-volume bulk payment transfer",
            Set.of(PaymentWorkflowFeature.VALIDATION, PaymentWorkflowFeature.BATCH_OPTIMIZATION, PaymentWorkflowFeature.PROGRESS_TRACKING),
            3600000L); // 1 hour
    
    private final String displayName;
    private final String description;
    private final Set<PaymentWorkflowFeature> features;
    private final long defaultTimeoutMillis;
    
    PaymentWorkflowType(
            String displayName,
            String description,
            Set<PaymentWorkflowFeature> features,
            long defaultTimeoutMillis) {
        
        this.displayName = displayName;
        this.description = description;
        this.features = features;
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }
    
    /**
     * Checks if this workflow type has a specific feature.
     */
    public boolean hasFeature(PaymentWorkflowFeature feature) {
        return features.contains(feature);
    }
    
    /**
     * Checks if this workflow type requires approval.
     */
    public boolean requiresApproval() {
        return hasFeature(PaymentWorkflowFeature.APPROVAL);
    }
    
    /**
     * Checks if this workflow type supports parallel processing.
     */
    public boolean supportsParallelProcessing() {
        return hasFeature(PaymentWorkflowFeature.PARALLEL_PROCESSING);
    }
    
    /**
     * Checks if this workflow type is real-time.
     */
    public boolean isRealTime() {
        return hasFeature(PaymentWorkflowFeature.REAL_TIME);
    }
    
    /**
     * Gets workflow types that support batch processing.
     */
    public static Set<PaymentWorkflowType> getBatchTypes() {
        return Set.of(BATCH_PAYMENT, BULK_TRANSFER);
    }
    
    /**
     * Gets workflow types that require compliance checks.
     */
    public static Set<PaymentWorkflowType> getComplianceRequiredTypes() {
        return Set.of(INTERNATIONAL_PAYMENT, BULK_TRANSFER);
    }
    
    /**
     * Gets the appropriate workflow type for a payment amount and destination.
     */
    public static PaymentWorkflowType determineType(
            java.math.BigDecimal amount,
            boolean isDomestic,
            boolean isUrgent,
            boolean isRecurring) {
        
        if (isRecurring) {
            return RECURRING_PAYMENT;
        }
        
        if (!isDomestic) {
            return INTERNATIONAL_PAYMENT;
        }
        
        if (isUrgent) {
            return INSTANT_PAYMENT;
        }
        
        if (amount.compareTo(new java.math.BigDecimal("1000000")) > 0) {
            return BULK_TRANSFER;
        }
        
        return STANDARD_PAYMENT;
    }
    
    /**
     * Features that a workflow type can have.
     */
    public enum PaymentWorkflowFeature {
        VALIDATION,
        APPROVAL,
        SCHEDULING,
        NOTIFICATION,
        PARALLEL_PROCESSING,
        PROGRESS_TRACKING,
        COMPENSATION,
        AUDIT_TRAIL,
        REAL_TIME,
        PRIORITY_PROCESSING,
        COMPLIANCE_CHECK,
        FX_CONVERSION,
        BATCH_OPTIMIZATION
    }
}