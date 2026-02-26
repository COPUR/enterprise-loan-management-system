package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import java.time.LocalDateTime;

/**
 * Payment discrepancy model
 */
public record PaymentDiscrepancy(
    String discrepancyId,
    DiscrepancyType type,
    String description,
    Money amount,
    String paymentId,
    LocalDateTime discoveredAt,
    DiscrepancySeverity severity,
    String details
) {
    public enum DiscrepancyType {
        MISSING_PAYMENT,
        EXTRA_PAYMENT,
        AMOUNT_MISMATCH,
        DATE_MISMATCH,
        ALLOCATION_ERROR,
        DUPLICATE_PAYMENT
    }
    
    public enum DiscrepancySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public boolean isCritical() {
        return severity == DiscrepancySeverity.CRITICAL;
    }
}