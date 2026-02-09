package com.enterprise.openfinance.uc06.infrastructure.security;

import com.enterprise.openfinance.uc06.infrastructure.config.Uc06SecurityProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PrefixJwsSignatureValidationAdapterTest {

    @Test
    void shouldValidateSignaturePrefix() {
        Uc06SecurityProperties properties = new Uc06SecurityProperties();
        properties.setDetachedSignaturePrefix("detached-");
        PrefixJwsSignatureValidationAdapter adapter = new PrefixJwsSignatureValidationAdapter(properties);

        assertThat(adapter.isValid("detached-abc", "{\"amount\":\"100.00\"}")).isTrue();
        assertThat(adapter.isValid("invalid-abc", "{\"amount\":\"100.00\"}")).isFalse();
        assertThat(adapter.isValid("detached-abc", " ")).isFalse();
    }
}
