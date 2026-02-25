package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object representing a payment trend.
 */
@Value
@Builder
@With
public class PaymentTrend {
    
    String trendId;
    Long loanId;
    String customerId;
    String trendType;
    String trendDirection;
    BigDecimal trendValue;
    BigDecimal trendStrength;
    String trendPeriod;
    LocalDateTime trendStartDate;
    LocalDateTime trendEndDate;
    String trendDescription;
    boolean isSignificant;
    BigDecimal confidenceLevel;
    String trendCategory;
    
    public static PaymentTrend create(
            String trendId,
            Long loanId,
            String customerId,
            String trendType,
            String trendDirection,
            BigDecimal trendValue,
            BigDecimal trendStrength,
            String trendPeriod,
            LocalDateTime trendStartDate,
            LocalDateTime trendEndDate,
            String trendDescription,
            boolean isSignificant,
            BigDecimal confidenceLevel,
            String trendCategory) {
        
        if (trendId == null || trendId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend ID cannot be null or empty");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (trendType == null || trendType.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend type cannot be null or empty");
        }
        if (trendDirection == null || trendDirection.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend direction cannot be null or empty");
        }
        if (trendValue == null) {
            throw new IllegalArgumentException("Trend value cannot be null");
        }
        if (trendStrength == null || trendStrength.compareTo(BigDecimal.ZERO) < 0 || trendStrength.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Trend strength must be between 0 and 1");
        }
        if (trendPeriod == null || trendPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend period cannot be null or empty");
        }
        if (trendStartDate == null) {
            throw new IllegalArgumentException("Trend start date cannot be null");
        }
        if (trendEndDate == null) {
            throw new IllegalArgumentException("Trend end date cannot be null");
        }
        if (trendStartDate.isAfter(trendEndDate)) {
            throw new IllegalArgumentException("Trend start date must be before end date");
        }
        if (trendDescription == null || trendDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend description cannot be null or empty");
        }
        if (confidenceLevel == null || confidenceLevel.compareTo(BigDecimal.ZERO) < 0 || confidenceLevel.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence level must be between 0 and 1");
        }
        if (trendCategory == null || trendCategory.trim().isEmpty()) {
            throw new IllegalArgumentException("Trend category cannot be null or empty");
        }
        
        return PaymentTrend.builder()
                .trendId(trendId.trim())
                .loanId(loanId)
                .customerId(customerId.trim())
                .trendType(trendType.trim())
                .trendDirection(trendDirection.trim())
                .trendValue(trendValue)
                .trendStrength(trendStrength)
                .trendPeriod(trendPeriod.trim())
                .trendStartDate(trendStartDate)
                .trendEndDate(trendEndDate)
                .trendDescription(trendDescription.trim())
                .isSignificant(isSignificant)
                .confidenceLevel(confidenceLevel)
                .trendCategory(trendCategory.trim())
                .build();
    }
    
    public boolean isUpwardTrend() {
        return "UPWARD".equalsIgnoreCase(trendDirection) || "INCREASING".equalsIgnoreCase(trendDirection);
    }
    
    public boolean isDownwardTrend() {
        return "DOWNWARD".equalsIgnoreCase(trendDirection) || "DECREASING".equalsIgnoreCase(trendDirection);
    }
    
    public boolean isStableTrend() {
        return "STABLE".equalsIgnoreCase(trendDirection) || "FLAT".equalsIgnoreCase(trendDirection);
    }
    
    public boolean isStrongTrend() {
        return trendStrength.compareTo(BigDecimal.valueOf(0.7)) >= 0;
    }
    
    public boolean isHighConfidence() {
        return confidenceLevel.compareTo(BigDecimal.valueOf(0.8)) >= 0;
    }
}