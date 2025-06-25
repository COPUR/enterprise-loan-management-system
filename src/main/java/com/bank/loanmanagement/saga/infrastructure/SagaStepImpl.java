package com.bank.loanmanagement.saga.infrastructure;

import com.bank.loanmanagement.saga.domain.SagaDefinition;
import com.bank.loanmanagement.saga.domain.SagaStep;
import com.bank.loanmanagement.saga.domain.SagaOrchestrator;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SAGA Step Implementation
 * Represents the execution state of an individual SAGA step
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SagaStepImpl implements SagaStep {
    
    private final String stepId;
    private final String stepType;
    private final Map<String, Object> stepData;
    private final String targetServiceDomain;
    private final String behaviorQualifier;
    
    private SagaOrchestrator.SagaStepStatus status;
    private Object stepResult;
    private Exception failureReason;
    private int retryCount = 0;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private boolean compensationRequired;
    
    public SagaStepImpl(SagaDefinition.SagaStepDefinition stepDefinition) {
        this.stepId = stepDefinition.getStepId();
        this.stepType = stepDefinition.getStepType();
        this.stepData = new ConcurrentHashMap<>(stepDefinition.getStepData());
        this.targetServiceDomain = stepDefinition.getTargetServiceDomain();
        this.behaviorQualifier = stepDefinition.getBehaviorQualifier();
        this.status = SagaOrchestrator.SagaStepStatus.PENDING;
        this.compensationRequired = stepDefinition.isCompensationRequired();
        this.startedAt = OffsetDateTime.now();
    }
    
    @Override
    public String getStepId() {
        return stepId;
    }
    
    @Override
    public String getStepType() {
        return stepType;
    }
    
    @Override
    public SagaOrchestrator.SagaStepStatus getStatus() {
        return status;
    }
    
    @Override
    public Map<String, Object> getStepData() {
        return new ConcurrentHashMap<>(stepData);
    }
    
    @Override
    public Object getStepResult() {
        return stepResult;
    }
    
    @Override
    public Exception getFailureReason() {
        return failureReason;
    }
    
    @Override
    public int getRetryCount() {
        return retryCount;
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
    public boolean isCompensationRequired() {
        return compensationRequired;
    }
    
    // Implementation-specific methods
    
    public void start() {
        this.status = SagaOrchestrator.SagaStepStatus.RUNNING;
        this.startedAt = OffsetDateTime.now();
    }
    
    public void complete(Object result) {
        this.status = SagaOrchestrator.SagaStepStatus.COMPLETED;
        this.stepResult = result;
        this.completedAt = OffsetDateTime.now();
    }
    
    public void fail(Exception exception) {
        this.status = SagaOrchestrator.SagaStepStatus.FAILED;
        this.failureReason = exception;
        this.completedAt = OffsetDateTime.now();
    }
    
    public void compensate() {
        this.status = SagaOrchestrator.SagaStepStatus.COMPENSATED;
        this.completedAt = OffsetDateTime.now();
    }
    
    public void retry() {
        this.retryCount++;
        this.status = SagaOrchestrator.SagaStepStatus.RUNNING; // Changed from RETRYING to RUNNING
        this.failureReason = null;
        this.startedAt = OffsetDateTime.now();
        this.completedAt = null;
    }
    
    public String getTargetServiceDomain() {
        return targetServiceDomain;
    }
    
    public String getBehaviorQualifier() {
        return behaviorQualifier;
    }
    
    public void updateStepData(String key, Object value) {
        stepData.put(key, value);
    }
    
    // Domain interface required methods
    @Override
    public String getServiceOperationId() {
        return stepId; // Using step ID as operation ID for now
    }
    
    @Override
    public String getCompensationAction() {
        return "compensate_" + stepId; // Default compensation action
    }
    
    @Override
    public boolean isCompensationCompleted() {
        return status == SagaOrchestrator.SagaStepStatus.COMPENSATED;
    }
}