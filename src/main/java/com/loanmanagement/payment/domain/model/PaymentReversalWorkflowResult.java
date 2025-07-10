package com.loanmanagement.payment.domain.model;

import com.loanmanagement.payment.domain.workflow.PaymentWorkflowResult;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of a payment reversal workflow execution.
 * Contains details specific to reversal processing.
 */
@Value
@Builder
public class PaymentReversalWorkflowResult {
    
    PaymentWorkflowId workflowId;
    String reversalId;
    String originalPaymentId;
    
    boolean success;
    ReversalStatus reversalStatus;
    
    Instant startedAt;
    Instant completedAt;
    Long durationMillis;
    
    // Reversal transaction details
    String reversalTransactionId;
    String confirmationNumber;
    BigDecimal reversedAmount;
    BigDecimal remainingAmount;
    String currency;
    
    // Processing details
    List<String> processedSteps;
    Map<String, Object> reversalContext;
    
    // Reconciliation info
    boolean reconciledWithOriginal;
    String reconciliationReference;
    Instant reconciledAt;
    
    // Error information
    String errorCode;
    String errorMessage;
    boolean canRetry;
    
    // Notifications
    List<String> notificationsSent;
    Map<String, String> metadata;
    
    /**
     * Creates a successful reversal result.
     */
    public static PaymentReversalWorkflowResult success(
            PaymentWorkflowId workflowId,
            String reversalId,
            String originalPaymentId,
            String reversalTransactionId,
            BigDecimal reversedAmount) {
        
        Instant now = Instant.now();
        return PaymentReversalWorkflowResult.builder()
                .workflowId(workflowId)
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .success(true)
                .reversalStatus(ReversalStatus.COMPLETED)
                .completedAt(now)
                .reversalTransactionId(reversalTransactionId)
                .reversedAmount(reversedAmount)
                .reconciledWithOriginal(true)
                .reconciledAt(now)
                .build();
    }
    
    /**
     * Creates a failed reversal result.
     */
    public static PaymentReversalWorkflowResult failure(
            PaymentWorkflowId workflowId,
            String reversalId,
            String originalPaymentId,
            String errorCode,
            String errorMessage,
            boolean canRetry) {
        
        return PaymentReversalWorkflowResult.builder()
                .workflowId(workflowId)
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .success(false)
                .reversalStatus(ReversalStatus.FAILED)
                .completedAt(Instant.now())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .canRetry(canRetry)
                .reconciledWithOriginal(false)
                .build();
    }
    
    /**
     * Creates a partial reversal result.
     */
    public static PaymentReversalWorkflowResult partial(
            PaymentWorkflowId workflowId,
            String reversalId,
            String originalPaymentId,
            String reversalTransactionId,
            BigDecimal reversedAmount,
            BigDecimal remainingAmount) {
        
        Instant now = Instant.now();
        return PaymentReversalWorkflowResult.builder()
                .workflowId(workflowId)
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .success(true)
                .reversalStatus(ReversalStatus.PARTIAL)
                .completedAt(now)
                .reversalTransactionId(reversalTransactionId)
                .reversedAmount(reversedAmount)
                .remainingAmount(remainingAmount)
                .reconciledWithOriginal(true)
                .reconciledAt(now)
                .build();
    }
    
    /**
     * Converts to a base workflow result.
     */
    public PaymentWorkflowResult toWorkflowResult() {
        PaymentWorkflowState finalState = success ? 
                PaymentWorkflowState.COMPLETED : PaymentWorkflowState.FAILED;
        
        return PaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .paymentId(reversalId)
                .success(success)
                .finalState(finalState)
                .status(PaymentWorkflowStatus.builder()
                        .state(finalState)
                        .message(getStatusMessage())
                        .updatedAt(completedAt)
                        .build())
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMillis(durationMillis)
                .transactionId(reversalTransactionId)
                .confirmationNumber(confirmationNumber)
                .processedSteps(processedSteps)
                .resultData(buildResultData())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Gets a status message for the reversal.
     */
    private String getStatusMessage() {
        if (success) {
            return String.format("Reversal completed successfully. Amount: %s %s",
                    reversedAmount, currency);
        } else {
            return String.format("Reversal failed: %s", errorMessage);
        }
    }
    
    /**
     * Builds result data map.
     */
    private Map<String, Object> buildResultData() {
        Map<String, Object> data = new java.util.HashMap<>();
        
        data.put("reversalId", reversalId);
        data.put("originalPaymentId", originalPaymentId);
        data.put("reversalStatus", reversalStatus);
        data.put("reversedAmount", reversedAmount);
        
        if (remainingAmount != null) {
            data.put("remainingAmount", remainingAmount);
        }
        
        if (reversalTransactionId != null) {
            data.put("reversalTransactionId", reversalTransactionId);
        }
        
        data.put("reconciledWithOriginal", reconciledWithOriginal);
        
        if (reconciliationReference != null) {
            data.put("reconciliationReference", reconciliationReference);
        }
        
        if (reversalContext != null) {
            data.putAll(reversalContext);
        }
        
        return data;
    }
    
    /**
     * Checks if the reversal was complete.
     */
    public boolean isCompleteReversal() {
        return success && 
               reversalStatus == ReversalStatus.COMPLETED &&
               (remainingAmount == null || 
                remainingAmount.compareTo(BigDecimal.ZERO) == 0);
    }
    
    /**
     * Checks if the reversal was partial.
     */
    public boolean isPartialReversal() {
        return success && 
               reversalStatus == ReversalStatus.PARTIAL &&
               remainingAmount != null &&
               remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Gets a summary of the reversal result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Reversal ")
               .append(reversalId)
               .append(" for payment ")
               .append(originalPaymentId);
        
        if (success) {
            summary.append(" - SUCCESS");
            summary.append(" - Reversed: ")
                   .append(reversedAmount)
                   .append(" ")
                   .append(currency);
            
            if (isPartialReversal()) {
                summary.append(" - Remaining: ")
                       .append(remainingAmount)
                       .append(" ")
                       .append(currency);
            }
            
            if (reversalTransactionId != null) {
                summary.append(" - Transaction: ")
                       .append(reversalTransactionId);
            }
        } else {
            summary.append(" - FAILED");
            if (errorMessage != null) {
                summary.append(" - Error: ")
                       .append(errorMessage);
            }
            if (canRetry) {
                summary.append(" - Retryable");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Reversal status types.
     */
    public enum ReversalStatus {
        PENDING,        // Reversal is being processed
        COMPLETED,      // Full reversal completed
        PARTIAL,        // Partial reversal completed
        FAILED,         // Reversal failed
        REJECTED,       // Reversal was rejected
        CANCELLED       // Reversal was cancelled
    }
}