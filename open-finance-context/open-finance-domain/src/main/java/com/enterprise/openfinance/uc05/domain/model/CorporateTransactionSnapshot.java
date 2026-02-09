package com.enterprise.openfinance.uc05.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CorporateTransactionSnapshot(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        Instant bookingDateTime,
        String transactionCode,
        String proprietaryCode,
        String description
) {

    public CorporateTransactionSnapshot {
        if (isBlank(transactionId)) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (bookingDateTime == null) {
            throw new IllegalArgumentException("bookingDateTime is required");
        }
        if (isBlank(transactionCode)) {
            throw new IllegalArgumentException("transactionCode is required");
        }

        transactionId = transactionId.trim();
        accountId = accountId.trim();
        currency = currency.trim();
        transactionCode = transactionCode.trim();
        proprietaryCode = proprietaryCode == null ? null : proprietaryCode.trim();
        description = description == null ? null : description.trim();
    }

    public boolean isSweeping() {
        return "SWEEP".equalsIgnoreCase(transactionCode) || "ZBA".equalsIgnoreCase(proprietaryCode);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
