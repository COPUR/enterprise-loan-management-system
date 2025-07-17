package com.bank.loan.domain;

/**
 * Enumeration representing the possible states of a loan installment
 */
public enum InstallmentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}