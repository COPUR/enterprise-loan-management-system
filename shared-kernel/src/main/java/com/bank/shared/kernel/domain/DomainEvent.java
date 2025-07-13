package com.bank.shared.kernel.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for Domain Events in Domain-Driven Design
 * 
 * Domain Events represent something that happened in the domain that
 * domain experts care about. They are used to capture side effects
 * and coordinate between bounded contexts.
 */
public interface DomainEvent {
    
    /**
     * Get the unique identifier for this event
     * @return the event identifier
     */
    default String getEventId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Get when this event occurred
     * @return the timestamp when the event occurred
     */
    default Instant getOccurredOn() {
        return Instant.now();
    }
    
    /**
     * Get the type/name of this event
     * @return the event type
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Get the version of this event schema
     * @return the event version
     */
    default int getVersion() {
        return 1;
    }
}