package com.bank.infrastructure.event;

import com.bank.shared.kernel.domain.DomainEvent;

import java.util.List;

/**
 * Event Store interface for Event Sourcing capabilities
 * 
 * Implements Event-Driven Architecture (EDA) with persistent event storage
 * Following CQRS pattern for read/write separation
 */
public interface EventStore {
    
    /**
     * Store a domain event
     */
    void store(DomainEvent event);
    
    /**
     * Store multiple domain events atomically
     */
    void store(List<DomainEvent> events);
    
    /**
     * Get all events for a specific aggregate
     */
    List<DomainEvent> getEvents(String aggregateId);
    
    /**
     * Get events for an aggregate from a specific version
     */
    List<DomainEvent> getEventsFromVersion(String aggregateId, Long version);
    
    /**
     * Get all events of a specific type
     */
    List<DomainEvent> getEventsByType(Class<? extends DomainEvent> eventType);
    
    /**
     * Get events within a time range
     */
    List<DomainEvent> getEventsByTimeRange(java.time.Instant from, java.time.Instant to);
}