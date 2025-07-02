package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loanmanagement.loan.domain.shared.Money;
import com.bank.loanmanagement.loan.domain.shared.DomainEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing a loan offer
 * Clean DDD entity following hexagonal architecture
 */
public class LoanOffer extends DomainEntity {

    private final LoanOfferId id;
    private final LoanType loanType;
    private final Money amount;
    private final InterestRate interestRate;
    private final LoanTerm term;
    private final Money monthlyPayment;
    private final RiskLevel riskLevel;
    private final String reasoning;
    private final Double confidenceScore;
    private final List<String> features;

    public LoanOffer(
            LoanOfferId id,
            LoanType loanType,
            Money amount,
            InterestRate interestRate,
            LoanTerm term,
            Money monthlyPayment,
            RiskLevel riskLevel,
            String reasoning,
            Double confidenceScore,
            List<String> features) {
        
        this.id = Objects.requireNonNull(id, "Loan offer ID cannot be null");
        this.loanType = Objects.requireNonNull(loanType, "Loan type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.interestRate = Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        this.term = Objects.requireNonNull(term, "Term cannot be null");
        this.monthlyPayment = Objects.requireNonNull(monthlyPayment, "Monthly payment cannot be null");
        this.riskLevel = Objects.requireNonNull(riskLevel, "Risk level cannot be null");
        this.reasoning = reasoning;
        this.confidenceScore = confidenceScore;
        this.features = features != null ? List.copyOf(features) : List.of();
    }

    /**
     * Factory method for creating competitive loan offers
     */
    public static LoanOffer createCompetitiveOffer(
            LoanType loanType,
            Money amount,
            InterestRate rate,
            LoanTerm term,
            String reasoning) {
        
        Money monthlyPayment = calculateMonthlyPayment(amount, rate, term);
        
        return new LoanOffer(
                LoanOfferId.generate(),
                loanType,
                amount,
                rate,
                term,
                monthlyPayment,
                RiskLevel.LOW,
                reasoning,
                0.9,
                List.of("Competitive Rate", "Fast Approval")
        );
    }

    /**
     * Calculate total cost of the loan
     */
    public Money calculateTotalCost() {
        BigDecimal totalPayments = monthlyPayment.getAmount()
                .multiply(new BigDecimal(term.getMonths()));
        return Money.of(totalPayments, amount.getCurrency());
    }

    /**
     * Calculate total interest for the loan
     */
    public Money calculateTotalInterest() {
        return calculateTotalCost().subtract(amount);
    }

    /**
     * Check if offer is competitive compared to market
     */
    public boolean isCompetitive(InterestRate marketRate) {
        return interestRate.isLowerThan(marketRate) && confidenceScore >= 0.8;
    }

    /**
     * Domain method to check affordability
     */
    public boolean isAffordableFor(Money monthlyIncome, Money monthlyExpenses) {
        Money availableIncome = monthlyIncome.subtract(monthlyExpenses);
        BigDecimal paymentRatio = monthlyPayment.getAmount()
                .divide(availableIncome.getAmount(), 4, BigDecimal.ROUND_HALF_UP);
        
        // Conservative affordability: payment should not exceed 30% of available income
        return paymentRatio.compareTo(new BigDecimal("0.30")) <= 0;
    }

    private static Money calculateMonthlyPayment(Money amount, InterestRate rate, LoanTerm term) {
        BigDecimal monthlyRate = rate.getAnnualRate().divide(new BigDecimal("12"), 10, BigDecimal.ROUND_HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusRate.pow(term.getMonths());
        
        BigDecimal monthlyPayment = amount.getAmount()
                .multiply(monthlyRate)
                .multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
        
        return Money.of(monthlyPayment, amount.getCurrency());
    }

    // Getters
    public LoanOfferId getId() { return id; }
    public LoanType getLoanType() { return loanType; }
    public Money getAmount() { return amount; }
    public InterestRate getInterestRate() { return interestRate; }
    public LoanTerm getTerm() { return term; }
    public Money getMonthlyPayment() { return monthlyPayment; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getReasoning() { return reasoning; }
    public Double getConfidenceScore() { return confidenceScore; }
    public List<String> getFeatures() { return List.copyOf(features); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanOffer that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}