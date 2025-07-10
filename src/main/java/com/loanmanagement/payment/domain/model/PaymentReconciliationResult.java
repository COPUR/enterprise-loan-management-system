package com.loanmanagement.payment.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment reconciliation result
 */
public record PaymentReconciliationResult(
    String resultId,
    boolean success,
    LocalDateTime processedAt,
    int totalProcessed,
    int reconciled,
    int unreconciled,
    List<PaymentDiscrepancy> resolvedDiscrepancies,
    List<PaymentDiscrepancy> unresolvedDiscrepancies,
    String summary,
    String errorMessage
) {
    public double reconciliationRate() {
        if (totalProcessed == 0) return 0.0;
        return (double) reconciled / totalProcessed * 100.0;
    }
    
    public boolean isFullyReconciled() {
        return reconciled == totalProcessed;
    }
}