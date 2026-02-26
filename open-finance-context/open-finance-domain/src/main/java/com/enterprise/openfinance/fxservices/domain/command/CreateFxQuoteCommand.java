package com.enterprise.openfinance.fxservices.domain.command;

import java.math.BigDecimal;

public record CreateFxQuoteCommand(
        String tppId,
        String interactionId,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount
) {

    public CreateFxQuoteCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (!isCurrency(sourceCurrency)) {
            throw new IllegalArgumentException("sourceCurrency must be ISO-4217 alpha-3");
        }
        if (!isCurrency(targetCurrency)) {
            throw new IllegalArgumentException("targetCurrency must be ISO-4217 alpha-3");
        }
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            throw new IllegalArgumentException("sourceCurrency and targetCurrency must differ");
        }
        if (sourceAmount == null || sourceAmount.signum() <= 0) {
            throw new IllegalArgumentException("sourceAmount must be positive");
        }

        tppId = tppId.trim();
        interactionId = interactionId.trim();
        sourceCurrency = sourceCurrency.trim().toUpperCase();
        targetCurrency = targetCurrency.trim().toUpperCase();
    }

    public String pair() {
        return sourceCurrency + '-' + targetCurrency;
    }

    private static boolean isCurrency(String value) {
        return value != null && value.trim().matches("[A-Za-z]{3}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
