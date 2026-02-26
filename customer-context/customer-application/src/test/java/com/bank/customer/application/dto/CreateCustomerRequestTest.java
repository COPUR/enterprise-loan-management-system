package com.bank.customer.application.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCustomerRequestTest {

    @Test
    void getCreditLimitAsMoneyShouldDefaultToUsdWhenCurrencyMissing() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("5000.00"),
            null
        );

        assertThat(request.getCreditLimitAsMoney().getCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(request.getCreditLimitAsMoney().getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void validateShouldRejectShortFirstName() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "A",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("5000.00"),
            "AED"
        );

        assertThatThrownBy(request::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("First name must be at least 2 characters");
    }

    @Test
    void validateShouldRejectTooSmallAndTooLargeCreditLimit() {
        CreateCustomerRequest small = new CreateCustomerRequest(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("999.00"),
            "AED"
        );
        CreateCustomerRequest large = new CreateCustomerRequest(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            new BigDecimal("1000001.00"),
            "AED"
        );

        assertThatThrownBy(small::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minimum credit limit");
        assertThatThrownBy(large::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maximum credit limit");
    }
}
