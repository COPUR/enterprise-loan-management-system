package com.bank.loanmanagement.ai.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.model.AggregateRoot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI Loan Analysis Result - Domain Aggregate Root
 * Pure domain model without infrastructure concerns
 * Following DDD and Hexagonal Architecture principles
 */
@Getter
public class LoanAnalysisResult extends AggregateRoot<LoanAnalysisResultId> {

    private final LoanAnalysisResultId id;
    private final String requestId;
    private final LoanRecommendation overallRecommendation;
    private final BigDecimal confidenceScore;
    private final BigDecimal riskScore;
    private final Instant processedAt;

    private BigDecimal suggestedInterestRate;
    private BigDecimal suggestedLoanAmount;
    private Integer suggestedTermMonths;
    private BigDecimal monthlyPaymentEstimate;
    private BigDecimal debtToIncomeRatio;
    private BigDecimal loanToIncomeRatio;
    private BigDecimal paymentToIncomeRatio;
    private String fraudRiskIndicators;
    private String analysisSummary;
    private String keyFactors;
    private String improvementSuggestions;
    private List<RiskFactor> riskFactors;
    private String aiModelVersion;
    private Long processingTimeMs;

    // Private constructor for aggregate creation
    private LoanAnalysisResult(LoanAnalysisResultId id, String requestId, LoanRecommendation overallRecommendation,
                              BigDecimal confidenceScore, BigDecimal riskScore) {
        this.id = id;
        this.requestId = requestId;
        this.overallRecommendation = overallRecommendation;
        this.confidenceScore = confidenceScore;
        this.riskScore = riskScore;
        this.processedAt = Instant.now();
        this.riskFactors = new ArrayList<>();
    }

    /**
     * Factory method to create analysis result
     */
    public static LoanAnalysisResult create(String requestId, LoanRecommendation recommendation,
                                          BigDecimal confidenceScore, BigDecimal riskScore) {
        validateAnalysisResult(requestId, recommendation, confidenceScore, riskScore);
        
        return new LoanAnalysisResult(
            LoanAnalysisResultId.generate(),
            requestId,
            recommendation,
            confidenceScore,
            riskScore
        );
    }

    /**
     * Set loan terms suggestions
     */
    public void setSuggestedTerms(BigDecimal interestRate, BigDecimal loanAmount, Integer termMonths) {
        validateLoanTerms(interestRate, loanAmount, termMonths);
        
        this.suggestedInterestRate = interestRate;
        this.suggestedLoanAmount = loanAmount;
        this.suggestedTermMonths = termMonths;
        
        // Calculate monthly payment if all terms are available
        if (interestRate != null && loanAmount != null && termMonths != null) {
            this.monthlyPaymentEstimate = calculateMonthlyPayment(loanAmount, interestRate, termMonths);
        }
    }

    /**
     * Set financial ratios
     */
    public void setFinancialRatios(BigDecimal debtToIncome, BigDecimal loanToIncome, BigDecimal paymentToIncome) {
        validateFinancialRatios(debtToIncome, loanToIncome, paymentToIncome);
        
        this.debtToIncomeRatio = debtToIncome;
        this.loanToIncomeRatio = loanToIncome;
        this.paymentToIncomeRatio = paymentToIncome;
    }

    /**
     * Set analysis content
     */
    public void setAnalysisContent(String summary, String keyFactors, String improvementSuggestions) {
        this.analysisSummary = summary;
        this.keyFactors = keyFactors;
        this.improvementSuggestions = improvementSuggestions;
    }

    /**
     * Add risk factor
     */
    public void addRiskFactor(RiskFactor riskFactor) {
        Objects.requireNonNull(riskFactor, "Risk factor cannot be null");
        if (!this.riskFactors.contains(riskFactor)) {
            this.riskFactors.add(riskFactor);
        }
    }

    /**
     * Set fraud risk indicators
     */
    public void setFraudRiskIndicators(String fraudRiskIndicators) {
        this.fraudRiskIndicators = fraudRiskIndicators;
    }

    /**
     * Set AI processing metadata
     */
    public void setProcessingMetadata(String modelVersion, Long processingTimeMs) {
        this.aiModelVersion = modelVersion;
        this.processingTimeMs = processingTimeMs;
    }

    // Business logic methods

    /**
     * Check if loan is recommended
     */
    public boolean isLoanRecommended() {
        return overallRecommendation == LoanRecommendation.APPROVE ||
               overallRecommendation == LoanRecommendation.APPROVE_WITH_CONDITIONS;
    }

    /**
     * Check if result indicates high risk
     */
    public boolean isHighRisk() {
        return riskScore.compareTo(new BigDecimal("7.0")) >= 0;
    }

    /**
     * Check if confidence is high
     */
    public boolean isHighConfidence() {
        return confidenceScore.compareTo(new BigDecimal("0.8")) >= 0;
    }

    /**
     * Check if fraud risk is detected
     */
    public boolean hasFraudRisk() {
        return fraudRiskIndicators != null && !fraudRiskIndicators.trim().isEmpty();
    }

    /**
     * Get risk category based on score
     */
    public RiskCategory getRiskCategory() {
        if (riskScore.compareTo(new BigDecimal("3.0")) < 0) {
            return RiskCategory.LOW;
        } else if (riskScore.compareTo(new BigDecimal("6.0")) < 0) {
            return RiskCategory.MEDIUM;
        } else if (riskScore.compareTo(new BigDecimal("8.0")) < 0) {
            return RiskCategory.HIGH;
        } else {
            return RiskCategory.VERY_HIGH;
        }
    }

    /**
     * Calculate monthly payment using standard loan formula
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer months) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 8, BigDecimal.ROUND_HALF_UP)
                                           .divide(new BigDecimal("100"), 8, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
    }

    // Validation methods

    private static void validateAnalysisResult(String requestId, LoanRecommendation recommendation,
                                             BigDecimal confidenceScore, BigDecimal riskScore) {
        Objects.requireNonNull(requestId, "Request ID cannot be null");
        Objects.requireNonNull(recommendation, "Recommendation cannot be null");
        Objects.requireNonNull(confidenceScore, "Confidence score cannot be null");
        Objects.requireNonNull(riskScore, "Risk score cannot be null");

        if (confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence score must be between 0 and 1");
        }

        if (riskScore.compareTo(BigDecimal.ZERO) < 0 || riskScore.compareTo(new BigDecimal("10")) > 0) {
            throw new IllegalArgumentException("Risk score must be between 0 and 10");
        }
    }

    private void validateLoanTerms(BigDecimal interestRate, BigDecimal loanAmount, Integer termMonths) {
        if (interestRate != null && (interestRate.compareTo(BigDecimal.ZERO) < 0 || 
                                   interestRate.compareTo(new BigDecimal("50")) > 0)) {
            throw new IllegalArgumentException("Interest rate must be between 0% and 50%");
        }

        if (loanAmount != null && loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }

        if (termMonths != null && (termMonths <= 0 || termMonths > 360)) {
            throw new IllegalArgumentException("Term must be between 1 and 360 months");
        }
    }

    private void validateFinancialRatios(BigDecimal debtToIncome, BigDecimal loanToIncome, BigDecimal paymentToIncome) {
        validateRatio(debtToIncome, "Debt-to-income");
        validateRatio(loanToIncome, "Loan-to-income");
        validateRatio(paymentToIncome, "Payment-to-income");
    }

    private void validateRatio(BigDecimal ratio, String ratioName) {
        if (ratio != null && (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(new BigDecimal("10")) > 0)) {
            throw new IllegalArgumentException(ratioName + " ratio must be between 0 and 10");
        }
    }

    @Override
    public LoanAnalysisResultId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanAnalysisResult that = (LoanAnalysisResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LoanAnalysisResult{" +
                "id=" + id +
                ", requestId='" + requestId + '\'' +
                ", overallRecommendation=" + overallRecommendation +
                ", confidenceScore=" + confidenceScore +
                ", riskScore=" + riskScore +
                ", processedAt=" + processedAt +
                '}';
    }

    // Inner enums

    public enum RiskCategory {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}