package com.bank.loanmanagement.loan.saga.infrastructure;

import com.bank.loanmanagement.loan.saga.domain.SagaOrchestrator;

/**
 * SAGA Orchestration Exception
 * Thrown when SAGA orchestration operations fail
 */
public class SagaOrchestrationException extends RuntimeException {
    
    private final String sagaId;
    private final String stepId;
    private final SagaOrchestrator.SagaStatus sagaStatus;
    
    public SagaOrchestrationException(String message, String sagaId, String stepId, SagaOrchestrator.SagaStatus sagaStatus) {
        super(message);
        this.sagaId = sagaId;
        this.stepId = stepId;
        this.sagaStatus = sagaStatus;
    }
    
    public SagaOrchestrationException(String message, Throwable cause, String sagaId, String stepId, SagaOrchestrator.SagaStatus sagaStatus) {
        super(message, cause);
        this.sagaId = sagaId;
        this.stepId = stepId;
        this.sagaStatus = sagaStatus;
    }
    
    public String getSagaId() {
        return sagaId;
    }
    
    public String getStepId() {
        return stepId;
    }
    
    public SagaOrchestrator.SagaStatus getSagaStatus() {
        return sagaStatus;
    }
}