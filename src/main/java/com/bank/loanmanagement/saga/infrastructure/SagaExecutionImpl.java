package com.bank.loanmanagement.saga.infrastructure;

import com.bank.loanmanagement.saga.domain.SagaDefinition;
import com.bank.loanmanagement.saga.domain.SagaExecution;
import com.bank.loanmanagement.saga.domain.SagaStep;
import com.bank.loanmanagement.saga.domain.SagaOrchestrator;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SAGA Execution Implementation
 * Tracks the state and progress of a SAGA execution
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SagaExecutionImpl<T extends SagaDefinition> implements SagaExecution<T> {
    
    private final String sagaId;
    private final T sagaDefinition;
    private SagaOrchestrator.SagaStatus status;
    private final Map<String, Object> sagaData;
    private final String fapiInteractionId;
    private final String clientId;
    private final String correlationId;
    
    private SagaStepImpl currentStep;
    private final List<SagaStepImpl> completedSteps = new ArrayList<>();
    private final List<SagaStepImpl> failedSteps = new ArrayList<>();
    
    private final OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime timeoutAt;
    private String failureReason;
    private int retryCount = 0;
    
    public SagaExecutionImpl(String sagaId, T sagaDefinition, SagaOrchestrator.SagaStatus status, 
                           Map<String, Object> initialData, String fapiInteractionId, 
                           String clientId, String correlationId) {
        this.sagaId = sagaId;
        this.sagaDefinition = sagaDefinition;
        this.status = status;
        this.sagaData = new ConcurrentHashMap<>(initialData);
        this.fapiInteractionId = fapiInteractionId;
        this.clientId = clientId;
        this.correlationId = correlationId;
        this.startedAt = OffsetDateTime.now();
        
        // Set timeout if defined in SAGA definition
        if (sagaDefinition != null && sagaDefinition.getTimeout() != null) {
            this.timeoutAt = startedAt.plus(sagaDefinition.getTimeout());
        }
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
    public SagaOrchestrator.SagaStatus getStatus() {
        return status;
    }
    
    @Override
    public Map<String, Object> getSagaData() {
        return new ConcurrentHashMap<>(sagaData);
    }
    
    @Override
    public String getCurrentStepId() {
        return currentStep != null ? currentStep.getStepId() : null;
    }
    
    @Override
    public List<SagaStep> getCompletedSteps() {
        return new ArrayList<>(completedSteps);
    }
    
    @Override
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }
    
    @Override
    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
    
    @Override
    public String getFailureReason() {
        return failureReason;
    }
    
    @Override
    public int getRetryCount() {
        return retryCount;
    }
    
    // Implementation-specific methods
    
    public SagaStepImpl getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(SagaStepImpl currentStep) {
        this.currentStep = currentStep;
    }
    
    public void addCompletedStep(SagaStepImpl step) {
        completedSteps.add(step);
        currentStep = null;
    }
    
    public void addFailedStep(SagaStepImpl step) {
        failedSteps.add(step);
        currentStep = null;
    }
    
    public List<SagaStepImpl> getFailedSteps() {
        return new ArrayList<>(failedSteps);
    }
    
    public void updateSagaData(String key, Object value) {
        sagaData.put(key, value);
    }
    
    public void setStatus(SagaOrchestrator.SagaStatus status) {
        this.status = status;
    }
    
    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    public void timeout() {
        this.status = SagaOrchestrator.SagaStatus.TIMEOUT;
        this.failureReason = "SAGA execution timed out";
        this.completedAt = OffsetDateTime.now();
    }
    
    public OffsetDateTime getTimeoutAt() {
        return timeoutAt;
    }
    
    public String getFapiInteractionId() {
        return fapiInteractionId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
}