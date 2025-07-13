package com.amanahfi.payments.domain.payment;

import java.time.LocalDateTime;

public class PaymentProcessingStartedEvent {
    private final String paymentId;
    private final String processingReference;
    private final LocalDateTime timestamp;

    public PaymentProcessingStartedEvent(String paymentId, String processingReference) {
        this.paymentId = paymentId;
        this.processingReference = processingReference;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getProcessingReference() { return processingReference; }
    public LocalDateTime getTimestamp() { return timestamp; }
}