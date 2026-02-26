package com.bank.risk.application.dto;

import com.bank.risk.domain.command.RiskEvaluationCommand;

import java.math.BigDecimal;

public record EvaluateRiskRequest(
        String transactionId,
        BigDecimal amount,
        String currency,
        boolean highRiskCountry,
        int velocityScore
) {
    public RiskEvaluationCommand toCommand() {
        return new RiskEvaluationCommand(transactionId, amount, currency, highRiskCountry, velocityScore);
    }
}
