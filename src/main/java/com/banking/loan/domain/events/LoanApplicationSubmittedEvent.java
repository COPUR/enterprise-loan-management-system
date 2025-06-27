package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when a loan application is submitted
 */
public record LoanApplicationSubmittedEvent(
    String loanId,
    Long version,
    String submittedBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    String customerId,
    java.math.BigDecimal amount,
    Integer termMonths,
    String loanType
) implements DomainEvent {

    public LoanApplicationSubmittedEvent {
        if (loanId == null || customerId == null) {
            throw new IllegalArgumentException("LoanId and CustomerId cannot be null");
        }
    }

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "LoanApplicationSubmitted";
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
        return submittedBy;
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