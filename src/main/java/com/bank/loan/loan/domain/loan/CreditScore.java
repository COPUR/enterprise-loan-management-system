package com.bank.loanmanagement.loan.domain.loan;

import java.util.Objects;

/**
 * Domain value object for credit scores
 */
public class CreditScore {

    private static final int MIN_SCORE = 300;
    private static final int MAX_SCORE = 850;
    private static final int MINIMUM_LENDING_SCORE = 580;
    
    private final int value;

    private CreditScore(int value) {
        if (value < MIN_SCORE || value > MAX_SCORE) {
            throw new IllegalArgumentException(
                String.format("Credit score must be between %d and %d", MIN_SCORE, MAX_SCORE));
        }
        this.value = value;
    }

    public static CreditScore of(int value) {
        return new CreditScore(value);
    }

    public int getValue() {
        return value;
    }

    public boolean isAboveMinimum() {
        return value >= MINIMUM_LENDING_SCORE;
    }

    public boolean isExcellent() {
        return value >= 750;
    }

    public boolean isVeryGood() {
        return value >= 700 && value < 750;
    }

    public boolean isGood() {
        return value >= 650 && value < 700;
    }

    public boolean isFair() {
        return value >= 600 && value < 650;
    }

    public boolean isPoor() {
        return value < 600;
    }

    public String getCategory() {
        if (isExcellent()) return "Excellent";
        if (isVeryGood()) return "Very Good";
        if (isGood()) return "Good";
        if (isFair()) return "Fair";
        return "Poor";
    }

    public int getContributionToStabilityScore() {
        if (isExcellent()) return 30;
        if (isVeryGood()) return 25;
        if (isGood()) return 20;
        if (isFair()) return 15;
        if (value >= MINIMUM_LENDING_SCORE) return 10;
        return 0;
    }

    public RiskLevel getRiskLevel() {
        if (isExcellent() || isVeryGood()) return RiskLevel.LOW;
        if (isGood() || isFair()) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreditScore that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}