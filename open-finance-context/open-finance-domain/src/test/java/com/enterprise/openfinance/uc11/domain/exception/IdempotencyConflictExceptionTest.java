package com.enterprise.openfinance.uc11.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IdempotencyConflictExceptionTest {

    @Test
    void shouldPreserveMessage() {
        IdempotencyConflictException ex = new IdempotencyConflictException("conflict");
        assertThat(ex.getMessage()).isEqualTo("conflict");
    }
}
