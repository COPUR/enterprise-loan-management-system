package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that reserved credit has been released for a customer
 */
public class CustomerCreditReleasedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final Money releasedAmount;
    private final Instant occurredOn;
    
    public CustomerCreditReleasedEvent(CustomerId customerId, Money releasedAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.releasedAmount = releasedAmount;
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
    
    public Money getReleasedAmount() {
        return releasedAmount;
    }
}