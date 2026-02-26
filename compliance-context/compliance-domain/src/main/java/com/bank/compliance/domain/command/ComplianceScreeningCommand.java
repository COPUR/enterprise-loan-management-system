package com.bank.compliance.domain.command;

import java.math.BigDecimal;

public record ComplianceScreeningCommand(
        String transactionId,
        String customerId,
        BigDecimal amount,
        boolean sanctionsHit,
        boolean kycVerified,
        boolean pep
) {
    public ComplianceScreeningCommand {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
