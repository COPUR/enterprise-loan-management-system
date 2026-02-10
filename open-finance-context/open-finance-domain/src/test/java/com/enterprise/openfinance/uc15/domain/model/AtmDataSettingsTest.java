package com.enterprise.openfinance.uc15.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtmDataSettingsTest {

    @Test
    void shouldCreateSettingsWithPositiveTtl() {
        AtmDataSettings settings = new AtmDataSettings(Duration.ofMinutes(5));

        assertThat(settings.cacheTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectNonPositiveTtl() {
        assertThatThrownBy(() -> new AtmDataSettings(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new AtmDataSettings(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");
    }
}
