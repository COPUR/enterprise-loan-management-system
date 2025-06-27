package com.banking.loan.domain.loan;

public record CustomerId(String value) {
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}