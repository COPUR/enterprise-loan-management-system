package com.enterprise.openfinance.payeeverification.infrastructure.rest.dto;

import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmationOfPayeeRequest(
        @JsonProperty("Data") @NotNull @Valid Data data
) {
    public ConfirmationRequest toDomain(String tppId, String interactionId) {
        return new ConfirmationRequest(
                data.identification(),
                data.schemeName(),
                data.name(),
                tppId,
                interactionId
        );
    }

    public record Data(
            @JsonProperty("Identification") @NotBlank String identification,
            @JsonProperty("SchemeName") @NotBlank String schemeName,
            @JsonProperty("Name") @NotBlank String name
    ) {
    }
}
