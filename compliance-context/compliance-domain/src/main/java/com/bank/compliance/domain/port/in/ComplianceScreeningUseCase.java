package com.bank.compliance.domain.port.in;

import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;

import java.util.Optional;

public interface ComplianceScreeningUseCase {
    ComplianceResult screen(ComplianceScreeningCommand command);
    Optional<ComplianceResult> findByTransactionId(String transactionId);
}
