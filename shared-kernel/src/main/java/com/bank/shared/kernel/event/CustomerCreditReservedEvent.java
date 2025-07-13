package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Shared event contract for customer credit reservation
 * 
 * Located in shared kernel to enable cross-context communication
 * without violating bounded context boundaries
 */
public class CustomerCreditReservedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final Money reservedAmount;
    private final Instant occurredOn;
    
    public CustomerCreditReservedEvent(CustomerId customerId, Money reservedAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.reservedAmount = reservedAmount;
        this.occurredOn = Instant.now();
    }
    
    @Override
    public String getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getReservedAmount() {
        return reservedAmount;
    }
}