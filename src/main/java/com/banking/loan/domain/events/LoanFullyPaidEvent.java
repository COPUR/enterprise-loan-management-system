package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a loan is fully paid off
 */
public record LoanFullyPaidEvent(
    String loanId,
    Long version,
    String paidBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,   
    String customerId,
    Instant paidOffDate
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "LoanFullyPaid";
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
        return paidOffDate;
    }

    @Override
    public String getTriggeredBy() {
        return paidBy;
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