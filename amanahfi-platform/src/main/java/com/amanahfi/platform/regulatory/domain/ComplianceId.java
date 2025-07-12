package com.amanahfi.platform.regulatory.domain;

import lombok.Value;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for Regulatory Compliance
 */
@Value
public class ComplianceId {
    String value;
    
    private ComplianceId(String value) {
        this.value = Objects.requireNonNull(value, "Compliance ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Compliance ID cannot be empty");
        }
    }
    
    public static ComplianceId of(String value) {
        return new ComplianceId(value);
    }
    
    public static ComplianceId generate() {
        return new ComplianceId("COMP-" + UUID.randomUUID().toString());
    }
}