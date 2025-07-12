package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when Digital Dirham is received
 */
@Value
@Builder
public class DigitalDirhamReceivedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    String transactionId;
    String fromWalletId;
    String toWalletId;
    Money amount;
    String reference;
    Money newBalance;
    Instant receivedAt;
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
        return receivedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return amount.getAmount().compareTo(new java.math.BigDecimal("10000")) >= 0; // 10K AED threshold
    }
}