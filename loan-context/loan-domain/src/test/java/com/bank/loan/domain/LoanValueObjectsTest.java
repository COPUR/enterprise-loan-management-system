package com.bank.loan.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanValueObjectsTest {

    @Test
    void loanTermFactoriesAndClassificationsShouldWork() {
        LoanTerm shortTerm = LoanTerm.ofMonths(6);
        LoanTerm mediumTerm = LoanTerm.ofYears(3);
        LoanTerm longTerm = LoanTerm.of(6, 0);

        assertThat(shortTerm.getMonths()).isEqualTo(6);
        assertThat(shortTerm.isShortTerm()).isTrue();
        assertThat(mediumTerm.getMonths()).isEqualTo(36);
        assertThat(mediumTerm.isMediumTerm()).isTrue();
        assertThat(longTerm.getMonths()).isEqualTo(72);
        assertThat(longTerm.isLongTerm()).isTrue();
        assertThat(LoanTerm.of(2, 6).toString()).isEqualTo("2 years 6 months");
    }

    @Test
    void loanTermShouldRejectInvalidRanges() {
        assertThatThrownBy(() -> LoanTerm.ofMonths(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");

        assertThatThrownBy(() -> LoanTerm.ofMonths(601))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("600 months");
    }

    @Test
    void interestRateShouldSupportCoreCalculations() {
        InterestRate rate = InterestRate.of(new BigDecimal("12.00"));

        assertThat(rate.getAnnualRate()).isEqualByComparingTo("12.0000");
        assertThat(rate.getMonthlyRate()).isEqualByComparingTo("0.0100000000");
        assertThat(rate.getDailyRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(rate.isZero()).isFalse();
        assertThat(rate.isNegative()).isFalse();
        assertThat(rate.toString()).isEqualTo("12.0000%");
    }

    @Test
    void interestRateShouldRejectOutOfBoundsValues() {
        assertThatThrownBy(() -> InterestRate.of(new BigDecimal("-0.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative");

        assertThatThrownBy(() -> InterestRate.of(new BigDecimal("100.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("100%");
    }

    @Test
    void idsShouldGenerateAndValidateCorrectly() {
        LoanId generatedLoanId = LoanId.generate();
        LoanId fromLong = LoanId.fromLong(12L);
        PaymentId generatedPaymentId = PaymentId.generate();
        PaymentId manualPaymentId = PaymentId.of("PAY-001");

        assertThat(generatedLoanId.getValue()).startsWith("LOAN-");
        assertThat(fromLong.getValue()).isEqualTo("LOAN-00000012");
        assertThat(LoanId.of("  LOAN-ABC  ").getValue()).isEqualTo("LOAN-ABC");
        assertThat(generatedPaymentId.getValue()).isNotBlank();
        assertThat(manualPaymentId.toString()).contains("PAY-001");
    }

    @Test
    void idsShouldRejectInvalidInputs() {
        assertThatThrownBy(() -> LoanId.of(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null or empty");

        assertThatThrownBy(() -> LoanId.fromLong(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("cannot be null");

        assertThatThrownBy(() -> PaymentId.of(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be empty");
    }

    @Test
    void loanStatusLifecycleFlagsShouldBeConsistent() {
        assertThat(LoanStatus.DISBURSED.canAcceptPayments()).isTrue();
        assertThat(LoanStatus.APPROVED.canBeDisbursed()).isTrue();
        assertThat(LoanStatus.CREATED.canBeApproved()).isTrue();
        assertThat(LoanStatus.CREATED.canBeRejected()).isTrue();
        assertThat(LoanStatus.APPROVED.canBeCancelled()).isTrue();
        assertThat(LoanStatus.REJECTED.isTerminalStatus()).isTrue();
        assertThat(LoanStatus.ACTIVE.isTerminalStatus()).isFalse();
        assertThat(LoanStatus.DEFAULTED.getDisplayName()).isEqualTo("Defaulted");
    }
}
