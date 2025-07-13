package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Domain Event indicating that a payment has been refunded
 */
public class PaymentRefundedEvent implements DomainEvent {
    
    private final String eventId;
    private final PaymentId paymentId;
    private final CustomerId customerId;
    private final Money refundAmount;
    private final Instant occurredOn;
    
    public PaymentRefundedEvent(PaymentId paymentId, CustomerId customerId, Money refundAmount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.refundAmount = refundAmount;
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
    
    public Money getRefundAmount() {
        return refundAmount;
    }
}