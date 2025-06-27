package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a customer is blocked
 */
public record CustomerBlockedEvent(
    String customerId,
    Long version,
    String blockedBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    String blockReason,
    Instant blockedDate
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "CustomerBlocked";
    }

    @Override
    public String getAggregateId() {
        return customerId;
    }

    @Override
    public Long getAggregateVersion() {
        return version;
    }

    @Override
    public Instant getOccurredOn() {
        return blockedDate;
    }

    @Override
    public String getTriggeredBy() {
        return blockedBy;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public EventMetadata getMetadata() {
        return metadata;
    }
}