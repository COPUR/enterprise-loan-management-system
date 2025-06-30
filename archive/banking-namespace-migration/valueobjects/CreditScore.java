package com.banking.loan.domain.valueobjects;

/**
 * Credit Score Value Object
 * Follows DDD principles for domain modeling
 * 
 * ARCHIVED: 2024-12 - Migrated to com.bank.loanmanagement.domain.shared.CreditScore
 * REASON: Consolidating duplicate namespaces to eliminate cross-namespace dependencies
 */
public record CreditScore(int score) {
    
    public CreditScore {
        if (score < 300 || score > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
    }
    
    public static CreditScore of(int score) {
        return new CreditScore(score);
    }
    
    public boolean isExcellent() {
        return score >= 750;
    }
    
    public boolean isGood() {
        return score >= 670;
    }
    
    public boolean isFair() {
        return score >= 580;
    }
    
    public boolean isPoor() {
        return score < 580;
    }
    
    public int getScore() {
        return score;
    }
}