package com.amanahfi.payments.domain.payment;

import java.time.LocalDateTime;

public class PaymentComplianceCheckedEvent {
    private final String paymentId;
    private final String checkReference;
    private final LocalDateTime timestamp;

    public PaymentComplianceCheckedEvent(String paymentId, String checkReference) {
        this.paymentId = paymentId;
        this.checkReference = checkReference;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getCheckReference() { return checkReference; }
    public LocalDateTime getTimestamp() { return timestamp; }
}