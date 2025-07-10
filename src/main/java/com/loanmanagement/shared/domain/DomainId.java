package com.loanmanagement.shared.domain;

import java.util.Objects;

/**
 * Base class for domain identifiers
 * Provides type-safe identifier abstraction
 */
public abstract class DomainId {
    
    private final String value;

    protected DomainId(String value) {
        Objects.requireNonNull(value, "Domain ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID value cannot be empty");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DomainId domainId = (DomainId) obj;
        return Objects.equals(value, domainId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), value);
    }

    @Override
    public String toString() {
        return value;
    }
}