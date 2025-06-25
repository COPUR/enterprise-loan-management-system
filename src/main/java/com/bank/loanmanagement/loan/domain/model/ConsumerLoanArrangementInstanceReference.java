package com.bank.loanmanagement.loan.domain.model;

import com.bank.loanmanagement.domain.shared.DomainId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * BIAN Consumer Loan Arrangement Instance Reference
 * Value Object representing unique identifier for consumer loan arrangements
 * Following BIAN service domain identification patterns
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumerLoanArrangementInstanceReference extends DomainId {

    private ConsumerLoanArrangementInstanceReference(String value) {
        super(value);
    }

    /**
     * Generate new arrangement instance reference
     * Following BIAN naming conventions
     */
    public static ConsumerLoanArrangementInstanceReference generate() {
        String reference = "CLA-" + UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return new ConsumerLoanArrangementInstanceReference(reference);
    }

    /**
     * Create from existing reference value
     */
    public static ConsumerLoanArrangementInstanceReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Consumer loan arrangement reference cannot be null or empty");
        }
        return new ConsumerLoanArrangementInstanceReference(value);
    }

    /**
     * Validate BIAN reference format
     */
    public boolean isValidBianFormat() {
        return getValue() != null && getValue().matches("^CLA-[A-F0-9]{32}$");
    }
}