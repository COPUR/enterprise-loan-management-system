package com.bank.compliance.domain;

import java.util.Objects;
import java.util.UUID;

public final class ComplianceResultId {
    private final String value;

    private ComplianceResultId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ComplianceResultId cannot be blank");
        }
        this.value = value;
    }

    public static ComplianceResultId generate() {
        return new ComplianceResultId("CMP-" + UUID.randomUUID());
    }

    public static ComplianceResultId of(String value) {
        return new ComplianceResultId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComplianceResultId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
