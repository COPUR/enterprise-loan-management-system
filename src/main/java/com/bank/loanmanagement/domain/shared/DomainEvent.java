package com.bank.loanmanagement.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class DomainEvent {
    
    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public abstract String getEventType();
}