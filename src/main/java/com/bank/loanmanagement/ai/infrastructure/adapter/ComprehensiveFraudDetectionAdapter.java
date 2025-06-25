package com.bank.loanmanagement.ai.infrastructure.adapter;

import com.bank.loanmanagement.ai.application.port.out.FraudDetectionPort;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequest;
import com.bank.loanmanagement.ai.domain.model.RiskFactor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive fraud detection adapter using AI
 * Implements sophisticated fraud detection patterns
 * Following enterprise-grade fraud detection practices
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveFraudDetectionAdapter implements FraudDetectionPort {

    private final ChatClient chatClient;

    @Override
    public FraudAnalysisResult analyzeFraudRisk(LoanAnalysisRequest request) {
        log.info("Performing comprehensive fraud analysis for request: {}", request.getId());

        try {
            // Multi-layered fraud detection approach
            FraudScore identityFraudScore = analyzeIdentityFraud(request);
            FraudScore financialFraudScore = analyzeFinancialFraud(request);
            FraudScore behavioralFraudScore = analyzeBehavioralFraud(request);
            FraudScore documentFraudScore = analyzeDocumentFraud(request);

            // Combine fraud scores with weighted algorithm
            double overallFraudScore = calculateOverallFraudScore(
                identityFraudScore, financialFraudScore, behavioralFraudScore, documentFraudScore);

            // Determine if fraud risk exists
            boolean hasFraudRisk = overallFraudScore >= 0.6; // 60% threshold

            // Generate comprehensive risk indicators
            String riskIndicators = generateRiskIndicators(
                identityFraudScore, financialFraudScore, behavioralFraudScore, documentFraudScore);

            // Generate recommendation based on risk level
            String recommendation = generateFraudRecommendation(overallFraudScore, hasFraudRisk);

            log.info("Fraud analysis completed for request: {} - Risk: {}, Score: {}", 
                    request.getId(), hasFraudRisk, overallFraudScore);

            return new FraudAnalysisResult(
                hasFraudRisk,
                overallFraudScore,
                riskIndicators,
                recommendation
            );

        } catch (Exception e) {
            log.error("Fraud analysis failed for request: {}", request.getId(), e);
            // Return conservative result on error
            return new FraudAnalysisResult(
                true, // Assume fraud risk on error for safety
                0.8,
                "Error during fraud analysis - manual review required",
                "Manual investigation required due to analysis error"
            );
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple health check for fraud detection service
            chatClient.prompt()
                .user("Fraud detection health check")
                .call()
                .content();
            return true;
        } catch (Exception e) {
            log.warn("Fraud detection service is not available: {}", e.getMessage());
            return false;
        }
    }

    // Identity fraud detection
    private FraudScore analyzeIdentityFraud(LoanAnalysisRequest request) {
        log.debug("Analyzing identity fraud for request: {}", request.getId());

        List<String> indicators = new ArrayList<>();
        double score = 0.0;

        // Check for suspicious identity patterns
        if (request.getApplicantName() != null) {
            String name = request.getApplicantName().toLowerCase();
            
            // Check for obviously fake names
            if (name.contains("test") || name.contains("fake") || name.contains("sample")) {
                indicators.add("Suspicious name pattern detected");
                score += 0.8;
            }
            
            // Check for unusual name patterns
            if (name.matches(".*\\d.*")) {
                indicators.add("Name contains numbers");
                score += 0.3;
            }
            
            // Check for extremely short or long names
            if (name.length() < 3 || name.length() > 100) {
                indicators.add("Unusual name length");
                score += 0.4;
            }
        }

        // Check for suspicious applicant ID patterns
        if (request.getApplicantId() != null) {
            String applicantId = request.getApplicantId();
            
            // Check for sequential or pattern-based IDs that might indicate fraud
            if (applicantId.matches("123456|654321|111111|000000")) {
                indicators.add("Sequential or pattern-based ID detected");
                score += 0.7;
            }
        }

        return new FraudScore("Identity", Math.min(score, 1.0), indicators);
    }

    // Financial fraud detection
    private FraudScore analyzeFinancialFraud(LoanAnalysisRequest request) {
        log.debug("Analyzing financial fraud for request: {}", request.getId());

        List<String> indicators = new ArrayList<>();
        double score = 0.0;

        // Check for suspicious income patterns
        if (request.getMonthlyIncome() != null) {
            BigDecimal income = request.getMonthlyIncome();
            
            // Check for round numbers (potential fabrication)
            if (income.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0 && 
                income.compareTo(new BigDecimal("5000")) > 0) {
                indicators.add("Income is suspiciously round number");
                score += 0.2;
            }
            
            // Check for unrealistic income levels
            if (income.compareTo(new BigDecimal("100000")) > 0) {
                indicators.add("Unusually high monthly income reported");
                score += 0.4;
            }
            
            if (income.compareTo(new BigDecimal("500")) < 0) {
                indicators.add("Unusually low monthly income reported");
                score += 0.3;
            }
        }

        // Check for suspicious debt-to-income ratios
        if (request.getCurrentDebt() != null && request.getMonthlyIncome() != null &&
            request.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal dtiRatio = request.getCurrentDebt().divide(
                request.getMonthlyIncome(), 4, BigDecimal.ROUND_HALF_UP);
            
            // Extremely low debt might indicate hiding actual debt
            if (dtiRatio.compareTo(new BigDecimal("0.05")) < 0 && 
                request.getRequestedAmount().compareTo(new BigDecimal("50000")) > 0) {
                indicators.add("Suspiciously low debt-to-income ratio for large loan request");
                score += 0.5;
            }
            
            // Extremely high debt might indicate desperation or false reporting
            if (dtiRatio.compareTo(new BigDecimal("2.0")) > 0) {
                indicators.add("Extremely high debt-to-income ratio");
                score += 0.6;
            }
        }

        // Check for suspicious loan amount patterns
        if (request.getRequestedAmount() != null) {
            BigDecimal amount = request.getRequestedAmount();
            
            // Check for maximum amount requests (potential fraud)
            if (amount.compareTo(new BigDecimal("1000000")) > 0) {
                indicators.add("Extremely high loan amount requested");
                score += 0.7;
            }
        }

        return new FraudScore("Financial", Math.min(score, 1.0), indicators);
    }

    // Behavioral fraud detection
    private FraudScore analyzeBehavioralFraud(LoanAnalysisRequest request) {
        log.debug("Analyzing behavioral fraud for request: {}", request.getId());

        List<String> indicators = new ArrayList<>();
        double score = 0.0;

        // Check for suspicious employment patterns
        if (request.getEmploymentTenureMonths() != null) {
            Integer tenure = request.getEmploymentTenureMonths();
            
            // Very short employment tenure with high loan requests
            if (tenure < 3 && request.getRequestedAmount() != null && 
                request.getRequestedAmount().compareTo(new BigDecimal("100000")) > 0) {
                indicators.add("Short employment tenure with large loan request");
                score += 0.6;
            }
            
            // Unrealistic employment tenure
            if (tenure > 600) { // More than 50 years
                indicators.add("Unrealistic employment tenure reported");
                score += 0.8;
            }
        }

        // Check for inconsistent data patterns
        if (request.getCreditScore() != null && request.getCurrentDebt() != null) {
            Integer creditScore = request.getCreditScore();
            BigDecimal currentDebt = request.getCurrentDebt();
            
            // High credit score with very high debt (inconsistent)
            if (creditScore > 750 && request.getMonthlyIncome() != null) {
                BigDecimal dtiRatio = currentDebt.divide(
                    request.getMonthlyIncome(), 4, BigDecimal.ROUND_HALF_UP);
                
                if (dtiRatio.compareTo(new BigDecimal("1.5")) > 0) {
                    indicators.add("High credit score inconsistent with high debt levels");
                    score += 0.5;
                }
            }
        }

        // Check natural language request for fraud indicators
        if (request.getNaturalLanguageRequest() != null) {
            String nlRequest = request.getNaturalLanguageRequest().toLowerCase();
            
            // Check for urgency indicators (pressure tactics)
            if (nlRequest.contains("urgent") || nlRequest.contains("emergency") || 
                nlRequest.contains("asap") || nlRequest.contains("immediately")) {
                indicators.add("Urgent language detected in request");
                score += 0.3;
            }
            
            // Check for vague or evasive language
            if (nlRequest.contains("approximately") || nlRequest.contains("around") || 
                nlRequest.contains("maybe") || nlRequest.contains("sort of")) {
                indicators.add("Vague language patterns detected");
                score += 0.2;
            }
        }

        return new FraudScore("Behavioral", Math.min(score, 1.0), indicators);
    }

    // Document fraud detection (placeholder for future implementation)
    private FraudScore analyzeDocumentFraud(LoanAnalysisRequest request) {
        log.debug("Analyzing document fraud for request: {}", request.getId());

        List<String> indicators = new ArrayList<>();
        double score = 0.0;

        // In a real implementation, this would analyze uploaded documents
        // For now, provide basic checks based on available data
        
        // Check for missing critical information
        int missingFields = 0;
        if (request.getCreditScore() == null) missingFields++;
        if (request.getEmploymentTenureMonths() == null) missingFields++;
        if (request.getCurrentDebt() == null) missingFields++;
        
        if (missingFields > 2) {
            indicators.add("Multiple critical fields missing");
            score += 0.4;
        }

        return new FraudScore("Document", Math.min(score, 1.0), indicators);
    }

    // Calculate overall fraud score using weighted algorithm
    private double calculateOverallFraudScore(FraudScore identity, FraudScore financial, 
                                            FraudScore behavioral, FraudScore document) {
        
        // Weighted average with higher weights for more critical fraud types
        double identityWeight = 0.3;
        double financialWeight = 0.4;
        double behavioralWeight = 0.2;
        double documentWeight = 0.1;
        
        return (identity.score() * identityWeight) +
               (financial.score() * financialWeight) +
               (behavioral.score() * behavioralWeight) +
               (document.score() * documentWeight);
    }

    // Generate comprehensive risk indicators
    private String generateRiskIndicators(FraudScore identity, FraudScore financial,
                                        FraudScore behavioral, FraudScore document) {
        
        List<String> allIndicators = new ArrayList<>();
        
        if (!identity.indicators().isEmpty()) {
            allIndicators.add("Identity: " + String.join(", ", identity.indicators()));
        }
        if (!financial.indicators().isEmpty()) {
            allIndicators.add("Financial: " + String.join(", ", financial.indicators()));
        }
        if (!behavioral.indicators().isEmpty()) {
            allIndicators.add("Behavioral: " + String.join(", ", behavioral.indicators()));
        }
        if (!document.indicators().isEmpty()) {
            allIndicators.add("Document: " + String.join(", ", document.indicators()));
        }
        
        return allIndicators.isEmpty() ? "No specific fraud indicators detected" : 
               String.join("; ", allIndicators);
    }

    // Generate fraud recommendation based on risk level
    private String generateFraudRecommendation(double fraudScore, boolean hasFraudRisk) {
        if (fraudScore >= 0.8) {
            return "HIGH RISK: Deny application and flag for investigation";
        } else if (fraudScore >= 0.6) {
            return "MEDIUM RISK: Require additional verification and manual review";
        } else if (fraudScore >= 0.3) {
            return "LOW RISK: Consider enhanced due diligence";
        } else {
            return "MINIMAL RISK: Proceed with standard processing";
        }
    }

    // Helper record for fraud scoring
    private record FraudScore(String category, double score, List<String> indicators) {}
}