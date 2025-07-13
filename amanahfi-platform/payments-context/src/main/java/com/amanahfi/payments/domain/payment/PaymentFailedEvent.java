package com.amanahfi.payments.domain.payment;

import java.time.LocalDateTime;

public class PaymentFailedEvent {
    private final String paymentId;
    private final String reason;
    private final LocalDateTime timestamp;

    public PaymentFailedEvent(String paymentId, String reason) {
        this.paymentId = paymentId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}