package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.shared.Money;

import java.util.List;

/**
 * Domain use case for loan recommendations
 * Following hexagonal architecture - this is a port (interface)
 */
public interface LoanRecommendationUseCase {

    /**
     * Generate personalized loan recommendations based on customer profile
     */
    LoanRecommendationResult generateRecommendations(LoanRecommendationCommand command);

    /**
     * Command object for loan recommendation request
     */
    record LoanRecommendationCommand(
            CustomerId customerId,
            Money desiredAmount,
            String loanPurpose,
            CustomerFinancialProfile financialProfile,
            LoanTermPreferences preferences
    ) {}

    /**
     * Result object containing loan recommendations
     */
    record LoanRecommendationResult(
            CustomerId customerId,
            List<LoanOffer> recommendations,
            RiskAssessment riskAssessment,
            String analysisVersion,
            java.time.LocalDateTime generatedAt
    ) {}
}