package com.bank.compliance.application;

import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;
import com.bank.compliance.domain.port.in.ComplianceScreeningUseCase;
import com.bank.compliance.domain.port.out.ComplianceResultRepository;
import com.bank.compliance.domain.service.ComplianceRuleService;

import java.util.Optional;

public class ComplianceScreeningService implements ComplianceScreeningUseCase {
    private final ComplianceRuleService ruleService;
    private final ComplianceResultRepository repository;

    public ComplianceScreeningService(ComplianceRuleService ruleService, ComplianceResultRepository repository) {
        this.ruleService = ruleService;
        this.repository = repository;
    }

    @Override
    public ComplianceResult screen(ComplianceScreeningCommand command) {
        Optional<ComplianceResult> existing = repository.findByTransactionId(command.transactionId());
        if (existing.isPresent()) {
            return existing.get();
        }

        ComplianceResult result = ruleService.screen(command);
        return repository.save(result);
    }

    @Override
    public Optional<ComplianceResult> findByTransactionId(String transactionId) {
        return repository.findByTransactionId(transactionId);
    }
}
