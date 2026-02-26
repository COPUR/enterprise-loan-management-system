package com.enterprise.openfinance.bankingmetadata.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForbiddenExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ForbiddenException exception = new ForbiddenException("forbidden");

        assertThat(exception).hasMessage("forbidden");
    }
}
