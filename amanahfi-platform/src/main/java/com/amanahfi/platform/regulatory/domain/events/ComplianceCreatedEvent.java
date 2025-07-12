package com.amanahfi.platform.regulatory.domain.events;

import com.amanahfi.platform.regulatory.domain.ComplianceType;
import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a new regulatory compliance record is created
 */
@Value
@Builder
public class ComplianceCreatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String complianceId;
    String entityId;
    ComplianceType complianceType;
    Jurisdiction jurisdiction;
    Instant createdAt;
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
        return createdAt;
    }
    
    @Override
    public boolean requiresShariaCompliance() {
        return complianceType == ComplianceType.SHARIA_GOVERNANCE || 
               complianceType == ComplianceType.ISLAMIC_BANKING;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true;
    }
}