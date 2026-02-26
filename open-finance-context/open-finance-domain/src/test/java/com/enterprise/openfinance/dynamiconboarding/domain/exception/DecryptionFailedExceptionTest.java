package com.enterprise.openfinance.dynamiconboarding.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DecryptionFailedExceptionTest {

    @Test
    void shouldPreserveMessage() {
        DecryptionFailedException exception = new DecryptionFailedException("decrypt");
        assertThat(exception).hasMessage("decrypt");
    }
}
