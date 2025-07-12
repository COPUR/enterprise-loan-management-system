package com.amanahfi.platform.regulatory.domain.events;

import com.amanahfi.platform.regulatory.domain.AssessmentResult;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a compliance assessment is performed
 */
@Value
@Builder
public class ComplianceAssessedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String complianceId;
    RegulatoryAuthority authority;
    AssessmentResult assessmentResult;
    Double score;
    Instant assessedAt;
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
        return assessedAt;
    }
    
    @Override
    public boolean requiresShariaCompliance() {
        return authority == RegulatoryAuthority.HSA;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true;
    }
}