package com.loanmanagement.loan.domain.model.entity;

import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.InterestRate;
import com.loanmanagement.sharedkernel.InstallmentCount;
import com.loanmanagement.loan.domain.model.value.PaymentResult;
import com.loanmanagement.loan.domain.model.value.PaymentCalculation;
import com.loanmanagement.loan.domain.model.value.LoanId;
import com.loanmanagement.loan.domain.model.value.InstallmentId;
import com.loanmanagement.loan.exception.NoPayableInstallmentsException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
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
        // Simple interest calculation - in real scenario this would be more sophisticated
        BigDecimal interestAmount = principal.getAmount().multiply(rate.getValue());
        Money interest = Money.of(interestAmount);
        return principal.add(interest);
    }

    private static List<LoanInstallment> generateInstallments(
            Long loanId, Money totalAmount, InstallmentCount count
    ) {
        List<LoanInstallment> installments = new ArrayList<>();
        Money installmentAmount = totalAmount.divide(count.getValue());

        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        LocalDate dueDate = nextMonth.withDayOfMonth(1);

        for (int i = 0; i < count.getValue(); i++) {
            installments.add(LoanInstallment.create(
                    InstallmentId.generate(),
                    LoanId.of(loanId.toString()),
                    i + 1,
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
        Money totalSpent = Money.of(BigDecimal.ZERO);

        for (LoanInstallment installment : unpaidInstallments) {
            PaymentCalculation calculation = installment.calculatePaymentAmount(paymentDate);
            Money paymentRequired = calculation.getTotalAmount();

            if (remainingAmount.isGreaterThanOrEqualTo(paymentRequired)) {
                installment.markAsPaid(paymentDate, calculation);
                remainingAmount = remainingAmount.subtract(paymentRequired);
                totalSpent = totalSpent.add(paymentRequired);
                installmentsPaid++;

                if (remainingAmount.equals(Money.of(BigDecimal.ZERO))) {
                    break;
                }
            } else {
                break;
            }
        }

        updateLoanStatus();

        // Create a simple payment calculation for the result
        PaymentCalculation resultCalculation = PaymentCalculation.onTime(totalSpent);
        return PaymentResult.successful(resultCalculation);
    }

    private List<LoanInstallment> getPayableInstallments(LocalDate paymentDate) {
        return installments.stream()
                .filter(installment -> !installment.isPaid())
                .filter(installment -> !installment.getDueDate().isAfter(paymentDate.plusDays(30)))
                .sorted(Comparator.comparing(LoanInstallment::getDueDate))
                .collect(Collectors.toList());
    }

    private void updateLoanStatus() {
        this.isPaid = installments.stream().allMatch(LoanInstallment::isPaid);
    }

    // Getters following DDD principles - expose only necessary data
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Money getLoanAmount() { return loanAmount; }
    public Money getPrincipalAmount() { return principalAmount; }
    public InterestRate getInterestRate() { return interestRate; }
    public InstallmentCount getNumberOfInstallments() { return numberOfInstallments; }
    public LocalDate getCreateDate() { return createDate; }
    public boolean isPaid() { return isPaid; }
    public List<LoanInstallment> getInstallments() { return new ArrayList<>(installments); }

    // Domain events support for eventual consistency
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}
