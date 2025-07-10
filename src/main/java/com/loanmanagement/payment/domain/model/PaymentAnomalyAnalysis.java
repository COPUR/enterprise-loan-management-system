package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Value object representing payment anomaly analysis.
 */
@Value
@Builder
@With
public class PaymentAnomalyAnalysis {
    
    String analysisId;
    Long loanId;
    String customerId;
    LocalDateTime analysisDate;
    String analysisMethod;
    Integer totalPaymentsAnalyzed;
    Integer anomaliesDetected;
    BigDecimal anomalyScore;
    AnomalySeverity overallSeverity;
    List<PaymentAnomaly> detectedAnomalies;
    String analysisNotes;
    boolean requiresInvestigation;
    LocalDateTime nextAnalysisDate;
    
    public static PaymentAnomalyAnalysis create(
            String analysisId,
            Long loanId,
            String customerId,
            String analysisMethod,
            Integer totalPaymentsAnalyzed,
            Integer anomaliesDetected,
            BigDecimal anomalyScore,
            AnomalySeverity overallSeverity,
            List<PaymentAnomaly> detectedAnomalies,
            String analysisNotes,
            boolean requiresInvestigation,
            LocalDateTime nextAnalysisDate) {
        
        if (analysisId == null || analysisId.trim().isEmpty()) {
            throw new IllegalArgumentException("Analysis ID cannot be null or empty");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (analysisMethod == null || analysisMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Analysis method cannot be null or empty");
        }
        if (totalPaymentsAnalyzed == null || totalPaymentsAnalyzed < 0) {
            throw new IllegalArgumentException("Total payments analyzed must be non-negative");
        }
        if (anomaliesDetected == null || anomaliesDetected < 0) {
            throw new IllegalArgumentException("Anomalies detected must be non-negative");
        }
        if (anomalyScore == null || anomalyScore.compareTo(BigDecimal.ZERO) < 0 || anomalyScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Anomaly score must be between 0 and 1");
        }
        if (overallSeverity == null) {
            throw new IllegalArgumentException("Overall severity cannot be null");
        }
        if (anomaliesDetected > 0 && (detectedAnomalies == null || detectedAnomalies.isEmpty())) {
            throw new IllegalArgumentException("Detected anomalies list cannot be empty when anomalies detected > 0");
        }
        
        return PaymentAnomalyAnalysis.builder()
                .analysisId(analysisId.trim())
                .loanId(loanId)
                .customerId(customerId.trim())
                .analysisDate(LocalDateTime.now())
                .analysisMethod(analysisMethod.trim())
                .totalPaymentsAnalyzed(totalPaymentsAnalyzed)
                .anomaliesDetected(anomaliesDetected)
                .anomalyScore(anomalyScore)
                .overallSeverity(overallSeverity)
                .detectedAnomalies(detectedAnomalies)
                .analysisNotes(analysisNotes != null ? analysisNotes.trim() : null)
                .requiresInvestigation(requiresInvestigation)
                .nextAnalysisDate(nextAnalysisDate)
                .build();
    }
    
    public boolean hasAnomalies() {
        return anomaliesDetected > 0;
    }
    
    public boolean isHighRisk() {
        return overallSeverity == AnomalySeverity.HIGH || overallSeverity == AnomalySeverity.CRITICAL;
    }
    
    public BigDecimal getAnomalyRate() {
        if (totalPaymentsAnalyzed == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(anomaliesDetected)
                .divide(BigDecimal.valueOf(totalPaymentsAnalyzed), 4, java.math.RoundingMode.HALF_UP);
    }
}