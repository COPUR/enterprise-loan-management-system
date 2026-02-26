package com.enterprise.openfinance.paymentinitiation.infrastructure.security;

import com.enterprise.openfinance.paymentinitiation.infrastructure.config.PaymentInitiationSecurityProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PrefixJwsSignatureValidationAdapterTest {

    @Test
    void shouldValidateSignaturePrefix() {
        PaymentInitiationSecurityProperties properties = new PaymentInitiationSecurityProperties();
        properties.setDetachedSignaturePrefix("detached-");
        PrefixJwsSignatureValidationAdapter adapter = new PrefixJwsSignatureValidationAdapter(properties);

        assertThat(adapter.isValid("detached-abc", "{\"amount\":\"100.00\"}")).isTrue();
        assertThat(adapter.isValid("invalid-abc", "{\"amount\":\"100.00\"}")).isFalse();
        assertThat(adapter.isValid("detached-abc", " ")).isFalse();
    }
}
