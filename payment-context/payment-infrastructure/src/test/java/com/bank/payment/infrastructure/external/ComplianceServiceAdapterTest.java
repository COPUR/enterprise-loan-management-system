package com.bank.payment.infrastructure.external;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceServiceAdapterTest {

    private final ComplianceServiceAdapter adapter = new ComplianceServiceAdapter();

    @Test
    void validatePaymentShouldPassForCompliantTransaction() {
        Payment payment = payment(
            "PAY-CMP-001",
            "CUST-CMP-001",
            "ACC-FROM-001",
            "ACC-TO-001",
            "1000.00",
            PaymentType.TRANSFER
        );

        assertThat(adapter.validatePayment(payment)).isTrue();
        assertThat(adapter.requiresKycVerification(payment)).isFalse();
        assertThat(adapter.exceedsReportingThreshold(payment)).isFalse();
        assertThat(adapter.involvesSanctionedEntities(payment)).isFalse();
    }

    @Test
    void validatePaymentShouldFailForKycSanctionsAndEddScenarios() {
        Payment pendingKycWire = payment(
            "PAY-CMP-002",
            "PENDING_KYC-CUSTOMER",
            "ACC-FROM-002",
            "ACC-TO-002",
            "2000.00",
            PaymentType.WIRE_TRANSFER
        );
        Payment sanctioned = payment(
            "PAY-CMP-003",
            "CUST-CMP-003",
            "ACC-BLOCKED-003",
            "ACC-TO-003",
            "500.00",
            PaymentType.TRANSFER
        );
        Payment highValue = payment(
            "PAY-CMP-004",
            "CUST-CMP-004",
            "ACC-FROM-004",
            "ACC-TO-004",
            "60000.00",
            PaymentType.TRANSFER
        );

        assertThat(adapter.requiresKycVerification(pendingKycWire)).isTrue();
        assertThat(adapter.validatePayment(pendingKycWire)).isFalse();
        assertThat(adapter.involvesSanctionedEntities(sanctioned)).isTrue();
        assertThat(adapter.validatePayment(sanctioned)).isFalse();
        assertThat(adapter.exceedsReportingThreshold(highValue)).isTrue();
        assertThat(adapter.validatePayment(highValue)).isFalse();
    }

    @Test
    void generateComplianceReportShouldIncludeRelevantFlags() {
        Payment reportableWire = payment(
            "PAY-CMP-005",
            "CUST-CMP-005",
            "ACC-FROM-005",
            "ACC-TO-005",
            "15000.00",
            PaymentType.WIRE_TRANSFER
        );

        String report = adapter.generateComplianceReport(reportableWire);

        assertThat(report).contains("COMPLIANCE REPORT");
        assertThat(report).contains("Payment ID: PAY-CMP-005");
        assertThat(report).contains("FLAG: Exceeds reporting threshold");
        assertThat(report).contains("FLAG: Requires KYC verification");
        assertThat(report).contains("FLAG: Wire transfer - enhanced monitoring");
    }

    private static Payment payment(
        String paymentId,
        String customerId,
        String fromAccount,
        String toAccount,
        String amount,
        PaymentType paymentType
    ) {
        return Payment.create(
            PaymentId.of(paymentId),
            CustomerId.of(customerId),
            AccountId.of(fromAccount),
            AccountId.of(toAccount),
            Money.aed(new BigDecimal(amount)),
            paymentType,
            "compliance-test"
        );
    }
}
