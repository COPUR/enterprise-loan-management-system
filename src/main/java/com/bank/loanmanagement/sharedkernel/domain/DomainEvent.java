package com.bank.loanmanagement.sharedkernel.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Domain events represent important business occurrences within aggregates.
 */
public abstract class DomainEvent {
    
    private final String eventId;
    private final LocalDateTime occurredOn;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    public abstract String getEventType();
    
    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", occurredOn=" + occurredOn +
                '}';
    }
}