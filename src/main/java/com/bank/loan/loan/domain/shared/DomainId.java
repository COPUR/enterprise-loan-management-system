package com.bank.loan.loan.domain.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all domain identifiers
 * Provides type safety and validation for domain entity IDs
 */
public abstract class DomainId {
    
    private final String value;
    
    protected DomainId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    protected DomainId() {
        this.value = generateNewId();
    }
    
    protected String generateNewId() {
        return UUID.randomUUID().toString();
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
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}