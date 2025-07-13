package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class CbdcPaymentCreatedEvent {
    private final String paymentId;
    private final Money amount;
    private final LocalDateTime timestamp;

    public CbdcPaymentCreatedEvent(String paymentId, Money amount) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}