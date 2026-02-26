package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenAuthenticationEntryPointTest {

    @Test
    void commenceShouldReturnUnauthorizedJsonPayload() throws Exception {
        InternalTokenAuthenticationEntryPoint entryPoint =
                new InternalTokenAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "interaction-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
        assertThat(response.getContentAsString()).contains("\"interactionId\":\"interaction-id\"");
    }
}
