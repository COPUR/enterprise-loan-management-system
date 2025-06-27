package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a payment fails
 */
public record PaymentFailedEvent(
    String loanId,
    Long version,
    String customerId,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    BigDecimal attemptedAmount,
    String failureReason,
    String paymentReference
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "PaymentFailed";
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
        return customerId;
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