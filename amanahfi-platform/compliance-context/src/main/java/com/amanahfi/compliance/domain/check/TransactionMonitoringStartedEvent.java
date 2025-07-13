package com.amanahfi.compliance.domain.check;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class TransactionMonitoringStartedEvent {
    private final String checkId;
    private final String transactionId;
    private final Money amount;
    private final LocalDateTime timestamp;

    public TransactionMonitoringStartedEvent(String checkId, String transactionId, Money amount) {
        this.checkId = checkId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public String getTransactionId() { return transactionId; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}