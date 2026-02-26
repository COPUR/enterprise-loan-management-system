package com.bank.compliance.application.dto;

import com.bank.compliance.domain.command.ComplianceScreeningCommand;

import java.math.BigDecimal;

public record ComplianceScreeningRequest(
        String transactionId,
        String customerId,
        BigDecimal amount,
        boolean sanctionsHit,
        boolean kycVerified,
        boolean pep
) {
    public ComplianceScreeningCommand toCommand() {
        return new ComplianceScreeningCommand(
                transactionId,
                customerId,
                amount,
                sanctionsHit,
                kycVerified,
                pep
        );
    }
}
