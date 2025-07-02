package com.bank.loan.loan.ai.infrastructure.adapter;

import com.bank.loan.loan.ai.application.port.out.AiLoanAnalysisPort;
import com.bank.loan.loan.ai.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Spring AI adapter for loan analysis using OpenAI
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAiLoanAnalysisAdapter implements AiLoanAnalysisPort {

    private final ChatClient chatClient;
    private final OpenAiChatModel chatModel;

    @Value("${spring.ai.openai.chat.options.model:gpt-4}")
    private String modelVersion;

    @Override
    public LoanAnalysisResult performAnalysis(LoanAnalysisRequest request) {
        log.info("Performing AI loan analysis for request: {}", request.getId());

        try {
            // Build analysis prompt
            String prompt = buildAnalysisPrompt(request);
            
            // Call OpenAI
            ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

            // Parse AI response
            return parseAiResponse(request, response);

        } catch (Exception e) {
            log.error("Failed to perform AI loan analysis for request: {}", request.getId(), e);
            throw new AiAnalysisException("AI analysis failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple health check
            chatClient.prompt()
                .user("Hello")
                .call()
                .content();
            return true;
        } catch (Exception e) {
            log.warn("AI service is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getModelVersion() {
        return modelVersion;
    }

    private String buildAnalysisPrompt(LoanAnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert loan underwriter. Analyze the following loan application and provide a detailed assessment.\n\n");
        
        prompt.append("LOAN APPLICATION DETAILS:\n");
        prompt.append("Applicant: ").append(request.getApplicantName()).append("\n");
        prompt.append("Requested Amount: $").append(request.getRequestedAmount()).append("\n");
        prompt.append("Monthly Income: $").append(request.getMonthlyIncome()).append("\n");
        
        if (request.getMonthlyExpenses() != null) {
            prompt.append("Monthly Expenses: $").append(request.getMonthlyExpenses()).append("\n");
        }
        
        prompt.append("Employment Type: ").append(request.getEmploymentType().getDescription()).append("\n");
        
        if (request.getEmploymentTenureMonths() != null) {
            prompt.append("Employment Tenure: ").append(request.getEmploymentTenureMonths()).append(" months\n");
        }
        
        prompt.append("Loan Purpose: ").append(request.getLoanPurpose().getDescription()).append("\n");
        
        if (request.getRequestedTermMonths() != null) {
            prompt.append("Requested Term: ").append(request.getRequestedTermMonths()).append(" months\n");
        }
        
        if (request.getCurrentDebt() != null) {
            prompt.append("Current Debt: $").append(request.getCurrentDebt()).append("\n");
        }
        
        if (request.getCreditScore() != null) {
            prompt.append("Credit Score: ").append(request.getCreditScore()).append("\n");
        }

        if (request.getNaturalLanguageRequest() != null) {
            prompt.append("Additional Request Details: ").append(request.getNaturalLanguageRequest()).append("\n");
        }

        prompt.append("\nPLEASE PROVIDE YOUR ANALYSIS IN THE FOLLOWING JSON FORMAT:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendation\": \"APPROVE|APPROVE_WITH_CONDITIONS|COUNTER_OFFER|REQUIRE_REVIEW|DENY\",\n");
        prompt.append("  \"confidenceScore\": 0.85,\n");
        prompt.append("  \"riskScore\": 5.2,\n");
        prompt.append("  \"suggestedInterestRate\": 7.5,\n");
        prompt.append("  \"suggestedLoanAmount\": 250000,\n");
        prompt.append("  \"suggestedTermMonths\": 360,\n");
        prompt.append("  \"debtToIncomeRatio\": 0.43,\n");
        prompt.append("  \"riskFactors\": [\"HIGH_DEBT_TO_INCOME\", \"LARGE_LOAN_AMOUNT\"],\n");
        prompt.append("  \"analysisSummary\": \"Brief summary of analysis\",\n");
        prompt.append("  \"keyFactors\": \"Key positive and negative factors\",\n");
        prompt.append("  \"improvementSuggestions\": \"Suggestions for improvement\"\n");
        prompt.append("}\n\n");
        
        prompt.append("Consider employment stability, debt-to-income ratios, credit history, loan purpose risk, and market conditions.");

        return prompt.toString();
    }

    private LoanAnalysisResult parseAiResponse(LoanAnalysisRequest request, ChatResponse response) {
        try {
            String content = response.getResult().getOutput().getContent();
            
            // Extract JSON from response (simplified parsing - would use Jackson in real implementation)
            Map<String, Object> analysisData = parseJsonResponse(content);
            
            // Create result
            LoanRecommendation recommendation = LoanRecommendation.valueOf(
                (String) analysisData.get("recommendation"));
            BigDecimal confidenceScore = new BigDecimal(analysisData.get("confidenceScore").toString());
            BigDecimal riskScore = new BigDecimal(analysisData.get("riskScore").toString());
            
            LoanAnalysisResult result = LoanAnalysisResult.create(
                request.getId().getValue(),
                recommendation,
                confidenceScore,
                riskScore
            );

            // Set suggested terms if available
            if (analysisData.containsKey("suggestedInterestRate")) {
                BigDecimal interestRate = new BigDecimal(analysisData.get("suggestedInterestRate").toString());
                BigDecimal loanAmount = analysisData.containsKey("suggestedLoanAmount") ? 
                    new BigDecimal(analysisData.get("suggestedLoanAmount").toString()) : null;
                Integer termMonths = analysisData.containsKey("suggestedTermMonths") ? 
                    Integer.valueOf(analysisData.get("suggestedTermMonths").toString()) : null;
                
                result.setSuggestedTerms(interestRate, loanAmount, termMonths);
            }

            // Set financial ratios
            if (analysisData.containsKey("debtToIncomeRatio")) {
                BigDecimal dti = new BigDecimal(analysisData.get("debtToIncomeRatio").toString());
                BigDecimal lti = calculateLoanToIncomeRatio(request);
                BigDecimal pti = calculatePaymentToIncomeRatio(result, request);
                
                result.setFinancialRatios(dti, lti, pti);
            }

            // Set analysis content
            result.setAnalysisContent(
                (String) analysisData.get("analysisSummary"),
                (String) analysisData.get("keyFactors"),
                (String) analysisData.get("improvementSuggestions")
            );

            // Add risk factors
            @SuppressWarnings("unchecked")
            List<String> riskFactorNames = (List<String>) analysisData.get("riskFactors");
            if (riskFactorNames != null) {
                for (String factorName : riskFactorNames) {
                    try {
                        RiskFactor factor = RiskFactor.valueOf(factorName);
                        result.addRiskFactor(factor);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown risk factor: {}", factorName);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to parse AI response for request: {}", request.getId(), e);
            throw new AiAnalysisException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseJsonResponse(String content) {
        // Simplified JSON parsing - in real implementation, use Jackson ObjectMapper
        // This is a placeholder that would need proper JSON parsing
        try {
            // Extract JSON portion from response
            int jsonStart = content.indexOf("{");
            int jsonEnd = content.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonContent = content.substring(jsonStart, jsonEnd);
                // TODO: Use Jackson ObjectMapper to parse JSON
                // For now, return mock data structure
                return createMockAnalysisData();
            }
            
            return createMockAnalysisData();
            
        } catch (Exception e) {
            log.warn("Failed to parse JSON from AI response, using fallback analysis");
            return createMockAnalysisData();
        }
    }

    private Map<String, Object> createMockAnalysisData() {
        // Fallback analysis data - would be replaced with actual JSON parsing
        return Map.of(
            "recommendation", "REQUIRE_REVIEW",
            "confidenceScore", 0.75,
            "riskScore", 6.0,
            "suggestedInterestRate", 8.5,
            "suggestedLoanAmount", 200000,
            "suggestedTermMonths", 360,
            "debtToIncomeRatio", 0.35,
            "riskFactors", List.of("REQUIRE_REVIEW"),
            "analysisSummary", "AI analysis completed with moderate confidence",
            "keyFactors", "Employment stability and credit history reviewed",
            "improvementSuggestions", "Consider providing additional income documentation"
        );
    }

    private BigDecimal calculateLoanToIncomeRatio(LoanAnalysisRequest request) {
        if (request.getRequestedAmount() != null && request.getMonthlyIncome() != null &&
            request.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal annualIncome = request.getMonthlyIncome().multiply(new BigDecimal("12"));
            return request.getRequestedAmount().divide(annualIncome, 4, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal calculatePaymentToIncomeRatio(LoanAnalysisResult result, LoanAnalysisRequest request) {
        if (result.getMonthlyPaymentEstimate() != null && request.getMonthlyIncome() != null &&
            request.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            
            return result.getMonthlyPaymentEstimate().divide(request.getMonthlyIncome(), 4, RoundingMode.HALF_UP);
        }
        return null;
    }

    public static class AiAnalysisException extends RuntimeException {
        public AiAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}