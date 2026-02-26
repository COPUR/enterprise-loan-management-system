package com.enterprise.openfinance.consent.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeConsentRequest(@NotBlank String reason) {
}
