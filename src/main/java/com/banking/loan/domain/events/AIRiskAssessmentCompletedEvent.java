package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.DomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when AI risk assessment is completed
 */
public record AIRiskAssessmentCompletedEvent(
    String loanId,
    Long version,
    String assessedBy,
    String correlationId,
    String tenantId,
    EventMetadata metadata,
    BigDecimal riskScore,
    BigDecimal confidenceLevel,
    String recommendation
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    @Override
    public String getEventType() {
        return "AIRiskAssessmentCompleted";
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
        return assessedBy;
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