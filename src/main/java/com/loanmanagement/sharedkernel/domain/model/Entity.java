package com.loanmanagement.sharedkernel.domain.model;

import java.util.Objects;

/**
 * Base class for all domain entities.
 * Provides identity-based equality semantics following DDD principles.
 */
public abstract class Entity<T> {

    private final T id;

    protected Entity(T id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        this.id = id;
    }

    public T getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Entity<?> entity = (Entity<?>) obj;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
