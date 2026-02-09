package com.enterprise.openfinance.uc07.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldPreserveMessage() {
        assertThat(new ResourceNotFoundException("missing")).hasMessage("missing");
    }
}
