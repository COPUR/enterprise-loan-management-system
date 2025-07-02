package com.loanmanagement.loan.domain.model.entity;

import com.loanmanagement.loan.domain.model.value.*;
import com.loanmanagement.loan.exception.InstallmentAlreadyPaidException;
import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.Entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

/**
 * Domain entity representing a loan installment.
 * Follows DDD principles by encapsulating installment business logic
 * and maintaining consistency through domain methods.
 */
public class LoanInstallment extends Entity<InstallmentId> {

    private final LoanId loanId;
    private final int installmentNumber;
    private final Money amount;
    private Money paidAmount;
    private final LocalDate dueDate;
    private LocalDate paymentDate;
    private InstallmentStatus status;

    /**
     * Factory method for creating a new pending installment.
     * Follows DDD principles by using intention-revealing factory methods.
     */
    public static LoanInstallment create(
            InstallmentId installmentId,
            LoanId loanId,
            int installmentNumber,
            Money amount,
            LocalDate dueDate
    ) {
        validateCreationParameters(installmentId, loanId, installmentNumber, amount, dueDate);

        return new LoanInstallment(
                installmentId,
                loanId,
                installmentNumber,
                amount,
                null,
                dueDate,
                null,
                InstallmentStatus.PENDING
        );
    }

    private LoanInstallment(
            InstallmentId installmentId,
            LoanId loanId,
            int installmentNumber,
            Money amount,
            Money paidAmount,
            LocalDate dueDate,
            LocalDate paymentDate,
            InstallmentStatus status
    ) {
        super(installmentId);
        this.loanId = loanId;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    /**
     * Calculates payment amount based on payment date.
     * Business logic encapsulated within the domain entity.
     * In a production system, payment policies should be externalized
     * to support different business rules per 12-Factor App principles.
     */
    public PaymentCalculation calculatePaymentAmount(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }

        if (isPaid()) {
            throw new InstallmentAlreadyPaidException(getId());
        }

        if (paymentDate.isBefore(dueDate)) {
            return calculateEarlyPayment(paymentDate);
        } else if (paymentDate.isAfter(dueDate)) {
            return calculateLatePayment(paymentDate);
        } else {
            return PaymentCalculation.onTime(amount);
        }
    }

    /**
     * Marks the installment as paid with the calculated amount.
     * Enforces business invariants and state transitions.
     */
    public void markAsPaid(LocalDate paymentDate, PaymentCalculation calculation) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }
        if (calculation == null) {
            throw new IllegalArgumentException("Payment calculation cannot be null");
        }

        if (isPaid()) {
            throw new InstallmentAlreadyPaidException(getId());
        }

        this.paymentDate = paymentDate;
        this.paidAmount = calculation.getTotalAmount();
        this.status = InstallmentStatus.PAID;
    }

    public boolean isPaid() {
        return status == InstallmentStatus.PAID;
    }

    // Private helper methods to reduce complexity and improve readability
    private PaymentCalculation calculateEarlyPayment(LocalDate paymentDate) {
        long daysEarly = ChronoUnit.DAYS.between(paymentDate, dueDate);
        BigDecimal discountRate = getEarlyPaymentDiscountRate();
        BigDecimal discountMultiplier = discountRate.multiply(BigDecimal.valueOf(daysEarly));
        Money discountAmount = amount.multiply(discountMultiplier);
        Money paymentAmount = amount.subtract(discountAmount);

        return PaymentCalculation.withDiscount(
                amount,
                paymentAmount,
                discountAmount,
                daysEarly
        );
    }

    private PaymentCalculation calculateLatePayment(LocalDate paymentDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, paymentDate);
        BigDecimal penaltyRate = getLatePaymentPenaltyRate();
        BigDecimal penaltyMultiplier = penaltyRate.multiply(BigDecimal.valueOf(daysLate));
        Money penaltyAmount = amount.multiply(penaltyMultiplier);
        Money paymentAmount = amount.add(penaltyAmount);

        return PaymentCalculation.withPenalty(
                amount,
                paymentAmount,
                penaltyAmount,
                daysLate
        );
    }

    /**
     * These methods would ideally be injected via domain services
     * to support configurable business rules per 12-Factor App principles.
     * Currently using default values for demonstration.
     */
    private BigDecimal getEarlyPaymentDiscountRate() {
        // TODO: Extract to configuration service following 12-Factor App principles
        return new BigDecimal("0.001");
    }

    private BigDecimal getLatePaymentPenaltyRate() {
        // TODO: Extract to configuration service following 12-Factor App principles
        return new BigDecimal("0.001");
    }

    private static void validateCreationParameters(
            InstallmentId installmentId,
            LoanId loanId,
            int installmentNumber,
            Money amount,
            LocalDate dueDate
    ) {
        if (installmentId == null) {
            throw new IllegalArgumentException("Installment ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (installmentNumber <= 0) {
            throw new IllegalArgumentException("Installment number must be positive");
        }
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
    }

    // Getters following clean code principles with meaningful names
    public LoanId getLoanId() {
        return loanId;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public InstallmentStatus getStatus() {
        return status;
    }
}

