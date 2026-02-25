package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Value object containing workflow status information.
 * Provides detailed status context beyond just the state.
 */
@Value
@Builder(toBuilder = true)
public class PaymentWorkflowStatus {
    
    PaymentWorkflowState state;
    PaymentWorkflowHealth health;
    
    String message;
    String details;
    
    Instant updatedAt;
    String updatedBy;
    
    Integer progressPercentage;
    String currentStep;
    String nextStep;
    
    Long estimatedTimeRemainingMillis;
    Instant estimatedCompletionTime;
    
    String errorCode;
    String errorMessage;
    Integer errorCount;
    
    Map<String, Object> statusContext;
    Map<String, String> metrics;
    
    boolean actionRequired;
    String requiredAction;
    String actionOwner;
    
    /**
     * Creates a status for successful state transition.
     */
    public static PaymentWorkflowStatus success(
            PaymentWorkflowState state,
            String message,
            String updatedBy) {
        
        return PaymentWorkflowStatus.builder()
                .state(state)
                .health(PaymentWorkflowHealth.HEALTHY)
                .message(message)
                .updatedAt(Instant.now())
                .updatedBy(updatedBy)
                .build();
    }
    
    /**
     * Creates a status for error condition.
     */
    public static PaymentWorkflowStatus error(
            PaymentWorkflowState state,
            String errorCode,
            String errorMessage,
            String updatedBy) {
        
        return PaymentWorkflowStatus.builder()
                .state(state)
                .health(PaymentWorkflowHealth.UNHEALTHY)
                .message("Error occurred")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .updatedAt(Instant.now())
                .updatedBy(updatedBy)
                .build();
    }
    
    /**
     * Creates a status requiring action.
     */
    public static PaymentWorkflowStatus actionRequired(
            PaymentWorkflowState state,
            String message,
            String requiredAction,
            String actionOwner) {
        
        return PaymentWorkflowStatus.builder()
                .state(state)
                .health(PaymentWorkflowHealth.WARNING)
                .message(message)
                .actionRequired(true)
                .requiredAction(requiredAction)
                .actionOwner(actionOwner)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Validates the status consistency.
     */
    public void validate() {
        Objects.requireNonNull(state, "State is required");
        Objects.requireNonNull(updatedAt, "Updated timestamp is required");
        
        if (progressPercentage != null && 
            (progressPercentage < 0 || progressPercentage > 100)) {
            throw new IllegalArgumentException(
                    "Progress percentage must be between 0 and 100");
        }
        
        if (actionRequired && requiredAction == null) {
            throw new IllegalStateException(
                    "Required action must be specified when action is required");
        }
    }
    
    /**
     * Checks if the status indicates an error.
     */
    public boolean hasError() {
        return errorCode != null || errorMessage != null || 
               health == PaymentWorkflowHealth.UNHEALTHY;
    }
    
    /**
     * Checks if the workflow is making progress.
     */
    public boolean isMakingProgress() {
        return state.isActive() && 
               health != PaymentWorkflowHealth.UNHEALTHY &&
               progressPercentage != null &&
               progressPercentage > 0;
    }
    
    /**
     * Updates progress information.
     */
    public PaymentWorkflowStatus withProgress(
            int progressPercentage,
            String currentStep) {
        
        return this.toBuilder()
                .progressPercentage(progressPercentage)
                .currentStep(currentStep)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Updates health status.
     */
    public PaymentWorkflowStatus withHealth(PaymentWorkflowHealth newHealth) {
        return this.toBuilder()
                .health(newHealth)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Adds error information.
     */
    public PaymentWorkflowStatus withError(
            String errorCode,
            String errorMessage) {
        
        return this.toBuilder()
                .health(PaymentWorkflowHealth.UNHEALTHY)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorCount((errorCount != null ? errorCount : 0) + 1)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Clears error information.
     */
    public PaymentWorkflowStatus clearError() {
        return this.toBuilder()
                .errorCode(null)
                .errorMessage(null)
                .health(PaymentWorkflowHealth.HEALTHY)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Gets a summary of the status.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("State: ").append(state.getDisplayName());
        
        if (health != null) {
            summary.append(" - Health: ").append(health);
        }
        
        if (progressPercentage != null) {
            summary.append(" - Progress: ").append(progressPercentage).append("%");
        }
        
        if (currentStep != null) {
            summary.append(" - Step: ").append(currentStep);
        }
        
        if (hasError()) {
            summary.append(" - ERROR: ").append(errorMessage != null ? errorMessage : errorCode);
        }
        
        if (actionRequired) {
            summary.append(" - ACTION REQUIRED: ").append(requiredAction);
        }
        
        return summary.toString();
    }
    
    /**
     * Determines the overall status level.
     */
    public StatusLevel getStatusLevel() {
        if (state.isTerminal()) {
            if (state == PaymentWorkflowState.COMPLETED) {
                return StatusLevel.SUCCESS;
            } else if (state.isErrorState()) {
                return StatusLevel.ERROR;
            } else {
                return StatusLevel.INFO;
            }
        }
        
        if (hasError() || health == PaymentWorkflowHealth.UNHEALTHY) {
            return StatusLevel.ERROR;
        }
        
        if (actionRequired || health == PaymentWorkflowHealth.WARNING) {
            return StatusLevel.WARNING;
        }
        
        return StatusLevel.INFO;
    }
    
    /**
     * Status levels for categorization.
     */
    public enum StatusLevel {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }
}