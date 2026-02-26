package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanAggregateTest {

    @Test
    void createShouldInitializeStateAndEmitCreatedEvent() {
        Loan loan = Loan.create(
            LoanId.of("LOAN-AGG-001"),
            CustomerId.of("CUST-AGG-001"),
            Money.aed(new BigDecimal("10000.00")),
            InterestRate.of(new BigDecimal("5.0")),
            LoanTerm.ofMonths(12)
        );

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CREATED);
        assertThat(loan.getOutstandingBalance()).isEqualTo(Money.aed(new BigDecimal("10000.00")));
        assertThat(loan.getApplicationDate()).isNotNull();
        assertThat(loan.getDomainEvents())
            .anySatisfy(event -> assertThat(event).isInstanceOf(LoanCreatedEvent.class));
    }

    @Test
    void lifecycleTransitionsShouldFollowRules() {
        Loan loan = loan("LOAN-AGG-002", "20000.00", "6.0", 24);

        loan.approve();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(loan.getApprovalDate()).isNotNull();

        loan.disburse();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(loan.getDisbursementDate()).isNotNull();
        assertThat(loan.getMaturityDate()).isNotNull();
    }

    @Test
    void invalidLifecycleTransitionsShouldFailFast() {
        Loan loan = loan("LOAN-AGG-003", "15000.00", "7.0", 24);

        assertThatThrownBy(loan::disburse)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be disbursed");

        loan.reject("Credit criteria not met");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.REJECTED);

        assertThatThrownBy(loan::approve)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be approved");
    }

    @Test
    void cancelShouldWorkOnlyInAllowedStates() {
        Loan loan = loan("LOAN-AGG-004", "18000.00", "5.0", 24);

        loan.cancel("Customer withdrawn");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CANCELLED);

        Loan disbursed = approvedAndDisbursedLoan("LOAN-AGG-005");
        assertThatThrownBy(() -> disbursed.cancel("Too late"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void makePaymentShouldRequirePayableStateAndValidAmount() {
        Loan loan = loan("LOAN-AGG-006", "9000.00", "4.0", 12);

        assertThatThrownBy(() -> loan.makePayment(Money.aed(new BigDecimal("100.00"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot accept payments");

        Loan disbursed = approvedAndDisbursedLoan("LOAN-AGG-007");
        assertThatThrownBy(() -> disbursed.makePayment(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null");
        assertThatThrownBy(() -> disbursed.makePayment(Money.aed(BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> disbursed.makePayment(Money.aed(new BigDecimal("20000.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot exceed outstanding balance");
    }

    @Test
    void makePaymentShouldReduceBalanceAndReturnDistribution() {
        Loan disbursed = approvedAndDisbursedLoan("LOAN-AGG-008");
        Money previous = disbursed.getOutstandingBalance();

        PaymentResult result = disbursed.makePayment(Money.aed(new BigDecimal("1000.00")));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentDistribution().getPrincipalPayment().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getPaymentDistribution().getInterestPayment().getAmount()).isEqualByComparingTo("0.00");
        assertThat(disbursed.getOutstandingBalance().getAmount())
            .isEqualByComparingTo(previous.subtract(Money.aed(new BigDecimal("1000.00"))).getAmount());
        assertThat(disbursed.getDomainEvents())
            .anySatisfy(event -> assertThat(event).isInstanceOf(LoanPaymentMadeEvent.class));
    }

    @Test
    void fullPaymentShouldMarkLoanAsFullyPaidAndEmitEvent() {
        Loan disbursed = approvedAndDisbursedLoan("LOAN-AGG-009");

        PaymentResult result = disbursed.makePayment(disbursed.getOutstandingBalance());

        assertThat(disbursed.getStatus()).isEqualTo(LoanStatus.FULLY_PAID);
        assertThat(disbursed.getOutstandingBalance().isZero()).isTrue();
        assertThat(result.isLoanFullyPaid()).isTrue();
        assertThat(disbursed.getDomainEvents())
            .anySatisfy(event -> assertThat(event).isInstanceOf(LoanFullyPaidEvent.class));
    }

    @Test
    void calculateMonthlyPaymentShouldSupportZeroAndNonZeroInterest() {
        Loan zeroInterest = Loan.create(
            LoanId.of("LOAN-AGG-010"),
            CustomerId.of("CUST-AGG-010"),
            Money.aed(new BigDecimal("12000.00")),
            InterestRate.zero(),
            LoanTerm.ofMonths(12)
        );
        Loan oneMonth = Loan.create(
            LoanId.of("LOAN-AGG-011"),
            CustomerId.of("CUST-AGG-011"),
            Money.aed(new BigDecimal("5000.00")),
            InterestRate.of(new BigDecimal("6.0")),
            LoanTerm.ofMonths(1)
        );

        assertThat(zeroInterest.calculateMonthlyPayment().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(oneMonth.calculateMonthlyPayment().getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void createWithInstallmentsShouldGenerateAndTrackInstallments() {
        Loan loan = Loan.createWithInstallments(
            LoanId.of("LOAN-AGG-012"),
            CustomerId.of("CUST-AGG-012"),
            Money.aed(new BigDecimal("12000.00")),
            InterestRate.zero(),
            LoanTerm.ofMonths(12)
        );

        assertThat(loan.getInstallments()).hasSize(12);
        assertThat(loan.getRemainingInstallments()).isEqualTo(12);
        assertThat(loan.getTotalInstallmentAmount().getAmount()).isEqualByComparingTo("12000.00");
        assertThat(loan.getTotalInterest().getAmount()).isEqualByComparingTo("0.00");
        assertThat(loan.getRemainingInstallmentAmount().getAmount()).isEqualByComparingTo("12000.00");
        assertThat(loan.getOverdueInstallments()).isEmpty();
        assertThat(loan.isFullyPaid()).isFalse();

        loan.getInstallments().forEach(installment -> installment.makePayment(installment.getAmount()));
        assertThat(loan.isFullyPaid()).isTrue();
        assertThat(loan.getRemainingInstallments()).isZero();
    }

    @Test
    void createWithInstallmentsShouldValidateBounds() {
        assertThatThrownBy(() -> Loan.createWithInstallments(
            LoanId.of("LOAN-AGG-013"),
            CustomerId.of("CUST-AGG-013"),
            Money.aed(new BigDecimal("999.99")),
            InterestRate.of(new BigDecimal("5.0")),
            LoanTerm.ofMonths(12)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least");

        assertThatThrownBy(() -> Loan.createWithInstallments(
            LoanId.of("LOAN-AGG-014"),
            CustomerId.of("CUST-AGG-014"),
            Money.aed(new BigDecimal("10000.00")),
            InterestRate.of(new BigDecimal("5.0")),
            LoanTerm.ofMonths(5)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least");
    }

    private static Loan approvedAndDisbursedLoan(String loanId) {
        Loan loan = loan(loanId, "10000.00", "5.0", 12);
        loan.approve();
        loan.disburse();
        return loan;
    }

    private static Loan loan(String loanId, String amount, String rate, int termInMonths) {
        return Loan.create(
            LoanId.of(loanId),
            CustomerId.of("CUST-" + loanId.substring(5)),
            Money.aed(new BigDecimal(amount)),
            InterestRate.of(new BigDecimal(rate)),
            LoanTerm.ofMonths(termInMonths)
        );
    }
}
