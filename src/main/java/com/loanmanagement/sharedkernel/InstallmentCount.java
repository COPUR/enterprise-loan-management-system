
// shared-kernel/domain/value/InstallmentCount.java
package com.loanmanagement.sharedkernel;

import java.util.Set;

public final class InstallmentCount {
    private static final Set<Integer> ALLOWED_VALUES = Set.of(6, 9, 12, 24);
    
    private final int value;
    
    private InstallmentCount(int value) {
        validate(value);
        this.value = value;
    }
    
    public static InstallmentCount of(int value) {
        return new InstallmentCount(value);
    }
    
    private void validate(int value) {
        if (!ALLOWED_VALUES.contains(value)) {
            throw new IllegalArgumentException(
                "Number of installments must be one of: " + ALLOWED_VALUES
            );
        }
    }
    
    public int getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstallmentCount that = (InstallmentCount) o;
        return value == that.value;
    }
    
    @Override
    public int hashCode() {
        return value;
    }
}