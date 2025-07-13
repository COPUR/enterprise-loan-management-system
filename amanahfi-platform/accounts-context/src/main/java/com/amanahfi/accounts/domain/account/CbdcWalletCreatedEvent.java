package com.amanahfi.accounts.domain.account;

import java.time.LocalDateTime;

public class CbdcWalletCreatedEvent {
    private final String accountId;
    private final String customerId;
    private final LocalDateTime timestamp;

    public CbdcWalletCreatedEvent(String accountId, String customerId) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}