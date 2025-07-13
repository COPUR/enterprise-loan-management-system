package com.amanahfi.events.domain;

import java.time.Instant;

/**
 * Base interface for all domain events in the AmanahFi platform
 * 
 * Provides common event metadata for:
 * - Event sourcing and audit trails
 * - Regulatory compliance tracking
 * - Event replay and debugging
 * - Cross-context communication
 */
public interface DomainEvent {
    
    /**
     * Unique identifier of the aggregate that generated this event
     */
    String getAggregateId();
    
    /**
     * Type of event for routing and processing
     */
    String getEventType();
    
    /**
     * When the event occurred
     */
    Instant getTimestamp();
    
    /**
     * Event version for schema evolution
     */
    default int getVersion() {
        return 1;
    }
    
    /**
     * Correlation ID for tracing across contexts
     */
    default String getCorrelationId() {
        return null;
    }
}