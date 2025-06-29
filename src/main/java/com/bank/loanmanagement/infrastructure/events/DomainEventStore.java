package com.bank.loanmanagement.infrastructure.events;

import com.bank.loanmanagement.domain.shared.DomainEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for storing and retrieving domain events.
 * 
 * Enables Event Sourcing capabilities and audit trail functionality.
 * Part of the Event-Driven Communication infrastructure.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Clear interface with focused responsibility
 * ✅ Hexagonal: Port for event storage (infrastructure concern)
 * ✅ Event-Driven: Core component for event sourcing
 * ✅ DDD: Infrastructure service for domain event persistence
 */
public interface DomainEventStore {
    
    /**
     * Store a domain event for event sourcing and audit purposes
     */
    void store(DomainEvent event);
    
    /**
     * Retrieve all events for a specific aggregate
     */
    List<DomainEvent> getEvents(String aggregateId);
    
    /**
     * Retrieve events that occurred since a specific time
     */
    List<DomainEvent> getEventsSince(LocalDateTime since);
    
    /**
     * Retrieve events of a specific type
     */
    List<DomainEvent> getEventsByType(String eventType);
    
    /**
     * Retrieve events for a specific aggregate since a given time
     */
    List<DomainEvent> getEventsForAggregateSince(String aggregateId, LocalDateTime since);
    
    /**
     * Get the total number of stored events
     */
    long getTotalEventCount();
    
    /**
     * Get the number of events stored today
     */
    long getEventCountForToday();
    
    /**
     * Get the timestamp of the last stored event
     */
    LocalDateTime getLastEventTime();
    
    /**
     * Business method to check if events exist for an aggregate
     */
    boolean hasEventsForAggregate(String aggregateId);
    
    /**
     * Business method to get event count for a specific aggregate
     */
    long getEventCountForAggregate(String aggregateId);
    
    /**
     * Business method to get the last event for an aggregate
     */
    DomainEvent getLastEventForAggregate(String aggregateId);
}