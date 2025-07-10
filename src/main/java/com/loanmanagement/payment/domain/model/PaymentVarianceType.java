package com.loanmanagement.payment.domain.model;

/**
 * Types of payment variances for reconciliation
 */
public enum PaymentVarianceType {
    EXACT_MATCH,
    AMOUNT_VARIANCE_ONLY,
    DATE_VARIANCE_ONLY,
    BOTH_VARIANCE
}