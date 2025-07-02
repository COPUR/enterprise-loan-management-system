package com.bank.loan.loan.ai.infrastructure.web.dto;

import com.bank.loan.loan.ai.domain.model.LoanAnalysisResult;
import com.bank.loan.loan.ai.domain.model.LoanRecommendation;
import com.bank.loan.loan.ai.domain.model.RiskFactor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for AI loan analysis responses
 */
@Data
@Schema(description = "AI loan analysis result")
public class AiLoanAnalysisResponse {

    @Schema(description = "Analysis result ID", example = "RESULT-123456")
    private String resultId;

    @Schema(description = "Original request ID", example = "REQ-123456")
    private String requestId;

    @Schema(description = "Overall loan recommendation")
    private LoanRecommendation overallRecommendation;

    @Schema(description = "AI confidence in the recommendation (0.0 - 1.0)", example = "0.85")
    private BigDecimal confidenceScore;

    @Schema(description = "Risk score (0.0 - 10.0, higher is riskier)", example = "5.2")
    private BigDecimal riskScore;

    @Schema(description = "Risk category based on score")
    private String riskCategory;

    @Schema(description = "Suggested interest rate (%)", example = "7.5")
    private BigDecimal suggestedInterestRate;

    @Schema(description = "Suggested loan amount", example = "240000.00")
    private BigDecimal suggestedLoanAmount;

    @Schema(description = "Suggested loan term in months", example = "360")
    private Integer suggestedTermMonths;

    @Schema(description = "Estimated monthly payment", example = "1687.50")
    private BigDecimal monthlyPaymentEstimate;

    @Schema(description = "Debt-to-income ratio", example = "0.43")
    private BigDecimal debtToIncomeRatio;

    @Schema(description = "Loan-to-income ratio", example = "4.8")
    private BigDecimal loanToIncomeRatio;

    @Schema(description = "Payment-to-income ratio", example = "0.21")
    private BigDecimal paymentToIncomeRatio;

    @Schema(description = "Fraud risk indicators")
    private String fraudRiskIndicators;

    @Schema(description = "Summary of the analysis")
    private String analysisSummary;

    @Schema(description = "Key factors influencing the decision")
    private String keyFactors;

    @Schema(description = "Suggestions for improving the application")
    private String improvementSuggestions;

    @Schema(description = "List of identified risk factors")
    private List<RiskFactor> riskFactors;

    @Schema(description = "When the analysis was processed")
    private Instant processedAt;

    @Schema(description = "AI model version used")
    private String aiModelVersion;

    @Schema(description = "Processing time in milliseconds")
    private Long processingTimeMs;

    @Schema(description = "Next recommended action")
    private String nextAction;

    @Schema(description = "Priority level for processing (1-5)")
    private Integer priorityLevel;

    @Schema(description = "Whether this is a high confidence result")
    private Boolean highConfidence;

    @Schema(description = "Whether fraud risk was detected")
    private Boolean hasFraudRisk;

    /**
     * Create response from domain model
     */
    public static AiLoanAnalysisResponse from(LoanAnalysisResult result) {
        AiLoanAnalysisResponse response = new AiLoanAnalysisResponse();
        
        response.setResultId(result.getId().getValue());
        response.setRequestId(result.getRequestId());
        response.setOverallRecommendation(result.getOverallRecommendation());
        response.setConfidenceScore(result.getConfidenceScore());
        response.setRiskScore(result.getRiskScore());
        response.setRiskCategory(result.getRiskCategory().name());
        response.setSuggestedInterestRate(result.getSuggestedInterestRate());
        response.setSuggestedLoanAmount(result.getSuggestedLoanAmount());
        response.setSuggestedTermMonths(result.getSuggestedTermMonths());
        response.setMonthlyPaymentEstimate(result.getMonthlyPaymentEstimate());
        response.setDebtToIncomeRatio(result.getDebtToIncomeRatio());
        response.setLoanToIncomeRatio(result.getLoanToIncomeRatio());
        response.setPaymentToIncomeRatio(result.getPaymentToIncomeRatio());
        response.setFraudRiskIndicators(result.getFraudRiskIndicators());
        response.setAnalysisSummary(result.getAnalysisSummary());
        response.setKeyFactors(result.getKeyFactors());
        response.setImprovementSuggestions(result.getImprovementSuggestions());
        response.setRiskFactors(result.getRiskFactors());
        response.setProcessedAt(result.getProcessedAt());
        response.setAiModelVersion(result.getAiModelVersion());
        response.setProcessingTimeMs(result.getProcessingTimeMs());
        
        // Derived fields
        response.setNextAction(result.getOverallRecommendation().getNextAction());
        response.setPriorityLevel(result.getOverallRecommendation().getPriorityLevel());
        response.setHighConfidence(result.isHighConfidence());
        response.setHasFraudRisk(result.hasFraudRisk());
        
        return response;
    }
}