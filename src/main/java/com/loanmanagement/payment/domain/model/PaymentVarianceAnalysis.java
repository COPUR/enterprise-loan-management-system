package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Payment variance analysis
 */
public record PaymentVarianceAnalysis(
    String analysisId,
    LocalDateTime analysisDate,
    String scheduleId,
    List<PaymentVariance> variances,
    VarianceStatistics statistics,
    Map<String, Object> insights,
    List<String> recommendations
) {
    public record VarianceStatistics(
        int totalVariances,
        Money totalVarianceAmount,
        Money averageVarianceAmount,
        Money maxVarianceAmount,
        double standardDeviation,
        String mostCommonVarianceType
    ) {}
    
    public boolean hasSignificantVariances() {
        return variances.stream().anyMatch(v -> 
            v.severity() == PaymentVariance.VarianceSeverity.HIGH ||
            v.severity() == PaymentVariance.VarianceSeverity.CRITICAL
        );
    }
}