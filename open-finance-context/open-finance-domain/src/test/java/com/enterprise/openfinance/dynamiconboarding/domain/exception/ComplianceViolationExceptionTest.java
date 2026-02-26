package com.enterprise.openfinance.dynamiconboarding.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ComplianceViolationExceptionTest {

    @Test
    void shouldPreserveMessage() {
        ComplianceViolationException exception = new ComplianceViolationException("rejected");
        assertThat(exception).hasMessage("rejected");
    }
}
