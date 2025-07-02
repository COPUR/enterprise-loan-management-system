package com.bank.loanmanagement.loan.saga.orchestrator;

import com.bank.loanmanagement.loan.saga.definition.SagaDefinition;
import com.bank.loanmanagement.loan.saga.model.SagaExecution;

import java.util.List;

public interface SagaOrchestrator {

    <T extends SagaDefinition> SagaExecution<T> startSaga(T sagaDefinition, com.bank.loanmanagement.loan.saga.context.SagaContext context);

    <T extends SagaDefinition> SagaExecution<T> resumeSaga(String sagaId);

    <T extends SagaDefinition> SagaExecution<T> completeStep(String sagaId, String stepId, Object stepResult);

    <T extends SagaDefinition> SagaExecution<T> failStep(String sagaId, String stepId, String reason);

    <T extends SagaDefinition> SagaExecution<T> getSagaExecution(String sagaId);

    <T extends SagaDefinition> List<SagaExecution<T>> getActiveSagas();

    enum SagaStatus {
        NEW,
        RUNNING,
        COMPLETED,
        FAILED,
        COMPENSATING,
        TIMED_OUT
    }
}
