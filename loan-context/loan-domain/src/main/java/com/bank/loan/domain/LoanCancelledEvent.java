package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import java.time.Instant;

public class LoanCancelledEvent implements DomainEvent {
    private final String eventId;
    private final LoanId loanId;
    private final CustomerId customerId;
    private final String reason;
    private final Instant occurredOn;
    
    public LoanCancelledEvent(LoanId loanId, CustomerId customerId, String reason) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.loanId = loanId;
        this.customerId = customerId;
        this.reason = reason;
        this.occurredOn = Instant.now();
    }
    
    @Override
    public String getEventId() { return eventId; }
    
    @Override
    public Instant getOccurredOn() { return occurredOn; }
    
    public LoanId getLoanId() { return loanId; }
    public CustomerId getCustomerId() { return customerId; }
    public String getReason() { return reason; }
}