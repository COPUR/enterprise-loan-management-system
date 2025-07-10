package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowState;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Records a state transition in a payment workflow.
 * Immutable value object that captures the details of a workflow state change.
 */
@Value
@Builder
public class PaymentWorkflowStateTransition {
    
    PaymentWorkflowState fromState;
    PaymentWorkflowState toState;
    
    Instant transitionedAt;
    String transitionedBy;
    
    String reason;
    String trigger;
    
    Map<String, Object> transitionContext;
    Map<String, String> metadata;
    
    Long durationInPreviousStateMillis;
    
    boolean isAutomated;
    boolean isForced;
    
    String validationResult;
    String approvalReference;
    
    /**
     * Validates the state transition.
     */
    public void validate() {
        Objects.requireNonNull(fromState, "From state is required");
        Objects.requireNonNull(toState, "To state is required");
        Objects.requireNonNull(transitionedAt, "Transition time is required");
        Objects.requireNonNull(transitionedBy, "Transitioner is required");
        
        if (fromState == toState) {
            throw new IllegalArgumentException("Cannot transition to the same state");
        }
        
        validateTransitionRules();
    }
    
    /**
     * Validates state transition rules.
     */
    private void validateTransitionRules() {
        // Validate allowed transitions based on workflow state machine
        switch (fromState) {
            case CREATED:
                if (toState != PaymentWorkflowState.VALIDATING && 
                    toState != PaymentWorkflowState.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case VALIDATING:
                if (toState != PaymentWorkflowState.APPROVED && 
                    toState != PaymentWorkflowState.REJECTED &&
                    toState != PaymentWorkflowState.FAILED) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case APPROVED:
                if (toState != PaymentWorkflowState.PROCESSING && 
                    toState != PaymentWorkflowState.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case PROCESSING:
                if (toState != PaymentWorkflowState.COMPLETED && 
                    toState != PaymentWorkflowState.FAILED &&
                    toState != PaymentWorkflowState.COMPENSATING) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case COMPLETED:
            case FAILED:
            case CANCELLED:
                // Terminal states - no transitions allowed
                throw new IllegalStateException(
                    String.format("Cannot transition from terminal state %s", fromState));
                
            case REJECTED:
                if (toState != PaymentWorkflowState.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case COMPENSATING:
                if (toState != PaymentWorkflowState.COMPENSATED && 
                    toState != PaymentWorkflowState.FAILED) {
                    throw new IllegalStateException(
                        String.format("Invalid transition from %s to %s", fromState, toState));
                }
                break;
                
            case COMPENSATED:
                // Terminal state - no transitions allowed
                throw new IllegalStateException(
                    String.format("Cannot transition from terminal state %s", fromState));
        }
    }
    
    /**
     * Checks if this is a terminal transition.
     */
    public boolean isTerminalTransition() {
        return toState == PaymentWorkflowState.COMPLETED ||
               toState == PaymentWorkflowState.FAILED ||
               toState == PaymentWorkflowState.CANCELLED ||
               toState == PaymentWorkflowState.COMPENSATED;
    }
    
    /**
     * Checks if this is an error transition.
     */
    public boolean isErrorTransition() {
        return toState == PaymentWorkflowState.FAILED ||
               toState == PaymentWorkflowState.REJECTED;
    }
    
    /**
     * Checks if this transition requires approval.
     */
    public boolean requiresApproval() {
        return fromState == PaymentWorkflowState.VALIDATING && 
               toState == PaymentWorkflowState.APPROVED;
    }
    
    /**
     * Gets a description of the transition.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(fromState).append(" -> ").append(toState);
        
        if (reason != null) {
            desc.append(" (").append(reason).append(")");
        }
        
        if (isAutomated) {
            desc.append(" [Automated]");
        }
        
        if (isForced) {
            desc.append(" [Forced]");
        }
        
        return desc.toString();
    }
    
    /**
     * Creates a transition for workflow creation.
     */
    public static PaymentWorkflowStateTransition initialTransition(String createdBy) {
        return PaymentWorkflowStateTransition.builder()
                .fromState(PaymentWorkflowState.CREATED)
                .toState(PaymentWorkflowState.VALIDATING)
                .transitionedAt(Instant.now())
                .transitionedBy(createdBy)
                .reason("Workflow initiated")
                .isAutomated(true)
                .build();
    }
}