package com.bank.loanmanagement.application.service;

import com.bank.loanmanagement.domain.model.*;
import com.bank.loanmanagement.domain.port.NaturalLanguageProcessingPort;
import org.springframework.stereotype.Service;

/**
 * Application Service for Natural Language Processing
 * Orchestrates NLP operations and business logic for AI-powered banking services
 */
@Service
public class NLPApplicationService {

    private final NaturalLanguageProcessingPort nlpPort;

    public NLPApplicationService(NaturalLanguageProcessingPort nlpPort) {
        this.nlpPort = nlpPort;
    }

    /**
     * Convert user prompt to loan request with business validation
     */
    public LoanRequestConversionResult convertPromptToLoanRequest(String userPrompt) {
        try {
            // Use AI to convert prompt to structured loan request
            LoanRequest loanRequest = nlpPort.convertPromptToLoanRequest(userPrompt);
            
            // Apply business validation rules
            ValidationResult validation = validateLoanRequest(loanRequest);
            
            // Create conversion result
            return LoanRequestConversionResult.builder()
                    .originalPrompt(userPrompt)
                    .convertedRequest(loanRequest)
                    .validationResult(validation)
                    .success(validation.isValid())
                    .confidence(0.92) // Will be improved with actual AI confidence scores
                    .build();
                    
        } catch (Exception e) {
            return LoanRequestConversionResult.builder()
                    .originalPrompt(userPrompt)
                    .success(false)
                    .error("Failed to convert prompt: " + e.getMessage())
                    .confidence(0.0)
                    .build();
        }
    }

    /**
     * Analyze user intent with comprehensive banking context
     */
    public UserIntentAnalysisResult analyzeUserIntent(String userInput) {
        try {
            // Use AI to analyze user intent
            UserIntentAnalysis analysis = nlpPort.analyzeUserIntent(userInput);
            
            // Extract financial parameters
            FinancialParameters financialParams = nlpPort.extractFinancialParameters(userInput);
            
            // Assess request complexity
            RequestAssessment assessment = nlpPort.assessRequestComplexity(userInput);
            
            // Create comprehensive result
            return UserIntentAnalysisResult.builder()
                    .intentAnalysis(analysis)
                    .financialParameters(financialParams)
                    .requestAssessment(assessment)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            return UserIntentAnalysisResult.builder()
                    .success(false)
                    .error("Failed to analyze intent: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Process natural language banking request end-to-end
     */
    public BankingRequestProcessingResult processNaturalLanguageRequest(String userInput) {
        try {
            // Step 1: Analyze user intent
            UserIntentAnalysisResult intentResult = analyzeUserIntent(userInput);
            
            if (!intentResult.isSuccess()) {
                return BankingRequestProcessingResult.builder()
                        .success(false)
                        .error("Failed to analyze user intent")
                        .build();
            }
            
            // Step 2: Determine processing path based on intent
            String primaryIntent = intentResult.getIntentAnalysis().getPrimaryIntent();
            
            BankingRequestProcessingResult result = BankingRequestProcessingResult.builder()
                    .userInput(userInput)
                    .intentAnalysis(intentResult.getIntentAnalysis())
                    .requestAssessment(intentResult.getRequestAssessment())
                    .success(true)
                    .build();
            
            // Step 3: Handle based on intent type
            switch (primaryIntent) {
                case "LOAN_APPLICATION":
                    LoanRequestConversionResult loanResult = convertPromptToLoanRequest(userInput);
                    result = result.withLoanConversion(loanResult);
                    break;
                    
                case "PAYMENT_INQUIRY":
                case "ACCOUNT_INQUIRY":
                case "RATE_INQUIRY":
                    // These would be handled by other services
                    result = result.withRecommendedAction("Route to " + primaryIntent.toLowerCase() + " service");
                    break;
                    
                default:
                    result = result.withRecommendedAction("Route to customer support");
            }
            
            return result;
            
        } catch (Exception e) {
            return BankingRequestProcessingResult.builder()
                    .userInput(userInput)
                    .success(false)
                    .error("Failed to process request: " + e.getMessage())
                    .build();
        }
    }

    private ValidationResult validateLoanRequest(LoanRequest loanRequest) {
        ValidationResult.Builder validation = ValidationResult.builder().valid(true);
        
        // Validate loan amount
        if (loanRequest.getLoanAmount() < 1000) {
            validation.addError("Loan amount must be at least $1,000");
            validation.valid(false);
        } else if (loanRequest.getLoanAmount() > 500000) {
            validation.addError("Loan amount cannot exceed $500,000");
            validation.valid(false);
        }
        
        // Validate term
        int[] validTerms = {6, 9, 12, 24, 36, 48, 60};
        boolean validTerm = false;
        for (int term : validTerms) {
            if (loanRequest.getTermMonths() == term) {
                validTerm = true;
                break;
            }
        }
        if (!validTerm) {
            validation.addError("Invalid loan term. Valid terms: 6, 9, 12, 24, 36, 48, 60 months");
            validation.valid(false);
        }
        
        // Validate customer profile
        CustomerProfile profile = loanRequest.getCustomerProfile();
        if (profile.getCreditScore() < 300 || profile.getCreditScore() > 850) {
            validation.addError("Credit score must be between 300 and 850");
            validation.valid(false);
        }
        
        if (profile.getDebtToIncomeRatio() > 0.6) {
            validation.addWarning("High debt-to-income ratio may affect approval");
        }
        
        return validation.build();
    }
}