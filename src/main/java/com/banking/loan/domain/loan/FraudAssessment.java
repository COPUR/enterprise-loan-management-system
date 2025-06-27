package com.banking.loan.domain.loan;

import java.time.LocalDateTime;
import java.util.List;

public record FraudAssessment(
    String assessmentId,
    Double riskScore,
    String riskLevel,
    List<String> indicators,
    LocalDateTime assessedAt,
    String status
) {
    public static FraudAssessment pending() {
        return new FraudAssessment(
            "PENDING",
            0.0,
            "LOW",
            List.of(),
            LocalDateTime.now(),
            "PENDING"
        );
    }
    
    public static FraudAssessment fromIndicators(FraudIndicators indicators) {
        return new FraudAssessment(
            java.util.UUID.randomUUID().toString(),
            indicators.getRiskScore(),
            indicators.getRiskLevel().name(),
            indicators.getIndicators(),
            LocalDateTime.now(),
            "ASSESSED"
        );
    }
}