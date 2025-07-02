// domain/model/value/InstallmentCount.java
package com.loanmanagement.domain.model.value;

import java.util.Set;

public class InstallmentCount {
    private static final Set<Integer> VALID_COUNTS = Set.of(6, 9, 12, 24);
    
    private final int count;
    
    public InstallmentCount(int count) {
        if (!VALID_COUNTS.contains(count)) {
            throw new IllegalArgumentException(
                "Number of installments must be one of: " + VALID_COUNTS
            );
        }
        this.count = count;
    }
    
    public int getValue() {
        return count;
    }
}