package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.util.Map;

/**
 * Domain event fired when a SAGA is started
 */
public class SagaStartedEvent extends BaseDomainEvent {
    
    private final String sagaId;
    private final String sagaType;
    private final String initiatingEvent;
    private final Map<String, Object> sagaContext;
    
    public SagaStartedEvent(String aggregateId, Long version, String triggeredBy, 
                          String correlationId, String tenantId, EventMetadata metadata,
                          String sagaId, String sagaType, String initiatingEvent,
                          Map<String, Object> sagaContext) {
        super(aggregateId, version, triggeredBy, correlationId, tenantId, metadata);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.initiatingEvent = initiatingEvent;
        this.sagaContext = sagaContext;
    }
    
    @Override
    public String getEventType() {
        return "SagaStarted";
    }
    
    public String getSagaId() {
        return sagaId;
    }
    
    public String getSagaType() {
        return sagaType;
    }
    
    public String getInitiatingEvent() {
        return initiatingEvent;
    }
    
    public Map<String, Object> getSagaContext() {
        return sagaContext;
    }
}