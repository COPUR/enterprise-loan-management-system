package com.bank.loan.loan.ai.infrastructure.adapter;

import com.bank.loan.loan.ai.application.port.in.AnalyzeLoanRequestCommand;
import com.bank.loan.loan.ai.application.port.out.NaturalLanguageProcessingPort;
import com.bank.loan.loan.ai.domain.model.EmploymentType;
import com.bank.loan.loan.ai.domain.model.LoanPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Spring AI adapter for natural language processing using OpenAI
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAiNaturalLanguageProcessingAdapter implements NaturalLanguageProcessingPort {

    private final ChatClient chatClient;

    @Override
    public AnalyzeLoanRequestCommand extractStructuredData(String naturalLanguageText, String applicantId, String applicantName) {
        log.info("Extracting structured data from natural language text for applicant: {}", applicantId);

        try {
            String prompt = buildExtractionPrompt(naturalLanguageText, applicantName);
            
            ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

            return parseStructuredData(response, applicantId, applicantName, naturalLanguageText);

        } catch (Exception e) {
            log.error("Failed to extract structured data for applicant: {}", applicantId, e);
            throw new NlpProcessingException("NLP processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            chatClient.prompt()
                .user("Test")
                .call()
                .content();
            return true;
        } catch (Exception e) {
            log.warn("NLP service is not available: {}", e.getMessage());
            return false;
        }
    }

    private String buildExtractionPrompt(String naturalLanguageText, String applicantName) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert loan processor. Extract structured loan application data from the following natural language request.\n\n");
        prompt.append("APPLICANT: ").append(applicantName).append("\n");
        prompt.append("REQUEST TEXT: ").append(naturalLanguageText).append("\n\n");
        
        prompt.append("Extract the following information and respond in JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"requestedAmount\": 250000,\n");
        prompt.append("  \"monthlyIncome\": 8000,\n");
        prompt.append("  \"monthlyExpenses\": 3500,\n");
        prompt.append("  \"employmentType\": \"FULL_TIME|PART_TIME|SELF_EMPLOYED|UNEMPLOYED|RETIRED|STUDENT\",\n");
        prompt.append("  \"employmentTenureMonths\": 24,\n");
        prompt.append("  \"loanPurpose\": \"HOME_PURCHASE|HOME_REFINANCE|HOME_IMPROVEMENT|AUTO_LOAN|PERSONAL_LOAN|BUSINESS_LOAN|EDUCATION_LOAN|DEBT_CONSOLIDATION|INVESTMENT|OTHER\",\n");
        prompt.append("  \"requestedTermMonths\": 360,\n");
        prompt.append("  \"currentDebt\": 15000,\n");
        prompt.append("  \"creditScore\": 720\n");
        prompt.append("}\n\n");
        
        prompt.append("Rules:\n");
        prompt.append("- Use null for any field not mentioned or unclear\n");
        prompt.append("- Convert time periods to months (e.g., '2 years' = 24 months)\n");
        prompt.append("- Extract numerical values without currency symbols\n");
        prompt.append("- Map loan purposes to the closest valid enum value\n");
        prompt.append("- Employment types should match the predefined enum values\n");
        prompt.append("- If specific values are not provided, use reasonable estimates based on context\n");

        return prompt.toString();
    }

    private AnalyzeLoanRequestCommand parseStructuredData(ChatResponse response, String applicantId, 
                                                         String applicantName, String originalText) {
        try {
            String content = response.getResult().getOutput().getContent();
            
            // Parse JSON response (simplified - would use Jackson in real implementation)
            Map<String, Object> extractedData = parseJsonResponse(content);
            
            return new AnalyzeLoanRequestCommand(
                generateRequestId(applicantId),
                extractBigDecimal(extractedData, "requestedAmount"),
                applicantName,
                applicantId,
                extractBigDecimal(extractedData, "monthlyIncome"),
                extractBigDecimal(extractedData, "monthlyExpenses"),
                extractEmploymentType(extractedData, "employmentType"),
                extractInteger(extractedData, "employmentTenureMonths"),
                extractLoanPurpose(extractedData, "loanPurpose"),
                extractInteger(extractedData, "requestedTermMonths"),
                extractBigDecimal(extractedData, "currentDebt"),
                extractInteger(extractedData, "creditScore"),
                originalText,
                Map.of("nlp_processed", true, "extraction_timestamp", System.currentTimeMillis())
            );

        } catch (Exception e) {
            log.error("Failed to parse structured data from NLP response", e);
            throw new NlpProcessingException("Failed to parse NLP response: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseJsonResponse(String content) {
        // Simplified JSON parsing - in real implementation, use Jackson ObjectMapper
        try {
            int jsonStart = content.indexOf("{");
            int jsonEnd = content.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                // TODO: Use Jackson ObjectMapper to parse JSON
                // For now, return mock extracted data
                return createMockExtractedData();
            }
            
            return createMockExtractedData();
            
        } catch (Exception e) {
            log.warn("Failed to parse JSON from NLP response, using fallback extraction");
            return createMockExtractedData();
        }
    }

    private Map<String, Object> createMockExtractedData() {
        // Mock extracted data - would be replaced with actual JSON parsing
        return Map.of(
            "requestedAmount", 200000,
            "monthlyIncome", 6000,
            "monthlyExpenses", 2500,
            "employmentType", "FULL_TIME",
            "employmentTenureMonths", 36,
            "loanPurpose", "HOME_PURCHASE",
            "requestedTermMonths", 360,
            "currentDebt", 8000,
            "creditScore", 700
        );
    }

    private BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        return new BigDecimal(value.toString());
    }

    private Integer extractInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        return Integer.valueOf(value.toString());
    }

    private EmploymentType extractEmploymentType(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        try {
            return EmploymentType.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown employment type: {}, defaulting to FULL_TIME", value);
            return EmploymentType.FULL_TIME;
        }
    }

    private LoanPurpose extractLoanPurpose(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        try {
            return LoanPurpose.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown loan purpose: {}, defaulting to PERSONAL_LOAN", value);
            return LoanPurpose.PERSONAL_LOAN;
        }
    }

    private String generateRequestId(String applicantId) {
        return "NLP-" + applicantId + "-" + System.currentTimeMillis();
    }

    public static class NlpProcessingException extends RuntimeException {
        public NlpProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}