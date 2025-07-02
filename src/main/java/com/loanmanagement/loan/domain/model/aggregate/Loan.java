package com.loanmanagement.loan.domain.model.aggregate;

import com.loanmanagement.loan.domain.model.value.*;
import com.loanmanagement.loan.domain.model.entity.LoanInstallment;
import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.AggregateRoot;
import com.loanmanagement.loan.exception.NoPayableInstallmentsException;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;

/**
 * Loan aggregate root following DDD principles.
 * Encapsulates all loan business logic and maintains consistency.
 */
public class Loan extends AggregateRoot<LoanId> {

    private final Long customerId;
    private final Money loanAmount;
    private final BigDecimal interestRate;
    private final Integer numberOfInstallments;
    private final LocalDate createDate;
    private final List<LoanInstallment> installments;

    public static Loan create(
            LoanId loanId,
            Long customerId,
            Money loanAmount,
            BigDecimal interestRate,
            Integer numberOfInstallments
    ) {
        validateCreationParameters(customerId, loanAmount, interestRate, numberOfInstallments);

        return new Loan(
                loanId,
                customerId,
                loanAmount,
                interestRate,
                numberOfInstallments,
                LocalDate.now()
        );
    }

    private Loan(
            LoanId loanId,
            Long customerId,
            Money loanAmount,
            BigDecimal interestRate,
            Integer numberOfInstallments,
            LocalDate createDate
    ) {
        super(loanId);
        this.customerId = customerId;
        this.loanAmount = loanAmount;
        this.interestRate = interestRate;
        this.numberOfInstallments = numberOfInstallments;
        this.createDate = createDate;
        this.installments = generateInstallments();
    }

    private List<LoanInstallment> generateInstallments() {
        List<LoanInstallment> installmentList = new ArrayList<>();
        Money installmentAmount = calculateInstallmentAmount();

        for (int i = 1; i <= numberOfInstallments; i++) {
            LocalDate dueDate = createDate.plusMonths(i);

            LoanInstallment installment = LoanInstallment.create(
                    InstallmentId.generate(),
                    getId(),
                    i,
                    installmentAmount,
                    dueDate
            );
            installmentList.add(installment);
        }

        return installmentList;
    }

    private Money calculateInstallmentAmount() {
        // Simple equal installment calculation
        BigDecimal totalAmount = loanAmount.getAmount()
                .multiply(BigDecimal.ONE.add(interestRate))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal installmentAmount = totalAmount
                .divide(BigDecimal.valueOf(numberOfInstallments), 2, RoundingMode.HALF_UP);

        return Money.of(installmentAmount);
    }

    public PaymentResult makePayment(Money amount, LocalDate paymentDate) {
        LoanInstallment nextInstallment = getNextPayableInstallment();
        if (nextInstallment == null) {
            throw new NoPayableInstallmentsException("No installments available for payment");
        }

        PaymentCalculation calculation = nextInstallment.calculatePaymentAmount(paymentDate);

        if (amount.isGreaterThanOrEqual(calculation.getTotalAmount())) {
            nextInstallment.markAsPaid(paymentDate, calculation);
            return PaymentResult.successful(calculation);
        } else {
            return PaymentResult.insufficient(amount, calculation.getTotalAmount());
        }
    }

    private LoanInstallment getNextPayableInstallment() {
        return installments.stream()
                .filter(installment -> !installment.isPaid())
                .findFirst()
                .orElse(null);
    }

    public boolean isLoanFullyPaid() {
        return installments.stream().allMatch(LoanInstallment::isPaid);
    }

    private static void validateCreationParameters(
            Long customerId,
            Money loanAmount,
            BigDecimal interestRate,
            Integer numberOfInstallments
    ) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
        if (loanAmount == null || !loanAmount.isPositive()) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (numberOfInstallments == null || numberOfInstallments <= 0) {
            throw new IllegalArgumentException("Number of installments must be positive");
        }
    }

    // Getters
    public Long getCustomerId() { return customerId; }
    public Money getLoanAmount() { return loanAmount; }
    public Money getPrincipalAmount() { return loanAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public Integer getNumberOfInstallments() { return numberOfInstallments; }
    public LocalDate getCreateDate() { return createDate; }
    public List<LoanInstallment> getInstallments() { return new ArrayList<>(installments); }
    public boolean isPaid() { return isLoanFullyPaid(); }
}
