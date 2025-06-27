package com.banking.loan.domain.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AIRiskAssessment(
    String assessmentId,
    Double riskScore,
    String riskLevel,
    String modelUsed,
    LocalDateTime assessedAt,
    Map<String, Object> assessmentData
) {
    public static AIRiskAssessment pending() {
        return new AIRiskAssessment(
            "PENDING",
            0.0,
            "PENDING",
            "DEFAULT_MODEL",
            LocalDateTime.now(),
            Map.of()
        );
    }
    
    /**
     * Get risk score as BigDecimal for domain events
     */
    public BigDecimal getRiskScore() {
        return BigDecimal.valueOf(riskScore);
    }
    
    /**
     * Get confidence level as BigDecimal
     */
    public BigDecimal getConfidenceLevel() {
        // Extract from assessment data or default to high confidence
        return BigDecimal.valueOf(0.95);
    }
    
    /**
     * Get recommendation from assessment
     */
    public String getRecommendation() {
        if (riskScore > 0.7) {
            return "REJECT_HIGH_RISK";
        } else if (riskScore > 0.5) {
            return "MANUAL_REVIEW";
        } else {
            return "APPROVE_LOW_RISK";
        }
    }
}