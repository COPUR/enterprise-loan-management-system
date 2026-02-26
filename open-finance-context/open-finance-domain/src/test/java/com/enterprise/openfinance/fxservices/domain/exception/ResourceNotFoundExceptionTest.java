package com.enterprise.openfinance.fxservices.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ResourceNotFoundExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("missing");
        assertThat(ex.getMessage()).isEqualTo("missing");
    }
}
