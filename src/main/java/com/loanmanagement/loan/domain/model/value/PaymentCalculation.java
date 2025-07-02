package com.loanmanagement.loan.domain.model.value;

import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.ValueObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing the result of a payment calculation.
 * Encapsulates payment amount, discounts, penalties, and related data.
 *
 * Follows DDD principles by:
 * - Representing a complex business concept as an immutable value object
 * - Encapsulating payment calculation logic within the domain
 * - Providing intention-revealing factory methods for different scenarios
 *
 * Supports 12-Factor App principles by:
 * - Immutable state supporting stateless application design
 * - Clear business rules that can be consistently applied across environments
 *
 * Follows clean code principles by:
 * - Comprehensive input validation
 * - Rich domain methods expressing business intent
 * - Self-documenting factory methods for different payment scenarios
 */
public final class PaymentCalculation extends ValueObject {

    private final Money originalAmount;
    private final Money totalAmount;
    private final Money discountAmount;
    private final Money penaltyAmount;
    private final long daysEarly;
    private final long daysLate;
    private final PaymentType paymentType;

    private PaymentCalculation(
            Money originalAmount,
            Money totalAmount,
            Money discountAmount,
            Money penaltyAmount,
            long daysEarly,
            long daysLate,
            PaymentType paymentType
    ) {
        validateInputs(originalAmount, totalAmount, discountAmount, penaltyAmount, daysEarly, daysLate, paymentType);
        this.originalAmount = originalAmount;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.penaltyAmount = penaltyAmount;
        this.daysEarly = daysEarly;
        this.daysLate = daysLate;
        this.paymentType = paymentType;
    }

    /**
     * Factory method for early payments with discount.
     * Follows DDD principles with intention-revealing method names.
     */
    public static PaymentCalculation withDiscount(
            Money originalAmount,
            Money totalAmount,
            Money discountAmount,
            long daysEarly
    ) {
        if (daysEarly <= 0) {
            throw new IllegalArgumentException("Days early must be positive for discount payments");
        }
        return new PaymentCalculation(
                originalAmount,
                totalAmount,
                discountAmount,
                Money.ZERO,
                daysEarly,
                0L,
                PaymentType.EARLY
        );
    }

    /**
     * Factory method for late payments with penalty.
     * Supports business rule enforcement through domain methods.
     */
    public static PaymentCalculation withPenalty(
            Money originalAmount,
            Money totalAmount,
            Money penaltyAmount,
            long daysLate
    ) {
        if (daysLate <= 0) {
            throw new IllegalArgumentException("Days late must be positive for penalty payments");
        }
        return new PaymentCalculation(
                originalAmount,
                totalAmount,
                Money.ZERO,
                penaltyAmount,
                0L,
                daysLate,
                PaymentType.LATE
        );
    }

    /**
     * Factory method for on-time payments.
     * Clean code principle of expressing business intent clearly.
     */
    public static PaymentCalculation onTime(Money originalAmount) {
        return new PaymentCalculation(
                originalAmount,
                originalAmount,
                Money.ZERO,
                Money.ZERO,
                0L,
                0L,
                PaymentType.ON_TIME
        );
    }

    /**
     * Factory method for creating payment calculations with automatic penalty/discount calculation.
     * Advanced factory method supporting complex business scenarios.
     */
    public static PaymentCalculation calculate(Money originalAmount, long daysDifference,
                                             BigDecimal discountRate, BigDecimal penaltyRate) {
        if (daysDifference > 0) {
            // Early payment with discount
            Money discountAmount = originalAmount.multiply(discountRate.multiply(BigDecimal.valueOf(daysDifference)));
            Money totalAmount = originalAmount.subtract(discountAmount);
            return withDiscount(originalAmount, totalAmount, discountAmount, daysDifference);
        } else if (daysDifference < 0) {
            // Late payment with penalty
            long daysLate = Math.abs(daysDifference);
            Money penaltyAmount = originalAmount.multiply(penaltyRate.multiply(BigDecimal.valueOf(daysLate)));
            Money totalAmount = originalAmount.add(penaltyAmount);
            return withPenalty(originalAmount, totalAmount, penaltyAmount, daysLate);
        } else {
            // On-time payment
            return onTime(originalAmount);
        }
    }

    /**
     * Business logic method: calculates effective savings or additional cost.
     * Supports domain-driven design with rich business methods.
     */
    public Money getEffectiveDifference() {
        if (hasDiscount()) {
            return discountAmount; // Positive savings
        } else if (hasPenalty()) {
            return penaltyAmount.multiply(BigDecimal.valueOf(-1)); // Negative cost
        }
        return Money.ZERO;
    }

    /**
     * Business logic method: calculates effective interest rate.
     * Provides business insights for reporting and analysis.
     */
    public BigDecimal getEffectiveRate() {
        if (originalAmount.isZero()) {
            return BigDecimal.ZERO;
        }

        Money difference = getEffectiveDifference();
        return difference.getAmount().divide(originalAmount.getAmount(), 4, RoundingMode.HALF_UP);
    }

    /**
     * Business predicate: checks if payment qualifies for early payment benefits.
     * Domain method expressing business rules clearly.
     */
    public boolean hasDiscount() {
        return discountAmount.isPositive();
    }

    /**
     * Business predicate: checks if payment incurs late payment penalties.
     * Supports conditional business logic in domain operations.
     */
    public boolean hasPenalty() {
        return penaltyAmount.isPositive();
    }

    /**
     * Business predicate: checks if this is a profitable early payment.
     * Advanced business logic for payment strategy decisions.
     */
    public boolean isProfitableEarlyPayment() {
        return hasDiscount() && discountAmount.isGreaterThan(Money.of("0.01"));
    }

    /**
     * Business method: returns payment summary for reporting.
     * Supports 12-Factor principle of structured data for different outputs.
     */
    public PaymentSummary getSummary() {
        return new PaymentSummary(
            originalAmount,
            totalAmount,
            getEffectiveDifference(),
            paymentType,
            hasDiscount() ? daysEarly : (hasPenalty() ? daysLate : 0),
            getEffectiveRate()
        );
    }

    /**
     * Validates all input parameters following clean code principles.
     * Private method ensuring data integrity and business rule compliance.
     */
    private void validateInputs(Money originalAmount, Money totalAmount, Money discountAmount,
                               Money penaltyAmount, long daysEarly, long daysLate, PaymentType paymentType) {
        if (originalAmount == null || totalAmount == null || discountAmount == null ||
            penaltyAmount == null || paymentType == null) {
            throw new IllegalArgumentException("Payment calculation parameters cannot be null");
        }

        if (originalAmount.isZero() || originalAmount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original amount must be positive");
        }

        if (totalAmount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }

        if (discountAmount.getAmount().compareTo(BigDecimal.ZERO) < 0 ||
            penaltyAmount.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount and penalty amounts cannot be negative");
        }

        if (daysEarly < 0 || daysLate < 0) {
            throw new IllegalArgumentException("Days early and days late cannot be negative");
        }

        if (daysEarly > 0 && daysLate > 0) {
            throw new IllegalArgumentException("Payment cannot be both early and late");
        }

        // Business rule validation
        if (paymentType == PaymentType.EARLY && (daysEarly == 0 || !discountAmount.isPositive())) {
            throw new IllegalArgumentException("Early payment must have positive days early and discount");
        }

        if (paymentType == PaymentType.LATE && (daysLate == 0 || !penaltyAmount.isPositive())) {
            throw new IllegalArgumentException("Late payment must have positive days late and penalty");
        }
    }

    // Getters with proper encapsulation
    public Money getTotalAmount() { return totalAmount; }
    public Money getOriginalAmount() { return originalAmount; }
    public Money getDiscountAmount() { return discountAmount; }
    public Money getPenaltyAmount() { return penaltyAmount; }
    public long getDaysEarly() { return daysEarly; }
    public long getDaysLate() { return daysLate; }
    public PaymentType getPaymentType() { return paymentType; }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{
                originalAmount, totalAmount, discountAmount, penaltyAmount,
                daysEarly, daysLate, paymentType
        };
    }

    @Override
    public String toString() {
        return String.format("PaymentCalculation{type=%s, original=%s, total=%s, difference=%s}",
                paymentType, originalAmount, totalAmount, getEffectiveDifference());
    }

    /**
     * Enumeration for payment timing classification.
     * Supports domain-driven design with clear business concepts.
     */
    public enum PaymentType {
        EARLY("Early Payment"),
        ON_TIME("On-Time Payment"),
        LATE("Late Payment");

        private final String displayName;

        PaymentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isEarly() { return this == EARLY; }
        public boolean isOnTime() { return this == ON_TIME; }
        public boolean isLate() { return this == LATE; }
    }

    /**
     * Value object for payment summary information.
     * Follows DDD principles with immutable nested value objects.
     */
    public static class PaymentSummary {
        private final Money originalAmount;
        private final Money finalAmount;
        private final Money netDifference;
        private final PaymentType paymentType;
        private final long daysDifference;
        private final BigDecimal effectiveRate;

        public PaymentSummary(Money originalAmount, Money finalAmount, Money netDifference,
                             PaymentType paymentType, long daysDifference, BigDecimal effectiveRate) {
            this.originalAmount = originalAmount;
            this.finalAmount = finalAmount;
            this.netDifference = netDifference;
            this.paymentType = paymentType;
            this.daysDifference = daysDifference;
            this.effectiveRate = effectiveRate;
        }

        // Getters
        public Money getOriginalAmount() { return originalAmount; }
        public Money getFinalAmount() { return finalAmount; }
        public Money getNetDifference() { return netDifference; }
        public PaymentType getPaymentType() { return paymentType; }
        public long getDaysDifference() { return daysDifference; }
        public BigDecimal getEffectiveRate() { return effectiveRate; }

        @Override
        public String toString() {
            return String.format("PaymentSummary{%s: %s -> %s (%s), rate=%.2f%%}",
                    paymentType.getDisplayName(), originalAmount, finalAmount,
                    netDifference, effectiveRate.multiply(BigDecimal.valueOf(100)));
        }
    }
}

/*
 * ARCHITECTURAL BENEFITS:
 *
 * 12-Factor App:
 * - Immutable value objects supporting stateless application design
 * - Business rules consistently applied across all environments
 * - Structured data (PaymentSummary) for different output formats
 *
 * DDD (Domain-Driven Design):
 * - Rich domain model with expressive business methods
 * - Intention-revealing factory methods for different scenarios
 * - Business rule validation within the domain layer
 * - Value objects following DDD patterns (immutable, self-validating)
 *
 * Hexagonal Architecture:
 * - Pure domain value object without infrastructure dependencies
 * - Can be used across all layers without coupling
 * - Provides structured data for different adapters
 *
 * Clean Code:
 * - Comprehensive input validation and error handling
 * - Single responsibility with focused payment calculation logic
 * - Meaningful method names expressing business intent
 * - Defensive programming with parameter validation
 * - Rich domain methods supporting business operations
 */
