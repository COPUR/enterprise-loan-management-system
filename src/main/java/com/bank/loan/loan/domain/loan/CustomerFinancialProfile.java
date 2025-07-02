package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loanmanagement.loan.domain.shared.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain value object representing customer financial profile
 * Clean DDD value object following hexagonal architecture
 */
public class CustomerFinancialProfile {

    private final Money monthlyIncome;
    private final Money monthlyExpenses;
    private final Money existingDebt;
    private final CreditScore creditScore;
    private final EmploymentStatus employmentStatus;
    private final int employmentDurationMonths;

    public CustomerFinancialProfile(
            Money monthlyIncome,
            Money monthlyExpenses,
            Money existingDebt,
            CreditScore creditScore,
            EmploymentStatus employmentStatus,
            int employmentDurationMonths) {
        
        this.monthlyIncome = Objects.requireNonNull(monthlyIncome, "Monthly income cannot be null");
        this.monthlyExpenses = Objects.requireNonNull(monthlyExpenses, "Monthly expenses cannot be null");
        this.existingDebt = Objects.requireNonNull(existingDebt, "Existing debt cannot be null");
        this.creditScore = Objects.requireNonNull(creditScore, "Credit score cannot be null");
        this.employmentStatus = Objects.requireNonNull(employmentStatus, "Employment status cannot be null");
        
        if (employmentDurationMonths < 0) {
            throw new IllegalArgumentException("Employment duration cannot be negative");
        }
        this.employmentDurationMonths = employmentDurationMonths;
    }

    /**
     * Calculate debt-to-income ratio as a domain operation
     */
    public BigDecimal calculateDebtToIncomeRatio() {
        if (monthlyIncome.isZero()) {
            return BigDecimal.ZERO;
        }
        
        return existingDebt.getAmount()
                .divide(monthlyIncome.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Calculate available monthly income for new loan payments
     */
    public Money calculateAvailableIncome() {
        return monthlyIncome.subtract(monthlyExpenses).subtract(existingDebt);
    }

    /**
     * Determine if profile meets basic lending criteria
     */
    public boolean meetsBasicCriteria() {
        return creditScore.isAboveMinimum() &&
               employmentStatus.isStable() &&
               employmentDurationMonths >= 6 &&
               calculateDebtToIncomeRatio().compareTo(new BigDecimal("50")) <= 0;
    }

    /**
     * Calculate financial stability score
     */
    public int calculateStabilityScore() {
        int score = 50; // Base score
        
        // Credit score contribution (0-30 points)
        score += creditScore.getContributionToStabilityScore();
        
        // Employment stability (0-20 points)
        score += employmentStatus.getStabilityPoints();
        if (employmentDurationMonths >= 60) score += 20;
        else if (employmentDurationMonths >= 36) score += 15;
        else if (employmentDurationMonths >= 24) score += 10;
        else if (employmentDurationMonths >= 12) score += 5;
        
        // DTI impact (0-20 points, inverse relationship)
        BigDecimal dti = calculateDebtToIncomeRatio();
        if (dti.compareTo(new BigDecimal("20")) <= 0) score += 20;
        else if (dti.compareTo(new BigDecimal("30")) <= 0) score += 15;
        else if (dti.compareTo(new BigDecimal("40")) <= 0) score += 10;
        else if (dti.compareTo(new BigDecimal("50")) <= 0) score += 5;
        
        return Math.max(0, Math.min(100, score));
    }

    // Getters
    public Money getMonthlyIncome() { return monthlyIncome; }
    public Money getMonthlyExpenses() { return monthlyExpenses; }
    public Money getExistingDebt() { return existingDebt; }
    public CreditScore getCreditScore() { return creditScore; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public int getEmploymentDurationMonths() { return employmentDurationMonths; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerFinancialProfile that)) return false;
        return employmentDurationMonths == that.employmentDurationMonths &&
               Objects.equals(monthlyIncome, that.monthlyIncome) &&
               Objects.equals(monthlyExpenses, that.monthlyExpenses) &&
               Objects.equals(existingDebt, that.existingDebt) &&
               Objects.equals(creditScore, that.creditScore) &&
               employmentStatus == that.employmentStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(monthlyIncome, monthlyExpenses, existingDebt, 
                          creditScore, employmentStatus, employmentDurationMonths);
    }
}