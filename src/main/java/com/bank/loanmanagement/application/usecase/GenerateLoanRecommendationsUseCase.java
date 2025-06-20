package com.bank.loanmanagement.application.usecase;

import com.bank.loanmanagement.domain.loan.*;
import com.bank.loanmanagement.domain.shared.Money;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified use case implementation for demonstration
 * In production, would integrate with real AI services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateLoanRecommendationsUseCase implements LoanRecommendationUseCase {

    @Override
    public LoanRecommendationResult generateRecommendations(LoanRecommendationCommand command) {
        log.info("Generating loan recommendations for customer: {}", 
                command.customerId().getValue());

        // Simplified implementation for demonstration
        List<LoanOffer> offers = generateSimpleOffers(command);
        RiskAssessment riskAssessment = assessSimpleRisk(command.financialProfile());

        return new LoanRecommendationResult(
                command.customerId(),
                offers,
                riskAssessment,
                "v2.1-demo",
                LocalDateTime.now()
        );
    }

    private List<LoanOffer> generateSimpleOffers(LoanRecommendationCommand command) {
        List<LoanOffer> offers = new ArrayList<>();
        
        // Generate a simple personal loan offer
        LoanOffer personalLoan = LoanOffer.createCompetitiveOffer(
                LoanType.PERSONAL,
                command.desiredAmount(),
                InterestRate.ofPercentage(7.25),
                LoanTerm.ofYears(5),
                "Competitive personal loan based on your profile"
        );
        
        offers.add(personalLoan);
        return offers;
    }

    private RiskAssessment assessSimpleRisk(CustomerFinancialProfile profile) {
        // Simplified risk assessment
        double dti = profile.calculateDebtToIncomeRatio().doubleValue();
        
        RiskLevel riskLevel;
        int riskScore;
        List<String> riskFactors = new ArrayList<>();
        List<String> mitigatingFactors = new ArrayList<>();
        
        if (dti > 40) {
            riskLevel = RiskLevel.HIGH;
            riskScore = 75;
            riskFactors.add("High debt-to-income ratio: " + String.format("%.1f%%", dti));
        } else if (dti > 30) {
            riskLevel = RiskLevel.MEDIUM;
            riskScore = 45;
            riskFactors.add("Moderate debt-to-income ratio: " + String.format("%.1f%%", dti));
        } else {
            riskLevel = RiskLevel.LOW;
            riskScore = 25;
            mitigatingFactors.add("Low debt-to-income ratio: " + String.format("%.1f%%", dti));
        }
        
        if (profile.getCreditScore().isExcellent()) {
            mitigatingFactors.add("Excellent credit score: " + profile.getCreditScore().getValue());
        }
        
        return new RiskAssessment(
                riskLevel,
                riskScore,
                riskScore / 100.0 * 0.3, // Simple default probability calculation
                riskFactors,
                mitigatingFactors,
                0.85 // Demo confidence level
        );
    }
}