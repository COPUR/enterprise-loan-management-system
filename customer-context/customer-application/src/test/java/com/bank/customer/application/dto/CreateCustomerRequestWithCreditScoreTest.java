package com.bank.customer.application.dto;

import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCustomerRequestWithCreditScoreTest {

    @Test
    void validateShouldPassForValidInput() {
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("2500.00")),
            700
        );

        assertThatCode(request::validate).doesNotThrowAnyException();
    }

    @Test
    void validateShouldRejectOutOfRangeCreditScore() {
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("2500.00")),
            200
        );

        assertThatThrownBy(request::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 300 and 850");
    }

    @Test
    void validateShouldRejectNonPositiveIncome() {
        CreateCustomerRequestWithCreditScore request = new CreateCustomerRequestWithCreditScore(
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(BigDecimal.ZERO),
            700
        );

        assertThatThrownBy(request::validate)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Monthly income must be positive");
    }
}
