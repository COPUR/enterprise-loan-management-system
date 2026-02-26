package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataSettingsTest {

    @Test
    void shouldExposeSettings() {
        MetadataSettings settings = new MetadataSettings(Duration.ofSeconds(30), 100, 250);

        assertThat(settings.cacheTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.defaultPageSize()).isEqualTo(100);
        assertThat(settings.maxPageSize()).isEqualTo(250);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new MetadataSettings(Duration.ZERO, 100, 250))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new MetadataSettings(Duration.ofSeconds(30), 0, 250))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultPageSize");

        assertThatThrownBy(() -> new MetadataSettings(Duration.ofSeconds(30), 200, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPageSize");
    }
}
