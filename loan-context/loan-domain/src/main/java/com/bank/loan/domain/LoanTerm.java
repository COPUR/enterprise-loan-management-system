package com.bank.loan.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.util.Objects;

/**
 * Value Object representing the term/duration of a loan
 */
public final class LoanTerm implements ValueObject {
    
    private final int months;
    
    private LoanTerm(int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("Loan term must be positive");
        }
        if (months > 600) { // 50 years maximum
            throw new IllegalArgumentException("Loan term cannot exceed 600 months (50 years)");
        }
        this.months = months;
    }
    
    public static LoanTerm ofMonths(int months) {
        return new LoanTerm(months);
    }
    
    public static LoanTerm ofYears(int years) {
        return new LoanTerm(years * 12);
    }
    
    public static LoanTerm of(int years, int months) {
        return new LoanTerm(years * 12 + months);
    }
    
    public int getMonths() {
        return months;
    }
    
    public int getYears() {
        return months / 12;
    }
    
    public int getRemainingMonths() {
        return months % 12;
    }
    
    public boolean isShortTerm() {
        return months <= 12; // 1 year or less
    }
    
    public boolean isMediumTerm() {
        return months > 12 && months <= 60; // 1-5 years
    }
    
    public boolean isLongTerm() {
        return months > 60; // More than 5 years
    }
    
    @Override
    public boolean isEmpty() {
        return months == 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LoanTerm loanTerm = (LoanTerm) obj;
        return months == loanTerm.months;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(months);
    }
    
    @Override
    public String toString() {
        if (months % 12 == 0) {
            return getYears() + " year" + (getYears() > 1 ? "s" : "");
        } else if (months < 12) {
            return months + " month" + (months > 1 ? "s" : "");
        } else {
            return getYears() + " year" + (getYears() > 1 ? "s" : "") + 
                   " " + getRemainingMonths() + " month" + (getRemainingMonths() > 1 ? "s" : "");
        }
    }
}