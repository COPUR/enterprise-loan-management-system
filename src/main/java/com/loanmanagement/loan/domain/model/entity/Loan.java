package com.loanmanagement.loan.domain.model.entity;

import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.domain.model.value.*;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

public class Loan {
    private final Long id;
    private final Long customerId;
    private final Money loanAmount;
    private final Money principalAmount;
    private final InterestRate interestRate;
    private final InstallmentCount numberOfInstallments;
    private final LocalDate createDate;
    private boolean isPaid;
    private final List<LoanInstallment> installments;

    private final List<DomainEvent> domainEvents = new ArrayList<>();
    public static Loan create(
            Long id,
            Long customerId,
            Money principalAmount,
            InterestRate interestRate,
            InstallmentCount numberOfInstallments
    ) {
        Money totalLoanAmount = calculateTotalAmount(principalAmount, interestRate);
        List<LoanInstallment> installments = generateInstallments(
                id, totalLoanAmount, numberOfInstallments
        );

        return new Loan(
                id, customerId, totalLoanAmount, principalAmount,
                interestRate, numberOfInstallments, LocalDate.now(), false, installments
        );
    }

    private Loan(Long id, Long customerId, Money loanAmount, Money principalAmount,
                 InterestRate interestRate, InstallmentCount numberOfInstallments,
                 LocalDate createDate, boolean isPaid, List<LoanInstallment> installments) {
        this.id = id;
        this.customerId = customerId;
        this.loanAmount = loanAmount;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.numberOfInstallments = numberOfInstallments;
        this.createDate = createDate;
        this.isPaid = isPaid;
        this.installments = new ArrayList<>(installments);
    }

    private static Money calculateTotalAmount(Money principal, InterestRate rate) {
        Money interest = rate.calculateInterest(principal);
        return principal.add(interest);
    }

    private static List<LoanInstallment> generateInstallments(
            Long loanId, Money totalAmount, InstallmentCount count
    ) {
        List<LoanInstallment> installments = new ArrayList<>();
        Money installmentAmount = new Money(
                totalAmount.getValue().divide(
                        BigDecimal.valueOf(count.getValue()), 2, RoundingMode.HALF_UP
                )
        );

        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        LocalDate dueDate = nextMonth.withDayOfMonth(1);

        for (int i = 0; i < count.getValue(); i++) {
            installments.add(new LoanInstallment(
                    null, // ID will be assigned by persistence layer
                    loanId,
                    installmentAmount,
                    dueDate.plusMonths(i)
            ));
        }

        return installments;
    }

    public PaymentResult makePayment(Money paymentAmount, LocalDate paymentDate) {
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }

        List<LoanInstallment> unpaidInstallments = getPayableInstallments(paymentDate);
        if (unpaidInstallments.isEmpty()) {
            throw new NoPayableInstallmentsException("No installments available for payment");
        }

        Money remainingAmount = paymentAmount;
        int installmentsPaid = 0;
        Money totalSpent = new Money(BigDecimal.ZERO);

        for (LoanInstallment installment : unpaidInstallments) {
            Money paymentRequired = installment.calculatePaymentAmount(paymentDate);

            if (remainingAmount.isGreaterThanOrEqual(paymentRequired)) {
                installment.pay(paymentDate);
                remainingAmount = remainingAmount.subtract(paymentRequired);
                totalSpent = totalSpent.add(paymentRequired);
                installmentsPaid++;
            } else {
                break; // Cannot pay partial installments
            }
        }

        updateLoanStatus();

        return new PaymentResult(installmentsPaid, totalSpent, isPaid);
    }

    private List<LoanInstallment> getPayableInstallments(LocalDate paymentDate) {
        LocalDate maxPayableDate = paymentDate.plusMonths(3);

        return installments.stream()
                .filter(i -> !i.isPaid())
                .filter(i -> !i.getDueDate().isAfter(maxPayableDate))
                .sorted(Comparator.comparing(LoanInstallment::getDueDate))
                .toList();
    }

    private void updateLoanStatus() {
        this.isPaid = installments.stream().allMatch(LoanInstallment::isPaid);
    }

    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Money getLoanAmount() { return loanAmount; }
    public Money getPrincipalAmount() { return principalAmount; }
    public boolean isPaid() { return isPaid; }
    public List<LoanInstallment> getInstallments() {
        return Collections.unmodifiableList(installments);
    }
}