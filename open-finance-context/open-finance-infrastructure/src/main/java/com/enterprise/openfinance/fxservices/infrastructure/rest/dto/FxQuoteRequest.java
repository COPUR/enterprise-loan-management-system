package com.enterprise.openfinance.fxservices.infrastructure.rest.dto;

import com.enterprise.openfinance.fxservices.domain.command.CreateFxQuoteCommand;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FxQuoteRequest(
        @JsonProperty("Data") Data data
) {

    public CreateFxQuoteCommand toCommand(String tppId, String interactionId) {
        if (data == null) {
            throw new IllegalArgumentException("Request Data is required");
        }
        return new CreateFxQuoteCommand(
                tppId,
                interactionId,
                data.sourceCurrency,
                data.targetCurrency,
                data.sourceAmount
        );
    }

    public record Data(
            @JsonProperty("SourceCurrency") String sourceCurrency,
            @JsonProperty("TargetCurrency") String targetCurrency,
            @JsonProperty("SourceAmount") java.math.BigDecimal sourceAmount
    ) {
    }
}
