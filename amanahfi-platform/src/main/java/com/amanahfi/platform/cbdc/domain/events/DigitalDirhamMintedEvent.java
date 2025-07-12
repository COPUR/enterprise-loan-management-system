package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when Digital Dirham is minted by Central Bank
 */
@Value
@Builder
public class DigitalDirhamMintedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    Money amount;
    String authorization;
    Money newBalance;
    Instant mintedAt;
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
        return mintedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true; // All minting operations require mandatory reporting
    }
}