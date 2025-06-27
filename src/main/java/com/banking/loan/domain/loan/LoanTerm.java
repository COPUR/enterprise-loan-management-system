package com.banking.loan.domain.loan;

public record LoanTerm(Integer months) {
    public static LoanTerm ofMonths(Integer months) {
        return new LoanTerm(months);
    }
    
    public static LoanTerm ofYears(Integer years) {
        return new LoanTerm(years * 12);
    }
    
    public Integer getYears() {
        return months / 12;
    }
    
    public Integer getRemainingMonths() {
        return months % 12;
    }
}