package com.bank.customer.domain.event;

import com.bank.loan.sharedkernel.domain.DomainEvent;
import com.bank.loan.sharedkernel.domain.Money;
import com.bank.loan.loan.domain.customer.CustomerId;

import java.util.Objects;

/**
 * Domain event published when previously reserved credit is released.
 */
public class CreditReleasedEvent extends DomainEvent {
    
    private final CustomerId customerId;
    private final Money releasedAmount;
    private final Money availableCredit;
    
    public CreditReleasedEvent(
        CustomerId customerId,
        Money releasedAmount,
        Money availableCredit
    ) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
        this.releasedAmount = Objects.requireNonNull(releasedAmount);
        this.availableCredit = Objects.requireNonNull(availableCredit);
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getReleasedAmount() {
        return releasedAmount;
    }
    
    public Money getAvailableCredit() {
        return availableCredit;
    }
    
    @Override
    public String getEventType() {
        return "CreditReleased";
    }
    
    @Override
    public String toString() {
        return "CreditReleasedEvent{" +
                "customerId=" + customerId +
                ", releasedAmount=" + releasedAmount +
                ", availableCredit=" + availableCredit +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}