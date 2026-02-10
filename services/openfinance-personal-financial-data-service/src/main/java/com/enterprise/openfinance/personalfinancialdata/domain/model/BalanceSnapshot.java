package com.enterprise.openfinance.personalfinancialdata.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceSnapshot(
        String accountId,
        String balanceType,
        BigDecimal amount,
        String currency,
        Instant asOf
) {

    public BalanceSnapshot {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(balanceType)) {
            throw new IllegalArgumentException("balanceType is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (asOf == null) {
            throw new IllegalArgumentException("asOf is required");
        }

        accountId = accountId.trim();
        balanceType = balanceType.trim();
        currency = currency.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
