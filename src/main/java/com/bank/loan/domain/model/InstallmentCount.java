package com.bank.loan.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Value object representing the number of installments for a loan
 */
@Getter
@EqualsAndHashCode
@ToString
public class InstallmentCount {
    
    private final Integer value;
    
    public InstallmentCount(Integer value) {
        Objects.requireNonNull(value, "Installment count cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("Installment count must be positive");
        }
        this.value = value;
    }
    
    public static InstallmentCount of(Integer value) {
        return new InstallmentCount(value);
    }
    
    public int asInt() {
        return value;
    }
}