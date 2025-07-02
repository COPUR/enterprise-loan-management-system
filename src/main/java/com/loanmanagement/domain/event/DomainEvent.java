// domain/event/DomainEvent.java
package com.loanmanagement.domain.event;

import java.time.Instant;
import java.util.UUID;

public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredOn;
    private final String aggregateId;
    private final String eventType;
    private final Long version;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
        this.eventType = this.getClass().getSimpleName();
        this.version = 1L;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public Instant getOccurredOn() { return occurredOn; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public Long getVersion() { return version; }
}