package com.enterprise.openfinance.uc08.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("missing");

        assertThat(exception.getMessage()).isEqualTo("missing");
    }
}
