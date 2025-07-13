package com.bank.loan.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique loan identifier
 */
public final class LoanId implements ValueObject {
    
    private final String value;
    
    private LoanId(String value) {
        this.value = Objects.requireNonNull(value, "Loan ID cannot be null");
    }
    
    public static LoanId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan ID cannot be null or empty");
        }
        return new LoanId(value.trim());
    }
    
    public static LoanId generate() {
        return new LoanId("LOAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    public static LoanId fromLong(Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return new LoanId("LOAN-" + String.format("%08d", id));
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LoanId loanId = (LoanId) obj;
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