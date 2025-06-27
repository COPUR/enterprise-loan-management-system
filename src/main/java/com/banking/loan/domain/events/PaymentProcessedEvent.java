package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a loan payment is processed
 */
public record PaymentProcessedEvent(
    String loanId,
    Long version,
    String paidBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    String customerId,
    BigDecimal paymentAmount,
    Integer installmentNumber,
    String paymentStatus
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "PaymentProcessed";
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