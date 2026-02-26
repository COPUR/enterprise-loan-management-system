package com.bank.risk.domain.port.out;

import com.bank.risk.domain.RiskAssessment;

import java.util.Optional;

public interface RiskAssessmentRepository {
    RiskAssessment save(RiskAssessment assessment);
    Optional<RiskAssessment> findByTransactionId(String transactionId);
}
