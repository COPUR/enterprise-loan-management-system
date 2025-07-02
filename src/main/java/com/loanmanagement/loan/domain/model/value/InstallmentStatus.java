package com.loanmanagement.loan.domain.model.value;

/**
 * Enumeration representing the status of a loan installment.
 * Follows DDD principles by providing explicit domain states.
 */
public enum InstallmentStatus {
    PENDING,
    PAID,
    OVERDUE,
    CANCELLED
}
