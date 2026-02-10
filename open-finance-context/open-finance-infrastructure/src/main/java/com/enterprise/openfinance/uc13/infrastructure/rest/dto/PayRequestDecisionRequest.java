package com.enterprise.openfinance.uc13.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PayRequestDecisionRequest(
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("reason") String reason
) {
}
