package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAggregateTest {

    @Test
    void regularPaymentShouldInitializeAndExposeTotalAmount() {
        Payment payment = regularPayment("PAY-AGG-001", "1000.00", PaymentType.ACH);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getFee().getAmount()).isEqualByComparingTo("1.50");
        assertThat(payment.getTotalAmount().getAmount()).isEqualByComparingTo("1001.50");
        assertThat(payment.getDomainEvents())
            .anySatisfy(event -> assertThat(event).isInstanceOf(PaymentCreatedEvent.class));
    }

    @Test
    void regularPaymentShouldValidateInputRules() {
        assertThatThrownBy(() -> Payment.create(
            PaymentId.of("PAY-AGG-002"),
            CustomerId.of("CUST-AGG-002"),
            AccountId.of("ACC-1"),
            AccountId.of("ACC-2"),
            Money.aed(BigDecimal.ZERO),
            PaymentType.TRANSFER,
            "invalid"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> Payment.create(
            PaymentId.of("PAY-AGG-003"),
            CustomerId.of("CUST-AGG-003"),
            AccountId.of("ACC-SAME"),
            AccountId.of("ACC-SAME"),
            Money.aed(new BigDecimal("10.00")),
            PaymentType.TRANSFER,
            "invalid"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be the same");
    }

    @Test
    void regularLifecycleShouldEnforceStatusTransitions() {
        Payment payment = regularPayment("PAY-AGG-004", "250.00", PaymentType.TRANSFER);
        payment.markAsProcessing();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.isPending()).isFalse();

        payment.confirm();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.getCompletedAt()).isNotNull();

        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        assertThatThrownBy(payment::confirm)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be confirmed");
    }

    @Test
    void cancelAndFailShouldSetFailureReason() {
        Payment cancellable = regularPayment("PAY-AGG-005", "100.00", PaymentType.TRANSFER);
        cancellable.cancel("Customer request");
        assertThat(cancellable.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(cancellable.getFailureReason()).isEqualTo("Customer request");

        Payment failed = regularPayment("PAY-AGG-006", "100.00", PaymentType.TRANSFER);
        failed.fail("Network error");
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Network error");
        assertThat(failed.isFailed()).isTrue();
    }

    @Test
    void markAsProcessingShouldOnlyAllowPendingPayments() {
        Payment payment = regularPayment("PAY-AGG-007", "100.00", PaymentType.TRANSFER);
        payment.markAsProcessing();

        assertThatThrownBy(payment::markAsProcessing)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only pending payments");
    }

    @Test
    void loanPaymentShouldCalculateInterestPrincipalAndPenalty() {
        Payment loanPayment = Payment.createLoanPayment(
            PaymentId.of("PAY-LOAN-001"),
            CustomerId.of("CUST-LOAN-001"),
            LoanId.of("LOAN-001"),
            Money.aed(new BigDecimal("1000.00")),
            Money.aed(new BigDecimal("8000.00")),
            new BigDecimal("0.02")
        );

        assertThat(loanPayment.getPaymentType()).isEqualTo(PaymentType.LOAN_PAYMENT);
        assertThat(loanPayment.getInterestAmount().getAmount()).isEqualByComparingTo("160.00");
        assertThat(loanPayment.getPrincipalAmount().getAmount()).isEqualByComparingTo("840.00");

        loanPayment.markAsCompleted(Money.aed(new BigDecimal("1000.00")), "TRX-001");
        assertThat(loanPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(loanPayment.getTransactionReference()).isEqualTo("TRX-001");
        assertThat(loanPayment.getActualPaymentDate()).isNotNull();
        assertThat(loanPayment.getTotalAmount().getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void loanPaymentShouldSupportLatePenaltyAndFailurePath() {
        Payment latePayment = Payment.createLoanPayment(
            PaymentId.of("PAY-LOAN-002"),
            CustomerId.of("CUST-LOAN-002"),
            LoanId.of("LOAN-002"),
            Money.aed(new BigDecimal("1200.00")),
            LocalDate.now().minusDays(3)
        );

        assertThat(latePayment.isLate()).isTrue();
        latePayment.applyLatePenalty(new BigDecimal("0.05"));
        assertThat(latePayment.getPenaltyAmount().getAmount()).isEqualByComparingTo("60.00");
        assertThat(latePayment.getTotalAmount().getAmount()).isEqualByComparingTo("1260.00");

        latePayment.markAsFailed("Gateway timeout");
        assertThat(latePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(latePayment.getFailureReason()).isEqualTo("Gateway timeout");
    }

    @Test
    void loanPaymentConstructorsShouldValidateRequiredFields() {
        assertThatThrownBy(() -> Payment.createLoanPayment(
            null,
            CustomerId.of("CUST-LOAN-003"),
            LoanId.of("LOAN-003"),
            Money.aed(new BigDecimal("500.00")),
            LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment ID");

        assertThatThrownBy(() -> Payment.createLoanPayment(
            PaymentId.of("PAY-LOAN-003"),
            CustomerId.of("CUST-LOAN-003"),
            LoanId.of("LOAN-003"),
            Money.aed(BigDecimal.ZERO),
            LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Scheduled amount must be positive");
    }

    private static Payment regularPayment(String paymentId, String amount, PaymentType type) {
        return Payment.create(
            PaymentId.of(paymentId),
            CustomerId.of("CUST-" + paymentId.substring(4)),
            AccountId.of("ACC-FROM-" + paymentId.substring(4)),
            AccountId.of("ACC-TO-" + paymentId.substring(4)),
            Money.aed(new BigDecimal(amount)),
            type,
            "Test payment"
        );
    }
}
