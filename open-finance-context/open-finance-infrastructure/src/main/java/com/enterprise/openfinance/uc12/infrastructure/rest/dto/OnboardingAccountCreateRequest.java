package com.enterprise.openfinance.uc12.infrastructure.rest.dto;

import com.enterprise.openfinance.uc12.domain.command.CreateOnboardingAccountCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OnboardingAccountCreateRequest(
        @JsonProperty("Data") @NotNull @Valid Data data
) {

    public CreateOnboardingAccountCommand toCommand(String tppId,
                                                    String interactionId,
                                                    String idempotencyKey) {
        return new CreateOnboardingAccountCommand(
                tppId,
                interactionId,
                idempotencyKey,
                data.encryptedKycPayload,
                data.preferredCurrency
        );
    }

    public record Data(
            @JsonProperty("EncryptedKycPayload") @NotBlank String encryptedKycPayload,
            @JsonProperty("PreferredCurrency") @NotBlank String preferredCurrency
    ) {
    }
}
