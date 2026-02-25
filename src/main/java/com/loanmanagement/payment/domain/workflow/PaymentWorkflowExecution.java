package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main workflow execution entity that tracks the lifecycle of a payment workflow.
 * Immutable value object representing a complete workflow execution.
 */
@Value
@Builder(toBuilder = true)
@Slf4j
public class PaymentWorkflowExecution {
    
    PaymentWorkflowId workflowId;
    PaymentWorkflowType workflowType;
    PaymentWorkflowState currentState;
    PaymentWorkflowPriority priority;
    PaymentWorkflowStatus status;
    
    String paymentId;
    String customerId;
    
    Instant startedAt;
    Instant updatedAt;
    Instant completedAt;
    
    List<PaymentWorkflowStateTransition> stateTransitions;
    Map<String, Object> executionContext;
    Map<String, String> metadata;
    
    String initiatedBy;
    String lastModifiedBy;
    
    Integer retryCount;
    Integer maxRetries;
    
    String errorMessage;
    String errorCode;
    
    PaymentWorkflowHealth health;
    
    /**
     * Validates the workflow execution state.
     */
    public boolean isValid() {
        return workflowId != null && 
               workflowType != null && 
               currentState != null && 
               startedAt != null;
    }
    
    /**
     * Checks if the workflow is in a terminal state.
     */
    public boolean isTerminal() {
        return currentState == PaymentWorkflowState.COMPLETED || 
               currentState == PaymentWorkflowState.FAILED || 
               currentState == PaymentWorkflowState.CANCELLED;
    }
    
    /**
     * Checks if the workflow can be retried.
     */
    public boolean canRetry() {
        return !isTerminal() && 
               retryCount != null && 
               maxRetries != null && 
               retryCount < maxRetries;
    }
    
    /**
     * Gets the duration of the workflow execution.
     */
    public Optional<Long> getDurationMillis() {
        if (completedAt != null) {
            return Optional.of(completedAt.toEpochMilli() - startedAt.toEpochMilli());
        }
        return Optional.empty();
    }
    
    /**
     * Creates an execution summary from this execution.
     */
    public PaymentWorkflowExecutionSummary toSummary() {
        return PaymentWorkflowExecutionSummary.builder()
                .workflowId(workflowId)
                .workflowType(workflowType)
                .currentState(currentState)
                .status(status)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMillis(getDurationMillis().orElse(null))
                .health(health)
                .build();
    }
    
    /**
     * Transitions to a new state.
     */
    public PaymentWorkflowExecution transitionTo(PaymentWorkflowState newState, String transitionedBy) {
        log.info("Transitioning workflow {} from {} to {}", workflowId, currentState, newState);
        
        PaymentWorkflowStateTransition transition = PaymentWorkflowStateTransition.builder()
                .fromState(currentState)
                .toState(newState)
                .transitionedAt(Instant.now())
                .transitionedBy(transitionedBy)
                .build();
        
        List<PaymentWorkflowStateTransition> updatedTransitions = 
                new java.util.ArrayList<>(stateTransitions);
        updatedTransitions.add(transition);
        
        return this.toBuilder()
                .currentState(newState)
                .stateTransitions(updatedTransitions)
                .updatedAt(Instant.now())
                .lastModifiedBy(transitionedBy)
                .build();
    }
    
    /**
     * Updates the workflow health status.
     */
    public PaymentWorkflowExecution updateHealth(PaymentWorkflowHealth newHealth) {
        return this.toBuilder()
                .health(newHealth)
                .updatedAt(Instant.now())
                .build();
    }
    
    /**
     * Records an error in the workflow.
     */
    public PaymentWorkflowExecution recordError(String errorCode, String errorMessage) {
        return this.toBuilder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .updatedAt(Instant.now())
                .build();
    }
}