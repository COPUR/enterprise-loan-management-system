package com.bank.loan.loan.ai.infrastructure.persistence.entity;

import com.bank.loan.loan.ai.domain.model.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for LoanAnalysisResult persistence
 * Infrastructure layer - separate from domain model to avoid contamination
 */
@Entity
@Table(name = "ai_loan_analysis_results", schema = "ai_domain")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanAnalysisResultEntity {

    @EmbeddedId
    private LoanAnalysisResultId id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "overall_recommendation", nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanRecommendation overallRecommendation;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "suggested_interest_rate", precision = 5, scale = 4)
    private BigDecimal suggestedInterestRate;

    @Column(name = "suggested_loan_amount", precision = 15, scale = 2)
    private BigDecimal suggestedLoanAmount;

    @Column(name = "suggested_term_months")
    private Integer suggestedTermMonths;

    @Column(name = "monthly_payment_estimate", precision = 15, scale = 2)
    private BigDecimal monthlyPaymentEstimate;

    @Column(name = "debt_to_income_ratio", precision = 5, scale = 4)
    private BigDecimal debtToIncomeRatio;

    @Column(name = "loan_to_income_ratio", precision = 5, scale = 4)
    private BigDecimal loanToIncomeRatio;

    @Column(name = "payment_to_income_ratio", precision = 5, scale = 4)
    private BigDecimal paymentToIncomeRatio;

    @Column(name = "fraud_risk_indicators", columnDefinition = "TEXT")
    private String fraudRiskIndicators;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "key_factors", columnDefinition = "TEXT")
    private String keyFactors;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @ElementCollection
    @CollectionTable(name = "analysis_risk_factors", schema = "ai_domain",
                    joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "risk_factor")
    @Enumerated(EnumType.STRING)
    private List<RiskFactor> riskFactors = new ArrayList<>();

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "ai_model_version")
    private String aiModelVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Version
    private Long version;

    /**
     * Convert domain model to JPA entity
     */
    public static LoanAnalysisResultEntity fromDomain(LoanAnalysisResult domain) {
        LoanAnalysisResultEntity entity = new LoanAnalysisResultEntity();
        entity.setId(domain.getId());
        entity.setRequestId(domain.getRequestId());
        entity.setOverallRecommendation(domain.getOverallRecommendation());
        entity.setConfidenceScore(domain.getConfidenceScore());
        entity.setRiskScore(domain.getRiskScore());
        entity.setSuggestedInterestRate(domain.getSuggestedInterestRate());
        entity.setSuggestedLoanAmount(domain.getSuggestedLoanAmount());
        entity.setSuggestedTermMonths(domain.getSuggestedTermMonths());
        entity.setMonthlyPaymentEstimate(domain.getMonthlyPaymentEstimate());
        entity.setDebtToIncomeRatio(domain.getDebtToIncomeRatio());
        entity.setLoanToIncomeRatio(domain.getLoanToIncomeRatio());
        entity.setPaymentToIncomeRatio(domain.getPaymentToIncomeRatio());
        entity.setFraudRiskIndicators(domain.getFraudRiskIndicators());
        entity.setAnalysisSummary(domain.getAnalysisSummary());
        entity.setKeyFactors(domain.getKeyFactors());
        entity.setImprovementSuggestions(domain.getImprovementSuggestions());
        entity.setRiskFactors(new ArrayList<>(domain.getRiskFactors()));
        entity.setProcessedAt(domain.getProcessedAt());
        entity.setAiModelVersion(domain.getAiModelVersion());
        entity.setProcessingTimeMs(domain.getProcessingTimeMs());
        
        return entity;
    }

    /**
     * Convert JPA entity to domain model
     */
    public LoanAnalysisResult toDomain() {
        LoanAnalysisResult domain = LoanAnalysisResult.create(
            requestId, overallRecommendation, confidenceScore, riskScore);

        // Set optional fields
        if (suggestedInterestRate != null || suggestedLoanAmount != null || suggestedTermMonths != null) {
            domain.setSuggestedTerms(suggestedInterestRate, suggestedLoanAmount, suggestedTermMonths);
        }

        if (debtToIncomeRatio != null || loanToIncomeRatio != null || paymentToIncomeRatio != null) {
            domain.setFinancialRatios(debtToIncomeRatio, loanToIncomeRatio, paymentToIncomeRatio);
        }

        if (analysisSummary != null || keyFactors != null || improvementSuggestions != null) {
            domain.setAnalysisContent(analysisSummary, keyFactors, improvementSuggestions);
        }

        if (fraudRiskIndicators != null) {
            domain.setFraudRiskIndicators(fraudRiskIndicators);
        }

        if (aiModelVersion != null || processingTimeMs != null) {
            domain.setProcessingMetadata(aiModelVersion, processingTimeMs);
        }

        // Add risk factors
        if (riskFactors != null) {
            riskFactors.forEach(domain::addRiskFactor);
        }

        return domain;
    }
}