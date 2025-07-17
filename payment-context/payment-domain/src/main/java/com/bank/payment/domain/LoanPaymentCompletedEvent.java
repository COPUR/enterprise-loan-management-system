package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that a loan payment has been completed
 */
public class LoanPaymentCompletedEvent implements DomainEvent {
    
    private final String eventId;
    private final PaymentId paymentId;
    private final CustomerId customerId;
    private final LoanId loanId;
    private final Money actualAmount;
    private final Instant occurredOn;
    
    public LoanPaymentCompletedEvent(PaymentId paymentId, CustomerId customerId, LoanId loanId, Money actualAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.loanId = loanId;
        this.actualAmount = actualAmount;
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
    
    public Money getActualAmount() {
        return actualAmount;
    }
    
    @Override
    public String toString() {
        return String.format("LoanPaymentCompletedEvent{paymentId=%s, customerId=%s, loanId=%s, actualAmount=%s, occurredOn=%s}",
                           paymentId, customerId, loanId, actualAmount, occurredOn);
    }
}