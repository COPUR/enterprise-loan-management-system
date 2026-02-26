package com.loanmanagement.payment.domain.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Enum representing the various states of a payment workflow.
 * Defines the complete state machine for payment processing.
 */
@Getter
public enum PaymentWorkflowState {
    
    // Initial states
    CREATED("Created", "Workflow has been created but not started", true, false),
    INITIALIZING("Initializing", "Workflow is being initialized", true, false),
    
    // Validation states
    VALIDATING("Validating", "Payment details are being validated", true, false),
    VALIDATION_FAILED("Validation Failed", "Payment validation failed", false, true),
    
    // Approval states
    PENDING_APPROVAL("Pending Approval", "Waiting for approval", true, false),
    APPROVED("Approved", "Payment has been approved", true, false),
    REJECTED("Rejected", "Payment has been rejected", false, true),
    
    // Processing states
    PROCESSING("Processing", "Payment is being processed", true, false),
    SENDING("Sending", "Payment is being sent to processor", true, false),
    CONFIRMING("Confirming", "Waiting for payment confirmation", true, false),
    
    // Completion states
    COMPLETED("Completed", "Payment workflow completed successfully", false, true),
    FAILED("Failed", "Payment workflow failed", false, true),
    CANCELLED("Cancelled", "Payment workflow was cancelled", false, true),
    
    // Compensation states
    COMPENSATING("Compensating", "Performing compensation actions", true, false),
    COMPENSATED("Compensated", "Compensation completed", false, true),
    
    // Special states
    SUSPENDED("Suspended", "Workflow is suspended", true, false),
    RESUMING("Resuming", "Workflow is resuming from suspension", true, false),
    RETRYING("Retrying", "Workflow is retrying after failure", true, false);
    
    private final String displayName;
    private final String description;
    private final boolean active;
    private final boolean terminal;
    
    PaymentWorkflowState(String displayName, String description, boolean active, boolean terminal) {
        this.displayName = displayName;
        this.description = description;
        this.active = active;
        this.terminal = terminal;
    }
    
    /**
     * Gets all terminal states.
     */
    public static Set<PaymentWorkflowState> getTerminalStates() {
        return Set.of(COMPLETED, FAILED, CANCELLED, COMPENSATED, VALIDATION_FAILED, REJECTED);
    }
    
    /**
     * Gets all active processing states.
     */
    public static Set<PaymentWorkflowState> getActiveStates() {
        return Arrays.stream(values())
                .filter(PaymentWorkflowState::isActive)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Gets all error states.
     */
    public static Set<PaymentWorkflowState> getErrorStates() {
        return Set.of(FAILED, VALIDATION_FAILED, REJECTED);
    }
    
    /**
     * Checks if transition to another state is allowed.
     */
    public boolean canTransitionTo(PaymentWorkflowState targetState) {
        if (this.isTerminal()) {
            return false; // Terminal states cannot transition
        }
        
        return getAllowedTransitions().contains(targetState);
    }
    
    /**
     * Gets allowed transitions from this state.
     */
    public Set<PaymentWorkflowState> getAllowedTransitions() {
        return switch (this) {
            case CREATED -> Set.of(INITIALIZING, VALIDATING, CANCELLED);
            case INITIALIZING -> Set.of(VALIDATING, FAILED, CANCELLED);
            case VALIDATING -> Set.of(VALIDATION_FAILED, PENDING_APPROVAL, APPROVED, PROCESSING, CANCELLED);
            case VALIDATION_FAILED -> Set.of(); // Terminal
            case PENDING_APPROVAL -> Set.of(APPROVED, REJECTED, CANCELLED, SUSPENDED);
            case APPROVED -> Set.of(PROCESSING, CANCELLED);
            case REJECTED -> Set.of(); // Terminal
            case PROCESSING -> Set.of(SENDING, COMPLETED, FAILED, COMPENSATING, SUSPENDED, RETRYING);
            case SENDING -> Set.of(CONFIRMING, FAILED, RETRYING);
            case CONFIRMING -> Set.of(COMPLETED, FAILED, RETRYING);
            case COMPLETED -> Set.of(); // Terminal
            case FAILED -> Set.of(); // Terminal
            case CANCELLED -> Set.of(); // Terminal
            case COMPENSATING -> Set.of(COMPENSATED, FAILED);
            case COMPENSATED -> Set.of(); // Terminal
            case SUSPENDED -> Set.of(RESUMING, CANCELLED);
            case RESUMING -> Set.of(PROCESSING, VALIDATING, PENDING_APPROVAL, FAILED);
            case RETRYING -> Set.of(PROCESSING, SENDING, CONFIRMING, FAILED);
        };
    }
    
    /**
     * Checks if this state requires user action.
     */
    public boolean requiresUserAction() {
        return this == PENDING_APPROVAL || this == SUSPENDED;
    }
    
    /**
     * Checks if this state indicates a problem.
     */
    public boolean isErrorState() {
        return getErrorStates().contains(this);
    }
    
    /**
     * Checks if this state allows retry.
     */
    public boolean allowsRetry() {
        return Set.of(FAILED, VALIDATION_FAILED).contains(this);
    }
    
    /**
     * Gets the state category.
     */
    public StateCategory getCategory() {
        if (Set.of(CREATED, INITIALIZING).contains(this)) {
            return StateCategory.INITIALIZATION;
        } else if (Set.of(VALIDATING, VALIDATION_FAILED).contains(this)) {
            return StateCategory.VALIDATION;
        } else if (Set.of(PENDING_APPROVAL, APPROVED, REJECTED).contains(this)) {
            return StateCategory.APPROVAL;
        } else if (Set.of(PROCESSING, SENDING, CONFIRMING).contains(this)) {
            return StateCategory.PROCESSING;
        } else if (Set.of(COMPLETED, FAILED, CANCELLED).contains(this)) {
            return StateCategory.COMPLETION;
        } else if (Set.of(COMPENSATING, COMPENSATED).contains(this)) {
            return StateCategory.COMPENSATION;
        } else {
            return StateCategory.SPECIAL;
        }
    }
    
    /**
     * State categories for grouping.
     */
    public enum StateCategory {
        INITIALIZATION,
        VALIDATION,
        APPROVAL,
        PROCESSING,
        COMPLETION,
        COMPENSATION,
        SPECIAL
    }
}