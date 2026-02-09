package com.enterprise.openfinance.uc04.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StandingOrderMetadata(
        String standingOrderId,
        String accountId,
        String frequency,
        Instant firstPaymentDate,
        Instant finalPaymentDate,
        BigDecimal amount,
        String currency
) {

    public StandingOrderMetadata {
        if (isBlank(standingOrderId)) {
            throw new IllegalArgumentException("standingOrderId is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(frequency)) {
            throw new IllegalArgumentException("frequency is required");
        }
        if (firstPaymentDate == null) {
            throw new IllegalArgumentException("firstPaymentDate is required");
        }
        if (finalPaymentDate != null && finalPaymentDate.isBefore(firstPaymentDate)) {
            throw new IllegalArgumentException("finalPaymentDate must be >= firstPaymentDate");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }

        standingOrderId = standingOrderId.trim();
        accountId = accountId.trim();
        frequency = frequency.trim();
        currency = currency.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
