package com.bank.loanmanagement.domain.shared;

import java.util.Objects;

/**
 * Base class for entity identifiers
 */
public abstract class EntityId {
    private final String value;

    protected EntityId(String value) {
        this.value = Objects.requireNonNull(value, "Entity ID cannot be null");
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityId entityId)) return false;
        return Objects.equals(value, entityId.value);
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