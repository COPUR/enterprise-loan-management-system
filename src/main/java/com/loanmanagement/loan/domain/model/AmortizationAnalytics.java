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
 * Comprehensive amortization analytics and insights.
 */
@Data
@Builder
@With
public class AmortizationAnalytics {
    
    @NotNull
    private final String analyticsId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate analysisDate;
    
    @NotNull
    private final InstallmentSchedule schedule;
    
    @NotNull
    private final BigDecimal totalLoanAmount;
    
    @NotNull
    private final BigDecimal totalInterestAmount;
    
    @NotNull
    private final BigDecimal totalPaymentAmount;
    
    @NotNull
    private final BigDecimal currentBalance;
    
    @NotNull
    private final BigDecimal paidToDate;
    
    @NotNull
    private final BigDecimal interestPaidToDate;
    
    @NotNull
    private final BigDecimal principalPaidToDate;
    
    private final BigDecimal remainingInterest;
    
    private final BigDecimal remainingPrincipal;
    
    private final Integer remainingPayments;
    
    private final Integer totalPayments;
    
    private final Integer paymentsMade;
    
    private final BigDecimal averagePayment;
    
    private final BigDecimal firstPayment;
    
    private final BigDecimal lastPayment;
    
    private final BigDecimal highestPayment;
    
    private final BigDecimal lowestPayment;
    
    private final BigDecimal effectiveInterestRate;
    
    private final BigDecimal apr;
    
    private final BigDecimal paymentToIncomeRatio;
    
    private final Map<String, BigDecimal> yearlyBreakdown;
    
    private final Map<String, BigDecimal> monthlyAverages;
    
    private final List<String> insights;
    
    private final List<String> recommendations;
    
    private final LocalDate projectedPayoffDate;
    
    private final String riskProfile;
    
    private final BigDecimal accelerationSavings;
    
    private final String analyticsVersion;
    
    /**
     * Calculates the loan completion percentage.
     */
    public BigDecimal calculateCompletionPercentage() {
        if (totalPaymentAmount == null || totalPaymentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return paidToDate.divide(totalPaymentAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the principal completion percentage.
     */
    public BigDecimal calculatePrincipalCompletionPercentage() {
        if (totalLoanAmount == null || totalLoanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return principalPaidToDate.divide(totalLoanAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the time completion percentage.
     */
    public BigDecimal calculateTimeCompletionPercentage() {
        if (totalPayments == null || totalPayments == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(paymentsMade)
                .divide(BigDecimal.valueOf(totalPayments), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the interest-to-principal ratio.
     */
    public BigDecimal calculateInterestToPrincipalRatio() {
        if (totalLoanAmount == null || totalLoanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalInterestAmount.divide(totalLoanAmount, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the remaining payment distribution.
     */
    public Map<String, BigDecimal> calculateRemainingPaymentDistribution() {
        return Map.of(
            "Principal", remainingPrincipal != null ? remainingPrincipal : BigDecimal.ZERO,
            "Interest", remainingInterest != null ? remainingInterest : BigDecimal.ZERO,
            "Total", getRemainingBalance()
        );
    }
    
    /**
     * Gets the remaining balance.
     */
    public BigDecimal getRemainingBalance() {
        BigDecimal remPrincipal = remainingPrincipal != null ? remainingPrincipal : BigDecimal.ZERO;
        BigDecimal remInterest = remainingInterest != null ? remainingInterest : BigDecimal.ZERO;
        return remPrincipal.add(remInterest);
    }
    
    /**
     * Calculates the payment variance (difference between highest and lowest).
     */
    public BigDecimal calculatePaymentVariance() {
        if (highestPayment == null || lowestPayment == null) {
            return BigDecimal.ZERO;
        }
        
        return highestPayment.subtract(lowestPayment);
    }
    
    /**
     * Calculates the payment variance percentage.
     */
    public BigDecimal calculatePaymentVariancePercentage() {
        if (averagePayment == null || averagePayment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return calculatePaymentVariance().divide(averagePayment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Estimates months to payoff at current pace.
     */
    public Integer estimateMonthsToPayoff() {
        if (remainingPayments != null) {
            return remainingPayments;
        }
        
        if (averagePayment == null || averagePayment.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        BigDecimal remaining = getRemainingBalance();
        return remaining.divide(averagePayment, 0, java.math.RoundingMode.CEILING).intValue();
    }
    
    /**
     * Calculates potential savings from additional principal payments.
     */
    public BigDecimal calculateAdditionalPrincipalSavings(BigDecimal additionalPayment) {
        if (additionalPayment == null || additionalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Simplified calculation - in practice would need detailed amortization
        if (remainingPayments == null || effectiveInterestRate == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal monthlyRate = effectiveInterestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal currentBalance = getCurrentBalance();
        
        // Calculate reduction in term due to additional payment
        BigDecimal newBalance = currentBalance.subtract(additionalPayment);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return remainingInterest != null ? remainingInterest : BigDecimal.ZERO;
        }
        
        // Estimate interest savings (simplified)
        BigDecimal interestOnAdditional = additionalPayment.multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(remainingPayments));
        
        return interestOnAdditional;
    }
    
    /**
     * Generates amortization insights.
     */
    public List<String> generateInsights() {
        List<String> generatedInsights = new java.util.ArrayList<>();
        
        BigDecimal completionPercentage = calculateCompletionPercentage();
        if (completionPercentage.compareTo(BigDecimal.valueOf(50)) > 0) {
            generatedInsights.add("Loan is more than 50% complete");
        }
        
        BigDecimal principalCompletion = calculatePrincipalCompletionPercentage();
        BigDecimal timeCompletion = calculateTimeCompletionPercentage();
        
        if (principalCompletion.compareTo(timeCompletion) > 0) {
            generatedInsights.add("Paying down principal faster than scheduled");
        } else if (timeCompletion.subtract(principalCompletion).compareTo(BigDecimal.valueOf(10)) > 0) {
            generatedInsights.add("Principal paydown is lagging behind schedule");
        }
        
        BigDecimal interestRatio = calculateInterestToPrincipalRatio();
        if (interestRatio.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            generatedInsights.add("High interest-to-principal ratio - consider additional principal payments");
        }
        
        if (paymentToIncomeRatio != null && paymentToIncomeRatio.compareTo(BigDecimal.valueOf(0.28)) > 0) {
            generatedInsights.add("Payment-to-income ratio exceeds recommended 28%");
        }
        
        BigDecimal variancePercentage = calculatePaymentVariancePercentage();
        if (variancePercentage.compareTo(BigDecimal.valueOf(20)) > 0) {
            generatedInsights.add("High payment variance - consider payment stabilization");
        }
        
        return generatedInsights;
    }
    
    /**
     * Generates optimization recommendations.
     */
    public List<String> generateRecommendations() {
        List<String> generatedRecommendations = new java.util.ArrayList<>();
        
        if (accelerationSavings != null && accelerationSavings.compareTo(BigDecimal.valueOf(1000)) > 0) {
            generatedRecommendations.add("Consider additional principal payments to save $" + 
                                       accelerationSavings + " in interest");
        }
        
        if (remainingPayments != null && remainingPayments > 60) {
            generatedRecommendations.add("Consider refinancing if rates have decreased");
        }
        
        BigDecimal interestRatio = calculateInterestToPrincipalRatio();
        if (interestRatio.compareTo(BigDecimal.valueOf(0.4)) > 0) {
            generatedRecommendations.add("Consider bi-weekly payments to reduce interest costs");
        }
        
        if (paymentToIncomeRatio != null && paymentToIncomeRatio.compareTo(BigDecimal.valueOf(0.35)) > 0) {
            generatedRecommendations.add("Consider loan modification to reduce payment burden");
        }
        
        return generatedRecommendations;
    }
    
    /**
     * Gets the analytics summary.
     */
    public String getAnalyticsSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Amortization Analytics Summary:\n");
        summary.append("Loan Completion: ").append(calculateCompletionPercentage()).append("%\n");
        summary.append("Principal Paid: ").append(calculatePrincipalCompletionPercentage()).append("%\n");
        summary.append("Time Elapsed: ").append(calculateTimeCompletionPercentage()).append("%\n");
        summary.append("Current Balance: $").append(currentBalance).append("\n");
        summary.append("Remaining Payments: ").append(remainingPayments).append("\n");
        summary.append("Average Payment: $").append(averagePayment).append("\n");
        summary.append("Interest-to-Principal Ratio: ").append(calculateInterestToPrincipalRatio()).append("\n");
        
        if (paymentToIncomeRatio != null) {
            summary.append("Payment-to-Income Ratio: ").append(paymentToIncomeRatio).append("%\n");
        }
        
        if (projectedPayoffDate != null) {
            summary.append("Projected Payoff Date: ").append(projectedPayoffDate).append("\n");
        }
        
        if (riskProfile != null) {
            summary.append("Risk Profile: ").append(riskProfile).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the amortization analytics.
     */
    public boolean isValid() {
        if (analyticsId == null || loanId == null || analysisDate == null) {
            return false;
        }
        
        if (schedule == null || !schedule.isValid()) {
            return false;
        }
        
        if (totalLoanAmount == null || totalLoanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (paidToDate != null && paidToDate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Validate that paid amounts don't exceed totals
        if (principalPaidToDate != null && principalPaidToDate.compareTo(totalLoanAmount) > 0) {
            return false;
        }
        
        if (interestPaidToDate != null && totalInterestAmount != null && 
            interestPaidToDate.compareTo(totalInterestAmount) > 0) {
            return false;
        }
        
        return true;
    }
}