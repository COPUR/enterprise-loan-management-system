package com.amanahfi.compliance.domain.check;

import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compliance Report Value Object
 */
@Value
public class ComplianceReport {
    String checkId;
    int complianceScore;
    String riskAssessment;
    List<String> recommendations;
    LocalDateTime generatedAt;
    
    public static ComplianceReport generate(ComplianceCheck check) {
        return new ComplianceReport(
            check.getCheckId(),
            check.calculateComplianceScore(),
            generateRiskAssessment(check),
            generateRecommendations(check),
            LocalDateTime.now()
        );
    }
    
    private static String generateRiskAssessment(ComplianceCheck check) {
        if (check.getFinalRiskScore() == RiskScore.LOW) {
            return "Low risk profile - standard monitoring recommended";
        } else if (check.getFinalRiskScore() == RiskScore.MEDIUM) {
            return "Medium risk profile - enhanced monitoring required";
        } else {
            return "High risk profile - intensive monitoring and regular review required";
        }
    }
    
    private static List<String> generateRecommendations(ComplianceCheck check) {
        return List.of(
            "Continue routine monitoring",
            "Update customer risk profile annually",
            "Ensure documentation remains current"
        );
    }
}