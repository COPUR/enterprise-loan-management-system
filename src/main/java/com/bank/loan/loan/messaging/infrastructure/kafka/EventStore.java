package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loanmanagement.loan.sharedkernel.domain.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise-grade Event Store for banking domain events
 * Provides immutable event storage for audit and regulatory compliance
 */
@Component
public class EventStore {
    
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();
    
    /**
     * Stores domain event for audit trail and regulatory compliance
     * 
     * @param event Domain event to store
     */
    public void store(DomainEvent event) {
        events.add(event);
    }
    
    /**
     * Retrieves all events for audit purposes
     * 
     * @return List of all stored events
     */
    public List<DomainEvent> getAllEvents() {
        return List.copyOf(events);
    }
    
    /**
     * Retrieves events by aggregate ID for banking entity reconstruction
     * 
     * @param aggregateId Aggregate identifier
     * @return List of events for the aggregate
     */
    public List<DomainEvent> getEventsByAggregateId(String aggregateId) {
        return events.stream()
            .filter(event -> aggregateId.equals(event.getAggregateId()))
            .toList();
    }
}