package com.amanahfi.accounts.domain.account;

import java.time.LocalDateTime;

public class AccountCreatedEvent {
    private final String accountId;
    private final String customerId;
    private final AccountType accountType;
    private final String currency;
    private final LocalDateTime timestamp;

    public AccountCreatedEvent(String accountId, String customerId, AccountType accountType, String currency) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountType = accountType;
        this.currency = currency;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public LocalDateTime getTimestamp() { return timestamp; }
}