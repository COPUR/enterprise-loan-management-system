package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Value object representing a payment schedule identifier.
 */
@Value
@Builder
@With
public class PaymentScheduleId {
    
    String value;

    public static PaymentScheduleId generate() {
        return PaymentScheduleId.builder()
                .value(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentScheduleId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment Schedule ID cannot be null or empty");
        }
        return PaymentScheduleId.builder()
                .value(value.trim())
                .build();
    }

    @Override
    public String toString() {
        return value;
    }
}