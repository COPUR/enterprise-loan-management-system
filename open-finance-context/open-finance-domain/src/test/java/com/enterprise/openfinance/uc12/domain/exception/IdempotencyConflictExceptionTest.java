package com.enterprise.openfinance.uc12.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IdempotencyConflictExceptionTest {

    @Test
    void shouldPreserveMessage() {
        IdempotencyConflictException exception = new IdempotencyConflictException("conflict");
        assertThat(exception).hasMessage("conflict");
    }
}
