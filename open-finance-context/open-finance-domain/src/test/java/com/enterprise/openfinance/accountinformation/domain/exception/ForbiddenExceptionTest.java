package com.enterprise.openfinance.accountinformation.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForbiddenExceptionTest {

    @Test
    void shouldExposeMessage() {
        ForbiddenException exception = new ForbiddenException("forbidden");

        assertThat(exception.getMessage()).isEqualTo("forbidden");
    }
}
