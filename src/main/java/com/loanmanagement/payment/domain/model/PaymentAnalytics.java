package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value object representing payment analytics data.
 */
@Value
@Builder
@With
public class PaymentAnalytics {
    
    String analyticsId;
    Long loanId;
    String customerId;
    BigDecimal totalPayments;
    BigDecimal averagePaymentAmount;
    Integer paymentCount;
    Integer onTimePayments;
    Integer latePayments;
    Integer missedPayments;
    BigDecimal totalLateFees;
    BigDecimal totalDiscounts;
    String currency;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
    List<PaymentTrend> trends;
    List<PaymentAnomaly> anomalies;
    PaymentPrediction prediction;
    LocalDateTime calculatedAt;
    
    public static PaymentAnalytics create(
            String analyticsId,
            Long loanId,
            String customerId,
            BigDecimal totalPayments,
            BigDecimal averagePaymentAmount,
            Integer paymentCount,
            Integer onTimePayments,
            Integer latePayments,
            Integer missedPayments,
            BigDecimal totalLateFees,
            BigDecimal totalDiscounts,
            String currency,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            List<PaymentTrend> trends,
            List<PaymentAnomaly> anomalies,
            PaymentPrediction prediction) {
        
        if (analyticsId == null || analyticsId.trim().isEmpty()) {
            throw new IllegalArgumentException("Analytics ID cannot be null or empty");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (totalPayments == null || totalPayments.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total payments must be non-negative");
        }
        if (averagePaymentAmount == null || averagePaymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Average payment amount must be non-negative");
        }
        if (paymentCount == null || paymentCount < 0) {
            throw new IllegalArgumentException("Payment count must be non-negative");
        }
        if (onTimePayments == null || onTimePayments < 0) {
            throw new IllegalArgumentException("On-time payments must be non-negative");
        }
        if (latePayments == null || latePayments < 0) {
            throw new IllegalArgumentException("Late payments must be non-negative");
        }
        if (missedPayments == null || missedPayments < 0) {
            throw new IllegalArgumentException("Missed payments must be non-negative");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (periodStart == null) {
            throw new IllegalArgumentException("Period start cannot be null");
        }
        if (periodEnd == null) {
            throw new IllegalArgumentException("Period end cannot be null");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("Period start must be before period end");
        }
        
        return PaymentAnalytics.builder()
                .analyticsId(analyticsId.trim())
                .loanId(loanId)
                .customerId(customerId.trim())
                .totalPayments(totalPayments)
                .averagePaymentAmount(averagePaymentAmount)
                .paymentCount(paymentCount)
                .onTimePayments(onTimePayments)
                .latePayments(latePayments)
                .missedPayments(missedPayments)
                .totalLateFees(totalLateFees != null ? totalLateFees : BigDecimal.ZERO)
                .totalDiscounts(totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO)
                .currency(currency.trim())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .trends(trends)
                .anomalies(anomalies)
                .prediction(prediction)
                .calculatedAt(LocalDateTime.now())
                .build();
    }
    
    public BigDecimal getPaymentSuccessRate() {
        if (paymentCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(onTimePayments)
                .divide(BigDecimal.valueOf(paymentCount), 4, java.math.RoundingMode.HALF_UP);
    }
    
    public BigDecimal getPaymentDelinquencyRate() {
        if (paymentCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(latePayments + missedPayments)
                .divide(BigDecimal.valueOf(paymentCount), 4, java.math.RoundingMode.HALF_UP);
    }
    
    public boolean hasAnomalies() {
        return anomalies != null && !anomalies.isEmpty();
    }
    
    public boolean hasTrends() {
        return trends != null && !trends.isEmpty();
    }
}