package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.DomainId;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

/**
 * Payment ID Value Object
 * Unique identifier for payment entities
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PaymentId extends DomainId {

    public PaymentId(String value) {
        super(value);
    }

    public static PaymentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return getValue();
    }
}