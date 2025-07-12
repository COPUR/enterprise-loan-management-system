package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when Digital Dirham wallet is unfrozen
 */
@Value
@Builder
public class DigitalDirhamUnfrozenEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    String reason;
    String authorizedBy;
    Instant unfrozenAt;
    EventMetadata metadata = EventMetadata.create();
    
    @Override
    public String getAggregateId() {
        return digitalDirhamId;
    }
    
    @Override
    public String getAggregateType() {
        return "DigitalDirham";
    }
    
    @Override
    public Instant getOccurredOn() {
        return unfrozenAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true; // All unfreezing operations require regulatory reporting
    }
}