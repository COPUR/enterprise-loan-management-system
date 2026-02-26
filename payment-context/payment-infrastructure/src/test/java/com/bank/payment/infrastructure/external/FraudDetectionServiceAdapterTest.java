package com.bank.payment.infrastructure.external;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.LoanId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FraudDetectionServiceAdapterTest {

    private final FraudDetectionServiceAdapter adapter = new FraudDetectionServiceAdapter();

    @Test
    void riskScoreAndValidationShouldReflectAmountAndPatterns() {
        Payment lowRisk = payment("PAY-FRD-001", "500.00", PaymentType.TRANSFER);
        Payment highRisk = payment("PAY-FRD-002", "60000.00", PaymentType.WIRE_TRANSFER);
        Payment suspiciousRound = payment("PAY-FRD-003", "20000.00", PaymentType.TRANSFER);
        Payment suspiciousMobile = payment("PAY-FRD-004", "6000.00", PaymentType.MOBILE_PAYMENT);

        assertThat(adapter.calculateRiskScore(lowRisk)).isBetween(0, 100);
        assertThat(adapter.calculateRiskScore(highRisk)).isGreaterThan(80);
        assertThat(adapter.isValidPayment(lowRisk)).isTrue();
        assertThat(adapter.isValidPayment(highRisk)).isFalse();
        assertThat(adapter.isSuspiciousPattern(suspiciousRound)).isTrue();
        assertThat(adapter.isSuspiciousPattern(suspiciousMobile)).isTrue();
        assertThat(adapter.exceedsVelocityLimits(payment("PAY-FRD-005", "150000.00", PaymentType.TRANSFER))).isTrue();
    }

    @Test
    void isValidPaymentShouldUseConservativeFallbackOnAdapterError() {
        Payment lowLoanPayment = Payment.createLoanPayment(
            PaymentId.of("PAY-FRD-006"),
            CustomerId.of("CUST-FRD-006"),
            LoanId.of("LOAN-FRD-006"),
            Money.aed(new BigDecimal("500.00")),
            LocalDate.now()
        );
        Payment highLoanPayment = Payment.createLoanPayment(
            PaymentId.of("PAY-FRD-007"),
            CustomerId.of("CUST-FRD-007"),
            LoanId.of("LOAN-FRD-007"),
            Money.aed(new BigDecimal("5000.00")),
            LocalDate.now()
        );

        assertThat(adapter.isValidPayment(lowLoanPayment)).isTrue();
        assertThat(adapter.isValidPayment(highLoanPayment)).isFalse();
    }

    private static Payment payment(String id, String amount, PaymentType type) {
        String suffix = id.substring(4);
        return Payment.create(
            PaymentId.of(id),
            CustomerId.of("CUST-" + suffix),
            AccountId.of("ACC-FROM-" + suffix),
            AccountId.of("ACC-TO-" + suffix),
            Money.aed(new BigDecimal(amount)),
            type,
            "fraud-test"
        );
    }
}
