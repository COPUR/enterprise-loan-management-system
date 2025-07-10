package com.loanmanagement.shared.domain;

import java.time.LocalDateTime;

/**
 * Base interface for domain events
 */
public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredOn();
    String getAggregateId();
    String getEventType();
}