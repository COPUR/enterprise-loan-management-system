package com.bank.risk.domain;

import java.util.Objects;
import java.util.UUID;

public final class RiskAssessmentId {
    private final String value;

    private RiskAssessmentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RiskAssessmentId cannot be blank");
        }
        this.value = value;
    }

    public static RiskAssessmentId generate() {
        return new RiskAssessmentId("RISK-" + UUID.randomUUID());
    }

    public static RiskAssessmentId of(String value) {
        return new RiskAssessmentId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RiskAssessmentId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
