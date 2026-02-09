package com.enterprise.openfinance.uc06.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record PaymentInitiation(
        String instructionIdentification,
        String endToEndIdentification,
        String debtorAccountId,
        BigDecimal instructedAmount,
        String currency,
        String creditorAccountScheme,
        String creditorAccountIdentification,
        String creditorName,
        LocalDate requestedExecutionDate
) {
    public PaymentInitiation {
        if (isBlank(instructionIdentification)) {
            throw new IllegalArgumentException("instructionIdentification is required");
        }
        if (isBlank(endToEndIdentification)) {
            throw new IllegalArgumentException("endToEndIdentification is required");
        }
        if (isBlank(debtorAccountId)) {
            throw new IllegalArgumentException("debtorAccountId is required");
        }
        if (instructedAmount == null || instructedAmount.signum() <= 0) {
            throw new IllegalArgumentException("instructedAmount must be greater than zero");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (isBlank(creditorAccountScheme)) {
            throw new IllegalArgumentException("creditorAccountScheme is required");
        }
        if (isBlank(creditorAccountIdentification)) {
            throw new IllegalArgumentException("creditorAccountIdentification is required");
        }
        if (isBlank(creditorName)) {
            throw new IllegalArgumentException("creditorName is required");
        }

        instructionIdentification = instructionIdentification.trim();
        endToEndIdentification = endToEndIdentification.trim();
        debtorAccountId = debtorAccountId.trim();
        instructedAmount = instructedAmount.stripTrailingZeros();
        currency = currency.trim().toUpperCase(Locale.ROOT);
        creditorAccountScheme = creditorAccountScheme.trim().toUpperCase(Locale.ROOT);
        creditorAccountIdentification = creditorAccountIdentification.trim().replace(" ", "");
        creditorName = creditorName.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
