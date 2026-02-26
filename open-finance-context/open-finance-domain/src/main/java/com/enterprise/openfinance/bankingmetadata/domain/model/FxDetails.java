package com.enterprise.openfinance.bankingmetadata.domain.model;

import java.math.BigDecimal;

public record FxDetails(
        BigDecimal exchangeRate,
        String originalCurrency,
        String targetCurrency
) {

    public FxDetails {
        if (exchangeRate == null || exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("exchangeRate must be positive");
        }
        if (isBlank(originalCurrency)) {
            throw new IllegalArgumentException("originalCurrency is required");
        }
        if (isBlank(targetCurrency)) {
            throw new IllegalArgumentException("targetCurrency is required");
        }

        originalCurrency = originalCurrency.trim();
        targetCurrency = targetCurrency.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
