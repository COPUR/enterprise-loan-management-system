package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.util.Map;

/**
 * Domain event fired when a SAGA fails
 */
public class SagaFailedEvent extends BaseDomainEvent {
    
    private final String sagaId;
    private final String sagaType;
    private final String failureReason;
    private final String failedStep;
    private final Exception exception;
    private final Map<String, Object> sagaContext;
    
    public SagaFailedEvent(String aggregateId, Long version, String triggeredBy, 
                         String correlationId, String tenantId, EventMetadata metadata,
                         String sagaId, String sagaType, String failureReason,
                         String failedStep, Exception exception, Map<String, Object> sagaContext) {
        super(aggregateId, version, triggeredBy, correlationId, tenantId, metadata);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.failureReason = failureReason;
        this.failedStep = failedStep;
        this.exception = exception;
        this.sagaContext = sagaContext;
    }
    
    @Override
    public String getEventType() {
        return "SagaFailed";
    }
    
    public String getSagaId() {
        return sagaId;
    }
    
    public String getSagaType() {
        return sagaType;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public String getFailedStep() {
        return failedStep;
    }
    
    public Exception getException() {
        return exception;
    }
    
    public Map<String, Object> getSagaContext() {
        return sagaContext;
    }
}