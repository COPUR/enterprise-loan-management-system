package com.amanahfi.payments.domain.payment;

import java.time.LocalDateTime;

public class PaymentCompletedEvent {
    private final String paymentId;
    private final String settlementReference;
    private final Long settlementTimeSeconds;
    private final LocalDateTime timestamp;

    public PaymentCompletedEvent(String paymentId, String settlementReference, Long settlementTimeSeconds) {
        this.paymentId = paymentId;
        this.settlementReference = settlementReference;
        this.settlementTimeSeconds = settlementTimeSeconds;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getSettlementReference() { return settlementReference; }
    public Long getSettlementTimeSeconds() { return settlementTimeSeconds; }
    public LocalDateTime getTimestamp() { return timestamp; }
}