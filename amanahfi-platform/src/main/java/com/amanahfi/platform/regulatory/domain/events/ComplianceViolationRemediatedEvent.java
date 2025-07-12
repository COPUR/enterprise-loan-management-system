package com.amanahfi.platform.regulatory.domain.events;

import com.amanahfi.platform.regulatory.domain.RemediationDetails;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a compliance violation is remediated
 */
@Value
@Builder
public class ComplianceViolationRemediatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String complianceId;
    String violationId;
    RemediationDetails remediationDetails;
    Instant remediatedAt;
    EventMetadata metadata = EventMetadata.create();
    
    @Override
    public String getAggregateId() {
        return complianceId;
    }
    
    @Override
    public String getAggregateType() {
        return "RegulatoryCompliance";
    }
    
    @Override
    public Instant getOccurredOn() {
        return remediatedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return remediationDetails.isRegulatoryNotificationRequired();
    }
}