package com.loanmanagement.sharedkernel.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Follows DDD principles by providing common event infrastructure.
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

    /**
     * Returns the aggregate ID that this event relates to.
     * Subclasses should implement this to return the appropriate ID.
     */
    public abstract String getAggregateId();

    /**
     * Returns the type of aggregate this event relates to.
     */
    public abstract String getAggregateType();
}
