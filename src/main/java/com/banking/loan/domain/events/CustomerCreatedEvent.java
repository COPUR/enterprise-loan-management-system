package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a customer is created
 */
public record CustomerCreatedEvent(
    String customerId,
    Long version,
    String createdBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    String customerName,
    String customerEmail
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "CustomerCreated";
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
        return Instant.now();
    }

    @Override
    public String getTriggeredBy() {
        return createdBy;
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