package com.bank.payment.domain;

import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentValueObjectsTest {

    @Test
    void accountAndPaymentIdsShouldValidateAndGenerate() {
        AccountId accountId = AccountId.of("ACC-001");
        PaymentId paymentId = PaymentId.of("PAY-001");
        LoanId loanId = LoanId.of("LOAN-001");
        AccountId sameAccountId = AccountId.of("ACC-001");
        PaymentId samePaymentId = PaymentId.of("PAY-001");
        LoanId sameLoanId = LoanId.of("LOAN-001");

        assertThat(accountId.getValue()).isEqualTo("ACC-001");
        assertThat(accountId.isEmpty()).isFalse();
        assertThat(accountId).isEqualTo(sameAccountId);
        assertThat(accountId.hashCode()).isEqualTo(sameAccountId.hashCode());
        assertThat(accountId.toString()).contains("ACC-001");

        assertThat(paymentId.getValue()).isEqualTo("PAY-001");
        assertThat(paymentId.isEmpty()).isFalse();
        assertThat(paymentId).isEqualTo(samePaymentId);
        assertThat(paymentId.hashCode()).isEqualTo(samePaymentId.hashCode());
        assertThat(paymentId.toString()).contains("PAY-001");

        assertThat(loanId.getValue()).isEqualTo("LOAN-001");
        assertThat(loanId).isEqualTo(sameLoanId);
        assertThat(loanId.hashCode()).isEqualTo(sameLoanId.hashCode());
        assertThat(loanId.toString()).isEqualTo("LOAN-001");
        assertThat(AccountId.generate().getValue()).startsWith("ACC-");
        assertThat(PaymentId.generate().getValue()).startsWith("PAY-");
        assertThat(LoanId.generate().getValue()).isNotBlank();
    }

    @Test
    void idsShouldRejectInvalidValues() {
        assertThatThrownBy(() -> AccountId.of(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be empty");
        assertThatThrownBy(() -> PaymentId.of(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be empty");
        assertThatThrownBy(() -> LoanId.of(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null or empty");
    }

    @Test
    void paymentTypeShouldExposeFeeAndClassificationRules() {
        Money amount = Money.aed(new BigDecimal("100.00"));

        assertThat(PaymentType.WIRE_TRANSFER.calculateFee(amount).getAmount()).isEqualByComparingTo("25.00");
        assertThat(PaymentType.TRANSFER.calculateFee(amount).getAmount()).isEqualByComparingTo("0.00");
        assertThat(PaymentType.CHECK.isElectronic()).isFalse();
        assertThat(PaymentType.ACH.isElectronic()).isTrue();
        assertThat(PaymentType.WIRE_TRANSFER.requiresEnhancedValidation()).isTrue();
        assertThat(PaymentType.DEBIT_CARD.requiresEnhancedValidation()).isFalse();
        assertThat(PaymentType.BILL_PAYMENT.getDisplayName()).isEqualTo("Bill Payment");
    }

    @Test
    void paymentStatusLifecycleRulesShouldBeConsistent() {
        assertThat(PaymentStatus.PENDING.canBeConfirmed()).isTrue();
        assertThat(PaymentStatus.PROCESSING.canBeConfirmed()).isTrue();
        assertThat(PaymentStatus.PENDING.canBeCancelled()).isTrue();
        assertThat(PaymentStatus.PROCESSING.canBeCancelled()).isFalse();
        assertThat(PaymentStatus.COMPLETED.canBeRefunded()).isTrue();
        assertThat(PaymentStatus.FAILED.canBeRefunded()).isFalse();
        assertThat(PaymentStatus.SETTLED.isTerminalStatus()).isTrue();
        assertThat(PaymentStatus.DISPUTED.isTerminalStatus()).isFalse();
        assertThat(PaymentStatus.COMPLETED.getDisplayName()).isEqualTo("Completed Successfully");
    }
}
