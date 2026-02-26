package com.bank.risk.domain.service;

import com.bank.risk.domain.RiskAssessment;
import com.bank.risk.domain.RiskDecision;
import com.bank.risk.domain.command.RiskEvaluationCommand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RiskPolicyService {

    public RiskAssessment evaluate(RiskEvaluationCommand command) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (command.amount().compareTo(new BigDecimal("10000")) > 0) {
            score += 30;
            reasons.add("HIGH_AMOUNT");
        }

        if (command.amount().compareTo(new BigDecimal("50000")) > 0) {
            score += 20;
            reasons.add("VERY_HIGH_AMOUNT");
        }

        if (command.highRiskCountry()) {
            score += 40;
            reasons.add("HIGH_RISK_COUNTRY");
        }

        if (command.velocityScore() >= 70) {
            score += 30;
            reasons.add("HIGH_VELOCITY");
        } else if (command.velocityScore() >= 40) {
            score += 15;
            reasons.add("MEDIUM_VELOCITY");
        }

        if (score > 100) {
            score = 100;
        }

        RiskDecision decision;
        if (score >= 80) {
            decision = RiskDecision.BLOCK;
        } else if (score >= 50) {
            decision = RiskDecision.REVIEW;
        } else {
            decision = RiskDecision.ALLOW;
        }

        return RiskAssessment.create(
                command.transactionId(),
                command.amount(),
                command.currency(),
                score,
                decision,
                reasons
        );
    }
}
