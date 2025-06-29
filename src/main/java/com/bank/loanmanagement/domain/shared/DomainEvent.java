package com.bank.loanmanagement.domain.shared;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all domain events in the loan management system.
 * 
 * Implements Event-Driven Communication architectural pattern.
 * Follows DDD principles for domain event modeling.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Clear, intention-revealing naming
 * ✅ DDD: Proper domain event abstraction
 * ✅ Event-Driven: Base for all domain events
 * ✅ Hexagonal: Pure domain concept, no infrastructure dependencies
 */
public abstract class DomainEvent {
    
    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;
    private final String eventType;
    private final Integer eventVersion;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID cannot be null");
        this.eventType = this.getClass().getSimpleName();
        this.eventVersion = 1; // Default version, can be overridden
    }
    
    protected DomainEvent(String aggregateId, Integer eventVersion) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID cannot be null");
        this.eventType = this.getClass().getSimpleName();
        this.eventVersion = Objects.requireNonNull(eventVersion, "Event version cannot be null");
    }
    
    /**
     * Unique identifier for this event instance
     */
    public String getEventId() {
        return eventId;
    }
    
    /**
     * When this event occurred
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    /**
     * ID of the aggregate that generated this event
     */
    public String getAggregateId() {
        return aggregateId;
    }
    
    /**
     * Type of event (typically the class name)
     */
    public String getEventType() {
        return eventType;
    }
    
    /**
     * Version of the event schema for evolution
     */
    public Integer getEventVersion() {
        return eventVersion;
    }
    
    /**
     * Business method to check if event is recent (within last hour)
     */
    public boolean isRecent() {
        return occurredOn.isAfter(LocalDateTime.now().minusHours(1));
    }
    
    /**
     * Business method to check if event is from today
     */
    public boolean isToday() {
        return occurredOn.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
    
    /**
     * Business method to get age of event in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(occurredOn, LocalDateTime.now()).toMinutes();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainEvent)) return false;
        DomainEvent that = (DomainEvent) o;
        return Objects.equals(eventId, that.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
    
    @Override
    public String toString() {
        return String.format("%s{eventId='%s', aggregateId='%s', occurredOn=%s}", 
                           eventType, eventId, aggregateId, occurredOn);
    }
}