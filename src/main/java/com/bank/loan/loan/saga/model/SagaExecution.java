package com.bank.loanmanagement.loan.saga.model;

import com.bank.loanmanagement.loan.saga.definition.SagaDefinition;
import com.bank.loanmanagement.loan.saga.orchestrator.SagaOrchestrator.SagaStatus;
import com.bank.loanmanagement.loan.saga.context.SagaContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SagaExecution<T extends SagaDefinition> {
    String getSagaId();
    T getSagaDefinition();
    SagaStatus getStatus();
    SagaContext getContext();
    String getCurrentStepId();
    LocalDateTime getTimeoutAt();
    String getFailureReason();
    Map<String, Object> getCompletedSteps();
    Map<String, String> getFailedSteps();
}
