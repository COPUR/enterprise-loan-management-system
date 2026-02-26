package com.enterprise.openfinance.corporatetreasury.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record CorporateBalanceSnapshot(
        String accountId,
        String balanceType,
        BigDecimal amount,
        String currency,
        Instant asOf
) {

    public CorporateBalanceSnapshot {
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

    public String formattedAmount() {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
