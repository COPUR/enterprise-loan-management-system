package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.cbdc.domain.TransferType;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a Digital Dirham transfer is initiated
 */
@Value
@Builder
public class DigitalDirhamTransferInitiatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    String transactionId;
    String fromWalletId;
    String toWalletId;
    Money amount;
    String reference;
    TransferType transferType;
    Money newBalance;
    Instant initiatedAt;
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
        return initiatedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return transferType == TransferType.CROSS_BORDER ||
               transferType == TransferType.ISLAMIC_FINANCE ||
               amount.getAmount().compareTo(new java.math.BigDecimal("10000")) >= 0; // 10K AED threshold
    }
}