package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowId;
import com.loanmanagement.payment.domain.model.PaymentWorkflowState;
import lombok.Getter;

import java.util.Map;

/**
 * Custom exception for payment workflow errors.
 * Provides detailed context about workflow failures.
 */
@Getter
public class PaymentWorkflowException extends RuntimeException {
    
    private final String errorCode;
    private final PaymentWorkflowId workflowId;
    private final PaymentWorkflowState currentState;
    private final Map<String, Object> errorContext;
    private final boolean retryable;
    
    /**
     * Creates a workflow exception with full context.
     */
    public PaymentWorkflowException(
            String message,
            String errorCode,
            PaymentWorkflowId workflowId,
            PaymentWorkflowState currentState,
            Map<String, Object> errorContext,
            boolean retryable,
            Throwable cause) {
        
        super(formatMessage(message, errorCode, workflowId, currentState), cause);
        this.errorCode = errorCode;
        this.workflowId = workflowId;
        this.currentState = currentState;
        this.errorContext = errorContext;
        this.retryable = retryable;
    }
    
    /**
     * Creates a workflow exception without a cause.
     */
    public PaymentWorkflowException(
            String message,
            String errorCode,
            PaymentWorkflowId workflowId,
            PaymentWorkflowState currentState,
            boolean retryable) {
        
        this(message, errorCode, workflowId, currentState, null, retryable, null);
    }
    
    /**
     * Creates a simple workflow exception.
     */
    public PaymentWorkflowException(String message, String errorCode, boolean retryable) {
        this(message, errorCode, null, null, null, retryable, null);
    }
    
    /**
     * Creates a workflow exception with a cause.
     */
    public PaymentWorkflowException(String message, String errorCode, Throwable cause) {
        this(message, errorCode, null, null, null, false, cause);
    }
    
    /**
     * Formats the exception message with context.
     */
    private static String formatMessage(
            String message,
            String errorCode,
            PaymentWorkflowId workflowId,
            PaymentWorkflowState currentState) {
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("[").append(errorCode).append("] ").append(message);
        
        if (workflowId != null) {
            formatted.append(" - Workflow: ").append(workflowId.getValue());
        }
        
        if (currentState != null) {
            formatted.append(" - State: ").append(currentState);
        }
        
        return formatted.toString();
    }
    
    /**
     * Common error codes for workflow exceptions.
     */
    public static class ErrorCodes {
        public static final String VALIDATION_FAILED = "WORKFLOW_VALIDATION_FAILED";
        public static final String STATE_TRANSITION_INVALID = "WORKFLOW_STATE_TRANSITION_INVALID";
        public static final String TIMEOUT_EXCEEDED = "WORKFLOW_TIMEOUT_EXCEEDED";
        public static final String APPROVAL_REJECTED = "WORKFLOW_APPROVAL_REJECTED";
        public static final String PROCESSING_FAILED = "WORKFLOW_PROCESSING_FAILED";
        public static final String COMPENSATION_FAILED = "WORKFLOW_COMPENSATION_FAILED";
        public static final String CONFIGURATION_ERROR = "WORKFLOW_CONFIGURATION_ERROR";
        public static final String RESOURCE_UNAVAILABLE = "WORKFLOW_RESOURCE_UNAVAILABLE";
        public static final String CONCURRENT_MODIFICATION = "WORKFLOW_CONCURRENT_MODIFICATION";
        public static final String SECURITY_VIOLATION = "WORKFLOW_SECURITY_VIOLATION";
        public static final String QUOTA_EXCEEDED = "WORKFLOW_QUOTA_EXCEEDED";
        public static final String DEPENDENCY_FAILED = "WORKFLOW_DEPENDENCY_FAILED";
    }
    
    /**
     * Creates a validation exception.
     */
    public static PaymentWorkflowException validation(
            String message,
            PaymentWorkflowId workflowId,
            Map<String, Object> validationErrors) {
        
        return new PaymentWorkflowException(
                message,
                ErrorCodes.VALIDATION_FAILED,
                workflowId,
                PaymentWorkflowState.VALIDATING,
                validationErrors,
                false,
                null);
    }
    
    /**
     * Creates a state transition exception.
     */
    public static PaymentWorkflowException invalidTransition(
            PaymentWorkflowState fromState,
            PaymentWorkflowState toState,
            PaymentWorkflowId workflowId) {
        
        String message = String.format(
                "Invalid state transition from %s to %s",
                fromState, toState);
        
        return new PaymentWorkflowException(
                message,
                ErrorCodes.STATE_TRANSITION_INVALID,
                workflowId,
                fromState,
                false);
    }
    
    /**
     * Creates a timeout exception.
     */
    public static PaymentWorkflowException timeout(
            PaymentWorkflowId workflowId,
            PaymentWorkflowState currentState,
            long timeoutMillis) {
        
        String message = String.format(
                "Workflow timed out after %d ms",
                timeoutMillis);
        
        Map<String, Object> context = Map.of(
                "timeoutMillis", timeoutMillis,
                "currentState", currentState);
        
        return new PaymentWorkflowException(
                message,
                ErrorCodes.TIMEOUT_EXCEEDED,
                workflowId,
                currentState,
                context,
                true,
                null);
    }
    
    /**
     * Creates a processing exception.
     */
    public static PaymentWorkflowException processingFailed(
            String message,
            PaymentWorkflowId workflowId,
            Throwable cause) {
        
        return new PaymentWorkflowException(
                message,
                ErrorCodes.PROCESSING_FAILED,
                workflowId,
                PaymentWorkflowState.PROCESSING,
                null,
                true,
                cause);
    }
}