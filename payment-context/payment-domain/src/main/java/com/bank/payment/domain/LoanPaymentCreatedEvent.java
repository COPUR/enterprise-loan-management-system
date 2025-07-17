package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that a loan payment has been created
 */
public class LoanPaymentCreatedEvent implements DomainEvent {
    
    private final String eventId;
    private final PaymentId paymentId;
    private final CustomerId customerId;
    private final LoanId loanId;
    private final Money scheduledAmount;
    private final Instant occurredOn;
    
    public LoanPaymentCreatedEvent(PaymentId paymentId, CustomerId customerId, LoanId loanId, Money scheduledAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.loanId = loanId;
        this.scheduledAmount = scheduledAmount;
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
    
    public Money getScheduledAmount() {
        return scheduledAmount;
    }
    
    @Override
    public String toString() {
        return String.format("LoanPaymentCreatedEvent{paymentId=%s, customerId=%s, loanId=%s, scheduledAmount=%s, occurredOn=%s}",
                           paymentId, customerId, loanId, scheduledAmount, occurredOn);
    }
}