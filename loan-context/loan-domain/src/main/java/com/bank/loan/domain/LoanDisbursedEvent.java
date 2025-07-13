package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;
import java.time.Instant;
import java.time.LocalDate;

public class LoanDisbursedEvent implements DomainEvent {
    private final String eventId;
    private final LoanId loanId;
    private final CustomerId customerId;
    private final Money principalAmount;
    private final LocalDate disbursementDate;
    private final Instant occurredOn;
    
    public LoanDisbursedEvent(LoanId loanId, CustomerId customerId, Money principalAmount, LocalDate disbursementDate) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.loanId = loanId;
        this.customerId = customerId;
        this.principalAmount = principalAmount;
        this.disbursementDate = disbursementDate;
        this.occurredOn = Instant.now();
    }
    
    @Override
    public String getEventId() { return eventId; }
    
    @Override
    public Instant getOccurredOn() { return occurredOn; }
    
    public LoanId getLoanId() { return loanId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getPrincipalAmount() { return principalAmount; }
    public LocalDate getDisbursementDate() { return disbursementDate; }
}