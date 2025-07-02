package com.bank.customer.domain.event;

import com.bank.loan.sharedkernel.domain.DomainEvent;
import com.bank.loan.loan.domain.customer.CustomerId;

import java.util.Objects;

/**
 * Domain event published when a customer account is suspended.
 */
public class CustomerSuspendedEvent extends DomainEvent {
    
    private final CustomerId customerId;
    private final String reason;
    
    public CustomerSuspendedEvent(CustomerId customerId, String reason) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
        this.reason = Objects.requireNonNull(reason);
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String getEventType() {
        return "CustomerSuspended";
    }
    
    @Override
    public String toString() {
        return "CustomerSuspendedEvent{" +
                "customerId=" + customerId +
                ", reason='" + reason + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}