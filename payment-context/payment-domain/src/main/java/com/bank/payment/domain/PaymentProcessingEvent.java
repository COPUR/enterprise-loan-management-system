package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Domain Event indicating that a payment is being processed
 */
public class PaymentProcessingEvent implements DomainEvent {
    
    private final String eventId;
    private final PaymentId paymentId;
    private final CustomerId customerId;
    private final Instant occurredOn;
    
    public PaymentProcessingEvent(PaymentId paymentId, CustomerId customerId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
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
    
    public PaymentId getPaymentId() {
        return paymentId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
}