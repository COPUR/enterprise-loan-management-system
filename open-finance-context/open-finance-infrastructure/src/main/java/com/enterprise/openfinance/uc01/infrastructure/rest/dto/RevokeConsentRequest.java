package com.enterprise.openfinance.uc01.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeConsentRequest(@NotBlank String reason) {
}
