package com.loanmanagement.sharedkernel.domain.model;

/**
 * Base class for all value objects in the domain.
 * Provides equality and hash code based on value semantics.
 */
public abstract class ValueObject {

    protected abstract Object[] getEqualityComponents();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ValueObject other = (ValueObject) obj;
        Object[] thisComponents = getEqualityComponents();
        Object[] otherComponents = other.getEqualityComponents();

        if (thisComponents.length != otherComponents.length) return false;

        for (int i = 0; i < thisComponents.length; i++) {
            if (!java.util.Objects.equals(thisComponents[i], otherComponents[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getEqualityComponents());
    }
}
