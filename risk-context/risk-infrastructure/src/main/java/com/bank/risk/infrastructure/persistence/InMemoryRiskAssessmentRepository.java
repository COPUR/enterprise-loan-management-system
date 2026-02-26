package com.bank.risk.infrastructure.persistence;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.port.out.RiskAssessmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRiskAssessmentRepository implements RiskAssessmentRepository {
    private final Map<String, RiskAssessment> byTransactionId = new ConcurrentHashMap<>();

    @Override
    public RiskAssessment save(RiskAssessment assessment) {
        byTransactionId.put(assessment.getTransactionId(), assessment);
        return assessment;
    }

    @Override
    public Optional<RiskAssessment> findByTransactionId(String transactionId) {
        return Optional.ofNullable(byTransactionId.get(transactionId));
    }
}
