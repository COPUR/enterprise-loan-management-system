package com.bank.loan.domain.model;

import java.time.LocalDateTime;

/**
 * Base interface for all domain events
 */
public interface DomainEvent {
    
    /**
     * Get the unique identifier of the event
     */
    String getEventId();
    
    /**
     * Get the timestamp when the event occurred
     */
    LocalDateTime getOccurredAt();
    
    /**
     * Get the type/name of the event
     */
    String getEventType();
    
    /**
     * Get the aggregate ID that this event relates to
     */
    String getAggregateId();
}