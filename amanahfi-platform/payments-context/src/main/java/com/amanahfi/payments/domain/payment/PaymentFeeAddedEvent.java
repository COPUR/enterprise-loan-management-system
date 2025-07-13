package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class PaymentFeeAddedEvent {
    private final String paymentId;
    private final PaymentFeeType feeType;
    private final Money amount;
    private final LocalDateTime timestamp;

    public PaymentFeeAddedEvent(String paymentId, PaymentFeeType feeType, Money amount) {
        this.paymentId = paymentId;
        this.feeType = feeType;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public PaymentFeeType getFeeType() { return feeType; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}