package com.amanahfi.accounts.domain.account;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class MoneyDepositedEvent {
    private final String accountId;
    private final Money amount;
    private final String description;
    private final LocalDateTime timestamp;

    public MoneyDepositedEvent(String accountId, Money amount, String description) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}