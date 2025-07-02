package com.bank.loan.loan.account.domain.model;

import com.bank.loan.loan.domain.shared.DomainId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * BIAN Payment Initiation Instance Reference
 * Value Object representing unique identifier for payment initiation instances
 * Following BIAN service domain identification patterns
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInitiationInstanceReference extends DomainId {

    private PaymentInitiationInstanceReference(String value) {
        super(value);
    }

    /**
     * Generate new payment initiation instance reference
     * Following BIAN naming conventions
     */
    public static PaymentInitiationInstanceReference generate() {
        String reference = "PI-" + UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return new PaymentInitiationInstanceReference(reference);
    }

    /**
     * Create from existing reference value
     */
    public static PaymentInitiationInstanceReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment initiation instance reference cannot be null or empty");
        }
        return new PaymentInitiationInstanceReference(value);
    }

    /**
     * Validate BIAN reference format
     */
    public boolean isValidBianFormat() {
        return getValue() != null && getValue().matches("^PI-[A-F0-9]{32}$");
    }
}