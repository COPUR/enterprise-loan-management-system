package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Individual installment plan option for comparison.
 */
@Data
@Builder
@With
public class InstallmentOption {
    
    @NotNull
    private final String optionId;
    
    @NotNull
    private final String optionName;
    
    @NotNull
    private final String optionType;
    
    @NotNull
    private final String planType;
    
    @NotNull
    @Positive
    private final BigDecimal loanAmount;
    
    @NotNull
    private final BigDecimal interestRate;
    
    @NotNull
    @Positive
    private final Integer termMonths;
    
    @NotNull
    private final BigDecimal monthlyPayment;
    
    @NotNull
    private final BigDecimal totalCost;
    
    @NotNull
    private final BigDecimal totalInterest;
    
    @NotNull
    private final BigDecimal totalPrincipal;
    
    private final BigDecimal firstPayment;
    
    private final BigDecimal lastPayment;
    
    private final BigDecimal highestPayment;
    
    private final BigDecimal lowestPayment;
    
    private final PaymentFrequency paymentFrequency;
    
    private final BigDecimal apr;
    
    private final BigDecimal effectiveRate;
    
    private final Map<String, BigDecimal> fees;
    
    private final BigDecimal setupFee;
    
    private final BigDecimal monthlyFee;
    
    private final BigDecimal prepaymentPenalty;
    
    private final boolean hasPrepaymentPenalty;
    
    private final LocalDate penaltyExpirationDate;
    
    private final BigDecimal flexibilityScore;
    
    private final String riskLevel;
    
    private final List<String> features;
    
    private final List<String> restrictions;
    
    private final List<String> benefits;
    
    private final List<String> drawbacks;
    
    private final boolean allowsEarlyPayoff;
    
    private final boolean allowsSkippedPayments;
    
    private final boolean allowsPaymentDefer;
    
    private final boolean allowsRateModification;
    
    private final boolean allowsTermModification;
    
    private final Integer maxSkippedPayments;
    
    private final Integer maxDeferralMonths;
    
    private final String qualificationRequirements;
    
    private final BigDecimal minimumIncome;
    
    private final BigDecimal minimumCreditScore;
    
    private final String description;
    
    private final String targetCustomerProfile;
    
    private final LocalDate availableUntil;
    
    private final String regulatoryNotes;
    
    /**
     * Calculates the payment-to-loan ratio.
     */
    public BigDecimal calculatePaymentToLoanRatio() {
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return monthlyPayment.divide(loanAmount, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the interest-to-principal ratio.
     */
    public BigDecimal calculateInterestToPrincipalRatio() {
        if (totalPrincipal == null || totalPrincipal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalInterest.divide(totalPrincipal, 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the total fees amount.
     */
    public BigDecimal calculateTotalFees() {
        BigDecimal totalFees = BigDecimal.ZERO;
        
        if (setupFee != null) {
            totalFees = totalFees.add(setupFee);
        }
        
        if (monthlyFee != null && termMonths != null) {
            totalFees = totalFees.add(monthlyFee.multiply(BigDecimal.valueOf(termMonths)));
        }
        
        if (fees != null) {
            totalFees = totalFees.add(fees.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        
        return totalFees;
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
     * Calculates the payment variance as a percentage of average payment.
     */
    public BigDecimal calculatePaymentVariancePercentage() {
        BigDecimal variance = calculatePaymentVariance();
        
        if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return variance.divide(monthlyPayment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Checks if the option is currently available.
     */
    public boolean isCurrentlyAvailable() {
        if (availableUntil == null) {
            return true;
        }
        
        return !LocalDate.now().isAfter(availableUntil);
    }
    
    /**
     * Checks if the option qualifies based on income.
     */
    public boolean qualifiesForIncome(BigDecimal customerIncome) {
        if (minimumIncome == null || customerIncome == null) {
            return true;
        }
        
        return customerIncome.compareTo(minimumIncome) >= 0;
    }
    
    /**
     * Checks if the option qualifies based on credit score.
     */
    public boolean qualifiesForCreditScore(BigDecimal customerCreditScore) {
        if (minimumCreditScore == null || customerCreditScore == null) {
            return true;
        }
        
        return customerCreditScore.compareTo(minimumCreditScore) >= 0;
    }
    
    /**
     * Calculates the flexibility score based on features.
     */
    public BigDecimal calculateFlexibilityScore() {
        BigDecimal score = BigDecimal.ZERO;
        
        if (allowsEarlyPayoff) {
            score = score.add(BigDecimal.valueOf(20));
        }
        
        if (allowsSkippedPayments) {
            score = score.add(BigDecimal.valueOf(15));
            if (maxSkippedPayments != null && maxSkippedPayments > 2) {
                score = score.add(BigDecimal.valueOf(5));
            }
        }
        
        if (allowsPaymentDefer) {
            score = score.add(BigDecimal.valueOf(15));
            if (maxDeferralMonths != null && maxDeferralMonths > 6) {
                score = score.add(BigDecimal.valueOf(5));
            }
        }
        
        if (allowsRateModification) {
            score = score.add(BigDecimal.valueOf(10));
        }
        
        if (allowsTermModification) {
            score = score.add(BigDecimal.valueOf(10));
        }
        
        if (!hasPrepaymentPenalty) {
            score = score.add(BigDecimal.valueOf(15));
        }
        
        if (calculatePaymentVariancePercentage().compareTo(BigDecimal.valueOf(10)) < 0) {
            score = score.add(BigDecimal.valueOf(10)); // Stable payments add flexibility
        }
        
        return score.min(BigDecimal.valueOf(100)); // Max score of 100
    }
    
    /**
     * Gets the risk assessment for this option.
     */
    public String calculateRiskAssessment() {
        if (riskLevel != null) {
            return riskLevel;
        }
        
        int riskPoints = 0;
        
        // Variable payments increase risk
        if (calculatePaymentVariancePercentage().compareTo(BigDecimal.valueOf(20)) > 0) {
            riskPoints += 2;
        }
        
        // High payment-to-loan ratio increases risk
        if (calculatePaymentToLoanRatio().compareTo(BigDecimal.valueOf(0.05)) > 0) {
            riskPoints += 1;
        }
        
        // Long terms increase risk
        if (termMonths > 360) {
            riskPoints += 2;
        } else if (termMonths > 240) {
            riskPoints += 1;
        }
        
        // Prepayment penalties increase risk
        if (hasPrepaymentPenalty) {
            riskPoints += 1;
        }
        
        // High fees increase risk
        if (calculateTotalFees().compareTo(loanAmount.multiply(BigDecimal.valueOf(0.05))) > 0) {
            riskPoints += 1;
        }
        
        if (riskPoints >= 4) {
            return "HIGH";
        } else if (riskPoints >= 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Gets the option summary.
     */
    public String getOptionSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Option: ").append(optionName).append("\n");
        summary.append("Type: ").append(planType).append("\n");
        summary.append("Monthly Payment: $").append(monthlyPayment).append("\n");
        summary.append("Term: ").append(termMonths).append(" months\n");
        summary.append("Interest Rate: ").append(interestRate).append("%\n");
        summary.append("Total Cost: $").append(totalCost).append("\n");
        summary.append("Total Interest: $").append(totalInterest).append("\n");
        
        if (apr != null) {
            summary.append("APR: ").append(apr).append("%\n");
        }
        
        BigDecimal totalFees = calculateTotalFees();
        if (totalFees.compareTo(BigDecimal.ZERO) > 0) {
            summary.append("Total Fees: $").append(totalFees).append("\n");
        }
        
        summary.append("Flexibility Score: ").append(calculateFlexibilityScore()).append("/100\n");
        summary.append("Risk Level: ").append(calculateRiskAssessment()).append("\n");
        
        if (hasPrepaymentPenalty) {
            summary.append("Prepayment Penalty: $").append(prepaymentPenalty);
            if (penaltyExpirationDate != null) {
                summary.append(" (until ").append(penaltyExpirationDate).append(")");
            }
            summary.append("\n");
        }
        
        if (features != null && !features.isEmpty()) {
            summary.append("Key Features: ").append(String.join(", ", features)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the installment option.
     */
    public boolean isValid() {
        if (optionId == null || optionName == null || optionType == null) {
            return false;
        }
        
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (termMonths == null || termMonths <= 0) {
            return false;
        }
        
        if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (totalInterest == null || totalInterest.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Validate that total cost = principal + interest + fees
        BigDecimal calculatedTotal = loanAmount.add(totalInterest).add(calculateTotalFees());
        BigDecimal tolerance = BigDecimal.valueOf(1.00); // $1 tolerance for rounding
        
        if (totalCost.subtract(calculatedTotal).abs().compareTo(tolerance) > 0) {
            return false;
        }
        
        if (hasPrepaymentPenalty && prepaymentPenalty == null) {
            return false;
        }
        
        return true;
    }
}