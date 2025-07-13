package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;
import java.time.Instant;

public class LoanPaymentMadeEvent implements DomainEvent {
    private final String eventId;
    private final LoanId loanId;
    private final CustomerId customerId;
    private final Money paymentAmount;
    private final Money previousBalance;
    private final Money newBalance;
    private final Instant occurredOn;
    
    public LoanPaymentMadeEvent(LoanId loanId, CustomerId customerId, Money paymentAmount, 
                               Money previousBalance, Money newBalance) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.loanId = loanId;
        this.customerId = customerId;
        this.paymentAmount = paymentAmount;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.occurredOn = Instant.now();
    }
    
    @Override
    public String getEventId() { return eventId; }
    
    @Override
    public Instant getOccurredOn() { return occurredOn; }
    
    public LoanId getLoanId() { return loanId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getPaymentAmount() { return paymentAmount; }
    public Money getPreviousBalance() { return previousBalance; }
    public Money getNewBalance() { return newBalance; }
}