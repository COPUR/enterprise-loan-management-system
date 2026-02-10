package com.enterprise.openfinance.uc12.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class OnboardingAccountStatusTest {

    @Test
    void shouldExposeApiValues() {
        assertThat(OnboardingAccountStatus.OPENED.apiValue()).isEqualTo("Opened");
        assertThat(OnboardingAccountStatus.REJECTED.apiValue()).isEqualTo("Rejected");
    }
}
