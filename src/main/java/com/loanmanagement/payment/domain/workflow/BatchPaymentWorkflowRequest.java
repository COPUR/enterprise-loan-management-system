package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowPriority;
import com.loanmanagement.payment.domain.model.PaymentWorkflowType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request to start a batch payment workflow.
 * Processes multiple payments as a single batch operation.
 */
@Value
@Builder
public class BatchPaymentWorkflowRequest {
    
    String batchId;
    String customerId;
    String sourceAccountId;
    
    List<BatchPaymentItem> paymentItems;
    
    BigDecimal totalAmount;
    String currency;
    
    BatchProcessingMode processingMode;
    BatchValidationMode validationMode;
    
    Instant scheduledAt;
    String batchReference;
    String batchDescription;
    
    boolean stopOnError;
    boolean validateBalanceBeforeStart;
    BigDecimal reservedBalance;
    
    Integer maxConcurrentPayments;
    Long itemTimeoutMillis;
    
    Map<String, Object> batchContext;
    Map<String, String> metadata;
    
    String requestedBy;
    String approvedBy;
    
    /**
     * Validates the batch payment request.
     */
    public void validate() {
        Objects.requireNonNull(batchId, "Batch ID is required");
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(sourceAccountId, "Source account is required");
        Objects.requireNonNull(paymentItems, "Payment items are required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(processingMode, "Processing mode is required");
        Objects.requireNonNull(requestedBy, "Requester is required");
        
        if (paymentItems.isEmpty()) {
            throw new IllegalArgumentException("Batch must contain at least one payment");
        }
        
        if (paymentItems.size() > 10000) {
            throw new IllegalArgumentException("Batch size exceeds maximum of 10000 items");
        }
        
        validatePaymentItems();
        validateTotalAmount();
    }
    
    /**
     * Validates individual payment items.
     */
    private void validatePaymentItems() {
        for (int i = 0; i < paymentItems.size(); i++) {
            BatchPaymentItem item = paymentItems.get(i);
            
            try {
                Objects.requireNonNull(item.getPaymentId(), "Payment ID is required");
                Objects.requireNonNull(item.getDestinationAccountId(), "Destination account is required");
                Objects.requireNonNull(item.getAmount(), "Amount is required");
                
                if (item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Amount must be positive");
                }
                
                if (!currency.equals(item.getCurrency())) {
                    throw new IllegalArgumentException("Currency mismatch");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Invalid payment item at index %d: %s", i, e.getMessage()));
            }
        }
    }
    
    /**
     * Validates the total amount matches sum of items.
     */
    private void validateTotalAmount() {
        BigDecimal calculatedTotal = paymentItems.stream()
                .map(BatchPaymentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount != null && totalAmount.compareTo(calculatedTotal) != 0) {
            throw new IllegalArgumentException(
                    String.format("Total amount mismatch. Expected: %s, Calculated: %s",
                            totalAmount, calculatedTotal));
        }
    }
    
    /**
     * Converts to a base payment workflow request.
     */
    public PaymentWorkflowRequest toWorkflowRequest() {
        return PaymentWorkflowRequest.builder()
                .paymentId(batchId)
                .customerId(customerId)
                .accountId(sourceAccountId)
                .workflowType(PaymentWorkflowType.BATCH_PAYMENT)
                .priority(determinePriority())
                .amount(calculateTotalAmount())
                .currency(currency)
                .sourceAccountId(sourceAccountId)
                .paymentReference(batchReference)
                .requestedAt(Instant.now())
                .requestedBy(requestedBy)
                .workflowContext(buildWorkflowContext())
                .metadata(metadata)
                .requiresValidation(true)
                .requiresApproval(requiresApproval())
                .maxRetries(2)
                .timeoutMillis(calculateTimeout())
                .build();
    }
    
    /**
     * Determines batch priority based on size and amount.
     */
    private PaymentWorkflowPriority determinePriority() {
        if (paymentItems.size() > 1000 || 
            calculateTotalAmount().compareTo(new BigDecimal("1000000")) > 0) {
            return PaymentWorkflowPriority.CRITICAL;
        } else if (paymentItems.size() > 100 || 
                   calculateTotalAmount().compareTo(new BigDecimal("100000")) > 0) {
            return PaymentWorkflowPriority.HIGH;
        } else if (paymentItems.size() > 10) {
            return PaymentWorkflowPriority.MEDIUM;
        }
        return PaymentWorkflowPriority.LOW;
    }
    
    /**
     * Calculates total amount from items.
     */
    private BigDecimal calculateTotalAmount() {
        if (totalAmount != null) {
            return totalAmount;
        }
        
        return paymentItems.stream()
                .map(BatchPaymentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Determines if batch requires approval.
     */
    private boolean requiresApproval() {
        // Large batches always require approval
        if (paymentItems.size() > 50) {
            return true;
        }
        
        // High-value batches require approval
        BigDecimal approvalThreshold = new BigDecimal("50000");
        return calculateTotalAmount().compareTo(approvalThreshold) > 0;
    }
    
    /**
     * Calculates timeout based on batch size.
     */
    private long calculateTimeout() {
        // Base timeout of 5 minutes
        long baseTimeout = 300000L;
        
        // Add 1 second per payment item
        long itemTimeout = paymentItems.size() * 1000L;
        
        // Cap at 1 hour
        return Math.min(baseTimeout + itemTimeout, 3600000L);
    }
    
    /**
     * Builds workflow context with batch-specific data.
     */
    private Map<String, Object> buildWorkflowContext() {
        Map<String, Object> context = new java.util.HashMap<>();
        
        if (batchContext != null) {
            context.putAll(batchContext);
        }
        
        context.put("batchSize", paymentItems.size());
        context.put("processingMode", processingMode);
        context.put("validationMode", validationMode);
        context.put("stopOnError", stopOnError);
        context.put("validateBalanceBeforeStart", validateBalanceBeforeStart);
        
        if (scheduledAt != null) {
            context.put("scheduledAt", scheduledAt.toString());
        }
        
        if (approvedBy != null) {
            context.put("approvedBy", approvedBy);
        }
        
        if (maxConcurrentPayments != null) {
            context.put("maxConcurrentPayments", maxConcurrentPayments);
        }
        
        return context;
    }
    
    /**
     * Gets effective concurrent payment limit.
     */
    public int getEffectiveConcurrentLimit() {
        if (maxConcurrentPayments != null && maxConcurrentPayments > 0) {
            return maxConcurrentPayments;
        }
        
        // Default based on processing mode
        return switch (processingMode) {
            case SEQUENTIAL -> 1;
            case PARALLEL -> 10;
            case ADAPTIVE -> 5;
        };
    }
    
    /**
     * Individual payment item in the batch.
     */
    @Value
    @Builder
    public static class BatchPaymentItem {
        String paymentId;
        String destinationAccountId;
        String beneficiaryName;
        
        BigDecimal amount;
        String currency;
        
        String reference;
        String description;
        
        Map<String, String> metadata;
        
        Integer priority;
        boolean critical;
    }
    
    /**
     * Batch processing modes.
     */
    public enum BatchProcessingMode {
        SEQUENTIAL,     // Process payments one by one
        PARALLEL,       // Process multiple payments concurrently
        ADAPTIVE        // Adjust concurrency based on performance
    }
    
    /**
     * Batch validation modes.
     */
    public enum BatchValidationMode {
        STRICT,         // Validate all items before processing any
        PROGRESSIVE,    // Validate as processing
        MINIMAL         // Basic validation only
    }
}