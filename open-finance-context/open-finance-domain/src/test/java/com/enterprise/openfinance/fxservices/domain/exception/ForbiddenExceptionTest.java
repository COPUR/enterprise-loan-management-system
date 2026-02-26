package com.enterprise.openfinance.fxservices.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ForbiddenExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ForbiddenException ex = new ForbiddenException("forbidden");
        assertThat(ex.getMessage()).isEqualTo("forbidden");
    }
}
