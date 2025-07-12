package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.cbdc.domain.WalletType;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a Digital Dirham wallet is created
 */
@Value
@Builder
public class DigitalDirhamCreatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    String walletId;
    WalletType walletType;
    String ownerId;
    Money initialBalance;
    String cordaNodeId;
    String notaryId;
    Instant createdAt;
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
        return createdAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return walletType == WalletType.CENTRAL_BANK || 
               walletType == WalletType.COMMERCIAL_BANK ||
               walletType == WalletType.ISLAMIC_BANK;
    }
}