package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that credit has been reserved for a customer
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