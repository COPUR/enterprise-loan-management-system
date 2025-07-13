package com.amanahfi.accounts.domain.account;

import java.time.LocalDateTime;

public class AccountMarkedIslamicCompliantEvent {
    private final String accountId;
    private final LocalDateTime timestamp;

    public AccountMarkedIslamicCompliantEvent(String accountId) {
        this.accountId = accountId;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}