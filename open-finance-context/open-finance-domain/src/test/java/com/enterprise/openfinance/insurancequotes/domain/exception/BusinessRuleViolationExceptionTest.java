package com.enterprise.openfinance.insurancequotes.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BusinessRuleViolationExceptionTest {

    @Test
    void shouldPreserveMessage() {
        BusinessRuleViolationException exception = new BusinessRuleViolationException("rule");
        assertThat(exception.getMessage()).isEqualTo("rule");
    }
}
