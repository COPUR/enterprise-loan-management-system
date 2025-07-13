package com.amanahfi.payments.domain.payment;

import java.time.LocalDateTime;

public class PaymentMarkedIslamicCompliantEvent {
    private final String paymentId;
    private final LocalDateTime timestamp;

    public PaymentMarkedIslamicCompliantEvent(String paymentId) {
        this.paymentId = paymentId;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}