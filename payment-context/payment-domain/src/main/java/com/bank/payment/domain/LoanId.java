package com.bank.payment.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Loan identifier for payment context
 */
public final class LoanId {
    
    private final String value;
    
    private LoanId(String value) {
        this.value = Objects.requireNonNull(value, "Loan ID value cannot be null");
    }
    
    public static LoanId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan ID value cannot be null or empty");
        }
        return new LoanId(value.trim());
    }
    
    public static LoanId generate() {
        return new LoanId(UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanId loanId = (LoanId) o;
        return Objects.equals(value, loanId.value);
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