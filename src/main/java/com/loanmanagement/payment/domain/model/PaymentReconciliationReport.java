package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Payment reconciliation report
 */
public record PaymentReconciliationReport(
    String reportId,
    String scheduleId,
    LocalDateTime reconciliationDate,
    LocalDateTime reportGeneratedAt,
    ReconciliationStatus status,
    ReconciliationSummary summary,
    List<PaymentDiscrepancy> discrepancies,
    List<PaymentVariance> variances,
    Map<String, Object> metadata,
    String notes
) {
    public enum ReconciliationStatus {
        RECONCILED,
        PARTIALLY_RECONCILED,
        UNRECONCILED,
        PENDING
    }
    
    public boolean isReconciled() {
        return status == ReconciliationStatus.RECONCILED;
    }
    
    public boolean hasDiscrepancies() {
        return discrepancies != null && !discrepancies.isEmpty();
    }
    
    public Money getTotalDiscrepancyAmount() {
        if (!hasDiscrepancies()) {
            return Money.of("USD", java.math.BigDecimal.ZERO);
        }
        
        return discrepancies.stream()
            .map(PaymentDiscrepancy::getAmount)
            .reduce(Money.of("USD", java.math.BigDecimal.ZERO), Money::add);
    }
}