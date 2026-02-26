package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalAuthenticateRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

