package com.bank.payment.application.dto;

import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResponseTest {

    @Test
    void fromShouldMapPaymentAggregateFields() {
        Payment payment = Payment.create(
            PaymentId.of("PAY-RESP-001"),
            CustomerId.of("CUST-RESP-001"),
            AccountId.of("ACC-FROM-001"),
            AccountId.of("ACC-TO-001"),
            Money.aed(new BigDecimal("1000.00")),
            PaymentType.ACH,
            "Invoice payment"
        );

        PaymentResponse response = PaymentResponse.from(payment);

        assertThat(response.paymentId()).isEqualTo("PAY-RESP-001");
        assertThat(response.customerId()).isEqualTo("CUST-RESP-001");
        assertThat(response.fromAccountId()).isEqualTo("ACC-FROM-001");
        assertThat(response.toAccountId()).isEqualTo("ACC-TO-001");
        assertThat(response.amount()).isEqualByComparingTo("1000.00");
        assertThat(response.fee()).isEqualByComparingTo("1.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("1001.50");
        assertThat(response.currency()).isEqualTo("AED");
        assertThat(response.paymentType()).isEqualTo("ACH");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.lastModifiedAt()).isNotNull();
    }
}
