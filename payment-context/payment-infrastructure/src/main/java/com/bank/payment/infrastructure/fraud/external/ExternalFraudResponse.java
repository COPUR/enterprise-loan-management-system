package com.bank.payment.infrastructure.fraud.external;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response model from external fraud detection services
 */
@Data
@Builder
public class ExternalFraudResponse {
    
    private String transactionId;
    private Integer riskScore; // 0-100 scale
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Boolean isBlocked;
    private Boolean requiresAdditionalAuth;
    private List<String> reasons;
    private List<RuleMatch> triggeredRules;
    private String recommendation; // APPROVE, DECLINE, REVIEW, AUTH_REQUIRED
    private Long processingTime; // milliseconds
    private String modelVersion;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalData;

    @Data
    @Builder
    public static class RuleMatch {
        private String ruleId;
        private String ruleName;
        private String ruleType;
        private Integer score;
        private String description;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    }
}