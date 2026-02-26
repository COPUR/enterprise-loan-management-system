package com.enterprise.openfinance.insurancedata.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceDataSettingsTest {

    @Test
    void shouldCreateValidSettings() {
        InsuranceDataSettings settings = new InsuranceDataSettings(Duration.ofSeconds(30), 50, 200);

        assertThat(settings.cacheTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.defaultPageSize()).isEqualTo(50);
        assertThat(settings.maxPageSize()).isEqualTo(200);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new InsuranceDataSettings(Duration.ZERO, 50, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new InsuranceDataSettings(Duration.ofSeconds(30), 0, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultPageSize");

        assertThatThrownBy(() -> new InsuranceDataSettings(Duration.ofSeconds(30), 201, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPageSize");
    }
}
