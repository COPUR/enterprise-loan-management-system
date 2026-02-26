package com.enterprise.openfinance.insurancequotes.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class QuoteSettingsAdditionalTest {

    @Test
    void shouldRejectAdditionalInvalidSettings() {
        assertThatThrownBy(() -> new QuoteSettings(
                Duration.ofMinutes(30),
                Duration.ZERO,
                Duration.ofSeconds(30),
                "AED",
                new BigDecimal("750.00")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new QuoteSettings(
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                Duration.ZERO,
                "AED",
                new BigDecimal("750.00")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new QuoteSettings(
                Duration.ofMinutes(30),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                "AED",
                new BigDecimal("0.00")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
