package com.bank.loanmanagement.application.port.out;

import com.bank.loanmanagement.domain.loan.CustomerFinancialProfile;
import com.bank.loanmanagement.domain.loan.LoanOffer;

import java.util.List;

/**
 * Port for AI analysis services
 * Hexagonal architecture - outbound port
 */
public interface AIAnalysisPort {

    /**
     * Enhance loan offers with AI insights and optimization
     */
    List<LoanOffer> enhanceOffers(List<LoanOffer> offers, CustomerFinancialProfile profile);

    /**
     * Generate AI-powered insights for customer profile
     */
    AIInsights generateInsights(CustomerFinancialProfile profile);

    record AIInsights(
            List<String> recommendations,
            List<String> riskFactors,
            List<String> opportunities,
            double confidenceScore
    ) {}
}