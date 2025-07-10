package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Value object representing a payment reversal identifier.
 */
@Value
@Builder
@With
public class PaymentReversalId {
    
    String value;

    public static PaymentReversalId generate() {
        return PaymentReversalId.builder()
                .value(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentReversalId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment reversal ID cannot be null or empty");
        }
        return PaymentReversalId.builder()
                .value(value.trim())
                .build();
    }

    @Override
    public String toString() {
        return value;
    }
}