package com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentResult;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PaymentResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                new Data(
                        result.paymentId(),
                        result.consentId(),
                        result.status().apiValue(),
                        result.createdAt()
                ),
                new Links("/open-finance/v1/payments/" + result.paymentId()),
                new Meta(result.idempotencyReplay())
        );
    }

    public static PaymentResponse from(PaymentTransaction transaction) {
        return new PaymentResponse(
                new Data(
                        transaction.paymentId(),
                        transaction.consentId(),
                        transaction.status().apiValue(),
                        transaction.createdAt()
                ),
                new Links("/open-finance/v1/payments/" + transaction.paymentId()),
                new Meta(false)
        );
    }

    public record Data(
            @JsonProperty("PaymentId") String paymentId,
            @JsonProperty("ConsentId") String consentId,
            @JsonProperty("Status") String status,
            @JsonProperty("CreationDateTime") Instant creationDateTime
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Meta(
            @JsonProperty("IdempotencyReplay") boolean idempotencyReplay
    ) {
    }
}
