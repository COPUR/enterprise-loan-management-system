package com.enterprise.openfinance.personalfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AisSettingsTest {

    @Test
    void shouldCreateValidSettings() {
        AisSettings settings = new AisSettings(Duration.ofSeconds(30), 50, 100);

        assertThat(settings.defaultPageSize()).isEqualTo(50);
        assertThat(settings.maxPageSize()).isEqualTo(100);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new AisSettings(Duration.ZERO, 50, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new AisSettings(Duration.ofSeconds(30), 0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultPageSize");

        assertThatThrownBy(() -> new AisSettings(Duration.ofSeconds(30), 100, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPageSize");
    }
}
