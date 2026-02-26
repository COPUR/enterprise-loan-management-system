package com.bank.loan.domain;

import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanPaymentModelsTest {

    @Test
    void paymentDistributionShouldValidateAndExposePercentages() {
        PaymentDistribution distribution = PaymentDistribution.builder()
            .totalPayment(Money.aed(new BigDecimal("1000.00")))
            .principalPayment(Money.aed(new BigDecimal("800.00")))
            .interestPayment(Money.aed(new BigDecimal("200.00")))
            .feePayment(Money.aed(BigDecimal.ZERO))
            .previousBalance(Money.aed(new BigDecimal("5000.00")))
            .paymentDate(LocalDate.now())
            .paymentNotes("Monthly installment")
            .build();

        distribution.validate();

        assertThat(distribution.isValid()).isTrue();
        assertThat(distribution.getEffectivePayment().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(distribution.hasInterestComponent()).isTrue();
        assertThat(distribution.hasFeeComponent()).isFalse();
        assertThat(distribution.getPrincipalPercentage()).isEqualByComparingTo("80.0000");
        assertThat(distribution.getInterestPercentage()).isEqualByComparingTo("20.0000");
        assertThat(distribution.getSummary()).contains("Payment Distribution");
    }

    @Test
    void paymentDistributionShouldRejectInvalidMath() {
        PaymentDistribution invalid = PaymentDistribution.builder()
            .totalPayment(Money.aed(new BigDecimal("1000.00")))
            .principalPayment(Money.aed(new BigDecimal("700.00")))
            .interestPayment(Money.aed(new BigDecimal("200.00")))
            .feePayment(Money.aed(BigDecimal.ZERO))
            .previousBalance(Money.aed(new BigDecimal("5000.00")))
            .paymentDate(LocalDate.now())
            .build();

        assertThat(invalid.isValid()).isFalse();
        assertThatThrownBy(invalid::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mathematically incorrect");
    }

    @Test
    void paymentResultShouldRepresentSuccessAndFailureStates() {
        PaymentDistribution distribution = PaymentDistribution.builder()
            .totalPayment(Money.aed(new BigDecimal("500.00")))
            .principalPayment(Money.aed(new BigDecimal("500.00")))
            .interestPayment(Money.aed(BigDecimal.ZERO))
            .feePayment(Money.aed(BigDecimal.ZERO))
            .previousBalance(Money.aed(new BigDecimal("500.00")))
            .paymentDate(LocalDate.now())
            .build();

        PaymentResult success = PaymentResult.success(
            LoanId.of("LOAN-PAY-001"),
            PaymentId.of("PAY-001"),
            distribution,
            Money.aed(BigDecimal.ZERO),
            LoanStatus.FULLY_PAID
        );
        PaymentResult successWithWarnings = PaymentResult.successWithWarnings(
            LoanId.of("LOAN-PAY-002"),
            PaymentId.of("PAY-002"),
            distribution,
            Money.aed(new BigDecimal("100.00")),
            LoanStatus.DISBURSED,
            "Manual verification completed"
        );
        PaymentResult failure = PaymentResult.failure(LoanId.of("LOAN-PAY-003"), "Gateway timeout");

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.isLoanFullyPaid()).isTrue();
        assertThat(success.getBalanceReduction().getAmount()).isEqualByComparingTo("500.00");
        assertThat(success.getSummary()).contains("Payment Processed Successfully");
        assertThat(success.getDetailedBreakdown()).contains("PAYMENT BREAKDOWN DETAILS");

        assertThat(successWithWarnings.hasWarnings()).isTrue();
        assertThat(successWithWarnings.getSummary()).contains("Note");

        assertThat(failure.hasFailed()).isTrue();
        assertThat(failure.getSummary()).contains("Payment Processing Failed");
        assertThat(failure.getDetailedBreakdown()).contains("not available");
    }
}
