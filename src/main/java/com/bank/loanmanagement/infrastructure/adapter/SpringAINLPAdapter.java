package com.bank.loanmanagement.infrastructure.adapter;

import com.bank.loanmanagement.domain.model.*;
import com.bank.loanmanagement.domain.port.NaturalLanguageProcessingPort;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SpringAI Infrastructure Adapter for Natural Language Processing
 * Implements NLP capabilities using OpenAI GPT-4 with banking-specific prompts
 */
@Component
public class SpringAINLPAdapter implements NaturalLanguageProcessingPort {

    private final OpenAiChatClient customerServiceChatClient;
    private final OpenAiChatClient loanAnalysisChatClient;

    public SpringAINLPAdapter(
            @Qualifier("customerServiceChatClient") OpenAiChatClient customerServiceChatClient,
            @Qualifier("loanAnalysisChatClient") OpenAiChatClient loanAnalysisChatClient) {
        this.customerServiceChatClient = customerServiceChatClient;
        this.loanAnalysisChatClient = loanAnalysisChatClient;
    }

    @Override
    public LoanRequest convertPromptToLoanRequest(String userPrompt) {
        String promptTemplate = """
            You are a banking AI assistant specialized in converting natural language loan requests into structured data.
            
            Parse this customer request and extract loan parameters:
            "{user_input}"
            
            Respond ONLY with a JSON structure like this:
            {{
                "loanType": "PERSONAL|BUSINESS|MORTGAGE|AUTO|EDUCATION",
                "loanAmount": number,
                "termMonths": number (6|9|12|24|36|48|60),
                "purpose": "purpose description",
                "monthlyIncome": number,
                "creditScore": number (300-850),
                "employmentStatus": "EMPLOYED|PART_TIME|SELF_EMPLOYED|UNEMPLOYED|RETIRED",
                "debtToIncomeRatio": decimal (0.0-1.0),
                "urgency": "LOW|MEDIUM|HIGH",
                "collateralOffered": boolean
            }}
            
            Extract reasonable defaults for missing information based on banking standards.
            """;

        Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of("user_input", userPrompt));

        try {
            ChatResponse response = loanAnalysisChatClient.call(prompt);
            String jsonResponse = response.getResult().getOutput().getContent();
            
            // Parse JSON response and create LoanRequest
            return parseLoanRequestFromJson(jsonResponse, userPrompt);
            
        } catch (Exception e) {
            // Fallback to default loan request if AI parsing fails
            return createDefaultLoanRequest(userPrompt);
        }
    }

    @Override
    public UserIntentAnalysis analyzeUserIntent(String userInput) {
        String promptTemplate = """
            You are a banking AI assistant that analyzes customer intent and categorizes banking service requests.
            
            Analyze this customer input:
            "{user_input}"
            
            Respond ONLY with a JSON structure like this:
            {{
                "primaryIntent": "LOAN_APPLICATION|PAYMENT_INQUIRY|ACCOUNT_INQUIRY|RATE_INQUIRY|CUSTOMER_SUPPORT",
                "secondaryIntents": ["intent1", "intent2"],
                "urgencyLevel": "LOW|MEDIUM|HIGH",
                "customerSentiment": "POSITIVE|NEUTRAL|NEGATIVE|CONCERNED",
                "complexityLevel": "LOW|MEDIUM|HIGH",
                "financialAmount": number,
                "timeframe": "IMMEDIATE|1_WEEK|1_MONTH|3_MONTHS|FLEXIBLE",
                "riskTolerance": "LOW|MODERATE|HIGH",
                "preferredContact": "EMAIL|PHONE|SMS",
                "documentationReady": boolean,
                "recommendedWorkflow": ["step1", "step2", "step3"],
                "estimatedProcessingTime": "time estimate",
                "confidence": decimal (0.0-1.0)
            }}
            """;

        Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of("user_input", userInput));

        try {
            ChatResponse response = customerServiceChatClient.call(prompt);
            String jsonResponse = response.getResult().getOutput().getContent();
            
            return parseUserIntentFromJson(jsonResponse, userInput);
            
        } catch (Exception e) {
            return createDefaultUserIntent(userInput);
        }
    }

    @Override
    public FinancialParameters extractFinancialParameters(String text) {
        String promptTemplate = """
            Extract financial parameters from this text:
            "{text}"
            
            Respond ONLY with JSON:
            {{
                "amount": number,
                "timeframe": "IMMEDIATE|1_WEEK|1_MONTH|3_MONTHS|FLEXIBLE",
                "riskTolerance": "LOW|MODERATE|HIGH",
                "preferredContact": "EMAIL|PHONE|SMS",
                "documentationReady": boolean,
                "loanPurpose": "purpose",
                "monthlyIncome": number,
                "creditScore": number,
                "employmentStatus": "status"
            }}
            """;

        Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of("text", text));

        try {
            ChatResponse response = customerServiceChatClient.call(prompt);
            String jsonResponse = response.getResult().getOutput().getContent();
            
            return parseFinancialParametersFromJson(jsonResponse);
            
        } catch (Exception e) {
            return createDefaultFinancialParameters();
        }
    }

    @Override
    public RequestAssessment assessRequestComplexity(String userInput) {
        String promptTemplate = """
            Assess the urgency and complexity of this banking request:
            "{user_input}"
            
            Respond ONLY with JSON:
            {{
                "urgencyLevel": "LOW|MEDIUM|HIGH",
                "complexityLevel": "LOW|MEDIUM|HIGH",
                "priorityScore": "STANDARD|HIGH|CRITICAL",
                "estimatedResolution": "time estimate",
                "requiresSpecialistReview": boolean,
                "recommendedChannel": "DIGITAL|PHONE|IN_PERSON"
            }}
            """;

        Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of("user_input", userInput));

        try {
            ChatResponse response = customerServiceChatClient.call(prompt);
            String jsonResponse = response.getResult().getOutput().getContent();
            
            return parseRequestAssessmentFromJson(jsonResponse);
            
        } catch (Exception e) {
            return createDefaultRequestAssessment();
        }
    }

    // Helper methods for parsing JSON responses
    private LoanRequest parseLoanRequestFromJson(String json, String originalPrompt) {
        // Simplified JSON parsing - in production, use proper JSON library
        try {
            return LoanRequest.builder()
                    .loanType(extractJsonValue(json, "loanType", "PERSONAL"))
                    .loanAmount(Double.parseDouble(extractJsonValue(json, "loanAmount", "50000")))
                    .termMonths(Integer.parseInt(extractJsonValue(json, "termMonths", "36")))
                    .purpose(extractJsonValue(json, "purpose", "General purpose"))
                    .customerProfile(CustomerProfile.builder()
                            .monthlyIncome(Double.parseDouble(extractJsonValue(json, "monthlyIncome", "5000")))
                            .creditScore(Integer.parseInt(extractJsonValue(json, "creditScore", "720")))
                            .employmentStatus(extractJsonValue(json, "employmentStatus", "EMPLOYED"))
                            .debtToIncomeRatio(Double.parseDouble(extractJsonValue(json, "debtToIncomeRatio", "0.3")))
                            .build())
                    .urgency(extractJsonValue(json, "urgency", "LOW"))
                    .collateralOffered(Boolean.parseBoolean(extractJsonValue(json, "collateralOffered", "false")))
                    .build();
        } catch (Exception e) {
            return createDefaultLoanRequest(originalPrompt);
        }
    }

    private UserIntentAnalysis parseUserIntentFromJson(String json, String originalInput) {
        try {
            return UserIntentAnalysis.builder()
                    .originalInput(originalInput)
                    .primaryIntent(extractJsonValue(json, "primaryIntent", "GENERAL_INQUIRY"))
                    .secondaryIntents(Arrays.asList(extractJsonValue(json, "secondaryIntents", "NONE").split(",")))
                    .urgencyLevel(extractJsonValue(json, "urgencyLevel", "LOW"))
                    .customerSentiment(extractJsonValue(json, "customerSentiment", "NEUTRAL"))
                    .complexityLevel(extractJsonValue(json, "complexityLevel", "LOW"))
                    .recommendedWorkflow(Arrays.asList(extractJsonValue(json, "recommendedWorkflow", "Standard processing").split(",")))
                    .estimatedProcessingTime(extractJsonValue(json, "estimatedProcessingTime", "1-2 business days"))
                    .confidence(Double.parseDouble(extractJsonValue(json, "confidence", "0.85")))
                    .build();
        } catch (Exception e) {
            return createDefaultUserIntent(originalInput);
        }
    }

    private FinancialParameters parseFinancialParametersFromJson(String json) {
        try {
            return FinancialParameters.builder()
                    .amount(Double.parseDouble(extractJsonValue(json, "amount", "50000")))
                    .timeframe(extractJsonValue(json, "timeframe", "FLEXIBLE"))
                    .riskTolerance(extractJsonValue(json, "riskTolerance", "MODERATE"))
                    .preferredContact(extractJsonValue(json, "preferredContact", "EMAIL"))
                    .documentationReady(Boolean.parseBoolean(extractJsonValue(json, "documentationReady", "false")))
                    .loanPurpose(extractJsonValue(json, "loanPurpose", "GENERAL_PURPOSE"))
                    .monthlyIncome(Double.parseDouble(extractJsonValue(json, "monthlyIncome", "5000")))
                    .creditScore(Integer.parseInt(extractJsonValue(json, "creditScore", "720")))
                    .employmentStatus(extractJsonValue(json, "employmentStatus", "EMPLOYED"))
                    .build();
        } catch (Exception e) {
            return createDefaultFinancialParameters();
        }
    }

    private RequestAssessment parseRequestAssessmentFromJson(String json) {
        try {
            return RequestAssessment.builder()
                    .urgencyLevel(extractJsonValue(json, "urgencyLevel", "LOW"))
                    .complexityLevel(extractJsonValue(json, "complexityLevel", "LOW"))
                    .priorityScore(extractJsonValue(json, "priorityScore", "STANDARD"))
                    .estimatedResolution(extractJsonValue(json, "estimatedResolution", "1-2 business days"))
                    .requiresSpecialistReview(Boolean.parseBoolean(extractJsonValue(json, "requiresSpecialistReview", "false")))
                    .recommendedChannel(extractJsonValue(json, "recommendedChannel", "DIGITAL"))
                    .build();
        } catch (Exception e) {
            return createDefaultRequestAssessment();
        }
    }

    private String extractJsonValue(String json, String key, String defaultValue) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return defaultValue;
            
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return defaultValue;
            
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
                valueStart++;
            }
            
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != '"' && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            
            return json.substring(valueStart, valueEnd).trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Default fallback methods
    private LoanRequest createDefaultLoanRequest(String prompt) {
        return LoanRequest.builder()
                .loanType("PERSONAL")
                .loanAmount(50000.0)
                .termMonths(36)
                .purpose("General loan request")
                .customerProfile(CustomerProfile.builder()
                        .monthlyIncome(5000.0)
                        .creditScore(720)
                        .employmentStatus("EMPLOYED")
                        .debtToIncomeRatio(0.3)
                        .build())
                .urgency("LOW")
                .collateralOffered(false)
                .build();
    }

    private UserIntentAnalysis createDefaultUserIntent(String originalInput) {
        return UserIntentAnalysis.builder()
                .originalInput(originalInput)
                .primaryIntent("GENERAL_INQUIRY")
                .secondaryIntents(Arrays.asList("NONE"))
                .urgencyLevel("LOW")
                .customerSentiment("NEUTRAL")
                .complexityLevel("LOW")
                .recommendedWorkflow(Arrays.asList("Route to customer service", "Gather requirements", "Provide assistance"))
                .estimatedProcessingTime("Same day response")
                .confidence(0.75)
                .build();
    }

    private FinancialParameters createDefaultFinancialParameters() {
        return FinancialParameters.builder()
                .amount(50000.0)
                .timeframe("FLEXIBLE")
                .riskTolerance("MODERATE")
                .preferredContact("EMAIL")
                .documentationReady(false)
                .loanPurpose("GENERAL_PURPOSE")
                .monthlyIncome(5000.0)
                .creditScore(720)
                .employmentStatus("EMPLOYED")
                .build();
    }

    private RequestAssessment createDefaultRequestAssessment() {
        return RequestAssessment.builder()
                .urgencyLevel("LOW")
                .complexityLevel("LOW")
                .priorityScore("STANDARD")
                .estimatedResolution("1-2 business days")
                .requiresSpecialistReview(false)
                .recommendedChannel("DIGITAL")
                .build();
    }
}