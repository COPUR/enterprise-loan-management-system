package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InternalTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public InternalTokenAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.of(
                "UNAUTHORIZED",
                "Unauthorized",
                request.getHeader("X-FAPI-Interaction-ID")
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

