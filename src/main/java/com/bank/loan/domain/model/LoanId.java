package com.bank.loan.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique loan identifier
 */
@Getter
@EqualsAndHashCode
@ToString
public class LoanId {
    
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
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
}