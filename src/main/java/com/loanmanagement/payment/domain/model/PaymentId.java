package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Value object representing a payment identifier.
 */
@Value
@Builder
@With
public class PaymentId {
    
    String value;

    public static PaymentId generate() {
        return PaymentId.builder()
                .value(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        return PaymentId.builder()
                .value(value.trim())
                .build();
    }

    @Override
    public String toString() {
        return value;
    }
}