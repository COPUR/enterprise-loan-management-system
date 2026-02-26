package com.loanmanagement.payment.domain.model;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Payment reconciliation configuration
 */
public record PaymentReconciliationConfig(
    BigDecimal toleranceAmount,
    Duration tolerancePeriod,
    boolean autoResolveMinorDiscrepancies,
    boolean enableVarianceAnalysis,
    int maxDiscrepanciesPerReport,
    ReconciliationMode mode
) {
    public enum ReconciliationMode {
        STRICT,
        TOLERANT,
        FLEXIBLE
    }
    
    public static PaymentReconciliationConfig defaultConfig() {
        return new PaymentReconciliationConfig(
            new BigDecimal("0.01"), // $0.01 tolerance
            Duration.ofDays(1), // 1 day tolerance
            true, // auto-resolve minor discrepancies
            true, // enable variance analysis
            100, // max 100 discrepancies per report
            ReconciliationMode.TOLERANT
        );
    }
}