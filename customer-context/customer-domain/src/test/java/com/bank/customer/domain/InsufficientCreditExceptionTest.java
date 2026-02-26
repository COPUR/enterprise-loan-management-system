package com.bank.customer.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InsufficientCreditExceptionTest {

    @Test
    void constructorShouldStoreMessage() {
        InsufficientCreditException exception = new InsufficientCreditException("insufficient");

        assertThat(exception).hasMessage("insufficient");
    }

    @Test
    void constructorWithCauseShouldStoreMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        InsufficientCreditException exception = new InsufficientCreditException("insufficient", cause);

        assertThat(exception).hasMessage("insufficient");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
