package com.bank.customer.domain.event;

import com.bank.loan.sharedkernel.domain.DomainEvent;
import com.bank.loan.sharedkernel.domain.Money;
import com.bank.loan.loan.domain.customer.CustomerId;

import java.util.Objects;

/**
 * Domain event published when credit is reserved for a customer.
 */
public class CreditReservedEvent extends DomainEvent {
    
    private final CustomerId customerId;
    private final Money reservedAmount;
    private final Money remainingCredit;
    
    public CreditReservedEvent(
        CustomerId customerId,
        Money reservedAmount,
        Money remainingCredit
    ) {
        super();
        this.customerId = Objects.requireNonNull(customerId);
        this.reservedAmount = Objects.requireNonNull(reservedAmount);
        this.remainingCredit = Objects.requireNonNull(remainingCredit);
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getReservedAmount() {
        return reservedAmount;
    }
    
    public Money getRemainingCredit() {
        return remainingCredit;
    }
    
    @Override
    public String getEventType() {
        return "CreditReserved";
    }
    
    @Override
    public String toString() {
        return "CreditReservedEvent{" +
                "customerId=" + customerId +
                ", reservedAmount=" + reservedAmount +
                ", remainingCredit=" + remainingCredit +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}