package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object representing a payment anomaly.
 */
@Value
@Builder
@With
public class PaymentAnomaly {
    
    String anomalyId;
    PaymentId paymentId;
    Long loanId;
    String customerId;
    PaymentAnomalyType type;
    AnomalySeverity severity;
    BigDecimal anomalyScore;
    String description;
    String detectionMethod;
    LocalDateTime detectedAt;
    String detectedBy;
    boolean isResolved;
    String resolution;
    LocalDateTime resolvedAt;
    String resolvedBy;
    
    public static PaymentAnomaly create(
            String anomalyId,
            PaymentId paymentId,
            Long loanId,
            String customerId,
            PaymentAnomalyType type,
            AnomalySeverity severity,
            BigDecimal anomalyScore,
            String description,
            String detectionMethod,
            String detectedBy) {
        
        if (anomalyId == null || anomalyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Anomaly ID cannot be null or empty");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Anomaly type cannot be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }
        if (anomalyScore == null || anomalyScore.compareTo(BigDecimal.ZERO) < 0 || anomalyScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Anomaly score must be between 0 and 1");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        if (detectionMethod == null || detectionMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Detection method cannot be null or empty");
        }
        if (detectedBy == null || detectedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Detected by cannot be null or empty");
        }
        
        return PaymentAnomaly.builder()
                .anomalyId(anomalyId.trim())
                .paymentId(paymentId)
                .loanId(loanId)
                .customerId(customerId.trim())
                .type(type)
                .severity(severity)
                .anomalyScore(anomalyScore)
                .description(description.trim())
                .detectionMethod(detectionMethod.trim())
                .detectedAt(LocalDateTime.now())
                .detectedBy(detectedBy.trim())
                .isResolved(false)
                .resolution(null)
                .resolvedAt(null)
                .resolvedBy(null)
                .build();
    }
    
    public PaymentAnomaly resolve(String resolution, String resolvedBy) {
        if (isResolved) {
            throw new IllegalStateException("Anomaly is already resolved");
        }
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new IllegalArgumentException("Resolution cannot be null or empty");
        }
        if (resolvedBy == null || resolvedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Resolved by cannot be null or empty");
        }
        
        return this.withIsResolved(true)
                .withResolution(resolution.trim())
                .withResolvedAt(LocalDateTime.now())
                .withResolvedBy(resolvedBy.trim());
    }
    
    public boolean requiresUrgentAction() {
        return severity == AnomalySeverity.HIGH || severity == AnomalySeverity.CRITICAL;
    }
}