package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value object representing payment predictions.
 */
@Value
@Builder
@With
public class PaymentPrediction {
    
    String predictionId;
    Long loanId;
    String customerId;
    BigDecimal predictedPaymentAmount;
    LocalDate predictedPaymentDate;
    BigDecimal confidenceScore;
    String predictionModel;
    String predictionReason;
    BigDecimal riskScore;
    String riskCategory;
    boolean isLikelyToDefault;
    BigDecimal defaultProbability;
    LocalDateTime predictionCreatedAt;
    LocalDateTime predictionValidUntil;
    
    public static PaymentPrediction create(
            String predictionId,
            Long loanId,
            String customerId,
            BigDecimal predictedPaymentAmount,
            LocalDate predictedPaymentDate,
            BigDecimal confidenceScore,
            String predictionModel,
            String predictionReason,
            BigDecimal riskScore,
            String riskCategory,
            boolean isLikelyToDefault,
            BigDecimal defaultProbability,
            LocalDateTime predictionValidUntil) {
        
        if (predictionId == null || predictionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Prediction ID cannot be null or empty");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (predictedPaymentAmount == null || predictedPaymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Predicted payment amount must be non-negative");
        }
        if (predictedPaymentDate == null) {
            throw new IllegalArgumentException("Predicted payment date cannot be null");
        }
        if (confidenceScore == null || confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence score must be between 0 and 1");
        }
        if (predictionModel == null || predictionModel.trim().isEmpty()) {
            throw new IllegalArgumentException("Prediction model cannot be null or empty");
        }
        if (predictionReason == null || predictionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Prediction reason cannot be null or empty");
        }
        if (riskScore == null || riskScore.compareTo(BigDecimal.ZERO) < 0 || riskScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Risk score must be between 0 and 1");
        }
        if (riskCategory == null || riskCategory.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk category cannot be null or empty");
        }
        if (defaultProbability == null || defaultProbability.compareTo(BigDecimal.ZERO) < 0 || defaultProbability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Default probability must be between 0 and 1");
        }
        if (predictionValidUntil == null) {
            throw new IllegalArgumentException("Prediction valid until cannot be null");
        }
        
        return PaymentPrediction.builder()
                .predictionId(predictionId.trim())
                .loanId(loanId)
                .customerId(customerId.trim())
                .predictedPaymentAmount(predictedPaymentAmount)
                .predictedPaymentDate(predictedPaymentDate)
                .confidenceScore(confidenceScore)
                .predictionModel(predictionModel.trim())
                .predictionReason(predictionReason.trim())
                .riskScore(riskScore)
                .riskCategory(riskCategory.trim())
                .isLikelyToDefault(isLikelyToDefault)
                .defaultProbability(defaultProbability)
                .predictionCreatedAt(LocalDateTime.now())
                .predictionValidUntil(predictionValidUntil)
                .build();
    }
    
    public boolean isHighRisk() {
        return riskScore.compareTo(BigDecimal.valueOf(0.7)) >= 0;
    }
    
    public boolean isHighConfidence() {
        return confidenceScore.compareTo(BigDecimal.valueOf(0.8)) >= 0;
    }
    
    public boolean isPredictionExpired() {
        return LocalDateTime.now().isAfter(predictionValidUntil);
    }
}