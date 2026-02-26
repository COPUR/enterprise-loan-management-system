package com.bank.risk.application;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.command.RiskEvaluationCommand;
import com.bank.risk.domain.port.in.RiskAssessmentUseCase;
import com.bank.risk.domain.port.out.RiskAssessmentRepository;
import com.bank.risk.domain.service.RiskPolicyService;

import java.util.Optional;

public class RiskAssessmentService implements RiskAssessmentUseCase {
    private final RiskPolicyService policyService;
    private final RiskAssessmentRepository repository;

    public RiskAssessmentService(RiskPolicyService policyService, RiskAssessmentRepository repository) {
        this.policyService = policyService;
        this.repository = repository;
    }

    @Override
    public RiskAssessment assess(RiskEvaluationCommand command) {
        Optional<RiskAssessment> existing = repository.findByTransactionId(command.transactionId());
        if (existing.isPresent()) {
            return existing.get();
        }

        RiskAssessment assessment = policyService.evaluate(command);
        return repository.save(assessment);
    }

    @Override
    public Optional<RiskAssessment> findByTransactionId(String transactionId) {
        return repository.findByTransactionId(transactionId);
    }
}
