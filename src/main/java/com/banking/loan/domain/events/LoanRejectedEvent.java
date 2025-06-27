package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event fired when a loan is rejected
 */
public record LoanRejectedEvent(
    String loanId,
    Long version,
    String rejectedBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    String customerId,
    List<String> rejectionReasons
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "LoanRejected";
    }

    @Override
    public String getAggregateId() {
        return loanId;
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
        return rejectedBy;
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