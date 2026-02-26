package com.enterprise.openfinance.dynamiconboarding.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingSettingsTest {

    @Test
    void shouldCreateSettings() {
        OnboardingSettings settings = new OnboardingSettings(
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                "ACC"
        );

        assertThat(settings.accountPrefix()).isEqualTo("ACC");
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new OnboardingSettings(Duration.ZERO, Duration.ofSeconds(30), "ACC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyTtl");

        assertThatThrownBy(() -> new OnboardingSettings(Duration.ofHours(24), Duration.ZERO, "ACC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new OnboardingSettings(Duration.ofHours(24), Duration.ofSeconds(30), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountPrefix");
    }
}
