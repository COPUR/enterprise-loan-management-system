package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowPriority;
import com.loanmanagement.payment.domain.model.PaymentWorkflowType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Request to start a payment workflow.
 * Immutable value object containing all necessary information to initiate a workflow.
 */
@Value
@Builder
public class PaymentWorkflowRequest {
    
    String paymentId;
    String customerId;
    String accountId;
    
    PaymentWorkflowType workflowType;
    PaymentWorkflowPriority priority;
    
    BigDecimal amount;
    String currency;
    
    String sourceAccountId;
    String destinationAccountId;
    
    String paymentMethod;
    String paymentReference;
    
    Instant requestedAt;
    String requestedBy;
    
    Map<String, Object> workflowContext;
    Map<String, String> metadata;
    
    Boolean requiresApproval;
    Boolean requiresValidation;
    
    Integer maxRetries;
    Long timeoutMillis;
    
    /**
     * Validates the workflow request.
     */
    public void validate() {
        Objects.requireNonNull(paymentId, "Payment ID is required");
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(workflowType, "Workflow type is required");
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(requestedBy, "Requester is required");
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
        
        validateWorkflowSpecificRequirements();
    }
    
    /**
     * Validates workflow-specific requirements.
     */
    private void validateWorkflowSpecificRequirements() {
        switch (workflowType) {
            case STANDARD_PAYMENT:
                Objects.requireNonNull(sourceAccountId, "Source account required for standard payment");
                Objects.requireNonNull(destinationAccountId, "Destination account required for standard payment");
                break;
            case RECURRING_PAYMENT:
                Objects.requireNonNull(sourceAccountId, "Source account required for recurring payment");
                break;
            case BATCH_PAYMENT:
                Objects.requireNonNull(accountId, "Account ID required for batch payment");
                break;
            case REVERSAL:
                Objects.requireNonNull(paymentReference, "Payment reference required for reversal");
                break;
        }
    }
    
    /**
     * Checks if the request requires approval based on amount or flags.
     */
    public boolean requiresApprovalCheck() {
        if (Boolean.TRUE.equals(requiresApproval)) {
            return true;
        }
        
        // High-value payments always require approval
        BigDecimal approvalThreshold = new BigDecimal("10000");
        return amount.compareTo(approvalThreshold) > 0;
    }
    
    /**
     * Gets the effective priority based on amount and specified priority.
     */
    public PaymentWorkflowPriority getEffectivePriority() {
        if (priority != null) {
            return priority;
        }
        
        // Determine priority based on amount
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            return PaymentWorkflowPriority.CRITICAL;
        } else if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return PaymentWorkflowPriority.HIGH;
        } else if (amount.compareTo(new BigDecimal("1000")) > 0) {
            return PaymentWorkflowPriority.MEDIUM;
        }
        
        return PaymentWorkflowPriority.LOW;
    }
    
    /**
     * Gets the effective timeout for the workflow.
     */
    public long getEffectiveTimeoutMillis() {
        if (timeoutMillis != null && timeoutMillis > 0) {
            return timeoutMillis;
        }
        
        // Default timeouts based on workflow type
        switch (workflowType) {
            case STANDARD_PAYMENT:
                return 300000; // 5 minutes
            case RECURRING_PAYMENT:
                return 600000; // 10 minutes
            case BATCH_PAYMENT:
                return 1800000; // 30 minutes
            case REVERSAL:
                return 180000; // 3 minutes
            default:
                return 300000; // 5 minutes default
        }
    }
    
    /**
     * Gets the effective max retries for the workflow.
     */
    public int getEffectiveMaxRetries() {
        if (maxRetries != null && maxRetries >= 0) {
            return maxRetries;
        }
        
        // Default retries based on workflow type
        switch (workflowType) {
            case REVERSAL:
                return 5; // More retries for reversals
            case BATCH_PAYMENT:
                return 2; // Fewer retries for batch
            default:
                return 3; // Standard retry count
        }
    }
}