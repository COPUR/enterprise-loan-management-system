package com.bank.loanmanagement.saga.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * SAGA Execution Interface
 * Represents the execution state and progress of a SAGA
 */
public interface SagaExecution<T extends SagaDefinition> {
    
    /**
     * Get the unique identifier for this SAGA execution
     */
    String getSagaId();
    
    /**
     * Get the SAGA definition being executed
     */
    T getSagaDefinition();
    
    /**
     * Get the current status of the SAGA
     */
    com.bank.loanmanagement.saga.domain.SagaOrchestrator.SagaStatus getStatus();
    
    /**
     * Get the SAGA data (shared state across steps)
     */
    Map<String, Object> getSagaData();
    
    /**
     * Get the ID of the currently executing step
     */
    String getCurrentStepId();
    
    /**
     * Get the list of completed steps
     */
    List<SagaStep> getCompletedSteps();
    
    /**
     * Get when the SAGA was started
     */
    OffsetDateTime getStartedAt();
    
    /**
     * Get when the SAGA was completed (if completed)
     */
    OffsetDateTime getCompletedAt();
    
    /**
     * Get the failure reason (if failed)
     */
    String getFailureReason();
    
    /**
     * Get the number of retry attempts
     */
    int getRetryCount();
}