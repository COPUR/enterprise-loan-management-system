package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Domain Event indicating that customer contact information has been updated
 */
public class CustomerContactUpdatedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final String newEmail;
    private final String newPhoneNumber;
    private final Instant occurredOn;
    
    public CustomerContactUpdatedEvent(CustomerId customerId, String newEmail, String newPhoneNumber) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.newEmail = newEmail;
        this.newPhoneNumber = newPhoneNumber;
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
    
    public String getNewEmail() {
        return newEmail;
    }
    
    public String getNewPhoneNumber() {
        return newPhoneNumber;
    }
}