package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;

import java.time.Instant;

/**
 * Shared event contract for payment failure
 */
public class PaymentFailedEvent implements DomainEvent {
    
    private final String eventId;
    private final String paymentId;
    private final CustomerId customerId;
    private final String failureReason;
    private final Instant occurredOn;
    
    public PaymentFailedEvent(String paymentId, CustomerId customerId, String failureReason) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
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
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
}