package com.enterprise.openfinance.payeeverification.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmationSettingsTest {

    @Test
    void shouldCreateSettingsWhenValuesAreValid() {
        ConfirmationSettings settings = new ConfirmationSettings(85, Duration.ofMinutes(5));

        assertThat(settings.closeMatchThreshold()).isEqualTo(85);
        assertThat(settings.cacheTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectInvalidThreshold() {
        assertThatThrownBy(() -> new ConfirmationSettings(0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closeMatchThreshold must be between 1 and 99");

        assertThatThrownBy(() -> new ConfirmationSettings(100, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closeMatchThreshold must be between 1 and 99");
    }

    @Test
    void shouldRejectInvalidCacheTtl() {
        assertThatThrownBy(() -> new ConfirmationSettings(85, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cacheTtl must be positive");

        assertThatThrownBy(() -> new ConfirmationSettings(85, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cacheTtl must be positive");

        assertThatThrownBy(() -> new ConfirmationSettings(85, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cacheTtl must be positive");
    }
}
