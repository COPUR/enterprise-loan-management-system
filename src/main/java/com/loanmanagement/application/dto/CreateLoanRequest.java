package com.loanmanagement.application.dto;

import java.math.BigDecimal;

/**
 * Request DTO for loan creation following DDD and hexagonal architecture principles.
 * Implements input validation at the application layer boundary.
 * Follows clean code principles with proper validation and documentation.
 */
public record CreateLoanRequest(
    Long customerId,
    BigDecimal amount,
    BigDecimal interestRate,
    Integer numberOfInstallments
) {

    /**
     * Factory method that validates input according to business rules.
     * Follows 12-Factor principle of fail-fast validation.
     */
    public static CreateLoanRequest of(Long customerId, BigDecimal amount,
                                     BigDecimal interestRate, Integer numberOfInstallments) {
        validateCustomerId(customerId);
        validateAmount(amount);
        validateInterestRate(interestRate);
        validateNumberOfInstallments(numberOfInstallments);

        return new CreateLoanRequest(customerId, amount, interestRate, numberOfInstallments);
    }

    /**
     * Compact constructor for record validation.
     * Ensures all business invariants are maintained at construction time.
     */
    public CreateLoanRequest {
        validateCustomerId(customerId);
        validateAmount(amount);
        validateInterestRate(interestRate);
        validateNumberOfInstallments(numberOfInstallments);
    }

    private static void validateCustomerId(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Loan amount is required");
        }
        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Loan amount too low (minimum: 1,000)");
        }
        if (amount.compareTo(new BigDecimal("500000")) > 0) {
            throw new IllegalArgumentException("Loan amount too high (maximum: 500,000)");
        }
    }

    private static void validateInterestRate(BigDecimal interestRate) {
        if (interestRate == null) {
            throw new IllegalArgumentException("Interest rate is required");
        }
        if (interestRate.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Interest rate too low (minimum: 1%)");
        }
        if (interestRate.compareTo(new BigDecimal("0.50")) > 0) {
            throw new IllegalArgumentException("Interest rate too high (maximum: 50%)");
        }
    }

    private static void validateNumberOfInstallments(Integer numberOfInstallments) {
        if (numberOfInstallments == null) {
            throw new IllegalArgumentException("Number of installments is required");
        }
        if (numberOfInstallments < 1) {
            throw new IllegalArgumentException("Must have at least 1 installment");
        }
        if (numberOfInstallments > 60) {
            throw new IllegalArgumentException("Cannot exceed 60 installments");
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
     * Converts interest rate to domain value object.
     */
    public com.loanmanagement.sharedkernel.InterestRate toInterestRate() {
        return com.loanmanagement.sharedkernel.InterestRate.of(interestRate);
    }

    /**
     * Converts number of installments to domain value object.
     */
    public com.loanmanagement.sharedkernel.InstallmentCount toInstallmentCount() {
        return com.loanmanagement.sharedkernel.InstallmentCount.of(numberOfInstallments);
    }

    /**
     * Business rule: Returns true if this is a high-value loan.
     */
    public boolean isHighValueLoan() {
        return amount.compareTo(new BigDecimal("100000")) >= 0;
    }

    /**
     * Business rule: Returns true if this is a short-term loan.
     */
    public boolean isShortTermLoan() {
        return numberOfInstallments <= 12;
    }
}
