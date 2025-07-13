package com.amanahfi.accounts.domain.account;

import java.time.LocalDateTime;

public class StablecoinWalletCreatedEvent {
    private final String accountId;
    private final String customerId;
    private final String stablecoinType;
    private final LocalDateTime timestamp;

    public StablecoinWalletCreatedEvent(String accountId, String customerId, String stablecoinType) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.stablecoinType = stablecoinType;
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public String getStablecoinType() { return stablecoinType; }
    public LocalDateTime getTimestamp() { return timestamp; }
}