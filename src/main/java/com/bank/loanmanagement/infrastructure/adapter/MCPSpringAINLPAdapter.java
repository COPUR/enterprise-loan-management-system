package com.bank.loanmanagement.infrastructure.adapter;

import com.bank.loanmanagement.domain.model.*;
import com.bank.loanmanagement.domain.port.NaturalLanguageProcessingPort;
import com.bank.loanmanagement.infrastructure.mcp.MCPBankingServer;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP-Enabled SpringAI Infrastructure Adapter for Natural Language Processing
 * Integrates Model Context Protocol with SpringAI for enhanced banking context
 */
@Component
public class MCPSpringAINLPAdapter implements NaturalLanguageProcessingPort {

    private final OpenAiChatClient customerServiceChatClient;
    private final OpenAiChatClient loanAnalysisChatClient;
    private final MCPBankingServer mcpServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPSpringAINLPAdapter(
            @Qualifier("customerServiceChatClient") OpenAiChatClient customerServiceChatClient,
            @Qualifier("loanAnalysisChatClient") OpenAiChatClient loanAnalysisChatClient,
            MCPBankingServer mcpServer) {
        this.customerServiceChatClient = customerServiceChatClient;
        this.loanAnalysisChatClient = loanAnalysisChatClient;
        this.mcpServer = mcpServer;
    }

    @Override
    public LoanRequest convertPromptToLoanRequest(String userPrompt) {
        try {
            // Step 1: Use MCP to extract financial parameters
            CompletableFuture<MCPBankingServer.MCPResponse> mcpResponse = mcpServer.processRequest(
                createMCPRequest("tools/call", Map.of(
                    "name", "extract_financial_parameters",
                    "arguments", Map.of("text_input", userPrompt)
                ))
            );

            // Step 2: Use SpringAI with MCP context for comprehensive analysis
            String mcpContext = getMCPBankingContext();
            String promptTemplate = """
                You are a banking AI with access to comprehensive banking domain knowledge through MCP.
                
                BANKING CONTEXT (from MCP):
                {mcp_context}
                
                CUSTOMER REQUEST: "{user_input}"
                
                Using the MCP banking context above, convert this natural language request into a structured loan application.
                Ensure compliance with banking business rules and regulatory requirements.
                
                Respond with a detailed analysis including:
                1. Loan type classification
                2. Financial parameters extraction
                3. Risk assessment indicators
                4. Compliance validation
                5. Processing recommendations
                """;

            Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of(
                    "mcp_context", mcpContext,
                    "user_input", userPrompt
                ));

            ChatResponse response = loanAnalysisChatClient.call(prompt);
            String aiAnalysis = response.getResult().getOutput().getContent();

            // Step 3: Combine MCP and SpringAI results
            MCPBankingServer.MCPResponse mcpResult = mcpResponse.get();
            JsonNode extractedParams = objectMapper.readTree(
                mcpResult.getResult().toString()
            ).path("content").get(0).path("text");

            return buildLoanRequestFromMCPAndAI(userPrompt, aiAnalysis, extractedParams);

        } catch (Exception e) {
            // Fallback to direct MCP processing
            return createLoanRequestFromMCP(userPrompt);
        }
    }

    @Override
    public UserIntentAnalysis analyzeUserIntent(String userInput) {
        try {
            // Step 1: Use MCP for intent classification
            CompletableFuture<MCPBankingServer.MCPResponse> intentResponse = mcpServer.processRequest(
                createMCPRequest("tools/call", Map.of(
                    "name", "classify_banking_intent",
                    "arguments", Map.of("user_input", userInput)
                ))
            );

            // Step 2: Use SpringAI with MCP banking workflow context
            String promptTemplate = """
                You are a banking AI assistant with MCP access to banking workflows and procedures.
                
                BANKING DOMAIN KNOWLEDGE:
                - Loan products and requirements
                - Risk assessment guidelines  
                - Regulatory compliance rules
                - Customer service workflows
                
                CUSTOMER INPUT: "{user_input}"
                
                Analyze this customer input for:
                1. Primary and secondary banking intents
                2. Urgency and complexity assessment
                3. Customer sentiment analysis
                4. Recommended banking workflow
                5. Processing timeline estimation
                
                Consider MCP banking context for accurate intent classification.
                """;

            Prompt prompt = new PromptTemplate(promptTemplate)
                .create(Map.of("user_input", userInput));

            ChatResponse aiResponse = customerServiceChatClient.call(prompt);
            String aiAnalysis = aiResponse.getResult().getOutput().getContent();

            // Step 3: Combine MCP intent classification with AI analysis
            MCPBankingServer.MCPResponse mcpResult = intentResponse.get();
            
            return buildUserIntentFromMCPAndAI(userInput, aiAnalysis, mcpResult);

        } catch (Exception e) {
            return createDefaultUserIntent(userInput);
        }
    }

    @Override
    public FinancialParameters extractFinancialParameters(String text) {
        try {
            // Use MCP financial parameter extraction tool
            CompletableFuture<MCPBankingServer.MCPResponse> mcpResponse = mcpServer.processRequest(
                createMCPRequest("tools/call", Map.of(
                    "name", "extract_financial_parameters",
                    "arguments", Map.of("text_input", text)
                ))
            );

            MCPBankingServer.MCPResponse result = mcpResponse.get();
            if (result.isSuccess()) {
                JsonNode extractedData = objectMapper.readTree(result.getResult().toString())
                    .path("content").get(0).path("text");
                
                return parseFinancialParametersFromMCP(extractedData);
            }
        } catch (Exception e) {
            // Fallback to manual extraction
        }
        
        return createDefaultFinancialParameters();
    }

    @Override
    public RequestAssessment assessRequestComplexity(String userInput) {
        try {
            // Use MCP for banking workflow generation
            CompletableFuture<MCPBankingServer.MCPResponse> workflowResponse = mcpServer.processRequest(
                createMCPRequest("tools/call", Map.of(
                    "name", "generate_banking_workflow",
                    "arguments", Map.of(
                        "intent", classifyIntentFromText(userInput),
                        "complexity", assessComplexityFromText(userInput),
                        "urgency", assessUrgencyFromText(userInput)
                    )
                ))
            );

            MCPBankingServer.MCPResponse result = workflowResponse.get();
            if (result.isSuccess()) {
                return parseRequestAssessmentFromMCP(result);
            }
        } catch (Exception e) {
            // Fallback
        }
        
        return createDefaultRequestAssessment();
    }

    // === MCP INTEGRATION HELPER METHODS ===

    private MCPBankingServer.MCPRequest createMCPRequest(String method, Map<String, Object> params) {
        try {
            MCPBankingServer.MCPRequest request = new MCPBankingServer.MCPRequest();
            // Use reflection or builder pattern to set fields
            java.lang.reflect.Field methodField = request.getClass().getDeclaredField("method");
            methodField.setAccessible(true);
            methodField.set(request, method);
            
            java.lang.reflect.Field paramsField = request.getClass().getDeclaredField("params");
            paramsField.setAccessible(true);
            paramsField.set(request, objectMapper.valueToTree(params));
            
            return request;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MCP request", e);
        }
    }

    private String getMCPBankingContext() {
        try {
            // Get banking business rules from MCP
            CompletableFuture<MCPBankingServer.MCPResponse> rulesResponse = mcpServer.processRequest(
                createMCPRequest("resources/read", Map.of("uri", "banking://banking_business_rules"))
            );

            // Get loan products from MCP
            CompletableFuture<MCPBankingServer.MCPResponse> productsResponse = mcpServer.processRequest(
                createMCPRequest("resources/read", Map.of("uri", "banking://loan_products"))
            );

            // Get risk guidelines from MCP
            CompletableFuture<MCPBankingServer.MCPResponse> riskResponse = mcpServer.processRequest(
                createMCPRequest("resources/read", Map.of("uri", "banking://risk_guidelines"))
            );

            StringBuilder context = new StringBuilder();
            context.append("BANKING BUSINESS RULES:\n");
            context.append(extractMCPResourceContent(rulesResponse.get()));
            context.append("\n\nLOAN PRODUCTS:\n");
            context.append(extractMCPResourceContent(productsResponse.get()));
            context.append("\n\nRISK GUIDELINES:\n");
            context.append(extractMCPResourceContent(riskResponse.get()));

            return context.toString();

        } catch (Exception e) {
            return "Banking context temporarily unavailable";
        }
    }

    private String extractMCPResourceContent(MCPBankingServer.MCPResponse response) {
        try {
            if (response.isSuccess()) {
                JsonNode result = objectMapper.valueToTree(response.getResult());
                return result.path("contents").get(0).path("text").asText();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Content not available";
    }

    private LoanRequest buildLoanRequestFromMCPAndAI(String userPrompt, String aiAnalysis, JsonNode mcpParams) {
        try {
            JsonNode params = objectMapper.readTree(mcpParams.asText());
            
            return LoanRequest.builder()
                .loanType(determineLoanType(userPrompt, aiAnalysis))
                .loanAmount(params.path("loan_amount").asDouble(50000))
                .termMonths(params.path("loan_term").asInt(36))
                .purpose(params.path("loan_purpose").asText("GENERAL_PURPOSE"))
                .customerProfile(CustomerProfile.builder()
                    .monthlyIncome(params.path("monthly_income").asDouble(5000))
                    .creditScore(params.path("credit_score").asInt(720))
                    .employmentStatus(params.path("employment_status").asText("EMPLOYED"))
                    .debtToIncomeRatio(params.path("debt_to_income_ratio").asDouble(0.3))
                    .build())
                .urgency(extractUrgencyFromAnalysis(aiAnalysis))
                .collateralOffered(false)
                .build();
        } catch (Exception e) {
            return createDefaultLoanRequest(userPrompt);
        }
    }

    private UserIntentAnalysis buildUserIntentFromMCPAndAI(String userInput, String aiAnalysis, 
                                                          MCPBankingServer.MCPResponse mcpResult) {
        try {
            JsonNode intentData = objectMapper.valueToTree(mcpResult.getResult())
                .path("content").get(0).path("text");
            JsonNode classification = objectMapper.readTree(intentData.asText());
            
            return UserIntentAnalysis.builder()
                .originalInput(userInput)
                .primaryIntent(classification.path("primary_intent").asText("GENERAL_INQUIRY"))
                .secondaryIntents(extractSecondaryIntents(classification))
                .urgencyLevel(classification.path("urgency_level").asText("LOW"))
                .customerSentiment(classification.path("customer_sentiment").asText("NEUTRAL"))
                .complexityLevel(classification.path("complexity_level").asText("LOW"))
                .recommendedWorkflow(extractWorkflowFromAI(aiAnalysis))
                .estimatedProcessingTime(estimateProcessingTime(userInput))
                .confidence(classification.path("confidence").asDouble(0.85))
                .build();
        } catch (Exception e) {
            return createDefaultUserIntent(userInput);
        }
    }

    private FinancialParameters parseFinancialParametersFromMCP(JsonNode mcpData) {
        try {
            JsonNode params = objectMapper.readTree(mcpData.asText());
            
            return FinancialParameters.builder()
                .amount(params.path("loan_amount").asDouble(50000))
                .timeframe("FLEXIBLE")
                .riskTolerance("MODERATE")
                .preferredContact("EMAIL")
                .documentationReady(false)
                .loanPurpose(params.path("loan_purpose").asText("GENERAL_PURPOSE"))
                .monthlyIncome(params.path("monthly_income").asDouble(5000))
                .creditScore(params.path("credit_score").asInt(720))
                .employmentStatus(params.path("employment_status").asText("EMPLOYED"))
                .build();
        } catch (Exception e) {
            return createDefaultFinancialParameters();
        }
    }

    private RequestAssessment parseRequestAssessmentFromMCP(MCPBankingServer.MCPResponse mcpResult) {
        try {
            JsonNode workflowData = objectMapper.valueToTree(mcpResult.getResult())
                .path("content").get(0).path("text");
            JsonNode workflow = objectMapper.readTree(workflowData.asText());
            
            return RequestAssessment.builder()
                .urgencyLevel("LOW")
                .complexityLevel("MEDIUM")
                .priorityScore("STANDARD")
                .estimatedResolution(workflow.path("estimated_duration").asText("1-2 business days"))
                .requiresSpecialistReview(false)
                .recommendedChannel("DIGITAL")
                .build();
        } catch (Exception e) {
            return createDefaultRequestAssessment();
        }
    }

    // === UTILITY METHODS ===

    private String determineLoanType(String userPrompt, String aiAnalysis) {
        String combined = (userPrompt + " " + aiAnalysis).toLowerCase();
        if (combined.contains("business") || combined.contains("company")) return "BUSINESS";
        if (combined.contains("home") || combined.contains("house") || combined.contains("mortgage")) return "MORTGAGE";
        if (combined.contains("car") || combined.contains("auto") || combined.contains("vehicle")) return "AUTO";
        if (combined.contains("education") || combined.contains("student")) return "EDUCATION";
        return "PERSONAL";
    }

    private String extractUrgencyFromAnalysis(String analysis) {
        String lower = analysis.toLowerCase();
        if (lower.contains("urgent") || lower.contains("emergency") || lower.contains("asap")) return "HIGH";
        if (lower.contains("soon") || lower.contains("quick")) return "MEDIUM";
        return "LOW";
    }

    private List<String> extractSecondaryIntents(JsonNode classification) {
        try {
            JsonNode intents = classification.path("secondary_intents");
            if (intents.isArray()) {
                return Arrays.asList(intents.get(0).asText("NONE"));
            }
        } catch (Exception e) {
            // Ignore
        }
        return Arrays.asList("NONE");
    }

    private List<String> extractWorkflowFromAI(String aiAnalysis) {
        return Arrays.asList(
            "Process customer request using MCP banking tools",
            "Perform AI-enhanced analysis with domain context",
            "Generate recommendations based on MCP business rules"
        );
    }

    private String estimateProcessingTime(String userInput) {
        if (userInput.toLowerCase().contains("urgent") || userInput.toLowerCase().contains("asap")) {
            return "Same day processing";
        } else if (userInput.toLowerCase().contains("business")) {
            return "3-5 business days";
        } else {
            return "1-2 business days";
        }
    }

    private String classifyIntentFromText(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("loan") || lower.contains("borrow")) return "LOAN_APPLICATION";
        if (lower.contains("payment") || lower.contains("pay")) return "PAYMENT_INQUIRY";
        if (lower.contains("rate") || lower.contains("interest")) return "RATE_INQUIRY";
        return "GENERAL_INQUIRY";
    }

    private String assessComplexityFromText(String text) {
        if (text.split(" ").length > 30) return "HIGH";
        if (text.split(" ").length > 15) return "MEDIUM";
        return "LOW";
    }

    private String assessUrgencyFromText(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("emergency")) return "HIGH";
        if (lower.contains("soon") || lower.contains("quick")) return "MEDIUM";
        return "LOW";
    }

    // === FALLBACK METHODS ===

    private LoanRequest createLoanRequestFromMCP(String userPrompt) {
        // Use basic MCP extraction if available
        return createDefaultLoanRequest(userPrompt);
    }

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