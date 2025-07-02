package com.bank.loanmanagement.loan.saga.model;

import com.bank.loanmanagement.loan.saga.definition.SagaDefinition;
import com.bank.loanmanagement.loan.saga.definition.SagaStepDefinition;
import com.bank.loanmanagement.loan.saga.orchestrator.SagaOrchestrator.SagaStatus;
import com.bank.loanmanagement.loan.saga.context.SagaContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SagaExecutionImpl<T extends SagaDefinition> implements SagaExecution<T> {
    private final String sagaId;
    private final T sagaDefinition;
    private SagaStatus status;
    private SagaContext context;
    private String currentStepId;
    private LocalDateTime timeoutAt;
    private String failureReason;
    private final Map<String, Object> completedSteps;
    private final Map<String, String> failedSteps;

    public SagaExecutionImpl(String sagaId, T sagaDefinition, SagaContext context) {
        this.sagaId = sagaId;
        this.sagaDefinition = sagaDefinition;
        this.status = SagaStatus.NEW;
        this.context = context;
        this.completedSteps = new ConcurrentHashMap<>();
        this.failedSteps = new ConcurrentHashMap<>();
    }

    @Override
    public String getSagaId() {
        return sagaId;
    }

    @Override
    public T getSagaDefinition() {
        return sagaDefinition;
    }

    @Override
    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    @Override
    public SagaContext getContext() {
        return context;
    }

    @Override
    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStep(SagaStepImpl step) {
        this.currentStepId = step.getStepId();
    }

    @Override
    public LocalDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(LocalDateTime timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public Map<String, Object> getCompletedSteps() {
        return completedSteps;
    }

    @Override
    public Map<String, String> getFailedSteps() {
        return failedSteps;
    }

    public void addCompletedStep(String stepId, Object result) {
        completedSteps.put(stepId, result);
    }

    public void addFailedStep(String stepId, String reason) {
        failedSteps.put(stepId, reason);
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
    }

    public void timeout() {
        this.status = SagaStatus.TIMED_OUT;
    }

    public void compensate() {
        this.status = SagaStatus.COMPENSATING;
        // Compensation logic would go here
    }

    public Object getSagaData() {
        return context.get("sagaData", Map.class);
    }

    public SagaStepDefinition getNextStepDefinition() {
        // Placeholder for logic to determine the next step
        return null;
    }

    public void skipCurrentStep() {
        // Placeholder for skipping current step logic
    }
}
