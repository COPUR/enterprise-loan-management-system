package com.bank.risk.domain.command;

import java.math.BigDecimal;

public record RiskEvaluationCommand(
        String transactionId,
        BigDecimal amount,
        String currency,
        boolean highRiskCountry,
        int velocityScore
) {
    public RiskEvaluationCommand {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (velocityScore < 0 || velocityScore > 100) {
            throw new IllegalArgumentException("velocityScore must be between 0 and 100");
        }
    }
}
