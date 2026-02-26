package com.bank.loan.application.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateLoanRequestTest {

    @Test
    void getPrincipalAsMoneyShouldUseProvidedCurrencyOrDefaultUsd() {
        CreateLoanRequest withCurrency = new CreateLoanRequest(
            "CUST-001",
            new BigDecimal("10000.00"),
            "AED",
            new BigDecimal("7.5"),
            24
        );
        CreateLoanRequest withoutCurrency = new CreateLoanRequest(
            "CUST-002",
            new BigDecimal("10000.00"),
            null,
            new BigDecimal("7.5"),
            24
        );

        assertThat(withCurrency.getPrincipalAsMoney().getCurrency().getCurrencyCode()).isEqualTo("AED");
        assertThat(withoutCurrency.getPrincipalAsMoney().getCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void validateShouldAcceptBoundaryValues() {
        CreateLoanRequest min = new CreateLoanRequest(
            "CUST-003",
            new BigDecimal("1000.00"),
            "USD",
            new BigDecimal("0.01"),
            6
        );
        CreateLoanRequest max = new CreateLoanRequest(
            "CUST-004",
            new BigDecimal("10000000.00"),
            "USD",
            new BigDecimal("50.00"),
            480
        );

        min.validate();
        max.validate();
    }

    @Test
    void validateShouldRejectOutOfRangeInputs() {
        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-005", new BigDecimal("999.99"), "USD", new BigDecimal("5.0"), 12
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minimum loan amount");

        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-006", new BigDecimal("10000000.01"), "USD", new BigDecimal("5.0"), 12
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum loan amount");

        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-007", new BigDecimal("2000.00"), "USD", BigDecimal.ZERO, 12
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Interest rate must be positive");

        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-008", new BigDecimal("2000.00"), "USD", new BigDecimal("50.01"), 12
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot exceed 50%");

        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-009", new BigDecimal("2000.00"), "USD", new BigDecimal("5.0"), 5
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minimum loan term");

        assertThatThrownBy(() -> new CreateLoanRequest(
            "CUST-010", new BigDecimal("2000.00"), "USD", new BigDecimal("5.0"), 481
        ).validate()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum loan term");
    }
}
