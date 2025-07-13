package com.bank.payment.infrastructure.fraud.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response containing updated customer risk profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRiskProfileResponse {
    
    private String customerId;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Integer riskScore; // 0-100
    private LocalDateTime lastUpdated;
    private List<RiskFactor> riskFactors;
    private RiskTrends trends;
    private String nextReviewDate;

    public CustomerRiskProfileResponse(String customerId, String riskLevel, Integer riskScore, LocalDateTime lastUpdated) {
        this.customerId = customerId;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.lastUpdated = lastUpdated;
    }

    @Data
    public static class RiskFactor {
        private String factorType;
        private String description;
        private Integer impact; // 1-10 scale
        private String category;
    }

    @Data
    public static class RiskTrends {
        private String trend; // INCREASING, DECREASING, STABLE
        private Integer changePercent;
        private String timeframe;
        private String reason;
    }
}