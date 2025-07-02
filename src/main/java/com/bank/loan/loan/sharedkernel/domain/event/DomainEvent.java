package com.bank.loan.loan.sharedkernel.domain.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base Domain Event following DDD and Event Sourcing principles
 * Provides foundation for Event-Driven Architecture (EDA)
 * Compatible with BIAN service domain event patterns
 */
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public abstract class DomainEvent {

    private final String eventId;
    private final String aggregateId;
    private final String aggregateType;
    private final OffsetDateTime occurredOn;
    private final long version;

    protected DomainEvent(String aggregateId, String aggregateType, long version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredOn = OffsetDateTime.now();
        this.version = version;
    }

    protected DomainEvent(String eventId, String aggregateId, String aggregateType, 
                         OffsetDateTime occurredOn, long version) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.occurredOn = occurredOn;
        this.version = version;
    }

    /**
     * Event type used for event store partitioning and routing
     */
    public abstract String getEventType();

    /**
     * Event data for serialization
     */
    public abstract Object getEventData();

    /**
     * BIAN service domain context
     */
    public abstract String getServiceDomain();

    /**
     * BIAN behavior qualifier associated with this event
     */
    public abstract String getBehaviorQualifier();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEvent that = (DomainEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", occurredOn=" + occurredOn +
                ", version=" + version +
                '}';
    }
}