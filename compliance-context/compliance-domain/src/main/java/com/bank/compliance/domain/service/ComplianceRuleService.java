package com.bank.compliance.domain.service;

import com.bank.compliance.domain.ComplianceDecision;
import com.bank.compliance.domain.ComplianceResult;
import com.bank.compliance.domain.command.ComplianceScreeningCommand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ComplianceRuleService {

    public ComplianceResult screen(ComplianceScreeningCommand command) {
        List<String> reasons = new ArrayList<>();

        if (command.sanctionsHit()) {
            reasons.add("SANCTIONS_HIT");
            return ComplianceResult.create(
                    command.transactionId(),
                    command.customerId(),
                    ComplianceDecision.FAIL,
                    reasons
            );
        }

        if (!command.kycVerified()) {
            reasons.add("KYC_NOT_VERIFIED");
            return ComplianceResult.create(
                    command.transactionId(),
                    command.customerId(),
                    ComplianceDecision.FAIL,
                    reasons
            );
        }

        if (command.pep() && command.amount().compareTo(new BigDecimal("10000")) > 0) {
            reasons.add("PEP_HIGH_VALUE_REVIEW");
            return ComplianceResult.create(
                    command.transactionId(),
                    command.customerId(),
                    ComplianceDecision.REVIEW,
                    reasons
            );
        }

        reasons.add("COMPLIANT");
        return ComplianceResult.create(
                command.transactionId(),
                command.customerId(),
                ComplianceDecision.PASS,
                reasons
        );
    }
}
