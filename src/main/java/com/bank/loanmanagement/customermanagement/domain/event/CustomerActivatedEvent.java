package com.bank.loanmanagement.customermanagement.domain.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;

import java.util.Objects;

/**
 * Domain event published when a customer account is activated.
 */
public class CustomerActivatedEvent extends DomainEvent {
    
    private final CustomerId customerId;
    
    public CustomerActivatedEvent(CustomerId customerId) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    @Override
    public String getEventType() {
        return "CustomerActivated";
    }
    
    @Override
    public String toString() {
        return "CustomerActivatedEvent{" +
                "customerId=" + customerId +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}