package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowId;
import com.loanmanagement.payment.domain.model.PaymentWorkflowState;
import com.loanmanagement.payment.domain.model.PaymentWorkflowStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of a payment workflow execution.
 * Immutable value object containing the outcome and details of a workflow run.
 */
@Value
@Builder
public class PaymentWorkflowResult {
    
    PaymentWorkflowId workflowId;
    String paymentId;
    
    boolean success;
    PaymentWorkflowState finalState;
    PaymentWorkflowStatus status;
    
    Instant startedAt;
    Instant completedAt;
    Long durationMillis;
    
    String transactionId;
    String confirmationNumber;
    
    List<String> processedSteps;
    List<String> skippedSteps;
    List<String> failedSteps;
    
    Map<String, Object> resultData;
    Map<String, String> metadata;
    
    String errorCode;
    String errorMessage;
    List<String> warnings;
    
    Integer retryCount;
    boolean wasRetried;
    
    /**
     * Creates a successful result.
     */
    public static PaymentWorkflowResult success(
            PaymentWorkflowId workflowId,
            String paymentId,
            String transactionId,
            Map<String, Object> resultData) {
        
        Instant now = Instant.now();
        return PaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .paymentId(paymentId)
                .success(true)
                .finalState(PaymentWorkflowState.COMPLETED)
                .status(PaymentWorkflowStatus.builder()
                        .state(PaymentWorkflowState.COMPLETED)
                        .message("Workflow completed successfully")
                        .updatedAt(now)
                        .build())
                .transactionId(transactionId)
                .completedAt(now)
                .resultData(resultData)
                .build();
    }
    
    /**
     * Creates a failed result.
     */
    public static PaymentWorkflowResult failure(
            PaymentWorkflowId workflowId,
            String paymentId,
            String errorCode,
            String errorMessage) {
        
        Instant now = Instant.now();
        return PaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .paymentId(paymentId)
                .success(false)
                .finalState(PaymentWorkflowState.FAILED)
                .status(PaymentWorkflowStatus.builder()
                        .state(PaymentWorkflowState.FAILED)
                        .message(errorMessage)
                        .errorCode(errorCode)
                        .updatedAt(now)
                        .build())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .completedAt(now)
                .build();
    }
    
    /**
     * Creates a cancelled result.
     */
    public static PaymentWorkflowResult cancelled(
            PaymentWorkflowId workflowId,
            String paymentId,
            String reason) {
        
        Instant now = Instant.now();
        return PaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .paymentId(paymentId)
                .success(false)
                .finalState(PaymentWorkflowState.CANCELLED)
                .status(PaymentWorkflowStatus.builder()
                        .state(PaymentWorkflowState.CANCELLED)
                        .message(reason)
                        .updatedAt(now)
                        .build())
                .completedAt(now)
                .build();
    }
    
    /**
     * Checks if the workflow completed successfully.
     */
    public boolean isSuccessful() {
        return success && finalState == PaymentWorkflowState.COMPLETED;
    }
    
    /**
     * Checks if the workflow failed.
     */
    public boolean isFailed() {
        return !success && finalState == PaymentWorkflowState.FAILED;
    }
    
    /**
     * Checks if the workflow was cancelled.
     */
    public boolean isCancelled() {
        return finalState == PaymentWorkflowState.CANCELLED;
    }
    
    /**
     * Gets the error details if available.
     */
    public Optional<String> getErrorDetails() {
        if (errorCode != null || errorMessage != null) {
            return Optional.of(String.format("[%s] %s", 
                    errorCode != null ? errorCode : "UNKNOWN",
                    errorMessage != null ? errorMessage : "Unknown error"));
        }
        return Optional.empty();
    }
    
    /**
     * Calculates the success rate of steps.
     */
    public double getStepSuccessRate() {
        if (processedSteps == null || processedSteps.isEmpty()) {
            return 0.0;
        }
        
        int totalSteps = processedSteps.size();
        if (failedSteps != null) {
            totalSteps += failedSteps.size();
        }
        if (skippedSteps != null) {
            totalSteps += skippedSteps.size();
        }
        
        return (double) processedSteps.size() / totalSteps * 100;
    }
    
    /**
     * Checks if the result has warnings.
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Gets a summary of the result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Workflow ").append(workflowId)
               .append(" for payment ").append(paymentId)
               .append(" - Status: ").append(finalState);
        
        if (isSuccessful() && transactionId != null) {
            summary.append(" - Transaction: ").append(transactionId);
        } else if (isFailed() && errorMessage != null) {
            summary.append(" - Error: ").append(errorMessage);
        }
        
        return summary.toString();
    }
}