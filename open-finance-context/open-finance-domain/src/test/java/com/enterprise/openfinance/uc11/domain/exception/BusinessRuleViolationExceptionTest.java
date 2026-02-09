package com.enterprise.openfinance.uc11.domain.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BusinessRuleViolationExceptionTest {

    @Test
    void shouldPreserveMessage() {
        BusinessRuleViolationException ex = new BusinessRuleViolationException("rule");
        assertThat(ex.getMessage()).isEqualTo("rule");
    }
}
