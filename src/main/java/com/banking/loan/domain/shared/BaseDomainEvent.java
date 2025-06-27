package com.banking.loan.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base implementation of DomainEvent
 */
public abstract class BaseDomainEvent implements DomainEvent {
    protected final UUID eventId;
    protected final String aggregateId;
    protected final Long aggregateVersion;
    protected final Instant occurredOn;
    protected final String triggeredBy;
    protected final String correlationId;
    protected final String tenantId;
    protected final EventMetadata metadata;
    
    protected BaseDomainEvent(String aggregateId, Long aggregateVersion, String triggeredBy, 
                            String correlationId, String tenantId, EventMetadata metadata) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredOn = Instant.now();
        this.triggeredBy = triggeredBy;
        this.correlationId = correlationId;
        this.tenantId = tenantId;
        this.metadata = metadata != null ? metadata : EventMetadata.empty();
    }
    
    @Override
    public UUID getEventId() { return eventId; }
    
    @Override
    public String getAggregateId() { return aggregateId; }
    
    @Override
    public Long getAggregateVersion() { return aggregateVersion; }
    
    @Override
    public Instant getOccurredOn() { return occurredOn; }
    
    @Override
    public String getTriggeredBy() { return triggeredBy; }
    
    @Override
    public String getCorrelationId() { return correlationId; }
    
    @Override
    public String getTenantId() { return tenantId; }
    
    @Override
    public EventMetadata getMetadata() { return metadata; }
}