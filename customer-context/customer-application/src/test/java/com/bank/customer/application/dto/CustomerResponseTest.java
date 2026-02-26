package com.bank.customer.application.dto;

import com.bank.customer.domain.Customer;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerResponseTest {

    @Test
    void fromShouldMapCustomerFields() {
        Customer customer = Customer.createWithCreditScore(
            CustomerId.of("CUST-RESP-001"),
            "Ali",
            "Copur",
            "ali@example.com",
            "+971500000001",
            Money.aed(new BigDecimal("2000.00")),
            700
        );
        customer.reserveCredit(Money.aed(new BigDecimal("500.00")));

        CustomerResponse response = CustomerResponse.from(customer);

        assertThat(response.customerId()).isEqualTo("CUST-RESP-001");
        assertThat(response.firstName()).isEqualTo("Ali");
        assertThat(response.lastName()).isEqualTo("Copur");
        assertThat(response.email()).isEqualTo("ali@example.com");
        assertThat(response.creditLimit()).isEqualByComparingTo("8000.00");
        assertThat(response.usedCredit()).isEqualByComparingTo("500.00");
        assertThat(response.availableCredit()).isEqualByComparingTo("7500.00");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.creditScore()).isEqualTo(700);
        assertThat(response.monthlyIncome()).isEqualByComparingTo("2000.00");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.lastModifiedAt()).isNotNull();
    }
}
