package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class StablecoinPaymentCreatedEvent {
    private final String paymentId;
    private final String stablecoinType;
    private final Money amount;
    private final LocalDateTime timestamp;

    public StablecoinPaymentCreatedEvent(String paymentId, String stablecoinType, Money amount) {
        this.paymentId = paymentId;
        this.stablecoinType = stablecoinType;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getStablecoinType() { return stablecoinType; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}