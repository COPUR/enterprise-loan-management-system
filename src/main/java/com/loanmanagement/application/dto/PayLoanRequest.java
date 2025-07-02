package com.loanmanagement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for loan payment following DDD and hexagonal architecture principles.
 * Implements input validation at the application layer boundary.
 * Follows clean code principles with proper validation and documentation.
 */
public record PayLoanRequest(
    Long loanId,
    BigDecimal amount,
    LocalDate paymentDate
) {

    /**
     * Factory method that validates input according to business rules.
     * Follows 12-Factor principle of fail-fast validation.
     */
    public static PayLoanRequest of(Long loanId, BigDecimal amount, LocalDate paymentDate) {
        validateLoanId(loanId);
        validateAmount(amount);
        validatePaymentDate(paymentDate);

        return new PayLoanRequest(loanId, amount, paymentDate);
    }

    /**
     * Compact constructor for record validation.
     * Ensures all business invariants are maintained at construction time.
     */
    public PayLoanRequest {
        validateLoanId(loanId);
        validateAmount(amount);
        validatePaymentDate(paymentDate);
    }

    private static void validateLoanId(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID is required");
        }
        if (loanId <= 0) {
            throw new IllegalArgumentException("Loan ID must be positive");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Payment amount too large (max: 1,000,000)");
        }
    }

    private static void validatePaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }
        // Allow reasonable historical payments (e.g., up to 1 year ago)
        if (paymentDate.isBefore(LocalDate.now().minusYears(1))) {
            throw new IllegalArgumentException("Payment date too far in the past");
        }
    }

    /**
     * Converts this DTO to domain-specific value objects.
     * Follows hexagonal architecture by translating from application layer to domain layer.
     */
    public com.loanmanagement.sharedkernel.domain.value.Money toMoney() {
        return com.loanmanagement.sharedkernel.domain.value.Money.of(amount);
    }

    /**
     * Returns true if this is a same-day payment.
     * Business logic helper method following DDD principles.
     */
    public boolean isSameDayPayment() {
        return paymentDate.equals(LocalDate.now());
    }

    /**
     * Returns true if this is a historical payment.
     * Business logic helper method following DDD principles.
     */
    public boolean isHistoricalPayment() {
        return paymentDate.isBefore(LocalDate.now());
    }
}
