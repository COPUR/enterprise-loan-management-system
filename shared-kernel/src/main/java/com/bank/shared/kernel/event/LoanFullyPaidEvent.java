package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Shared event contract for loan fully paid
 */
public class LoanFullyPaidEvent implements DomainEvent {
    
    private final String eventId;
    private final String loanId;
    private final CustomerId customerId;
    private final Instant occurredOn;
    
    public LoanFullyPaidEvent(String loanId, CustomerId customerId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.loanId = loanId;
        this.customerId = customerId;
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
    
    public String getLoanId() {
        return loanId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
}