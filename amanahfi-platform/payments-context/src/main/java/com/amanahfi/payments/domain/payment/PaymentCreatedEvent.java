package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class PaymentCreatedEvent {
    private final String paymentId;
    private final String fromAccountId;
    private final String toAccountId;
    private final Money amount;
    private final LocalDateTime timestamp;

    public PaymentCreatedEvent(String paymentId, String fromAccountId, String toAccountId, Money amount) {
        this.paymentId = paymentId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}