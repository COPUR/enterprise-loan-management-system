package com.bank.risk.domain.port.in;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.command.RiskEvaluationCommand;

import java.util.Optional;

public interface RiskAssessmentUseCase {
    RiskAssessment assess(RiskEvaluationCommand command);
    Optional<RiskAssessment> findByTransactionId(String transactionId);
}
