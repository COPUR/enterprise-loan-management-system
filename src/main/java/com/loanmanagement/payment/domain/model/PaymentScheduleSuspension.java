package com.loanmanagement.payment.domain.model;

import java.time.LocalDate;

/**
 * Payment schedule suspension model
 */
public record PaymentScheduleSuspension(
    String suspensionId,
    LocalDate startDate,
    LocalDate endDate,
    SuspensionReason reason,
    String notes
) {
    public enum SuspensionReason {
        CUSTOMER_REQUEST,
        HARDSHIP,
        SYSTEM_MAINTENANCE,
        ADMINISTRATIVE
    }
}