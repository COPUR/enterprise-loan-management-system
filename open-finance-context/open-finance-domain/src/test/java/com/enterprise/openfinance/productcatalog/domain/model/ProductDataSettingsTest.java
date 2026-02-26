package com.enterprise.openfinance.productcatalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDataSettingsTest {

    @Test
    void shouldCreateSettingsWithPositiveTtl() {
        ProductDataSettings settings = new ProductDataSettings(Duration.ofMinutes(2));

        assertThat(settings.cacheTtl()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void shouldRejectNonPositiveTtl() {
        assertThatThrownBy(() -> new ProductDataSettings(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");

        assertThatThrownBy(() -> new ProductDataSettings(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheTtl");
    }
}
