package com.enterprise.openfinance.businessfinancialdata.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DpopAwareBearerTokenResolverTest {

    private final DpopAwareBearerTokenResolver resolver = new DpopAwareBearerTokenResolver();

    @Test
    void shouldResolveBearerToken() {
        HttpServletRequest request = requestWithAuthorization("Bearer abc.def.ghi");
        assertThat(resolver.resolve(request)).isEqualTo("abc.def.ghi");
    }

    @Test
    void shouldResolveDpopAuthorizationToken() {
        HttpServletRequest request = requestWithAuthorization("DPoP token-value");
        assertThat(resolver.resolve(request)).isEqualTo("token-value");
    }

    @Test
    void shouldReturnNullForUnsupportedAuthorizationScheme() {
        HttpServletRequest request = requestWithAuthorization("Basic token-value");
        assertThat(resolver.resolve(request)).isNull();
    }

    private static HttpServletRequest requestWithAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
