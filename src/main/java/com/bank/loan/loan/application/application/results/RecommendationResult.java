package com.bank.loanmanagement.loan.application.results;

import java.util.List;
import java.util.Map;

/**
 * Result containing AI-generated recommendations
 */
public record RecommendationResult(
    String recommendationId,
    String customerId,
    String recommendationType,
    List<String> recommendations,
    Map<String, Object> metadata,
    Double confidenceScore,
    String status
) {
    
    public static RecommendationResult success(
            String recommendationId,
            String customerId,
            String recommendationType,
            List<String> recommendations,
            Double confidenceScore) {
        return new RecommendationResult(
            recommendationId,
            customerId,
            recommendationType,
            recommendations,
            Map.of("generated_at", java.time.Instant.now()),
            confidenceScore,
            "SUCCESS"
        );
    }
    
    public static RecommendationResult failed(String customerId, String reason) {
        return new RecommendationResult(
            null,
            customerId,
            null,
            List.of(),
            Map.of("error", reason),
            0.0,
            "FAILED"
        );
    }
}