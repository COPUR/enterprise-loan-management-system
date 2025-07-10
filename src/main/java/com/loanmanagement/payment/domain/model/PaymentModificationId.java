package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Value object representing a payment modification identifier.
 */
@Value
@Builder
@With
public class PaymentModificationId {
    
    String value;

    public static PaymentModificationId generate() {
        return PaymentModificationId.builder()
                .value(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentModificationId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment modification ID cannot be null or empty");
        }
        return PaymentModificationId.builder()
                .value(value.trim())
                .build();
    }

    @Override
    public String toString() {
        return value;
    }
}