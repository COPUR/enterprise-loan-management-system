package com.enterprise.openfinance.uc08.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyConflictExceptionTest {

    @Test
    void shouldPreserveMessage() {
        IdempotencyConflictException exception = new IdempotencyConflictException("conflict");

        assertThat(exception.getMessage()).isEqualTo("conflict");
    }
}
