package com.bank.loan.loan.account.domain.model;

import com.bank.loan.loan.domain.shared.DomainId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * BIAN Account Information Instance Reference
 * Value Object representing unique identifier for account information service instances
 * Following BIAN service domain identification patterns
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInformationInstanceReference extends DomainId {

    private AccountInformationInstanceReference(String value) {
        super(value);
    }

    /**
     * Generate new account information instance reference
     * Following BIAN naming conventions
     */
    public static AccountInformationInstanceReference generate() {
        String reference = "AIS-" + UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return new AccountInformationInstanceReference(reference);
    }

    /**
     * Create from existing reference value
     */
    public static AccountInformationInstanceReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account information instance reference cannot be null or empty");
        }
        return new AccountInformationInstanceReference(value);
    }

    /**
     * Validate BIAN reference format
     */
    public boolean isValidBianFormat() {
        return getValue() != null && getValue().matches("^AIS-[A-F0-9]{32}$");
    }
}