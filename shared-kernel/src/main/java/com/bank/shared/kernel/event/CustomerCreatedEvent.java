package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Shared event contract for customer creation
 */
public class CustomerCreatedEvent implements DomainEvent {
    
    private final String eventId;
    private final CustomerId customerId;
    private final String customerName;
    private final Instant occurredOn;
    
    public CustomerCreatedEvent(CustomerId customerId, String customerName) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.customerId = customerId;
        this.customerName = customerName;
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
    
    public String getCustomerName() {
        return customerName;
    }
}