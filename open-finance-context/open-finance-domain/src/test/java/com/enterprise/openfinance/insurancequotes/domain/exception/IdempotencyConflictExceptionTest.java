package com.enterprise.openfinance.insurancequotes.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IdempotencyConflictExceptionTest {

    @Test
    void shouldPreserveMessage() {
        IdempotencyConflictException exception = new IdempotencyConflictException("conflict");
        assertThat(exception.getMessage()).isEqualTo("conflict");
    }
}
