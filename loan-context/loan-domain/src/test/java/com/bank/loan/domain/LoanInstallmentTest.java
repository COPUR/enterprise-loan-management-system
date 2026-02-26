package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanInstallmentTest {

    @Test
    void createShouldInitializeInstallmentAsPending() {
        LoanInstallment installment = LoanInstallment.create(
            LoanId.of("LOAN-INST-001"),
            CustomerId.of("CUST-INST-001"),
            1,
            Money.aed(new BigDecimal("1000.00")),
            LocalDate.now().plusDays(10)
        );

        assertThat(installment.getInstallmentNumber()).isEqualTo(1);
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(installment.getPaidAmount()).isEqualTo(Money.aed(BigDecimal.ZERO));
        assertThat(installment.getRemainingAmount()).isEqualTo(Money.aed(new BigDecimal("1000.00")));
        assertThat(installment.isPaid()).isFalse();
        assertThat(installment.isOverdue()).isFalse();
    }

    @Test
    void makePaymentShouldMoveToPartiallyPaidThenPaid() {
        LoanInstallment installment = LoanInstallment.create(
            LoanId.of("LOAN-INST-002"),
            CustomerId.of("CUST-INST-002"),
            1,
            Money.aed(new BigDecimal("1000.00")),
            LocalDate.now().plusDays(1)
        );

        installment.makePayment(Money.aed(new BigDecimal("300.00")));
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
        assertThat(installment.getPaidAmount().getAmount()).isEqualByComparingTo("300.00");
        assertThat(installment.getRemainingAmount().getAmount()).isEqualByComparingTo("700.00");
        assertThat(installment.getPaidDate()).isNull();

        installment.makePayment(Money.aed(new BigDecimal("700.00")));
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(installment.getPaidAmount().getAmount()).isEqualByComparingTo("1000.00");
        assertThat(installment.getRemainingAmount().getAmount()).isEqualByComparingTo("0.00");
        assertThat(installment.getPaidDate()).isNotNull();
        assertThat(installment.isPaid()).isTrue();
    }

    @Test
    void shouldReportOverdueWhenDueDateInPastAndNotPaid() {
        LoanInstallment installment = LoanInstallment.create(
            LoanId.of("LOAN-INST-003"),
            CustomerId.of("CUST-INST-003"),
            1,
            Money.aed(new BigDecimal("900.00")),
            LocalDate.now().minusDays(1)
        );

        assertThat(installment.isOverdue()).isTrue();
    }

    @Test
    void shouldRejectInvalidInstallmentCreationAndPayments() {
        assertThatThrownBy(() -> LoanInstallment.create(
            LoanId.of("LOAN-INST-004"),
            CustomerId.of("CUST-INST-004"),
            0,
            Money.aed(new BigDecimal("1000.00")),
            LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("number must be positive");

        LoanInstallment installment = LoanInstallment.create(
            LoanId.of("LOAN-INST-005"),
            CustomerId.of("CUST-INST-005"),
            1,
            Money.aed(new BigDecimal("1000.00")),
            LocalDate.now().plusDays(5)
        );

        assertThatThrownBy(() -> installment.makePayment(Money.aed(BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> installment.makePayment(Money.aed(new BigDecimal("1100.00"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds installment amount");

        installment.makePayment(Money.aed(new BigDecimal("1000.00")));
        assertThatThrownBy(() -> installment.makePayment(Money.aed(new BigDecimal("1.00"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already paid");
    }
}
