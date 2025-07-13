package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.DomainEvent;
import com.bank.shared.kernel.domain.Money;

import java.time.Instant;

/**
 * Shared event contract for payment completion
 */
public class PaymentCompletedEvent implements DomainEvent {
    
    private final String eventId;
    private final String paymentId;
    private final CustomerId customerId;
    private final Money amount;
    private final Instant occurredOn;
    
    public PaymentCompletedEvent(String paymentId, CustomerId customerId, Money amount) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.amount = amount;
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
    
    public Money getAmount() {
        return amount;
    }
}