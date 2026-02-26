package com.enterprise.openfinance.insurancequotes.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ResourceNotFoundExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("missing");
        assertThat(exception.getMessage()).isEqualTo("missing");
    }
}
