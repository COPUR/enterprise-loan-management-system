package com.bank.loanmanagement.loan.saga.orchestrator;

import com.bank.loanmanagement.loan.saga.definition.SagaDefinition;
import com.bank.loanmanagement.loan.saga.definition.SagaStepDefinition;
import com.bank.loanmanagement.loan.saga.context.SagaContext;
import com.bank.loanmanagement.loan.saga.executor.SagaStepExecutor;
import com.bank.loanmanagement.loan.saga.model.SagaExecution;
import com.bank.loanmanagement.loan.saga.model.SagaExecutionImpl;
import com.bank.loanmanagement.loan.saga.model.SagaStepImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringSagaOrchestrator implements SagaOrchestrator {

    private final SagaStepExecutor sagaStepExecutor;
    private final Map<String, SagaExecutionImpl<?>> activeSagas = new ConcurrentHashMap<>();
    private final ExecutorService sagaExecutorService = Executors.newCachedThreadPool();

    @Override
    public <T extends SagaDefinition> SagaExecution<T> startSaga(T sagaDefinition, SagaContext context) {
        String sagaId = UUID.randomUUID().toString();
        SagaExecutionImpl<T> execution = new SagaExecutionImpl<>(sagaId, sagaDefinition, context);
        activeSagas.put(sagaId, execution);
        log.info("Starting SAGA: {} with ID: {}", sagaDefinition.getSagaType(), sagaId);
        executeNextStep(execution);
        return execution;
    }

    @Override
    public <T extends SagaDefinition> SagaExecution<T> resumeSaga(String sagaId) {
        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaNotFoundException("SAGA not found: " + sagaId);
        }
        log.info("Resuming SAGA: {} with ID: {}", execution.getSagaDefinition().getSagaType(), sagaId);
        executeNextStep(execution);
        return execution;
    }

    @Override
    public <T extends SagaDefinition> SagaExecution<T> completeStep(String sagaId, String stepId, Object stepResult) {
        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaNotFoundException("SAGA not found: " + sagaId);
        }
        log.info("Completing step {} for SAGA {}", stepId, sagaId);
        execution.addCompletedStep(stepId, stepResult);
        executeNextStep(execution);
        return execution;
    }

    @Override
    public <T extends SagaDefinition> SagaExecution<T> failStep(String sagaId, String stepId, String reason) {
        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaNotFoundException("SAGA not found: " + sagaId);
        }
        log.warn("Failing step {} for SAGA {}. Reason: {}", stepId, sagaId, reason);
        execution.addFailedStep(stepId, reason);
        handleStepFailure(execution, reason);
        return execution;
    }

    @Override
    public <T extends SagaDefinition> SagaExecution<T> getSagaExecution(String sagaId) {
        return getSagaExecutionImpl(sagaId);
    }

    @Override
    public <T extends SagaDefinition> List<SagaExecution<T>> getActiveSagas() {
        return new ArrayList<>(activeSagas.values());
    }

    private <T extends SagaDefinition> SagaExecutionImpl<T> getSagaExecutionImpl(String sagaId) {
        return (SagaExecutionImpl<T>) activeSagas.get(sagaId);
    }

    private <T extends SagaDefinition> void executeNextStep(SagaExecutionImpl<T> execution) {
        sagaExecutorService.submit(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                if (execution.getTimeoutAt() != null && now.isAfter(execution.getTimeoutAt())) {
                    log.warn("SAGA {} timed out", execution.getSagaId());
                    execution.timeout();
                    return;
                }

                SagaStepDefinition nextStepDef = execution.getNextStepDefinition();
                if (nextStepDef != null) {
                    if (nextStepDef.getExecutionCondition() == null || nextStepDef.getExecutionCondition().test(execution.getSagaData())) {
                        SagaStepImpl nextStep = new SagaStepImpl(nextStepDef.getStepId(), nextStepDef.getStepName(), nextStepDef.getServiceEndpoint(), nextStepDef.getCompensationEndpoint());
                        execution.setCurrentStep(nextStep);
                        execution.setStatus(SagaOrchestrator.SagaStatus.RUNNING);
                        log.info("Executing step '{}' for SAGA {}", nextStepDef.getStepName(), execution.getSagaId());
                        sagaStepExecutor.executeStep(execution.getSagaDefinition(), nextStep.getStepId(), nextStep.getStepData());
                    } else {
                        log.info("Skipping step '{}' for SAGA {} due to unmet condition",
                                nextStepDef.getStepId(), execution.getSagaId());
                        execution.skipCurrentStep();
                        executeNextStep(execution); // Proceed to next step
                    }
                } else {
                    execution.complete();
                    activeSagas.remove(execution.getSagaId());
                    log.info("SAGA {} completed successfully", execution.getSagaId());
                }
            } catch (Exception e) {
                log.error("Error scheduling next step for SAGA {}", execution.getSagaId(), e);
                handleStepFailure(execution, e.getMessage());
            }
        });
    }

    private <T extends SagaDefinition> void handleStepFailure(SagaExecutionImpl<T> execution, String reason) {
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPENSATING);
        execution.setFailureReason(reason);
        log.warn("Initiating compensation for SAGA {} due to failure in step {}. Reason: {}",
                execution.getSagaId(), execution.getCurrentStepId(), reason);
        // Implement compensation logic here
        execution.compensate();
        activeSagas.remove(execution.getSagaId());
        log.info("SAGA {} compensation completed", execution.getSagaId());
    }

    public static class SagaNotFoundException extends RuntimeException {
        public SagaNotFoundException(String message) {
            super(message);
        }
    }
}
