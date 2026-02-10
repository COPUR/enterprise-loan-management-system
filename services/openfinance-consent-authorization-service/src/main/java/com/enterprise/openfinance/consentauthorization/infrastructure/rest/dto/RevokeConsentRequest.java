package com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeConsentRequest(@NotBlank String reason) {
}
