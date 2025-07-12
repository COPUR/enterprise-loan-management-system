package com.amanahfi.platform.regulatory.domain.events;

import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.regulatory.domain.ViolationSeverity;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a compliance violation is recorded
 */
@Value
@Builder
public class ComplianceViolationRecordedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String complianceId;
    String violationId;
    RegulatoryAuthority authority;
    ViolationSeverity severity;
    String description;
    Instant recordedAt;
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
        return recordedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return severity == ViolationSeverity.HIGH || severity == ViolationSeverity.CRITICAL;
    }
}