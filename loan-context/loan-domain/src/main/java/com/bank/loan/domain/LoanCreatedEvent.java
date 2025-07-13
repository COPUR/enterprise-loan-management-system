package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that a new loan has been created
 */
public class LoanCreatedEvent implements DomainEvent {
    
    private final String eventId;
    private final LoanId loanId;
    private final CustomerId customerId;
    private final Money principalAmount;
    private final Instant occurredOn;
    
    public LoanCreatedEvent(LoanId loanId, CustomerId customerId, Money principalAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.loanId = loanId;
        this.customerId = customerId;
        this.principalAmount = principalAmount;
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
    
    public LoanId getLoanId() {
        return loanId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getPrincipalAmount() {
        return principalAmount;
    }
}