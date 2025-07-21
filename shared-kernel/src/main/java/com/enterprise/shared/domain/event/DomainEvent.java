package com.enterprise.shared.domain.event;

import java.time.Instant;
import java.util.Map;

/**
 * Base interface for all domain events in the system.
 * This is part of the shared kernel and is used across all bounded contexts.
 */
public interface DomainEvent {
    String getAggregateId();
    String getAggregateType();
    Long getVersion();
    Instant getOccurredAt();
    String getCorrelationId();
    String getCausationId();
    Map<String, Object> getData();
}