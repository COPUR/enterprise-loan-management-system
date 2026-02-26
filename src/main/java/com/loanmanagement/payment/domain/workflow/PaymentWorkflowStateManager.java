package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Payment Workflow State Manager
 * Manages state transitions and validates workflow state changes
 */
@Slf4j
@Service
public class PaymentWorkflowStateManager {
    
    // Define valid state transitions
    private static final Set<PaymentWorkflowState> INITIAL_STATES = EnumSet.of(
            PaymentWorkflowState.INITIATED
    );
    
    private static final Set<PaymentWorkflowState> VALIDATION_STATES = EnumSet.of(
            PaymentWorkflowState.VALIDATING,
            PaymentWorkflowState.VALIDATED,
            PaymentWorkflowState.VALIDATION_FAILED
    );
    
    private static final Set<PaymentWorkflowState> PROCESSING_STATES = EnumSet.of(
            PaymentWorkflowState.PRE_PROCESSING,
            PaymentWorkflowState.PRE_PROCESSING_COMPLETED,
            PaymentWorkflowState.PROCESSING,
            PaymentWorkflowState.PROCESSED
    );
    
    private static final Set<PaymentWorkflowState> TERMINAL_STATES = EnumSet.of(
            PaymentWorkflowState.COMPLETED,
            PaymentWorkflowState.FAILED,
            PaymentWorkflowState.CANCELLED,
            PaymentWorkflowState.POST_PROCESSING_FAILED
    );

    /**
     * Transition workflow execution to a new state
     */
    public PaymentWorkflowExecution transitionToState(PaymentWorkflowExecution execution, 
                                                    PaymentWorkflowState newState, 
                                                    String transitionReason) {
        log.debug("Transitioning workflow {} from {} to {}: {}", 
                execution.getWorkflowId(), execution.getCurrentState(), newState, transitionReason);
        
        // Validate state transition
        if (!isValidTransition(execution.getCurrentState(), newState)) {
            log.warn("Invalid state transition attempted for workflow {}: {} -> {}", 
                    execution.getWorkflowId(), execution.getCurrentState(), newState);
            throw new IllegalStateException(
                    String.format("Invalid state transition from %s to %s", 
                            execution.getCurrentState(), newState));
        }
        
        // Create state transition record
        PaymentWorkflowStateTransition transition = PaymentWorkflowStateTransition.builder()
                .fromState(execution.getCurrentState())
                .toState(newState)
                .transitionedAt(java.time.Instant.now())
                .transitionedBy("system")
                .reason(transitionReason)
                .build();
        
        // Update execution with new state and transition
        List<PaymentWorkflowStateTransition> transitions = new ArrayList<>(execution.getStateTransitions());
        transitions.add(transition);
        
        return execution.toBuilder()
                .currentState(newState)
                .updatedAt(java.time.Instant.now())
                .stateTransitions(transitions)
                .build();
    }
    
    /**
     * Check if a state transition is valid
     */
    public boolean isValidTransition(PaymentWorkflowState fromState, PaymentWorkflowState toState) {
        // Allow transitions to failure state from any non-terminal state
        if (toState == PaymentWorkflowState.FAILED && !TERMINAL_STATES.contains(fromState)) {
            return true;
        }
        
        // Allow transitions to cancelled state from non-terminal states
        if (toState == PaymentWorkflowState.CANCELLED && !TERMINAL_STATES.contains(fromState)) {
            return true;
        }
        
        // No transitions allowed from terminal states
        if (TERMINAL_STATES.contains(fromState)) {
            return false;
        }
        
        return switch (fromState) {
            case INITIATED -> toState == PaymentWorkflowState.VALIDATING;
            
            case VALIDATING -> toState == PaymentWorkflowState.VALIDATED || 
                              toState == PaymentWorkflowState.VALIDATION_FAILED;
            
            case VALIDATED -> toState == PaymentWorkflowState.PRE_PROCESSING;
            
            case PRE_PROCESSING -> toState == PaymentWorkflowState.PRE_PROCESSING_COMPLETED;
            
            case PRE_PROCESSING_COMPLETED -> toState == PaymentWorkflowState.PROCESSING;
            
            case PROCESSING -> toState == PaymentWorkflowState.PROCESSED;
            
            case PROCESSED -> toState == PaymentWorkflowState.POST_PROCESSING;
            
            case POST_PROCESSING -> toState == PaymentWorkflowState.COMPLETED || 
                                   toState == PaymentWorkflowState.POST_PROCESSING_FAILED;
            
            // Terminal states - no further transitions allowed
            case COMPLETED, FAILED, CANCELLED, VALIDATION_FAILED, POST_PROCESSING_FAILED -> false;
            
            default -> false;
        };
    }
    
    /**
     * Get all possible next states from the current state
     */
    public Set<PaymentWorkflowState> getPossibleNextStates(PaymentWorkflowState currentState) {
        Set<PaymentWorkflowState> possibleStates = EnumSet.noneOf(PaymentWorkflowState.class);
        
        for (PaymentWorkflowState state : PaymentWorkflowState.values()) {
            if (isValidTransition(currentState, state)) {
                possibleStates.add(state);
            }
        }
        
        return possibleStates;
    }
    
    /**
     * Check if the workflow is in a terminal state
     */
    public boolean isTerminalState(PaymentWorkflowState state) {
        return TERMINAL_STATES.contains(state);
    }
    
    /**
     * Check if the workflow is in a processing state
     */
    public boolean isProcessingState(PaymentWorkflowState state) {
        return PROCESSING_STATES.contains(state);
    }
    
    /**
     * Check if the workflow is in a validation state
     */
    public boolean isValidationState(PaymentWorkflowState state) {
        return VALIDATION_STATES.contains(state);
    }
    
    /**
     * Check if the workflow can be cancelled
     */
    public boolean canBeCancelled(PaymentWorkflowState currentState) {
        return !TERMINAL_STATES.contains(currentState) && 
               currentState != PaymentWorkflowState.PROCESSING; // Can't cancel during active processing
    }
    
    /**
     * Get the workflow progress percentage
     */
    public double getProgressPercentage(PaymentWorkflowState currentState) {
        return switch (currentState) {
            case INITIATED -> 0.0;
            case VALIDATING -> 10.0;
            case VALIDATED -> 20.0;
            case PRE_PROCESSING -> 30.0;
            case PRE_PROCESSING_COMPLETED -> 40.0;
            case PROCESSING -> 60.0;
            case PROCESSED -> 80.0;
            case POST_PROCESSING -> 90.0;
            case COMPLETED -> 100.0;
            case VALIDATION_FAILED, FAILED, CANCELLED, POST_PROCESSING_FAILED -> 0.0;
        };
    }
    
    /**
     * Get the expected duration for each state in milliseconds
     */
    public long getExpectedStateDuration(PaymentWorkflowState state) {
        return switch (state) {
            case INITIATED -> 100;
            case VALIDATING -> 500;
            case VALIDATED -> 100;
            case PRE_PROCESSING -> 1000;
            case PRE_PROCESSING_COMPLETED -> 100;
            case PROCESSING -> 2000;
            case PROCESSED -> 100;
            case POST_PROCESSING -> 1000;
            case COMPLETED -> 0;
            default -> 0;
        };
    }
    
    /**
     * Calculate total expected workflow duration
     */
    public long getTotalExpectedDuration() {
        return INITIAL_STATES.stream().mapToLong(this::getExpectedStateDuration).sum() +
               VALIDATION_STATES.stream().mapToLong(this::getExpectedStateDuration).sum() +
               PROCESSING_STATES.stream().mapToLong(this::getExpectedStateDuration).sum() +
               getExpectedStateDuration(PaymentWorkflowState.POST_PROCESSING) +
               getExpectedStateDuration(PaymentWorkflowState.COMPLETED);
    }
    
    /**
     * Get workflow state description
     */
    public String getStateDescription(PaymentWorkflowState state) {
        return switch (state) {
            case INITIATED -> "Workflow has been initiated and is ready to start";
            case VALIDATING -> "Payment request is being validated";
            case VALIDATED -> "Payment request validation completed successfully";
            case VALIDATION_FAILED -> "Payment request validation failed";
            case PRE_PROCESSING -> "Pre-processing tasks are being executed";
            case PRE_PROCESSING_COMPLETED -> "Pre-processing tasks completed successfully";
            case PROCESSING -> "Payment is being processed";
            case PROCESSED -> "Payment processing completed successfully";
            case POST_PROCESSING -> "Post-processing tasks are being executed";
            case POST_PROCESSING_FAILED -> "Post-processing tasks failed";
            case COMPLETED -> "Workflow completed successfully";
            case FAILED -> "Workflow failed";
            case CANCELLED -> "Workflow was cancelled";
        };
    }
    
    /**
     * Get workflow health status based on current state and execution time
     */
    public com.loanmanagement.payment.domain.model.PaymentWorkflowHealth getWorkflowHealth(PaymentWorkflowExecution execution) {
        PaymentWorkflowState currentState = execution.getCurrentState();
        long actualDuration = java.time.Duration.between(execution.getStartedAt(), java.time.Instant.now()).toMillis();
        long expectedDuration = getTotalExpectedDuration();
        
        if (TERMINAL_STATES.contains(currentState)) {
            if (currentState == PaymentWorkflowState.COMPLETED) {
                return com.loanmanagement.payment.domain.model.PaymentWorkflowHealth.HEALTHY;
            } else {
                return com.loanmanagement.payment.domain.model.PaymentWorkflowHealth.CRITICAL;
            }
        }
        
        if (actualDuration > expectedDuration * 2) {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowHealth.CRITICAL;
        } else if (actualDuration > expectedDuration * 1.5) {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowHealth.WARNING;
        } else {
            return com.loanmanagement.payment.domain.model.PaymentWorkflowHealth.HEALTHY;
        }
    }
    
    /**
     * Create workflow execution summary
     */
    public com.loanmanagement.payment.domain.model.PaymentWorkflowExecutionSummary createExecutionSummary(PaymentWorkflowExecution execution) {
        long totalDuration = execution.getUpdatedAt() != null ? 
                java.time.Duration.between(execution.getStartedAt(), execution.getUpdatedAt()).toMillis() : 0;
        
        return com.loanmanagement.payment.domain.model.PaymentWorkflowExecutionSummary.builder()
                .workflowId(execution.getWorkflowId())
                .workflowType(execution.getWorkflowType())
                .currentState(execution.getCurrentState())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getUpdatedAt())
                .durationMillis(totalDuration)
                .isTerminal(isTerminalState(execution.getCurrentState()))
                .progressPercentage((int)getProgressPercentage(execution.getCurrentState()))
                .health(getWorkflowHealth(execution))
                .build();
    }
}