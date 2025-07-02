package com.bank.loanmanagement.loan.infrastructure.web.dto;

import com.bank.loanmanagement.loan.domain.loan.LoanRecommendationUseCase.LoanRecommendationResult;
import com.bank.loanmanagement.loan.domain.loan.LoanOffer;
import com.bank.loanmanagement.loan.domain.loan.RiskAssessment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive loan recommendation response DTO
 * Maps domain results to HTTP responses following hexagonal architecture
 */
@Schema(description = "AI-generated loan recommendations with comprehensive analysis")
public record LoanRecommendationResponseDto(
        @Schema(description = "Customer identifier", example = "CUST-12345")
        String customerId,
        
        @Schema(description = "List of personalized loan recommendations")
        List<LoanOfferDto> recommendations,
        
        @Schema(description = "Comprehensive risk assessment results")
        RiskAssessmentDto riskAssessment,
        
        @Schema(description = "AI analysis engine version", example = "v2.1")
        String analysisVersion,
        
        @Schema(description = "Timestamp when recommendations were generated")
        LocalDateTime generatedAt
) {
    
    public static LoanRecommendationResponseDto fromDomainResult(LoanRecommendationResult result) {
        return new LoanRecommendationResponseDto(
                result.customerId().getValue(),
                result.recommendations().stream()
                        .map(LoanOfferDto::fromDomain)
                        .toList(),
                RiskAssessmentDto.fromDomain(result.riskAssessment()),
                result.analysisVersion(),
                result.generatedAt()
        );
    }

    @Schema(description = "Individual loan offer with AI-optimized terms")
    public record LoanOfferDto(
            @Schema(description = "Unique loan offer identifier", example = "OFFER-ABC123")
            String id,
            
            @Schema(description = "Type of loan", example = "PERSONAL")
            String loanType,
            
            @Schema(description = "Loan amount", example = "25000.00")
            BigDecimal amount,
            
            @Schema(description = "Currency code", example = "USD")
            String currency,
            
            @Schema(description = "Annual interest rate percentage", example = "7.25")
            BigDecimal interestRatePercentage,
            
            @Schema(description = "Loan term in months", example = "60")
            int termMonths,
            
            @Schema(description = "Monthly payment amount", example = "495.87")
            BigDecimal monthlyPayment,
            
            @Schema(description = "Risk level assessment", example = "MEDIUM")
            String riskLevel,
            
            @Schema(description = "AI-generated reasoning for this recommendation")
            String reasoning,
            
            @Schema(description = "AI confidence score", example = "0.92")
            Double confidenceScore,
            
            @Schema(description = "Loan features and benefits")
            List<String> features
    ) {
        
        public static LoanOfferDto fromDomain(LoanOffer offer) {
            return new LoanOfferDto(
                    offer.getId().getValue(),
                    offer.getLoanType().name(),
                    offer.getAmount().getAmount(),
                    offer.getAmount().getCurrency(),
                    offer.getInterestRate().getPercentage(),
                    offer.getTerm().getMonths(),
                    offer.getMonthlyPayment().getAmount(),
                    offer.getRiskLevel().name(),
                    offer.getReasoning(),
                    offer.getConfidenceScore(),
                    offer.getFeatures()
            );
        }
    }

    @Schema(description = "Comprehensive risk assessment results")
    public record RiskAssessmentDto(
            @Schema(description = "Overall risk level", example = "MEDIUM")
            String riskLevel,
            
            @Schema(description = "Numerical risk score", example = "35")
            int riskScore,
            
            @Schema(description = "Estimated default probability", example = "0.08")
            double defaultProbability,
            
            @Schema(description = "Identified risk factors")
            List<String> riskFactors,
            
            @Schema(description = "Factors that reduce risk")
            List<String> mitigatingFactors,
            
            @Schema(description = "Assessment confidence level", example = "0.89")
            double confidenceLevel
    ) {
        
        public static RiskAssessmentDto fromDomain(RiskAssessment assessment) {
            return new RiskAssessmentDto(
                    assessment.getOverallRiskLevel().name(),
                    assessment.getRiskScore(),
                    assessment.getDefaultProbability(),
                    assessment.getRiskFactors(),
                    assessment.getMitigatingFactors(),
                    assessment.getConfidenceLevel()
            );
        }
    }
}