package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Domain Event indicating that a customer's credit score has been updated
 */
public class CustomerCreditScoreUpdatedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final Integer newCreditScore;
    private final Instant occurredOn;
    
    public CustomerCreditScoreUpdatedEvent(CustomerId customerId, Integer newCreditScore) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.newCreditScore = newCreditScore;
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
    
    public Integer getNewCreditScore() {
        return newCreditScore;
    }
    
    @Override
    public String toString() {
        return String.format("CustomerCreditScoreUpdatedEvent{customerId=%s, newCreditScore=%d, occurredOn=%s}", 
            customerId, newCreditScore, occurredOn);
    }
}