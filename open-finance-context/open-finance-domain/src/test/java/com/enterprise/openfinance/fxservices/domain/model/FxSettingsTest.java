package com.enterprise.openfinance.fxservices.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxSettingsTest {

    @Test
    void shouldCreateSettings() {
        FxSettings settings = new FxSettings(Duration.ofSeconds(30), Duration.ofHours(24), Duration.ofSeconds(30), 6);

        assertThat(settings.quoteTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.rateScale()).isEqualTo(6);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new FxSettings(Duration.ZERO, Duration.ofHours(24), Duration.ofSeconds(30), 6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxSettings(Duration.ofSeconds(30), Duration.ofHours(24), Duration.ofSeconds(30), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
