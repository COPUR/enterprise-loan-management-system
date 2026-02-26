package com.bank.payment.application.dto;

import com.bank.payment.domain.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePaymentRequestTest {

    @Test
    void getAmountAsMoneyShouldUseProvidedCurrencyOrDefaultUsd() {
        CreatePaymentRequest withCurrency = new CreatePaymentRequest(
            "CUST-001",
            "ACC-1",
            "ACC-2",
            new BigDecimal("100.00"),
            "AED",
            PaymentType.TRANSFER,
            "Payment"
        );
        CreatePaymentRequest withoutCurrency = new CreatePaymentRequest(
            "CUST-002",
            "ACC-3",
            "ACC-4",
            new BigDecimal("100.00"),
            null,
            PaymentType.TRANSFER,
            "Payment"
        );

        assertThat(withCurrency.getAmountAsMoney().getCurrency().getCurrencyCode()).isEqualTo("AED");
        assertThat(withoutCurrency.getAmountAsMoney().getCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void validateShouldAcceptBoundaries() {
        CreatePaymentRequest min = new CreatePaymentRequest(
            "CUST-003",
            "ACC-5",
            "ACC-6",
            new BigDecimal("0.01"),
            "USD",
            PaymentType.TRANSFER,
            null
        );
        CreatePaymentRequest max = new CreatePaymentRequest(
            "CUST-004",
            "ACC-7",
            "ACC-8",
            new BigDecimal("100000000.00"),
            "USD",
            PaymentType.WIRE_TRANSFER,
            null
        );

        min.validate();
        max.validate();
    }

    @Test
    void validateShouldRejectInvalidRules() {
        assertThatThrownBy(() -> new CreatePaymentRequest(
            "CUST-005",
            "ACC-1",
            "ACC-2",
            new BigDecimal("0.001"),
            "USD",
            PaymentType.TRANSFER,
            null
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minimum payment amount");

        assertThatThrownBy(() -> new CreatePaymentRequest(
            "CUST-006",
            "ACC-1",
            "ACC-2",
            new BigDecimal("100000000.01"),
            "USD",
            PaymentType.TRANSFER,
            null
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum payment amount");

        assertThatThrownBy(() -> new CreatePaymentRequest(
            "CUST-007",
            "ACC-1",
            "ACC-1",
            new BigDecimal("10.00"),
            "USD",
            PaymentType.TRANSFER,
            null
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be the same");
    }
}
