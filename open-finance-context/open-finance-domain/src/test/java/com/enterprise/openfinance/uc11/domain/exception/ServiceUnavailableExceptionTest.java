package com.enterprise.openfinance.uc11.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ServiceUnavailableExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ServiceUnavailableException ex = new ServiceUnavailableException("market closed");
        assertThat(ex.getMessage()).isEqualTo("market closed");
    }
}
