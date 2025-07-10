package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Value object containing a summary of workflow execution.
 * Provides a lightweight view of execution details.
 */
@Value
@Builder
public class PaymentWorkflowExecutionSummary {
    
    PaymentWorkflowId workflowId;
    PaymentWorkflowType workflowType;
    PaymentWorkflowState currentState;
    PaymentWorkflowStatus status;
    PaymentWorkflowHealth health;
    
    Instant startedAt;
    Instant completedAt;
    Long durationMillis;
    
    String paymentId;
    String customerId;
    
    boolean isActive;
    boolean isTerminal;
    boolean hasErrors;
    
    Integer stepCount;
    Integer completedSteps;
    Integer progressPercentage;
    
    Map<String, Object> keyMetrics;
    List<String> recentEvents;
    
    /**
     * Creates a summary from a full execution object.
     */
    public static PaymentWorkflowExecutionSummary fromExecution(
            PaymentWorkflowId workflowId,
            PaymentWorkflowType workflowType,
            PaymentWorkflowState currentState,
            PaymentWorkflowStatus status,
            Instant startedAt,
            Instant completedAt) {
        
        Long duration = null;
        if (completedAt != null && startedAt != null) {
            duration = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        } else if (startedAt != null) {
            duration = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
        }
        
        return PaymentWorkflowExecutionSummary.builder()
                .workflowId(workflowId)
                .workflowType(workflowType)
                .currentState(currentState)
                .status(status)
                .health(status != null ? status.getHealth() : PaymentWorkflowHealth.HEALTHY)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMillis(duration)
                .isActive(currentState != null && currentState.isActive())
                .isTerminal(currentState != null && currentState.isTerminal())
                .hasErrors(status != null && status.hasError())
                .progressPercentage(status != null ? status.getProgressPercentage() : null)
                .build();
    }
    
    /**
     * Gets a brief description of the execution.
     */
    public String getBriefDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(workflowType.getDisplayName())
            .append(" [")
            .append(workflowId.toShortString())
            .append("] - ")
            .append(currentState.getDisplayName());
        
        if (progressPercentage != null) {
            desc.append(" (").append(progressPercentage).append("%)");
        }
        
        return desc.toString();
    }
    
    /**
     * Gets the execution age in milliseconds.
     */
    public long getAgeMillis() {
        if (startedAt == null) {
            return 0;
        }
        return Instant.now().toEpochMilli() - startedAt.toEpochMilli();
    }
    
    /**
     * Checks if the execution is overdue.
     */
    public boolean isOverdue() {
        if (isTerminal || workflowType == null) {
            return false;
        }
        
        long expectedDuration = workflowType.getDefaultTimeoutMillis();
        return getAgeMillis() > expectedDuration;
    }
    
    /**
     * Gets the completion percentage based on state.
     */
    public int getEstimatedCompletionPercentage() {
        if (progressPercentage != null) {
            return progressPercentage;
        }
        
        if (currentState == null) {
            return 0;
        }
        
        return switch (currentState.getCategory()) {
            case INITIALIZATION -> 10;
            case VALIDATION -> 20;
            case APPROVAL -> 40;
            case PROCESSING -> 70;
            case COMPLETION -> 100;
            case COMPENSATION -> 90;
            case SPECIAL -> 50;
        };
    }
    
    /**
     * Gets a status icon/emoji based on state and health.
     */
    public String getStatusIcon() {
        if (hasErrors) {
            return "❌";
        }
        
        if (isTerminal) {
            return currentState == PaymentWorkflowState.COMPLETED ? "✅" : "⚠️";
        }
        
        return switch (health) {
            case HEALTHY -> "🟢";
            case WARNING -> "🟡";
            case DEGRADED -> "🟠";
            case UNHEALTHY -> "🔴";
            case CRITICAL -> "🚨";
        };
    }
    
    /**
     * Formats the duration in a human-readable way.
     */
    public String getFormattedDuration() {
        if (durationMillis == null) {
            return "N/A";
        }
        
        long seconds = durationMillis / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        return hours + "h " + remainingMinutes + "m";
    }
    
    /**
     * Gets a priority score for sorting summaries.
     */
    public int getPriorityScore() {
        int score = 0;
        
        // Active workflows have higher priority
        if (isActive) {
            score += 1000;
        }
        
        // Errors increase priority
        if (hasErrors) {
            score += 500;
        }
        
        // Health affects priority
        if (health != null) {
            score += (4 - health.getSeverityLevel()) * 100;
        }
        
        // Overdue workflows have higher priority
        if (isOverdue()) {
            score += 200;
        }
        
        return score;
    }
}