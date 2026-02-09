package com.enterprise.openfinance.uc04.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("not found");

        assertThat(exception).hasMessage("not found");
    }
}
