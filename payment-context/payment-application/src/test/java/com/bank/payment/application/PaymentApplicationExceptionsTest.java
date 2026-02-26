package com.bank.payment.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApplicationExceptionsTest {

    @Test
    void invalidAccountExceptionFactoriesShouldBuildMessages() {
        assertThat(InvalidAccountException.fromAccount("ACC-1").getMessage()).contains("from account");
        assertThat(InvalidAccountException.toAccount("ACC-2").getMessage()).contains("to account");
        assertThat(InvalidAccountException.forAccount("ACC-3", "blocked").getMessage()).contains("blocked");
    }

    @Test
    void paymentAndBalanceExceptionsShouldBuildMessages() {
        assertThat(PaymentNotFoundException.withId("PAY-X").getMessage()).contains("PAY-X");
        assertThat(InsufficientBalanceException.forAccount("ACC-1", "AED 10", "AED 2").getMessage())
            .contains("insufficient balance");
    }

    @Test
    void fraudExceptionShouldSupportRiskMetadata() {
        FraudDetectedException withRisk = FraudDetectedException.forPayment(
            "PAY-1",
            "Velocity exceeded",
            92,
            List.of("velocity", "new-device")
        );

        assertThat(withRisk.getMessage()).contains("PAY-1");
        assertThat(withRisk.getRiskScore()).isEqualTo(92);
        assertThat(withRisk.getRiskFactors()).containsExactly("velocity", "new-device");
        assertThat(FraudDetectedException.generic().getMessage()).contains("fraud detection system");
    }

    @Test
    void complianceExceptionShouldProvideFactoryMessages() {
        assertThat(ComplianceViolationException.forPayment("PAY-2", "sanctions").getMessage())
            .contains("sanctions");
        assertThat(ComplianceViolationException.generic().getMessage()).contains("compliance rules");
    }
}
