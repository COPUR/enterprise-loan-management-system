package com.amanahfi.accounts.domain.account;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class MoneyTransferredEvent {
    private final String sourceAccountId;
    private final String targetAccountId;
    private final Money amount;
    private final String description;
    private final LocalDateTime timestamp;

    public MoneyTransferredEvent(String sourceAccountId, String targetAccountId, Money amount, String description) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public String getSourceAccountId() { return sourceAccountId; }
    public String getTargetAccountId() { return targetAccountId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}