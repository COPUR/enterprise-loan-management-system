package com.loanmanagement.payment.domain.model;

import com.loanmanagement.payment.domain.workflow.PaymentWorkflowRequest;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Request to start a payment reversal workflow.
 * Handles reversing a previously completed payment.
 */
@Value
@Builder
public class PaymentReversalWorkflowRequest {
    
    String reversalId;
    String originalPaymentId;
    String originalTransactionId;
    
    String customerId;
    String accountId;
    
    BigDecimal reversalAmount;
    BigDecimal originalAmount;
    String currency;
    
    ReversalType reversalType;
    ReversalReason reversalReason;
    
    String reasonCode;
    String reasonDescription;
    
    Instant originalPaymentDate;
    Instant reversalRequestedAt;
    
    boolean partialReversal;
    boolean requiresApproval;
    boolean urgentProcessing;
    
    String sourceAccountId;
    String destinationAccountId;
    
    Map<String, Object> reversalContext;
    Map<String, String> metadata;
    
    String requestedBy;
    String approvedBy;
    
    PaymentReversalValidationResult validationResult;
    
    /**
     * Validates the reversal request.
     */
    public void validate() {
        Objects.requireNonNull(originalPaymentId, "Original payment ID is required");
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(reversalAmount, "Reversal amount is required");
        Objects.requireNonNull(originalAmount, "Original amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(reversalType, "Reversal type is required");
        Objects.requireNonNull(reversalReason, "Reversal reason is required");
        Objects.requireNonNull(requestedBy, "Requester is required");
        
        if (reversalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reversal amount must be positive");
        }
        
        if (reversalAmount.compareTo(originalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Reversal amount cannot exceed original amount");
        }
        
        if (partialReversal && reversalAmount.equals(originalAmount)) {
            throw new IllegalArgumentException(
                    "Partial reversal flag set but amounts are equal");
        }
        
        validateReversalWindow();
    }
    
    /**
     * Validates the reversal is within allowed time window.
     */
    private void validateReversalWindow() {
        if (originalPaymentDate == null) {
            return;
        }
        
        long daysSincePayment = java.time.Duration
                .between(originalPaymentDate, Instant.now())
                .toDays();
        
        long maxReversalDays = switch (reversalType) {
            case FULL_REVERSAL -> 180;  // 6 months
            case PARTIAL_REVERSAL -> 90; // 3 months
            case CHARGEBACK -> 120;      // 4 months
            case REFUND -> 365;          // 1 year
            case CORRECTION -> 30;       // 30 days
        };
        
        if (daysSincePayment > maxReversalDays) {
            throw new IllegalStateException(
                    String.format("Reversal window exceeded. Maximum %d days, actual %d days",
                            maxReversalDays, daysSincePayment));
        }
    }
    
    /**
     * Converts to a base payment workflow request.
     */
    public PaymentWorkflowRequest toWorkflowRequest() {
        return PaymentWorkflowRequest.builder()
                .paymentId(reversalId)
                .customerId(customerId)
                .accountId(accountId)
                .workflowType(PaymentWorkflowType.REVERSAL)
                .priority(urgentProcessing ? 
                        PaymentWorkflowPriority.HIGH : PaymentWorkflowPriority.MEDIUM)
                .amount(reversalAmount)
                .currency(currency)
                .sourceAccountId(destinationAccountId) // Reversed direction
                .destinationAccountId(sourceAccountId) // Reversed direction
                .paymentReference(originalPaymentId)
                .requestedAt(Instant.now())
                .requestedBy(requestedBy)
                .workflowContext(buildWorkflowContext())
                .metadata(metadata)
                .requiresApproval(requiresApproval || isHighRiskReversal())
                .requiresValidation(true)
                .maxRetries(5) // More retries for reversals
                .timeoutMillis(180000L) // 3 minutes
                .build();
    }
    
    /**
     * Builds workflow context with reversal-specific data.
     */
    private Map<String, Object> buildWorkflowContext() {
        Map<String, Object> context = new java.util.HashMap<>();
        
        if (reversalContext != null) {
            context.putAll(reversalContext);
        }
        
        context.put("reversalType", reversalType);
        context.put("reversalReason", reversalReason);
        context.put("originalPaymentId", originalPaymentId);
        context.put("originalTransactionId", originalTransactionId);
        context.put("originalAmount", originalAmount);
        context.put("partialReversal", partialReversal);
        context.put("reasonCode", reasonCode);
        context.put("reasonDescription", reasonDescription);
        
        if (originalPaymentDate != null) {
            context.put("originalPaymentDate", originalPaymentDate.toString());
        }
        
        if (approvedBy != null) {
            context.put("approvedBy", approvedBy);
        }
        
        if (validationResult != null) {
            context.put("validationResult", validationResult);
        }
        
        return context;
    }
    
    /**
     * Checks if this is a high-risk reversal.
     */
    private boolean isHighRiskReversal() {
        // High amount reversals
        if (reversalAmount.compareTo(new BigDecimal("10000")) > 0) {
            return true;
        }
        
        // Old payments
        if (originalPaymentDate != null) {
            long daysSincePayment = java.time.Duration
                    .between(originalPaymentDate, Instant.now())
                    .toDays();
            if (daysSincePayment > 30) {
                return true;
            }
        }
        
        // Certain reversal types
        return reversalType == ReversalType.CHARGEBACK;
    }
    
    /**
     * Gets the reversal percentage.
     */
    public double getReversalPercentage() {
        if (originalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        return reversalAmount
                .divide(originalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }
    
    /**
     * Types of payment reversals.
     */
    public enum ReversalType {
        FULL_REVERSAL,      // Complete reversal of payment
        PARTIAL_REVERSAL,   // Partial amount reversal
        CHARGEBACK,         // Customer-initiated chargeback
        REFUND,             // Merchant-initiated refund
        CORRECTION          // Error correction
    }
    
    /**
     * Reasons for payment reversal.
     */
    public enum ReversalReason {
        CUSTOMER_REQUEST,
        DUPLICATE_PAYMENT,
        INCORRECT_AMOUNT,
        UNAUTHORIZED_TRANSACTION,
        PRODUCT_RETURN,
        SERVICE_CANCELLATION,
        PROCESSING_ERROR,
        FRAUD,
        DISPUTE,
        OTHER
    }
}