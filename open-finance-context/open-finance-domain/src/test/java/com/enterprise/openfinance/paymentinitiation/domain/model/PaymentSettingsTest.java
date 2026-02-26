package com.enterprise.openfinance.paymentinitiation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSettingsTest {

    @Test
    void shouldCreateSettingsForPositiveTtl() {
        PaymentSettings settings = new PaymentSettings(Duration.ofHours(24));

        assertThat(settings.idempotencyTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void shouldRejectInvalidTtl() {
        assertThatThrownBy(() -> new PaymentSettings(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyTtl must be positive");

        assertThatThrownBy(() -> new PaymentSettings(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyTtl must be positive");
    }
}
