package com.enterprise.openfinance.insurancequotes.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ForbiddenExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ForbiddenException exception = new ForbiddenException("forbidden");
        assertThat(exception.getMessage()).isEqualTo("forbidden");
    }
}
