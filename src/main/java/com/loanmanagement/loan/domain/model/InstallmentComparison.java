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
 * Comparison analysis between different installment plan options.
 */
@Data
@Builder
@With
public class InstallmentComparison {
    
    @NotNull
    private final String comparisonId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate comparisonDate;
    
    @NotNull
    private final List<InstallmentOption> options;
    
    @NotNull
    private final String baseCurrency;
    
    private final Map<String, BigDecimal> totalCosts;
    
    private final Map<String, BigDecimal> totalInterest;
    
    private final Map<String, BigDecimal> monthlyPayments;
    
    private final Map<String, Integer> paymentTerms;
    
    private final Map<String, BigDecimal> cashFlowImpact;
    
    private final Map<String, String> riskAssessment;
    
    private final Map<String, BigDecimal> flexibilityScore;
    
    private final String recommendedOption;
    
    private final String comparisonCriteria;
    
    private final List<String> comparisonInsights;
    
    private final Map<String, List<String>> prosAndCons;
    
    private final BigDecimal customerIncomeThreshold;
    
    private final BigDecimal customerRiskTolerance;
    
    private final String customerPreferences;
    
    private final LocalDate lastUpdated;
    
    private final String analystId;
    
    /**
     * Finds the option with the lowest total cost.
     */
    public InstallmentOption getLowestCostOption() {
        return options.stream()
                .min((o1, o2) -> o1.getTotalCost().compareTo(o2.getTotalCost()))
                .orElse(null);
    }
    
    /**
     * Finds the option with the lowest monthly payment.
     */
    public InstallmentOption getLowestPaymentOption() {
        return options.stream()
                .min((o1, o2) -> o1.getMonthlyPayment().compareTo(o2.getMonthlyPayment()))
                .orElse(null);
    }
    
    /**
     * Finds the option with the shortest term.
     */
    public InstallmentOption getShortestTermOption() {
        return options.stream()
                .min((o1, o2) -> o1.getTermMonths().compareTo(o2.getTermMonths()))
                .orElse(null);
    }
    
    /**
     * Finds the option with the highest flexibility score.
     */
    public InstallmentOption getMostFlexibleOption() {
        return options.stream()
                .max((o1, o2) -> o1.getFlexibilityScore().compareTo(o2.getFlexibilityScore()))
                .orElse(null);
    }
    
    /**
     * Calculates cost difference between options.
     */
    public BigDecimal calculateCostDifference(String option1Id, String option2Id) {
        InstallmentOption option1 = findOptionById(option1Id);
        InstallmentOption option2 = findOptionById(option2Id);
        
        if (option1 == null || option2 == null) {
            return BigDecimal.ZERO;
        }
        
        return option1.getTotalCost().subtract(option2.getTotalCost()).abs();
    }
    
    /**
     * Calculates payment difference between options.
     */
    public BigDecimal calculatePaymentDifference(String option1Id, String option2Id) {
        InstallmentOption option1 = findOptionById(option1Id);
        InstallmentOption option2 = findOptionById(option2Id);
        
        if (option1 == null || option2 == null) {
            return BigDecimal.ZERO;
        }
        
        return option1.getMonthlyPayment().subtract(option2.getMonthlyPayment()).abs();
    }
    
    /**
     * Finds an option by its ID.
     */
    private InstallmentOption findOptionById(String optionId) {
        return options.stream()
                .filter(option -> option.getOptionId().equals(optionId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Calculates affordability ranking based on customer income.
     */
    public List<InstallmentOption> rankByAffordability() {
        if (customerIncomeThreshold == null) {
            return options.stream()
                    .sorted((o1, o2) -> o1.getMonthlyPayment().compareTo(o2.getMonthlyPayment()))
                    .toList();
        }
        
        return options.stream()
                .filter(option -> isAffordable(option))
                .sorted((o1, o2) -> o1.getMonthlyPayment().compareTo(o2.getMonthlyPayment()))
                .toList();
    }
    
    /**
     * Checks if an option is affordable based on income threshold.
     */
    private boolean isAffordable(InstallmentOption option) {
        if (customerIncomeThreshold == null) {
            return true;
        }
        
        BigDecimal paymentToIncomeRatio = option.getMonthlyPayment()
                .divide(customerIncomeThreshold, 4, java.math.RoundingMode.HALF_UP);
        
        return paymentToIncomeRatio.compareTo(BigDecimal.valueOf(0.28)) <= 0; // 28% DTI threshold
    }
    
    /**
     * Calculates total interest savings compared to baseline option.
     */
    public Map<String, BigDecimal> calculateInterestSavings(String baselineOptionId) {
        InstallmentOption baseline = findOptionById(baselineOptionId);
        if (baseline == null) {
            return Map.of();
        }
        
        Map<String, BigDecimal> savings = new java.util.HashMap<>();
        
        for (InstallmentOption option : options) {
            if (!option.getOptionId().equals(baselineOptionId)) {
                BigDecimal saving = baseline.getTotalInterest().subtract(option.getTotalInterest());
                savings.put(option.getOptionId(), saving);
            }
        }
        
        return savings;
    }
    
    /**
     * Performs risk-adjusted comparison.
     */
    public List<InstallmentOption> rankByRiskAdjustedValue() {
        return options.stream()
                .sorted((o1, o2) -> {
                    BigDecimal score1 = calculateRiskAdjustedScore(o1);
                    BigDecimal score2 = calculateRiskAdjustedScore(o2);
                    return score2.compareTo(score1); // Higher score is better
                })
                .toList();
    }
    
    /**
     * Calculates risk-adjusted score for an option.
     */
    private BigDecimal calculateRiskAdjustedScore(InstallmentOption option) {
        BigDecimal baseScore = BigDecimal.valueOf(100);
        
        // Deduct points for higher cost
        BigDecimal costPenalty = option.getTotalCost().divide(BigDecimal.valueOf(10000), 2, java.math.RoundingMode.HALF_UP);
        baseScore = baseScore.subtract(costPenalty);
        
        // Add points for flexibility
        BigDecimal flexibilityBonus = option.getFlexibilityScore().multiply(BigDecimal.valueOf(10));
        baseScore = baseScore.add(flexibilityBonus);
        
        // Adjust for risk level
        if (option.getRiskLevel() != null) {
            switch (option.getRiskLevel().toUpperCase()) {
                case "LOW":
                    baseScore = baseScore.add(BigDecimal.valueOf(10));
                    break;
                case "HIGH":
                    baseScore = baseScore.subtract(BigDecimal.valueOf(10));
                    break;
                // MEDIUM = no adjustment
            }
        }
        
        return baseScore.max(BigDecimal.ZERO);
    }
    
    /**
     * Generates comparison insights.
     */
    public List<String> generateComparisonInsights() {
        List<String> insights = new java.util.ArrayList<>();
        
        InstallmentOption lowestCost = getLowestCostOption();
        InstallmentOption lowestPayment = getLowestPaymentOption();
        InstallmentOption shortestTerm = getShortestTermOption();
        
        if (lowestCost != null) {
            insights.add("Lowest total cost option: " + lowestCost.getOptionName() + 
                        " ($" + lowestCost.getTotalCost() + ")");
        }
        
        if (lowestPayment != null) {
            insights.add("Lowest monthly payment option: " + lowestPayment.getOptionName() + 
                        " ($" + lowestPayment.getMonthlyPayment() + ")");
        }
        
        if (shortestTerm != null) {
            insights.add("Shortest term option: " + shortestTerm.getOptionName() + 
                        " (" + shortestTerm.getTermMonths() + " months)");
        }
        
        // Cost spread analysis
        if (options.size() > 1) {
            BigDecimal maxCost = options.stream()
                    .map(InstallmentOption::getTotalCost)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal minCost = options.stream()
                    .map(InstallmentOption::getTotalCost)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal costSpread = maxCost.subtract(minCost);
            insights.add("Total cost spread: $" + costSpread);
        }
        
        // Payment spread analysis
        if (options.size() > 1) {
            BigDecimal maxPayment = options.stream()
                    .map(InstallmentOption::getMonthlyPayment)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal minPayment = options.stream()
                    .map(InstallmentOption::getMonthlyPayment)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal paymentSpread = maxPayment.subtract(minPayment);
            insights.add("Monthly payment spread: $" + paymentSpread);
        }
        
        return insights;
    }
    
    /**
     * Gets the recommended option based on customer profile.
     */
    public InstallmentOption getRecommendedOption() {
        if (recommendedOption != null) {
            return findOptionById(recommendedOption);
        }
        
        // Default recommendation logic
        List<InstallmentOption> affordableOptions = rankByAffordability();
        
        if (affordableOptions.isEmpty()) {
            return getLowestPaymentOption();
        }
        
        // If customer has high risk tolerance, recommend lowest cost
        if (customerRiskTolerance != null && customerRiskTolerance.compareTo(BigDecimal.valueOf(0.7)) > 0) {
            return affordableOptions.stream()
                    .min((o1, o2) -> o1.getTotalCost().compareTo(o2.getTotalCost()))
                    .orElse(null);
        }
        
        // Otherwise, balance cost and flexibility
        return rankByRiskAdjustedValue().stream()
                .filter(this::isAffordable)
                .findFirst()
                .orElse(affordableOptions.get(0));
    }
    
    /**
     * Gets the comparison summary.
     */
    public String getComparisonSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Installment Plan Comparison Summary:\n");
        summary.append("Number of Options: ").append(options.size()).append("\n");
        summary.append("Comparison Date: ").append(comparisonDate).append("\n");
        
        InstallmentOption recommended = getRecommendedOption();
        if (recommended != null) {
            summary.append("Recommended Option: ").append(recommended.getOptionName()).append("\n");
        }
        
        summary.append("\nOption Comparison:\n");
        for (InstallmentOption option : options) {
            summary.append("- ").append(option.getOptionName()).append(": ");
            summary.append("$").append(option.getMonthlyPayment()).append("/month, ");
            summary.append("$").append(option.getTotalCost()).append(" total, ");
            summary.append(option.getTermMonths()).append(" months\n");
        }
        
        List<String> insights = generateComparisonInsights();
        if (!insights.isEmpty()) {
            summary.append("\nKey Insights:\n");
            for (String insight : insights) {
                summary.append("- ").append(insight).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the installment comparison.
     */
    public boolean isValid() {
        if (comparisonId == null || loanId == null || comparisonDate == null) {
            return false;
        }
        
        if (options == null || options.isEmpty()) {
            return false;
        }
        
        if (baseCurrency == null || baseCurrency.isEmpty()) {
            return false;
        }
        
        // Validate all options are valid
        for (InstallmentOption option : options) {
            if (!option.isValid()) {
                return false;
            }
        }
        
        // Validate unique option IDs
        long uniqueIds = options.stream()
                .map(InstallmentOption::getOptionId)
                .distinct()
                .count();
        
        if (uniqueIds != options.size()) {
            return false;
        }
        
        return true;
    }
}