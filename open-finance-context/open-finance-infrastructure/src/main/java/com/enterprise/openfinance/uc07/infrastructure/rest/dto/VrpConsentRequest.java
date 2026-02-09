package com.enterprise.openfinance.uc07.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record VrpConsentRequest(
        @JsonProperty("Data") Data data
) {

    public record Data(
            @JsonProperty("PsuId") String psuId,
            @JsonProperty("Limit") Limit limit,
            @JsonProperty("ExpiryDateTime") Instant expiryDateTime
    ) {
    }

    public record Limit(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }
}
