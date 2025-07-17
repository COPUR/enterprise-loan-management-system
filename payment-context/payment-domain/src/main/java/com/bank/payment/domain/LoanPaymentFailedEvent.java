package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Domain Event indicating that a loan payment has failed
 */
public class LoanPaymentFailedEvent implements DomainEvent {
    
    private final String eventId;
    private final PaymentId paymentId;
    private final CustomerId customerId;
    private final LoanId loanId;
    private final String failureReason;
    private final Instant occurredOn;
    
    public LoanPaymentFailedEvent(PaymentId paymentId, CustomerId customerId, LoanId loanId, String failureReason) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.loanId = loanId;
        this.failureReason = failureReason;
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
    
    public PaymentId getPaymentId() {
        return paymentId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public LoanId getLoanId() {
        return loanId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    @Override
    public String toString() {
        return String.format("LoanPaymentFailedEvent{paymentId=%s, customerId=%s, loanId=%s, failureReason='%s', occurredOn=%s}",
                           paymentId, customerId, loanId, failureReason, occurredOn);
    }
}