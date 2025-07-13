package com.amanahfi.accounts.domain.account;

import java.time.LocalDateTime;

public class AccountUnfrozenEvent {
    private final String accountId;
    private final String reason;
    private final LocalDateTime timestamp;

    public AccountUnfrozenEvent(String accountId, String reason) {
        this.accountId = accountId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}