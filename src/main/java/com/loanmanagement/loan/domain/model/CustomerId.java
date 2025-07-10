package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.DomainId;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

/**
 * Customer ID Value Object
 * Unique identifier for customer entities
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class CustomerId extends DomainId {

    public CustomerId(String value) {
        super(value);
    }

    public static CustomerId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        return new CustomerId(value);
    }

    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return getValue();
    }
}