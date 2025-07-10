package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * Value object representing a payment note identifier.
 */
@Value
@Builder
@With
public class PaymentNoteId {
    
    String value;

    public static PaymentNoteId generate() {
        return PaymentNoteId.builder()
                .value(UUID.randomUUID().toString())
                .build();
    }

    public static PaymentNoteId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment note ID cannot be null or empty");
        }
        return PaymentNoteId.builder()
                .value(value.trim())
                .build();
    }

    @Override
    public String toString() {
        return value;
    }
}