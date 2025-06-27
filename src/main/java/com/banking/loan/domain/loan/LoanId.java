package com.banking.loan.domain.loan;

public record LoanId(String value) {
    public static LoanId of(String value) {
        return new LoanId(value);
    }
    
    public static LoanId generate() {
        return new LoanId(java.util.UUID.randomUUID().toString());
    }
}