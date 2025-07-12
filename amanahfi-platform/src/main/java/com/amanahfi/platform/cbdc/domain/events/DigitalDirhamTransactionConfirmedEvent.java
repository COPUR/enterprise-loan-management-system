package com.amanahfi.platform.cbdc.domain.events;

import com.amanahfi.platform.cbdc.domain.TransactionStatus;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a Digital Dirham transaction is confirmed on Corda network
 */
@Value
@Builder
public class DigitalDirhamTransactionConfirmedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String digitalDirhamId;
    String transactionId;
    String cordaTransactionHash;
    TransactionStatus finalStatus;
    Instant confirmedAt;
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
        return confirmedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true; // All transaction confirmations require reporting
    }
}