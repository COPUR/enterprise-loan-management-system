package com.banking.loan.domain.loan;

import com.bank.loanmanagement.domain.loan.RiskLevel;
import java.util.List;
import java.util.Set;

/**
 * Value Object representing fraud indicators in the banking domain
 * Follows DDD principles for immutable value objects
 */
public record FraudIndicators(
    Set<String> riskFactors,
    Double riskScore,
    String riskLevel,
    List<String> detectionMethods
) {
    
    public static FraudIndicators none() {
        return new FraudIndicators(
            Set.of(),
            0.0,
            "LOW",
            List.of()
        );
    }
    
    public static FraudIndicators high(Set<String> factors, String detectionMethod) {
        return new FraudIndicators(
            factors,
            85.0,
            "HIGH",
            List.of(detectionMethod)
        );
    }
    
    public boolean isHighRisk() {
        return riskScore > 70.0 || "HIGH".equals(riskLevel);
    }
    
    public boolean requiresManualReview() {
        return riskScore > 50.0 || !riskFactors.isEmpty();
    }
    
    public RiskLevel getRiskLevel() {
        return RiskLevel.valueOf(riskLevel);
    }
    
    public Double getRiskScore() {
        return riskScore;
    }
    
    public List<String> getIndicators() {
        return List.copyOf(riskFactors);
    }
}