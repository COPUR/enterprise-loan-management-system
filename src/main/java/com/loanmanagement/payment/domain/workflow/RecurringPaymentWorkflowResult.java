package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowId;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Result of a recurring payment workflow execution.
 * Contains details specific to recurring payment processing.
 */
@Value
@Builder
public class RecurringPaymentWorkflowResult {
    
    // Base result fields
    PaymentWorkflowId workflowId;
    String recurringPaymentId;
    boolean success;
    
    // Execution details
    String executionId;
    Integer occurrenceNumber;
    LocalDate executionDate;
    
    Instant startedAt;
    Instant completedAt;
    Long durationMillis;
    
    // Transaction details
    String transactionId;
    String confirmationNumber;
    BigDecimal processedAmount;
    String currency;
    
    // Recurring-specific results
    LocalDate nextScheduledDate;
    Integer remainingOccurrences;
    boolean isLastOccurrence;
    
    // Status information
    RecurringPaymentStatus recurringStatus;
    List<String> processedSteps;
    Map<String, Object> executionContext;
    
    // Error information
    String errorCode;
    String errorMessage;
    boolean shouldRetry;
    boolean shouldPause;
    
    // Warnings and notifications
    List<String> warnings;
    List<NotificationSent> notificationsSent;
    
    /**
     * Creates a successful recurring payment result.
     */
    public static RecurringPaymentWorkflowResult success(
            PaymentWorkflowId workflowId,
            String recurringPaymentId,
            String executionId,
            Integer occurrenceNumber,
            String transactionId,
            BigDecimal processedAmount,
            LocalDate nextScheduledDate) {
        
        return RecurringPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .recurringPaymentId(recurringPaymentId)
                .success(true)
                .executionId(executionId)
                .occurrenceNumber(occurrenceNumber)
                .executionDate(LocalDate.now())
                .completedAt(Instant.now())
                .transactionId(transactionId)
                .processedAmount(processedAmount)
                .nextScheduledDate(nextScheduledDate)
                .recurringStatus(RecurringPaymentStatus.ACTIVE)
                .build();
    }
    
    /**
     * Creates a failed recurring payment result.
     */
    public static RecurringPaymentWorkflowResult failure(
            PaymentWorkflowId workflowId,
            String recurringPaymentId,
            String executionId,
            Integer occurrenceNumber,
            String errorCode,
            String errorMessage,
            boolean shouldRetry,
            boolean shouldPause) {
        
        return RecurringPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .recurringPaymentId(recurringPaymentId)
                .success(false)
                .executionId(executionId)
                .occurrenceNumber(occurrenceNumber)
                .executionDate(LocalDate.now())
                .completedAt(Instant.now())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .shouldRetry(shouldRetry)
                .shouldPause(shouldPause)
                .recurringStatus(shouldPause ? 
                        RecurringPaymentStatus.PAUSED : RecurringPaymentStatus.ACTIVE)
                .build();
    }
    
    /**
     * Creates a result for the final occurrence.
     */
    public static RecurringPaymentWorkflowResult finalOccurrence(
            PaymentWorkflowId workflowId,
            String recurringPaymentId,
            String executionId,
            Integer occurrenceNumber,
            String transactionId,
            BigDecimal processedAmount) {
        
        return RecurringPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .recurringPaymentId(recurringPaymentId)
                .success(true)
                .executionId(executionId)
                .occurrenceNumber(occurrenceNumber)
                .executionDate(LocalDate.now())
                .completedAt(Instant.now())
                .transactionId(transactionId)
                .processedAmount(processedAmount)
                .isLastOccurrence(true)
                .remainingOccurrences(0)
                .recurringStatus(RecurringPaymentStatus.COMPLETED)
                .build();
    }
    
    /**
     * Converts to a base workflow result.
     */
    public PaymentWorkflowResult toWorkflowResult() {
        return PaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .paymentId(recurringPaymentId + "-" + occurrenceNumber)
                .success(success)
                .finalState(mapToWorkflowState())
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMillis(durationMillis)
                .transactionId(transactionId)
                .confirmationNumber(confirmationNumber)
                .processedSteps(processedSteps)
                .resultData(buildResultData())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .warnings(warnings)
                .build();
    }
    
    /**
     * Maps recurring status to workflow state.
     */
    private com.loanmanagement.payment.domain.model.PaymentWorkflowState mapToWorkflowState() {
        if (success) {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowState.COMPLETED;
        } else if (shouldRetry) {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowState.PROCESSING;
        } else {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowState.FAILED;
        }
    }
    
    /**
     * Builds result data map.
     */
    private Map<String, Object> buildResultData() {
        Map<String, Object> data = new java.util.HashMap<>();
        
        data.put("recurringPaymentId", recurringPaymentId);
        data.put("occurrenceNumber", occurrenceNumber);
        data.put("executionDate", executionDate.toString());
        data.put("processedAmount", processedAmount);
        
        if (nextScheduledDate != null) {
            data.put("nextScheduledDate", nextScheduledDate.toString());
        }
        
        if (remainingOccurrences != null) {
            data.put("remainingOccurrences", remainingOccurrences);
        }
        
        data.put("isLastOccurrence", isLastOccurrence);
        data.put("recurringStatus", recurringStatus);
        
        if (executionContext != null) {
            data.putAll(executionContext);
        }
        
        return data;
    }
    
    /**
     * Checks if the recurring payment should continue.
     */
    public boolean shouldContinue() {
        return success && 
               !isLastOccurrence && 
               recurringStatus == RecurringPaymentStatus.ACTIVE &&
               (remainingOccurrences == null || remainingOccurrences > 0);
    }
    
    /**
     * Checks if immediate retry is recommended.
     */
    public boolean shouldRetryImmediately() {
        return !success && shouldRetry && !shouldPause;
    }
    
    /**
     * Gets a summary of the result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Recurring payment ")
               .append(recurringPaymentId)
               .append(" - Occurrence #")
               .append(occurrenceNumber);
        
        if (success) {
            summary.append(" - Success")
                   .append(" - Amount: ")
                   .append(processedAmount)
                   .append(" ")
                   .append(currency);
            
            if (transactionId != null) {
                summary.append(" - Transaction: ")
                       .append(transactionId);
            }
            
            if (isLastOccurrence) {
                summary.append(" - FINAL OCCURRENCE");
            } else if (nextScheduledDate != null) {
                summary.append(" - Next: ")
                       .append(nextScheduledDate);
            }
        } else {
            summary.append(" - Failed")
                   .append(" - Error: ")
                   .append(errorMessage);
            
            if (shouldPause) {
                summary.append(" - PAUSED");
            } else if (shouldRetry) {
                summary.append(" - Will retry");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Recurring payment status.
     */
    public enum RecurringPaymentStatus {
        ACTIVE,          // Recurring payment is active
        PAUSED,          // Temporarily paused
        SUSPENDED,       // Suspended due to issues
        COMPLETED,       // All occurrences completed
        CANCELLED,       // Cancelled by user
        EXPIRED          // End date reached
    }
    
    /**
     * Notification sent record.
     */
    @Value
    @Builder
    public static class NotificationSent {
        String notificationType;
        String recipient;
        String channel;
        Instant sentAt;
        boolean success;
    }
}