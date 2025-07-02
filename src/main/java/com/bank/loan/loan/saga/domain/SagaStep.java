package com.bank.loanmanagement.loan.saga.domain;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * SAGA Step Interface
 * Represents an individual step within a SAGA execution
 */
public interface SagaStep {
    
    /**
     * Get the unique identifier for this step
     */
    String getStepId();
    
    /**
     * Get the type of this step
     */
    String getStepType();
    
    /**
     * Get the current status of this step
     */
    SagaOrchestrator.SagaStepStatus getStatus();
    
    /**
     * Get the step data (input parameters)
     */
    Map<String, Object> getStepData();
    
    /**
     * Get the step result (output data)
     */
    Object getStepResult();
    
    /**
     * Get the failure reason (if failed)
     */
    Exception getFailureReason();
    
    /**
     * Get the number of retry attempts for this step
     */
    int getRetryCount();
    
    /**
     * Get when the step was started
     */
    OffsetDateTime getStartedAt();
    
    /**
     * Get when the step was completed (if completed)
     */
    OffsetDateTime getCompletedAt();
    
    /**
     * Check if this step requires compensation on SAGA failure
     */
    boolean isCompensationRequired();
    
    /**
     * Get the target service domain for this step
     */
    String getTargetServiceDomain();
    
    /**
     * Get the behavior qualifier for this step
     */
    String getBehaviorQualifier();
    
    /**
     * Get the service operation ID for this step
     */
    String getServiceOperationId();
    
    /**
     * Get the compensation action for this step
     */
    String getCompensationAction();
    
    /**
     * Check if compensation has been completed for this step
     */
    boolean isCompensationCompleted();
}