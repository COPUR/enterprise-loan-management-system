package com.bank.customer.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNotFoundExceptionTest {

    @Test
    void withIdShouldBuildHelpfulMessage() {
        CustomerNotFoundException exception = CustomerNotFoundException.withId("CUST-123");

        assertThat(exception).hasMessage("Customer not found with ID: CUST-123");
    }

    @Test
    void constructorWithCauseShouldRetainCause() {
        RuntimeException cause = new RuntimeException("root cause");
        CustomerNotFoundException exception = new CustomerNotFoundException("CUST-123", cause);

        assertThat(exception).hasMessage("Customer not found with ID: CUST-123");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
