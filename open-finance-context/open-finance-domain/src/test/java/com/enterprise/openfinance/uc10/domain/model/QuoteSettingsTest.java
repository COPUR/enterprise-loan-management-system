package com.enterprise.openfinance.uc10.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class QuoteSettingsTest {

    @Test
    void shouldCreateValidSettings() {
        QuoteSettings settings = new QuoteSettings(
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                "AED",
                new BigDecimal("750.00")
        );

        assertThat(settings.currency()).isEqualTo("AED");
        assertThat(settings.basePremium()).isEqualByComparingTo("750.00");
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatThrownBy(() -> new QuoteSettings(
                Duration.ZERO,
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                "AED",
                new BigDecimal("750.00")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new QuoteSettings(
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                " ",
                new BigDecimal("750.00")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
