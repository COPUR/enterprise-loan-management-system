package com.bank.loanmanagement.saga.domain;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * SAGA Orchestrator interface for managing distributed transactions
 * Implements the orchestration pattern for complex business processes
 * Ensures consistency across BIAN service domains with compensation handling
 */
public interface SagaOrchestrator<T extends SagaDefinition> {

    /**
     * Start a new SAGA execution
     * Returns SAGA instance with initial state
     */
    SagaExecution<T> startSaga(T sagaDefinition, Map<String, Object> initialData);

    /**
     * Process incoming event for SAGA continuation
     * Determines next step based on current state and event type
     */
    SagaExecution<T> processEvent(String sagaId, DomainEvent event);

    /**
     * Handle SAGA step completion
     * Advances SAGA to next step or completes if all steps finished
     */
    SagaExecution<T> handleStepCompletion(String sagaId, String stepId, Object stepResult);

    /**
     * Handle SAGA step failure
     * Initiates compensation flow for failed step
     */
    SagaExecution<T> handleStepFailure(String sagaId, String stepId, Exception failure);

    /**
     * Compensate SAGA execution
     * Executes compensation actions in reverse order
     */
    SagaExecution<T> compensate(String sagaId, String reason);

    /**
     * Get current SAGA execution state
     */
    SagaExecution<T> getSagaExecution(String sagaId);

    /**
     * Get all active SAGAs
     */
    List<SagaExecution<T>> getActiveSagas();

    /**
     * Timeout handling for long-running SAGAs
     */
    List<SagaExecution<T>> handleTimeouts();

    /**
     * SAGA execution state and metadata
     */
    interface SagaExecution<T extends SagaDefinition> {
        String getSagaId();
        T getSagaDefinition();
        SagaStatus getStatus();
        String getCurrentStepId();
        Map<String, Object> getSagaData();
        List<SagaStep> getCompletedSteps();
        List<SagaStep> getFailedSteps();
        OffsetDateTime getStartedAt();
        OffsetDateTime getCompletedAt();
        String getFailureReason();
        int getRetryCount();
        OffsetDateTime getTimeoutAt();
        
        // BIAN compliance metadata
        String getServiceDomainContext();
        String getBehaviorQualifierSequence();
        String getComplianceChecksum();
        
        // FAPI security context
        String getFapiInteractionId();
        String getClientId();
        String getCorrelationId();
    }

    /**
     * SAGA execution status
     */
    enum SagaStatus {
        INITIATED,
        RUNNING,
        COMPLETED,
        COMPENSATING,
        COMPENSATED,
        FAILED,
        TIMEOUT
    }

    /**
     * Individual SAGA step representation
     */
    interface SagaStep {
        String getStepId();
        String getStepType();
        SagaStepStatus getStatus();
        Map<String, Object> getStepData();
        Object getStepResult();
        Exception getFailureReason();
        OffsetDateTime getStartedAt();
        OffsetDateTime getCompletedAt();
        int getRetryCount();
        
        // BIAN service domain context
        String getTargetServiceDomain();
        String getBehaviorQualifier();
        String getServiceOperationId();
        
        // Compensation information
        String getCompensationAction();
        boolean isCompensationRequired();
        boolean isCompensationCompleted();
    }

    /**
     * SAGA step execution status
     */
    enum SagaStepStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED,
        SKIPPED
    }

    /**
     * SAGA orchestration exception
     */
    class SagaOrchestrationException extends RuntimeException {
        private final String sagaId;
        private final String stepId;
        private final SagaStatus sagaStatus;

        public SagaOrchestrationException(String message, String sagaId, String stepId, SagaStatus sagaStatus) {
            super(message);
            this.sagaId = sagaId;
            this.stepId = stepId;
            this.sagaStatus = sagaStatus;
        }

        public SagaOrchestrationException(String message, Throwable cause, String sagaId, String stepId, SagaStatus sagaStatus) {
            super(message, cause);
            this.sagaId = sagaId;
            this.stepId = stepId;
            this.sagaStatus = sagaStatus;
        }

        public String getSagaId() { return sagaId; }
        public String getStepId() { return stepId; }
        public SagaStatus getSagaStatus() { return sagaStatus; }
    }
}