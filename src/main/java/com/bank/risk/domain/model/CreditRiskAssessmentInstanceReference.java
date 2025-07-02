package com.bank.risk.domain.model;

import com.bank.loan.domain.shared.DomainId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * BIAN Credit Risk Assessment Instance Reference
 * Value Object representing unique identifier for credit risk assessment instances
 * Following BIAN service domain identification patterns
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditRiskAssessmentInstanceReference extends DomainId {

    private CreditRiskAssessmentInstanceReference(String value) {
        super(value);
    }

    /**
     * Generate new credit risk assessment instance reference
     * Following BIAN naming conventions
     */
    public static CreditRiskAssessmentInstanceReference generate() {
        String reference = "CRA-" + UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return new CreditRiskAssessmentInstanceReference(reference);
    }

    /**
     * Create from existing reference value
     */
    public static CreditRiskAssessmentInstanceReference of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Credit risk assessment instance reference cannot be null or empty");
        }
        return new CreditRiskAssessmentInstanceReference(value);
    }

    /**
     * Validate BIAN reference format
     */
    public boolean isValidBianFormat() {
        return getValue() != null && getValue().matches("^CRA-[A-F0-9]{32}$");
    }
}