package com.enterprise.openfinance.uc06.infrastructure.security;

import com.enterprise.openfinance.uc06.infrastructure.config.Uc06SecurityProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class Sha256PayloadHashAdapterTest {

    @Test
    void shouldGenerateDeterministicHash() {
        Uc06SecurityProperties properties = new Uc06SecurityProperties();
        properties.setPayloadHashAlgorithm("SHA-256");
        Sha256PayloadHashAdapter adapter = new Sha256PayloadHashAdapter(properties);
        String payload = "{\"amount\":\"100.00\"}";
        String first = adapter.hash(payload);
        String second = adapter.hash(payload);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void shouldFailWhenAlgorithmIsInvalid() {
        Uc06SecurityProperties properties = new Uc06SecurityProperties();
        properties.setPayloadHashAlgorithm("INVALID");
        Sha256PayloadHashAdapter adapter = new Sha256PayloadHashAdapter(properties);

        assertThatThrownBy(() -> adapter.hash("{\"amount\":\"100.00\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hash algorithm not available");
    }
}
