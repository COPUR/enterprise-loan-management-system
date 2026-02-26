package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalJwtLifecycleUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalBearerAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateProtectedRequest() throws Exception {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        AuthenticationEntryPoint entryPoint = Mockito.mock(AuthenticationEntryPoint.class);
        InternalBearerAuthenticationFilter filter = new InternalBearerAuthenticationFilter(useCase, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/business");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(useCase.validateBearerToken("Bearer jwt")).thenReturn(new InternalTokenPrincipal(
                "svc-user",
                "jti-1",
                Instant.parse("2026-02-25T00:00:00Z"),
                Instant.parse("2026-02-25T00:10:00Z")
        ));

        filter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(InternalTokenPrincipal.class);
        verify(entryPoint, never()).commence(any(), any(), any());
    }

    @Test
    void shouldAuthenticateSystemSecretsPath() throws Exception {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        AuthenticationEntryPoint entryPoint = Mockito.mock(AuthenticationEntryPoint.class);
        InternalBearerAuthenticationFilter filter = new InternalBearerAuthenticationFilter(useCase, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/system/secrets");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(useCase.validateBearerToken("Bearer jwt")).thenReturn(new InternalTokenPrincipal(
                "svc-user",
                "jti-2",
                Instant.parse("2026-02-25T00:00:00Z"),
                Instant.parse("2026-02-25T00:10:00Z")
        ));

        filter.doFilter(request, response, new MockFilterChain());

        verify(useCase).validateBearerToken("Bearer jwt");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void shouldInvokeEntryPointWhenTokenValidationFails() throws Exception {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        AuthenticationEntryPoint entryPoint = Mockito.mock(AuthenticationEntryPoint.class);
        InternalBearerAuthenticationFilter filter = new InternalBearerAuthenticationFilter(useCase, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/logout");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(useCase.validateBearerToken("Bearer jwt")).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, response, new MockFilterChain());

        verify(entryPoint).commence(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipUnprotectedPaths() throws Exception {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        AuthenticationEntryPoint entryPoint = Mockito.mock(AuthenticationEntryPoint.class);
        InternalBearerAuthenticationFilter filter = new InternalBearerAuthenticationFilter(useCase, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/authenticate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(useCase, never()).validateBearerToken(any());
        verify(entryPoint, never()).commence(any(), any(), any());
    }
}
