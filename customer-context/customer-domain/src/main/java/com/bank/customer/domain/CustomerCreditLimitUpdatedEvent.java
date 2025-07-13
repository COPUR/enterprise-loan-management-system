package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that customer credit limit has been updated
 */
public class CustomerCreditLimitUpdatedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final Money oldCreditLimit;
    private final Money newCreditLimit;
    private final Instant occurredOn;
    
    public CustomerCreditLimitUpdatedEvent(CustomerId customerId, Money oldCreditLimit, Money newCreditLimit) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.oldCreditLimit = oldCreditLimit;
        this.newCreditLimit = newCreditLimit;
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
    
    public Money getOldCreditLimit() {
        return oldCreditLimit;
    }
    
    public Money getNewCreditLimit() {
        return newCreditLimit;
    }
}