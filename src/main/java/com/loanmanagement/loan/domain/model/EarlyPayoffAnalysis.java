package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Early payoff analysis with various scenarios and savings calculations.
 */
@Data
@Builder
@With
public class EarlyPayoffAnalysis {
    
    @NotNull
    private final String analysisId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate analysisDate;
    
    @NotNull
    private final BigDecimal currentBalance;
    
    @NotNull
    private final BigDecimal remainingInterest;
    
    @NotNull
    private final Integer remainingPayments;
    
    @NotNull
    private final BigDecimal regularPayment;
    
    @NotNull
    private final BigDecimal interestRate;
    
    private final BigDecimal prepaymentPenalty;
    
    private final boolean hasPrepaymentPenalty;
    
    private final LocalDate penaltyExpirationDate;
    
    private final Map<String, BigDecimal> payoffScenarios;
    
    private final Map<String, BigDecimal> interestSavings;
    
    private final Map<String, LocalDate> payoffDates;
    
    private final Map<String, Integer> monthsReduced;
    
    private final List<String> payoffStrategies;
    
    private final BigDecimal breakEvenAmount;
    
    private final Integer breakEvenMonths;
    
    private final BigDecimal optimalPayoffAmount;
    
    private final String recommendedStrategy;
    
    private final BigDecimal taxImplications;
    
    private final BigDecimal opportunityCost;
    
    private final String riskAssessment;
    
    private final List<String> considerations;
    
    private final LocalDate lastUpdated;
    
    /**
     * Calculates total payoff amount including penalties.
     */
    public BigDecimal calculateTotalPayoffAmount() {
        BigDecimal totalPayoff = currentBalance;
        
        if (hasPrepaymentPenalty && isPenaltyApplicable()) {
            totalPayoff = totalPayoff.add(prepaymentPenalty);
        }
        
        return totalPayoff;
    }
    
    /**
     * Calculates interest savings from immediate payoff.
     */
    public BigDecimal calculateImmediatePayoffSavings() {
        BigDecimal totalPayoff = calculateTotalPayoffAmount();
        BigDecimal futurePayments = regularPayment.multiply(BigDecimal.valueOf(remainingPayments));
        
        return futurePayments.subtract(totalPayoff);
    }
    
    /**
     * Calculates net savings after considering penalties.
     */
    public BigDecimal calculateNetSavings() {
        BigDecimal grossSavings = remainingInterest;
        BigDecimal penalty = hasPrepaymentPenalty && isPenaltyApplicable() ? 
                           prepaymentPenalty : BigDecimal.ZERO;
        
        return grossSavings.subtract(penalty);
    }
    
    /**
     * Checks if prepayment penalty is currently applicable.
     */
    public boolean isPenaltyApplicable() {
        if (!hasPrepaymentPenalty || penaltyExpirationDate == null) {
            return hasPrepaymentPenalty;
        }
        
        return LocalDate.now().isBefore(penaltyExpirationDate);
    }
    
    /**
     * Calculates payoff amount for additional monthly payment scenario.
     */
    public Map<String, Object> calculateAdditionalPaymentScenario(BigDecimal additionalPayment) {
        if (additionalPayment == null || additionalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        
        BigDecimal newPayment = regularPayment.add(additionalPayment);
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        // Calculate new payoff time using amortization formula
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            Integer newPayments = currentBalance.divide(newPayment, 0, java.math.RoundingMode.CEILING).intValue();
            return Map.of(
                "newPayments", newPayments,
                "monthsReduced", remainingPayments - newPayments,
                "interestSaved", calculateInterestSaved(newPayments),
                "totalPaid", newPayment.multiply(BigDecimal.valueOf(newPayments))
            );
        }
        
        // Amortization calculation for time to payoff
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        double logNumerator = Math.log(1 + (currentBalance.multiply(monthlyRate).divide(newPayment, 10, java.math.RoundingMode.HALF_UP)).doubleValue());
        double logDenominator = Math.log(onePlusRate.doubleValue());
        
        Integer newPayments = (int) Math.ceil(logNumerator / logDenominator);
        BigDecimal interestSaved = calculateInterestSaved(newPayments);
        
        return Map.of(
            "newPayments", newPayments,
            "monthsReduced", remainingPayments - newPayments,
            "interestSaved", interestSaved,
            "totalPaid", newPayment.multiply(BigDecimal.valueOf(newPayments)),
            "newPayoffDate", LocalDate.now().plusMonths(newPayments)
        );
    }
    
    /**
     * Calculates interest saved for a given payment count.
     */
    private BigDecimal calculateInterestSaved(Integer newPayments) {
        BigDecimal originalTotalPayments = regularPayment.multiply(BigDecimal.valueOf(remainingPayments));
        BigDecimal originalTotalInterest = originalTotalPayments.subtract(currentBalance);
        
        // Calculate new total interest (simplified)
        BigDecimal newTotalInterest = calculateInterestForPayments(newPayments);
        
        return originalTotalInterest.subtract(newTotalInterest);
    }
    
    /**
     * Calculates total interest for a given number of payments.
     */
    private BigDecimal calculateInterestForPayments(Integer payments) {
        // Simplified calculation - in practice would need detailed amortization
        BigDecimal totalPayments = regularPayment.multiply(BigDecimal.valueOf(payments));
        return totalPayments.subtract(currentBalance).max(BigDecimal.ZERO);
    }
    
    /**
     * Calculates biweekly payment scenario.
     */
    public Map<String, Object> calculateBiweeklyScenario() {
        BigDecimal biweeklyPayment = regularPayment.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        
        // 26 payments per year instead of 12
        BigDecimal effectiveExtraPayments = biweeklyPayment.multiply(BigDecimal.valueOf(2)); // 2 extra payments per year
        
        return calculateAdditionalPaymentScenario(effectiveExtraPayments.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP));
    }
    
    /**
     * Calculates annual bonus payment scenario.
     */
    public Map<String, Object> calculateAnnualBonusScenario(BigDecimal annualBonus) {
        if (annualBonus == null || annualBonus.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        
        // Apply bonus as additional principal once per year
        BigDecimal monthlyEquivalent = annualBonus.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
        
        return calculateAdditionalPaymentScenario(monthlyEquivalent);
    }
    
    /**
     * Calculates refinancing break-even analysis.
     */
    public Map<String, Object> calculateRefinanceBreakEven(BigDecimal newRate, BigDecimal closingCosts) {
        if (newRate == null || newRate.compareTo(interestRate) >= 0) {
            return Map.of("recommendation", "Not beneficial - rate not lower");
        }
        
        BigDecimal rateDifference = interestRate.subtract(newRate);
        BigDecimal monthlySavings = currentBalance.multiply(rateDifference)
                .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
        
        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("recommendation", "Not beneficial - no monthly savings");
        }
        
        Integer breakEvenMonths = closingCosts.divide(monthlySavings, 0, java.math.RoundingMode.CEILING).intValue();
        BigDecimal totalSavings = monthlySavings.multiply(BigDecimal.valueOf(remainingPayments))
                .subtract(closingCosts);
        
        return Map.of(
            "monthlySavings", monthlySavings,
            "breakEvenMonths", breakEvenMonths,
            "totalSavings", totalSavings,
            "recommendation", totalSavings.compareTo(BigDecimal.ZERO) > 0 ? 
                           "Beneficial if staying " + breakEvenMonths + "+ months" : "Not beneficial"
        );
    }
    
    /**
     * Gets the optimal payoff strategy based on analysis.
     */
    public String getOptimalStrategy() {
        if (recommendedStrategy != null) {
            return recommendedStrategy;
        }
        
        BigDecimal netSavings = calculateNetSavings();
        
        if (netSavings.compareTo(BigDecimal.ZERO) <= 0) {
            return "Continue regular payments - early payoff not beneficial due to penalties";
        }
        
        if (isPenaltyApplicable() && penaltyExpirationDate != null) {
            long monthsUntilPenaltyExpires = java.time.temporal.ChronoUnit.MONTHS
                    .between(LocalDate.now(), penaltyExpirationDate);
            
            if (monthsUntilPenaltyExpires <= 6) {
                return "Wait " + monthsUntilPenaltyExpires + " months for penalty to expire, then pay off";
            }
        }
        
        if (remainingPayments <= 12) {
            return "Continue regular payments - loan nearly complete";
        }
        
        return "Consider additional principal payments to reduce interest costs";
    }
    
    /**
     * Gets all payoff scenarios with calculations.
     */
    public Map<String, Map<String, Object>> getAllPayoffScenarios() {
        Map<String, Map<String, Object>> scenarios = new java.util.HashMap<>();
        
        // Immediate payoff
        scenarios.put("immediate", Map.of(
            "payoffAmount", calculateTotalPayoffAmount(),
            "interestSaved", calculateNetSavings(),
            "payoffDate", LocalDate.now()
        ));
        
        // Additional $50/month
        scenarios.put("additional_50", calculateAdditionalPaymentScenario(BigDecimal.valueOf(50)));
        
        // Additional $100/month
        scenarios.put("additional_100", calculateAdditionalPaymentScenario(BigDecimal.valueOf(100)));
        
        // Additional $200/month
        scenarios.put("additional_200", calculateAdditionalPaymentScenario(BigDecimal.valueOf(200)));
        
        // Biweekly payments
        scenarios.put("biweekly", calculateBiweeklyScenario());
        
        return scenarios;
    }
    
    /**
     * Gets the early payoff analysis summary.
     */
    public String getAnalysisSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Early Payoff Analysis Summary:\n");
        summary.append("Current Balance: $").append(currentBalance).append("\n");
        summary.append("Remaining Payments: ").append(remainingPayments).append("\n");
        summary.append("Remaining Interest: $").append(remainingInterest).append("\n");
        
        if (hasPrepaymentPenalty) {
            summary.append("Prepayment Penalty: $").append(prepaymentPenalty);
            if (penaltyExpirationDate != null) {
                summary.append(" (expires ").append(penaltyExpirationDate).append(")");
            }
            summary.append("\n");
        }
        
        summary.append("Immediate Payoff Amount: $").append(calculateTotalPayoffAmount()).append("\n");
        summary.append("Net Interest Savings: $").append(calculateNetSavings()).append("\n");
        summary.append("Optimal Strategy: ").append(getOptimalStrategy()).append("\n");
        
        return summary.toString();
    }
    
    /**
     * Validates the early payoff analysis.
     */
    public boolean isValid() {
        if (analysisId == null || loanId == null || analysisDate == null) {
            return false;
        }
        
        if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (remainingPayments == null || remainingPayments <= 0) {
            return false;
        }
        
        if (regularPayment == null || regularPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (hasPrepaymentPenalty && prepaymentPenalty == null) {
            return false;
        }
        
        return true;
    }
}