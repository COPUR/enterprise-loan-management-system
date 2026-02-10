package com.enterprise.openfinance.personalfinancialdata.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSnapshot(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        Instant bookingDateTime,
        Instant valueDateTime,
        String creditDebitIndicator,
        String status,
        String merchantName
) {

    public TransactionSnapshot {
        if (isBlank(transactionId)) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (bookingDateTime == null) {
            throw new IllegalArgumentException("bookingDateTime is required");
        }
        if (valueDateTime == null) {
            throw new IllegalArgumentException("valueDateTime is required");
        }
        if (isBlank(creditDebitIndicator)) {
            throw new IllegalArgumentException("creditDebitIndicator is required");
        }
        if (isBlank(status)) {
            throw new IllegalArgumentException("status is required");
        }

        transactionId = transactionId.trim();
        accountId = accountId.trim();
        currency = currency.trim();
        creditDebitIndicator = creditDebitIndicator.trim();
        status = status.trim();
        merchantName = merchantName == null ? null : merchantName.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
