package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;

/**
 * Reconciliation summary model
 */
public record ReconciliationSummary(
    int totalScheduledPayments,
    int totalActualPayments,
    int reconciledPayments,
    int unreconciledPayments,
    Money totalScheduledAmount,
    Money totalActualAmount,
    Money totalVarianceAmount,
    double reconciliationAccuracy,
    LocalDateTime lastReconciliationDate
) {
    public double getReconciliationRate() {
        if (totalScheduledPayments == 0) return 0.0;
        return (double) reconciledPayments / totalScheduledPayments * 100.0;
    }
    
    public boolean isFullyReconciled() {
        return reconciledPayments == totalScheduledPayments;
    }
}