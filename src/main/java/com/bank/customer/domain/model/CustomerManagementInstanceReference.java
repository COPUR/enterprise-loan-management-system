package com.bank.loan.loan.domain.customer;

import com.bank.loan.domain.shared.DomainId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * BIAN Customer Management Instance Reference
 * Value Object representing unique identifier for customer management instances
 * Following BIAN service domain identification patterns
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerManagementInstanceReference extends DomainId {

    private CustomerManagementInstanceReference(String value) {
        super(value);
    }

    /**
     * Generate new customer management instance reference
     * Following BIAN naming conventions
     */
    public static CustomerManagementInstanceReference generate() {
        String reference = "CM-" + UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return new CustomerManagementInstanceReference(reference);
    }

    /**
     * Create from existing reference value
     */
    public static CustomerManagementInstanceReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer management instance reference cannot be null or empty");
        }
        return new CustomerManagementInstanceReference(value);
    }

    /**
     * Validate BIAN reference format
     */
    public boolean isValidBianFormat() {
        return getValue() != null && getValue().matches("^CM-[A-F0-9]{32}$");
    }
}