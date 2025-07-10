package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;

/**
 * Payment variance model
 */
public record PaymentVariance(
    String varianceId,
    VarianceType type,
    String description,
    Money expectedAmount,
    Money actualAmount,
    Money varianceAmount,
    LocalDateTime expectedDate,
    LocalDateTime actualDate,
    VarianceSeverity severity,
    String reason
) {
    public enum VarianceType {
        AMOUNT_VARIANCE,
        TIMING_VARIANCE,
        FREQUENCY_VARIANCE,
        ALLOCATION_VARIANCE
    }
    
    public enum VarianceSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public double getVariancePercentage() {
        if (expectedAmount.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return varianceAmount.getAmount()
            .divide(expectedAmount.getAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(java.math.BigDecimal.valueOf(100))
            .doubleValue();
    }
}