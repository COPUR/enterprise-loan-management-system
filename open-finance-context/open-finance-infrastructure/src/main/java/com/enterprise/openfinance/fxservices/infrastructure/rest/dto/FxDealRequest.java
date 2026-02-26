package com.enterprise.openfinance.fxservices.infrastructure.rest.dto;

import com.enterprise.openfinance.fxservices.domain.command.ExecuteFxDealCommand;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FxDealRequest(
        @JsonProperty("Data") Data data
) {

    public ExecuteFxDealCommand toCommand(String tppId,
                                          String idempotencyKey,
                                          String interactionId) {
        if (data == null) {
            throw new IllegalArgumentException("Request Data is required");
        }
        return new ExecuteFxDealCommand(
                tppId,
                data.quoteId,
                idempotencyKey,
                interactionId
        );
    }

    public record Data(
            @JsonProperty("QuoteId") String quoteId
    ) {
    }
}
