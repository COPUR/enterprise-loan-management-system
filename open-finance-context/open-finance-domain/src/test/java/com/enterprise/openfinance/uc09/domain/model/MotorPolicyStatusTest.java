package com.enterprise.openfinance.uc09.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MotorPolicyStatusTest {

    @Test
    void shouldExposeApiValuesAndActiveFlag() {
        assertThat(MotorPolicyStatus.ACTIVE.apiValue()).isEqualTo("Active");
        assertThat(MotorPolicyStatus.ACTIVE.isActive()).isTrue();
        assertThat(MotorPolicyStatus.LAPSED.isActive()).isFalse();
    }
}
