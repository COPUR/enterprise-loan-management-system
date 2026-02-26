package com.bank.risk.application.dto;

import com.bank.risk.domain.RiskAssessment;

import java.time.Instant;
import java.util.List;

public record RiskAssessmentResponse(
        String assessmentId,
        String transactionId,
        int score,
        String decision,
        List<String> reasons,
        Instant assessedAt
) {
    public static RiskAssessmentResponse from(RiskAssessment assessment) {
        return new RiskAssessmentResponse(
                assessment.getId().getValue(),
                assessment.getTransactionId(),
                assessment.getScore(),
                assessment.getDecision().name(),
                assessment.getReasons(),
                assessment.getAssessedAt()
        );
    }
}
