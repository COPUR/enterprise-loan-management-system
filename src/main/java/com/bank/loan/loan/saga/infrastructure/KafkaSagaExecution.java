package com.bank.loanmanagement.loan.saga.infrastructure;

import com.bank.loanmanagement.loan.saga.domain.SagaDefinition;
import com.bank.loanmanagement.loan.saga.domain.SagaExecution;
import com.bank.loanmanagement.loan.saga.domain.SagaStep;
import com.bank.loanmanagement.loan.saga.domain.SagaOrchestrator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise-grade SAGA Execution context
 * Manages execution state for distributed banking transactions
 * 
 * Ensures financial transaction integrity and audit compliance
 */
public class KafkaSagaExecution<T extends SagaDefinition> implements SagaExecution<T> {
    
    private final String sagaId;
    private final T sagaDefinition;
    private String currentStepId;
    private final Map<String, Object> sagaData;
    private SagaOrchestrator.SagaStatus status;
    private final OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime timeoutAt;
    private int retryCount;
    private String failureReason;
    
    // Kafka-specific fields
    private final Set<String> completedSteps = ConcurrentHashMap.newKeySet();
    private final Set<String> skippedSteps = ConcurrentHashMap.newKeySet();
    private final Set<String> failedSteps = ConcurrentHashMap.newKeySet();
    
    // FAPI security context
    private final String fapiInteractionId;
    private final String clientId;
    private final String correlationId;
    
    public KafkaSagaExecution(String sagaId, T sagaDefinition, SagaOrchestrator.SagaStatus status,
                             Map<String, Object> initialData, String fapiInteractionId, 
                             String clientId, String correlationId) {
        this.sagaId = sagaId;
        this.sagaDefinition = sagaDefinition;
        this.sagaData = new ConcurrentHashMap<>(initialData != null ? initialData : new HashMap<>());
        this.status = status;
        this.startedAt = OffsetDateTime.now();
        this.retryCount = 0;
        this.fapiInteractionId = fapiInteractionId;
        this.clientId = clientId;
        this.correlationId = correlationId;
        
        // Set timeout if defined in SAGA definition
        if (sagaDefinition != null && sagaDefinition.getTimeout() != null) {
            this.timeoutAt = startedAt.plus(sagaDefinition.getTimeout());
        }
    }
    
    // SagaExecution interface implementation
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
        return currentStepId;
    }
    
    @Override
    public List<SagaStep> getCompletedSteps() {
        // For Kafka implementation, we only track step IDs, not full step objects
        // This would need to be enhanced in a full implementation
        return new ArrayList<>(); // Simplified for now
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
    
    // Kafka-specific implementation methods
    public void setCurrentStep(String stepId) {
        this.currentStepId = stepId;
    }
    
    public void setStatus(SagaOrchestrator.SagaStatus status) {
        this.status = status;
    }
    
    public void completeStep(String stepId, Object stepResult) {
        completedSteps.add(stepId);
        sagaData.put(stepId + "_result", stepResult);
    }
    
    public void failStep(String stepId, Exception failure) {
        failedSteps.add(stepId);
        sagaData.put(stepId + "_failure", failure.getMessage());
    }
    
    public void skipStep(String stepId) {
        skippedSteps.add(stepId);
    }
    
    public Set<String> getCompletedStepIds() {
        return new HashSet<>(completedSteps);
    }
    
    public Set<String> getSkippedSteps() {
        return new HashSet<>(skippedSteps);
    }
    
    public void putSagaData(String key, Object value) {
        this.sagaData.put(key, value);
    }
    
    public Object getSagaData(String key) {
        return this.sagaData.get(key);
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public OffsetDateTime getTimeoutAt() {
        return timeoutAt;
    }
    
    public void timeout() {
        this.status = SagaOrchestrator.SagaStatus.TIMEOUT;
        this.failureReason = "SAGA execution timed out";
        this.completedAt = OffsetDateTime.now();
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
    
    /**
     * Checks if SAGA execution has timed out
     * Important for banking transaction timeouts
     */
    public boolean isTimedOut() {
        return timeoutAt != null && OffsetDateTime.now().isAfter(timeoutAt);
    }
    
    /**
     * Checks if SAGA can be retried based on retry policy
     */
    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries;
    }
}