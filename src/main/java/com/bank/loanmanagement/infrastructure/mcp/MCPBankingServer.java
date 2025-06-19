package com.bank.loanmanagement.infrastructure.mcp;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MCP (Model Context Protocol) Server for Banking Domain
 * Provides structured context and tools for AI models in banking operations
 */
@Component
public class MCPBankingServer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MCPTool> tools = new HashMap<>();
    private final Map<String, MCPResource> resources = new HashMap<>();

    public MCPBankingServer() {
        initializeBankingTools();
        initializeBankingResources();
    }

    /**
     * Initialize banking-specific MCP tools
     */
    private void initializeBankingTools() {
        // Loan Analysis Tool
        tools.put("analyze_loan_application", new MCPTool(
            "analyze_loan_application",
            "Analyzes loan applications using banking business rules",
            Map.of(
                "application_data", new MCPParameter("object", "Loan application data", true),
                "customer_profile", new MCPParameter("object", "Customer financial profile", true),
                "loan_parameters", new MCPParameter("object", "Loan amount, term, purpose", true)
            ),
            this::analyzeLoanApplication
        ));

        // Risk Assessment Tool
        tools.put("assess_credit_risk", new MCPTool(
            "assess_credit_risk",
            "Performs comprehensive credit risk assessment",
            Map.of(
                "customer_data", new MCPParameter("object", "Customer financial data", true),
                "loan_data", new MCPParameter("object", "Loan details", true),
                "market_conditions", new MCPParameter("object", "Current market conditions", false)
            ),
            this::assessCreditRisk
        ));

        // Intent Classification Tool
        tools.put("classify_banking_intent", new MCPTool(
            "classify_banking_intent",
            "Classifies customer intent for banking services",
            Map.of(
                "user_input", new MCPParameter("string", "Customer's natural language input", true),
                "context", new MCPParameter("object", "Additional context", false)
            ),
            this::classifyBankingIntent
        ));

        // Financial Parameter Extraction Tool
        tools.put("extract_financial_parameters", new MCPTool(
            "extract_financial_parameters",
            "Extracts structured financial parameters from text",
            Map.of(
                "text_input", new MCPParameter("string", "Natural language text", true),
                "extraction_type", new MCPParameter("string", "Type of extraction (loan, income, etc.)", false)
            ),
            this::extractFinancialParameters
        ));

        // Banking Workflow Tool
        tools.put("generate_banking_workflow", new MCPTool(
            "generate_banking_workflow",
            "Generates appropriate banking workflow based on intent",
            Map.of(
                "intent", new MCPParameter("string", "Primary banking intent", true),
                "complexity", new MCPParameter("string", "Request complexity level", true),
                "urgency", new MCPParameter("string", "Urgency level", false)
            ),
            this::generateBankingWorkflow
        ));
    }

    /**
     * Initialize banking domain resources
     */
    private void initializeBankingResources() {
        // Banking Business Rules
        resources.put("banking_business_rules", new MCPResource(
            "banking_business_rules",
            "Comprehensive banking business rules and regulations",
            "application/json",
            getBankingBusinessRules()
        ));

        // Loan Products Catalog
        resources.put("loan_products", new MCPResource(
            "loan_products",
            "Available loan products and their specifications",
            "application/json",
            getLoanProductsCatalog()
        ));

        // Risk Assessment Guidelines
        resources.put("risk_guidelines", new MCPResource(
            "risk_guidelines",
            "Credit risk assessment guidelines and criteria",
            "application/json",
            getRiskAssessmentGuidelines()
        ));

        // Regulatory Compliance
        resources.put("compliance_rules", new MCPResource(
            "compliance_rules",
            "Banking regulatory compliance requirements",
            "application/json",
            getComplianceRules()
        ));
    }

    /**
     * Process MCP request with banking context
     */
    public CompletableFuture<MCPResponse> processRequest(MCPRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (request.getMethod()) {
                    case "tools/list":
                        return listTools();
                    case "tools/call":
                        return callTool(request);
                    case "resources/list":
                        return listResources();
                    case "resources/read":
                        return readResource(request);
                    default:
                        return MCPResponse.error("Unknown method: " + request.getMethod());
                }
            } catch (Exception e) {
                return MCPResponse.error("MCP processing error: " + e.getMessage());
            }
        });
    }

    /**
     * List available banking tools
     */
    private MCPResponse listTools() {
        List<Map<String, Object>> toolsList = new ArrayList<>();
        for (MCPTool tool : tools.values()) {
            toolsList.add(Map.of(
                "name", tool.getName(),
                "description", tool.getDescription(),
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", tool.getParameters()
                )
            ));
        }
        return MCPResponse.success(Map.of("tools", toolsList));
    }

    /**
     * Call a specific banking tool
     */
    private MCPResponse callTool(MCPRequest request) {
        String toolName = request.getParams().path("name").asText();
        JsonNode arguments = request.getParams().path("arguments");

        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            return MCPResponse.error("Tool not found: " + toolName);
        }

        try {
            Object result = tool.getHandler().apply(arguments);
            return MCPResponse.success(Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", objectMapper.writeValueAsString(result)
                ))
            ));
        } catch (Exception e) {
            return MCPResponse.error("Tool execution error: " + e.getMessage());
        }
    }

    /**
     * List available banking resources
     */
    private MCPResponse listResources() {
        List<Map<String, Object>> resourcesList = new ArrayList<>();
        for (MCPResource resource : resources.values()) {
            resourcesList.add(Map.of(
                "uri", "banking://" + resource.getName(),
                "name", resource.getName(),
                "description", resource.getDescription(),
                "mimeType", resource.getMimeType()
            ));
        }
        return MCPResponse.success(Map.of("resources", resourcesList));
    }

    /**
     * Read a specific banking resource
     */
    private MCPResponse readResource(MCPRequest request) {
        String uri = request.getParams().path("uri").asText();
        String resourceName = uri.replace("banking://", "");

        MCPResource resource = resources.get(resourceName);
        if (resource == null) {
            return MCPResponse.error("Resource not found: " + resourceName);
        }

        return MCPResponse.success(Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", resource.getMimeType(),
                "text", resource.getContent()
            ))
        ));
    }

    // === BANKING TOOL IMPLEMENTATIONS ===

    private Object analyzeLoanApplication(JsonNode arguments) {
        // Extract application data
        JsonNode applicationData = arguments.path("application_data");
        JsonNode customerProfile = arguments.path("customer_profile");
        JsonNode loanParameters = arguments.path("loan_parameters");

        // Perform banking analysis
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("creditworthiness", assessCreditworthiness(customerProfile));
        analysis.put("risk_score", calculateRiskScore(customerProfile, loanParameters));
        analysis.put("recommendation", generateRecommendation(customerProfile, loanParameters));
        analysis.put("compliance_check", checkCompliance(applicationData));
        analysis.put("processing_time", estimateProcessingTime(loanParameters));

        return analysis;
    }

    private Object assessCreditRisk(JsonNode arguments) {
        JsonNode customerData = arguments.path("customer_data");
        JsonNode loanData = arguments.path("loan_data");

        Map<String, Object> riskAssessment = new HashMap<>();
        riskAssessment.put("risk_category", calculateRiskCategory(customerData));
        riskAssessment.put("probability_of_default", calculateDefaultProbability(customerData, loanData));
        riskAssessment.put("risk_factors", identifyRiskFactors(customerData, loanData));
        riskAssessment.put("mitigation_strategies", suggestMitigationStrategies(customerData));
        riskAssessment.put("portfolio_impact", assessPortfolioImpact(loanData));

        return riskAssessment;
    }

    private Object classifyBankingIntent(JsonNode arguments) {
        String userInput = arguments.path("user_input").asText();

        Map<String, Object> intentClassification = new HashMap<>();
        intentClassification.put("primary_intent", classifyPrimaryIntent(userInput));
        intentClassification.put("secondary_intents", identifySecondaryIntents(userInput));
        intentClassification.put("confidence", calculateIntentConfidence(userInput));
        intentClassification.put("urgency_level", assessUrgency(userInput));
        intentClassification.put("customer_sentiment", analyzeSentiment(userInput));
        intentClassification.put("complexity_level", assessComplexity(userInput));

        return intentClassification;
    }

    private Object extractFinancialParameters(JsonNode arguments) {
        String textInput = arguments.path("text_input").asText();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("loan_amount", extractLoanAmount(textInput));
        parameters.put("loan_term", extractLoanTerm(textInput));
        parameters.put("loan_purpose", extractLoanPurpose(textInput));
        parameters.put("monthly_income", extractMonthlyIncome(textInput));
        parameters.put("credit_score", extractCreditScore(textInput));
        parameters.put("employment_status", extractEmploymentStatus(textInput));
        parameters.put("debt_to_income_ratio", extractDebtRatio(textInput));

        return parameters;
    }

    private Object generateBankingWorkflow(JsonNode arguments) {
        String intent = arguments.path("intent").asText();
        String complexity = arguments.path("complexity").asText();

        List<Map<String, Object>> workflow = new ArrayList<>();
        
        switch (intent.toUpperCase()) {
            case "LOAN_APPLICATION":
                workflow.add(createWorkflowStep("document_collection", "Collect required documentation", 1));
                workflow.add(createWorkflowStep("identity_verification", "Verify customer identity", 2));
                workflow.add(createWorkflowStep("credit_analysis", "Perform credit analysis using MCP tools", 3));
                workflow.add(createWorkflowStep("risk_assessment", "Assess credit risk with MCP", 4));
                workflow.add(createWorkflowStep("decision_generation", "Generate loan decision", 5));
                break;
            case "PAYMENT_INQUIRY":
                workflow.add(createWorkflowStep("account_lookup", "Retrieve account information", 1));
                workflow.add(createWorkflowStep("payment_analysis", "Analyze payment history", 2));
                workflow.add(createWorkflowStep("response_generation", "Generate customer response", 3));
                break;
            default:
                workflow.add(createWorkflowStep("intent_routing", "Route to appropriate service", 1));
        }

        return Map.of("workflow_steps", workflow, "estimated_duration", estimateWorkflowDuration(workflow));
    }

    // === BANKING BUSINESS LOGIC METHODS ===

    private String assessCreditworthiness(JsonNode customerProfile) {
        int creditScore = customerProfile.path("credit_score").asInt(720);
        if (creditScore >= 750) return "EXCELLENT";
        if (creditScore >= 700) return "GOOD";
        if (creditScore >= 650) return "FAIR";
        return "POOR";
    }

    private double calculateRiskScore(JsonNode customerProfile, JsonNode loanParameters) {
        int creditScore = customerProfile.path("credit_score").asInt(720);
        double debtRatio = customerProfile.path("debt_to_income_ratio").asDouble(0.3);
        double loanAmount = loanParameters.path("loan_amount").asDouble(50000);
        
        double riskScore = (850 - creditScore) * 0.4 + (debtRatio * 100) * 0.6;
        return Math.min(100, Math.max(0, riskScore));
    }

    private String generateRecommendation(JsonNode customerProfile, JsonNode loanParameters) {
        double riskScore = calculateRiskScore(customerProfile, loanParameters);
        if (riskScore <= 30) return "APPROVE";
        if (riskScore <= 60) return "CONDITIONAL_APPROVAL";
        return "DECLINE";
    }

    private boolean checkCompliance(JsonNode applicationData) {
        // Simplified compliance check
        return applicationData.has("customer_id") && 
               applicationData.has("loan_amount") && 
               applicationData.has("loan_purpose");
    }

    private String estimateProcessingTime(JsonNode loanParameters) {
        double amount = loanParameters.path("loan_amount").asDouble(50000);
        if (amount > 100000) return "5-7 business days";
        if (amount > 50000) return "3-5 business days";
        return "1-3 business days";
    }

    private String calculateRiskCategory(JsonNode customerData) {
        int creditScore = customerData.path("credit_score").asInt(720);
        if (creditScore >= 750) return "LOW";
        if (creditScore >= 650) return "MEDIUM";
        return "HIGH";
    }

    private double calculateDefaultProbability(JsonNode customerData, JsonNode loanData) {
        int creditScore = customerData.path("credit_score").asInt(720);
        double debtRatio = customerData.path("debt_to_income_ratio").asDouble(0.3);
        
        double probability = (1 - (creditScore / 850.0)) * 0.7 + debtRatio * 0.3;
        return Math.min(1.0, Math.max(0.0, probability));
    }

    private List<String> identifyRiskFactors(JsonNode customerData, JsonNode loanData) {
        List<String> factors = new ArrayList<>();
        
        if (customerData.path("credit_score").asInt() < 650) {
            factors.add("Low credit score");
        }
        if (customerData.path("debt_to_income_ratio").asDouble() > 0.4) {
            factors.add("High debt-to-income ratio");
        }
        if (customerData.path("employment_years").asInt() < 2) {
            factors.add("Limited employment history");
        }
        
        return factors;
    }

    private List<String> suggestMitigationStrategies(JsonNode customerData) {
        List<String> strategies = new ArrayList<>();
        strategies.add("Regular payment monitoring");
        strategies.add("Financial counseling if needed");
        strategies.add("Automated payment setup");
        return strategies;
    }

    private String assessPortfolioImpact(JsonNode loanData) {
        double amount = loanData.path("loan_amount").asDouble();
        if (amount > 100000) return "SIGNIFICANT";
        if (amount > 50000) return "MODERATE";
        return "MINIMAL";
    }

    private String classifyPrimaryIntent(String userInput) {
        String lower = userInput.toLowerCase();
        if (lower.contains("loan") || lower.contains("borrow")) return "LOAN_APPLICATION";
        if (lower.contains("payment") || lower.contains("pay")) return "PAYMENT_INQUIRY";
        if (lower.contains("rate") || lower.contains("interest")) return "RATE_INQUIRY";
        if (lower.contains("account") || lower.contains("balance")) return "ACCOUNT_INQUIRY";
        return "GENERAL_INQUIRY";
    }

    private List<String> identifySecondaryIntents(String userInput) {
        List<String> intents = new ArrayList<>();
        String lower = userInput.toLowerCase();
        
        if (lower.contains("document") || lower.contains("requirement")) {
            intents.add("DOCUMENTATION_REQUEST");
        }
        if (lower.contains("timeline") || lower.contains("when")) {
            intents.add("TIMELINE_INQUIRY");
        }
        if (lower.contains("qualify") || lower.contains("eligible")) {
            intents.add("ELIGIBILITY_CHECK");
        }
        
        return intents;
    }

    private double calculateIntentConfidence(String userInput) {
        // Simplified confidence calculation
        if (userInput.length() > 50 && userInput.contains("loan")) return 0.95;
        if (userInput.contains("loan") || userInput.contains("payment")) return 0.85;
        return 0.70;
    }

    private String assessUrgency(String userInput) {
        String lower = userInput.toLowerCase();
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("emergency")) return "HIGH";
        if (lower.contains("soon") || lower.contains("quick")) return "MEDIUM";
        return "LOW";
    }

    private String analyzeSentiment(String userInput) {
        String lower = userInput.toLowerCase();
        if (lower.contains("frustrated") || lower.contains("angry")) return "NEGATIVE";
        if (lower.contains("excited") || lower.contains("happy")) return "POSITIVE";
        if (lower.contains("worried") || lower.contains("concerned")) return "CONCERNED";
        return "NEUTRAL";
    }

    private String assessComplexity(String userInput) {
        if (userInput.split(" ").length > 30) return "HIGH";
        if (userInput.split(" ").length > 15) return "MEDIUM";
        return "LOW";
    }

    // Financial parameter extraction methods (reusing existing logic)
    private double extractLoanAmount(String text) {
        // Reuse existing pattern matching logic
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$([0-9,]+(?:\\.[0-9]{2})?)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (Exception e) { }
        }
        return 50000.0;
    }

    private int extractLoanTerm(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("3 year") || lower.contains("36 month")) return 36;
        if (lower.contains("2 year") || lower.contains("24 month")) return 24;
        if (lower.contains("5 year") || lower.contains("60 month")) return 60;
        return 36;
    }

    private String extractLoanPurpose(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("renovation") || lower.contains("kitchen")) return "HOME_RENOVATION";
        if (lower.contains("business") || lower.contains("expansion")) return "BUSINESS_EXPANSION";
        if (lower.contains("debt") || lower.contains("consolidation")) return "DEBT_CONSOLIDATION";
        return "GENERAL_PURPOSE";
    }

    private double extractMonthlyIncome(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$([0-9,]+).*month");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (Exception e) { }
        }
        return 5000.0;
    }

    private int extractCreditScore(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("credit.*?([0-9]{3})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception e) { }
        }
        return 720;
    }

    private String extractEmploymentStatus(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("unemployed")) return "UNEMPLOYED";
        if (lower.contains("part time")) return "PART_TIME";
        if (lower.contains("self employed")) return "SELF_EMPLOYED";
        return "EMPLOYED";
    }

    private double extractDebtRatio(String text) {
        return 0.3; // Default
    }

    private Map<String, Object> createWorkflowStep(String id, String description, int order) {
        return Map.of(
            "step_id", id,
            "description", description,
            "order", order,
            "estimated_duration", "30 minutes"
        );
    }

    private String estimateWorkflowDuration(List<Map<String, Object>> workflow) {
        return (workflow.size() * 30) + " minutes";
    }

    // === BANKING DOMAIN KNOWLEDGE ===

    private String getBankingBusinessRules() {
        return """
        {
          "loan_limits": {
            "personal": {"min": 1000, "max": 500000},
            "business": {"min": 10000, "max": 2000000},
            "mortgage": {"min": 50000, "max": 5000000}
          },
          "interest_rates": {
            "personal": {"min": 0.05, "max": 0.25},
            "business": {"min": 0.06, "max": 0.18},
            "mortgage": {"min": 0.03, "max": 0.08}
          },
          "credit_requirements": {
            "excellent": {"min_score": 750, "max_dti": 0.28},
            "good": {"min_score": 700, "max_dti": 0.36},
            "fair": {"min_score": 650, "max_dti": 0.43}
          },
          "processing_times": {
            "personal": "1-3 business days",
            "business": "3-7 business days",
            "mortgage": "15-30 business days"
          }
        }
        """;
    }

    private String getLoanProductsCatalog() {
        return """
        {
          "products": [
            {
              "name": "Personal Loan",
              "type": "PERSONAL",
              "min_amount": 1000,
              "max_amount": 500000,
              "terms": [6, 9, 12, 24, 36, 48, 60],
              "base_rate": 0.08,
              "features": ["No collateral required", "Fixed interest rate", "Flexible terms"]
            },
            {
              "name": "Business Loan",
              "type": "BUSINESS",
              "min_amount": 10000,
              "max_amount": 2000000,
              "terms": [12, 24, 36, 48, 60, 84, 120],
              "base_rate": 0.07,
              "features": ["Competitive rates", "Business use only", "Collateral may be required"]
            },
            {
              "name": "Home Equity Loan",
              "type": "HOME_EQUITY",
              "min_amount": 15000,
              "max_amount": 500000,
              "terms": [60, 84, 120, 180, 240],
              "base_rate": 0.05,
              "features": ["Tax deductible interest", "Fixed rate", "Home as collateral"]
            }
          ]
        }
        """;
    }

    private String getRiskAssessmentGuidelines() {
        return """
        {
          "risk_factors": {
            "credit_score": {
              "excellent": {"range": [750, 850], "weight": 0.35, "risk": "LOW"},
              "good": {"range": [700, 749], "weight": 0.35, "risk": "LOW"},
              "fair": {"range": [650, 699], "weight": 0.35, "risk": "MEDIUM"},
              "poor": {"range": [300, 649], "weight": 0.35, "risk": "HIGH"}
            },
            "debt_to_income": {
              "excellent": {"range": [0, 0.28], "weight": 0.25, "risk": "LOW"},
              "good": {"range": [0.28, 0.36], "weight": 0.25, "risk": "LOW"},
              "fair": {"range": [0.36, 0.43], "weight": 0.25, "risk": "MEDIUM"},
              "poor": {"range": [0.43, 1.0], "weight": 0.25, "risk": "HIGH"}
            },
            "employment_stability": {
              "stable": {"min_years": 2, "weight": 0.20, "risk": "LOW"},
              "moderate": {"min_years": 1, "weight": 0.20, "risk": "MEDIUM"},
              "unstable": {"min_years": 0, "weight": 0.20, "risk": "HIGH"}
            }
          },
          "decision_matrix": {
            "approve": {"max_risk_score": 30, "min_credit_score": 700},
            "conditional": {"max_risk_score": 60, "min_credit_score": 650},
            "decline": {"min_risk_score": 60, "max_credit_score": 650}
          }
        }
        """;
    }

    private String getComplianceRules() {
        return """
        {
          "regulatory_requirements": {
            "truth_in_lending": {
              "apr_disclosure": "required",
              "payment_schedule": "required",
              "total_cost": "required"
            },
            "fair_credit_reporting": {
              "credit_check_consent": "required",
              "adverse_action_notice": "required"
            },
            "equal_credit_opportunity": {
              "non_discrimination": "required",
              "reason_for_denial": "required"
            }
          },
          "internal_policies": {
            "maximum_exposure": 5000000,
            "approval_limits": {
              "loan_officer": 50000,
              "senior_officer": 250000,
              "committee": 5000000
            },
            "documentation_requirements": {
              "income_verification": "required",
              "identity_verification": "required",
              "credit_report": "required"
            }
          }
        }
        """;
    }

    // === MCP DATA CLASSES ===

    public static class MCPRequest {
        private String method;
        private JsonNode params;

        public String getMethod() { return method; }
        public JsonNode getParams() { return params; }
    }

    public static class MCPResponse {
        private boolean success;
        private Object result;
        private String error;

        public static MCPResponse success(Object result) {
            MCPResponse response = new MCPResponse();
            response.success = true;
            response.result = result;
            return response;
        }

        public static MCPResponse error(String error) {
            MCPResponse response = new MCPResponse();
            response.success = false;
            response.error = error;
            return response;
        }

        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getError() { return error; }
    }

    public static class MCPTool {
        private String name;
        private String description;
        private Map<String, MCPParameter> parameters;
        private java.util.function.Function<JsonNode, Object> handler;

        public MCPTool(String name, String description, Map<String, MCPParameter> parameters,
                       java.util.function.Function<JsonNode, Object> handler) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.handler = handler;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, MCPParameter> getParameters() { return parameters; }
        public java.util.function.Function<JsonNode, Object> getHandler() { return handler; }
    }

    public static class MCPParameter {
        private String type;
        private String description;
        private boolean required;

        public MCPParameter(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
    }

    public static class MCPResource {
        private String name;
        private String description;
        private String mimeType;
        private String content;

        public MCPResource(String name, String description, String mimeType, String content) {
            this.name = name;
            this.description = description;
            this.mimeType = mimeType;
            this.content = content;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMimeType() { return mimeType; }
        public String getContent() { return content; }
    }
}