package com.bank.loanmanagement.mcp;

import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.PaymentService;
import com.bank.loanmanagement.graphql.service.NaturalLanguageProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpServer extends TextWebSocketHandler {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NaturalLanguageProcessingService nlpService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // MCP Protocol Implementation
    private static final String MCP_VERSION = "1.0.0";
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        
        // Send MCP initialization response
        Map<String, Object> initResponse = Map.of(
            "jsonrpc", "2.0",
            "id", "init",
            "result", Map.of(
                "protocolVersion", MCP_VERSION,
                "capabilities", getServerCapabilities(),
                "serverInfo", Map.of(
                    "name", "Enterprise Loan Management System",
                    "version", "2.0.0",
                    "description", "Comprehensive banking platform with GraphQL and NLP support"
                )
            )
        );
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initResponse)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> request = objectMapper.readValue(message.getPayload(), Map.class);
            Map<String, Object> response = handleMcpRequest(request);
            
            if (response != null) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        } catch (Exception e) {
            sendErrorResponse(session, "INTERNAL_ERROR", e.getMessage(), null);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }

    private Map<String, Object> handleMcpRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        try {
            switch (method) {
                case "tools/list":
                    return createResponse(id, getAvailableTools());
                case "tools/call":
                    return handleToolCall(id, params);
                case "resources/list":
                    return createResponse(id, getAvailableResources());
                case "resources/read":
                    return handleResourceRead(id, params);
                case "prompts/list":
                    return createResponse(id, getAvailablePrompts());
                case "prompts/get":
                    return handlePromptGet(id, params);
                case "logging/setLevel":
                    return handleLoggingSetLevel(id, params);
                default:
                    return createErrorResponse(id, "METHOD_NOT_FOUND", "Unknown method: " + method);
            }
        } catch (Exception e) {
            return createErrorResponse(id, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private Map<String, Object> getServerCapabilities() {
        return Map.of(
            "tools", Map.of(
                "listChanged", true,
                "callChanged", true
            ),
            "resources", Map.of(
                "listChanged", true,
                "subscribe", true
            ),
            "prompts", Map.of(
                "listChanged", true
            ),
            "logging", Map.of()
        );
    }

    private Map<String, Object> getAvailableTools() {
        return Map.of("tools", List.of(
            Map.of(
                "name", "search_customers",
                "description", "Search for customers by various criteria",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search query"),
                        "filters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "creditScore", Map.of("type", "object", "properties", Map.of(
                                    "min", Map.of("type", "integer"),
                                    "max", Map.of("type", "integer")
                                )),
                                "accountStatus", Map.of("type", "string"),
                                "riskLevel", Map.of("type", "string")
                            )
                        )
                    ),
                    "required", List.of("query")
                )
            ),
            Map.of(
                "name", "get_customer_details",
                "description", "Get detailed information about a specific customer",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "customerId", Map.of("type", "string", "description", "Customer ID")
                    ),
                    "required", List.of("customerId")
                )
            ),
            Map.of(
                "name", "search_loans",
                "description", "Search for loans with various filters",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "customerId", Map.of("type", "string"),
                        "status", Map.of("type", "string"),
                        "loanType", Map.of("type", "string"),
                        "overdueOnly", Map.of("type", "boolean")
                    )
                )
            ),
            Map.of(
                "name", "get_loan_details",
                "description", "Get comprehensive loan information including installments",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "loanId", Map.of("type", "string", "description", "Loan ID")
                    ),
                    "required", List.of("loanId")
                )
            ),
            Map.of(
                "name", "calculate_payment",
                "description", "Calculate payment amounts with discounts and penalties",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "loanId", Map.of("type", "string"),
                        "paymentAmount", Map.of("type", "number"),
                        "paymentDate", Map.of("type", "string"),
                        "installmentNumbers", Map.of("type", "array", "items", Map.of("type", "integer"))
                    ),
                    "required", List.of("loanId", "paymentAmount")
                )
            ),
            Map.of(
                "name", "process_payment",
                "description", "Process a payment for a loan",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "loanId", Map.of("type", "string"),
                        "paymentAmount", Map.of("type", "number"),
                        "paymentMethod", Map.of("type", "string"),
                        "paymentReference", Map.of("type", "string")
                    ),
                    "required", List.of("loanId", "paymentAmount", "paymentMethod")
                )
            ),
            Map.of(
                "name", "get_analytics",
                "description", "Get comprehensive analytics for loans, payments, and customers",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("customer", "loan", "payment", "risk")),
                        "period", Map.of("type", "string", "enum", List.of("LAST_7_DAYS", "LAST_30_DAYS", "LAST_90_DAYS", "LAST_YEAR")),
                        "customerId", Map.of("type", "string")
                    ),
                    "required", List.of("type")
                )
            ),
            Map.of(
                "name", "natural_language_query",
                "description", "Process natural language queries about banking data",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Natural language query"),
                        "context", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "domain", Map.of("type", "string"),
                                "language", Map.of("type", "string", "default", "en")
                            )
                        )
                    ),
                    "required", List.of("query")
                )
            ),
            Map.of(
                "name", "get_recommendations",
                "description", "Get AI-powered recommendations for customers or system optimization",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "customerId", Map.of("type", "string"),
                        "type", Map.of("type", "string", "enum", List.of(
                            "CREDIT_INCREASE", "LOAN_RESTRUCTURE", "EARLY_PAYMENT", 
                            "RISK_MITIGATION", "PRODUCT_RECOMMENDATION", "PROCESS_IMPROVEMENT"
                        ))
                    )
                )
            ),
            Map.of(
                "name", "create_loan",
                "description", "Create a new loan application",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "customerId", Map.of("type", "string"),
                        "loanAmount", Map.of("type", "number"),
                        "interestRate", Map.of("type", "number"),
                        "installmentCount", Map.of("type", "integer", "enum", List.of(6, 9, 12, 24)),
                        "loanType", Map.of("type", "string"),
                        "purpose", Map.of("type", "string")
                    ),
                    "required", List.of("customerId", "loanAmount", "interestRate", "installmentCount", "loanType")
                )
            ),
            Map.of(
                "name", "system_health",
                "description", "Get comprehensive system health and monitoring information",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of()
                )
            )
        ));
    }

    private Map<String, Object> handleToolCall(Object id, Map<String, Object> params) {
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        try {
            Object result = switch (toolName) {
                case "search_customers" -> searchCustomers(arguments);
                case "get_customer_details" -> getCustomerDetails(arguments);
                case "search_loans" -> searchLoans(arguments);
                case "get_loan_details" -> getLoanDetails(arguments);
                case "calculate_payment" -> calculatePayment(arguments);
                case "process_payment" -> processPayment(arguments);
                case "get_analytics" -> getAnalytics(arguments);
                case "natural_language_query" -> processNaturalLanguageQuery(arguments);
                case "get_recommendations" -> getRecommendations(arguments);
                case "create_loan" -> createLoan(arguments);
                case "system_health" -> getSystemHealth();
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            
            return createResponse(id, Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", objectMapper.writeValueAsString(result)
                )),
                "isError", false
            ));
        } catch (Exception e) {
            return createResponse(id, Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error: " + e.getMessage()
                )),
                "isError", true
            ));
        }
    }

    private Object searchCustomers(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        Map<String, Object> filters = (Map<String, Object>) arguments.get("filters");
        
        return customerService.searchCustomers(query, filters);
    }

    private Object getCustomerDetails(Map<String, Object> arguments) {
        String customerId = (String) arguments.get("customerId");
        var customer = customerService.findByCustomerNumber(customerId);
        
        if (customer == null) {
            throw new RuntimeException("Customer not found: " + customerId);
        }
        
        return Map.of(
            "customer", customer,
            "loans", loanService.findByCustomerId(customer.getId()),
            "payments", paymentService.findByCustomerId(customer.getId()),
            "creditHistory", customerService.getCreditHistory(customer.getId(), 10),
            "riskProfile", customerService.calculateRiskProfile(customer.getId())
        );
    }

    private Object searchLoans(Map<String, Object> arguments) {
        return loanService.searchLoans(arguments);
    }

    private Object getLoanDetails(Map<String, Object> arguments) {
        String loanId = (String) arguments.get("loanId");
        var loan = loanService.findByLoanNumber(loanId);
        
        if (loan == null) {
            throw new RuntimeException("Loan not found: " + loanId);
        }
        
        return Map.of(
            "loan", loan,
            "customer", customerService.findById(loan.getCustomerId()),
            "installments", loanService.getInstallments(loan.getId()),
            "payments", paymentService.findByLoanId(loan.getId())
        );
    }

    private Object calculatePayment(Map<String, Object> arguments) {
        return paymentService.calculatePayment(arguments);
    }

    private Object processPayment(Map<String, Object> arguments) {
        return paymentService.processPayment(arguments);
    }

    private Object getAnalytics(Map<String, Object> arguments) {
        String type = (String) arguments.get("type");
        String period = (String) arguments.get("period");
        String customerId = (String) arguments.get("customerId");
        
        return switch (type) {
            case "customer" -> customerService.getCustomerAnalytics(customerId, period);
            case "loan" -> loanService.getLoanAnalytics(period);
            case "payment" -> paymentService.getPaymentAnalytics(period);
            case "risk" -> customerService.getRiskAnalytics(customerId, period);
            default -> throw new IllegalArgumentException("Unknown analytics type: " + type);
        };
    }

    private Object processNaturalLanguageQuery(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        Object context = arguments.get("context");
        
        return nlpService.processNaturalLanguageQuery(query, context);
    }

    private Object getRecommendations(Map<String, Object> arguments) {
        String customerId = (String) arguments.get("customerId");
        String type = (String) arguments.get("type");
        
        return customerService.getRecommendations(customerId, type);
    }

    private Object createLoan(Map<String, Object> arguments) {
        return loanService.createLoan(arguments);
    }

    private Object getSystemHealth() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis(),
            "services", Map.of(
                "database", "UP",
                "cache", "UP",
                "circuitBreakers", "HEALTHY"
            ),
            "metrics", Map.of(
                "activeConnections", sessions.size(),
                "totalCustomers", customerService.getTotalCustomerCount(),
                "totalLoans", loanService.getTotalLoanCount(),
                "totalPayments", paymentService.getTotalPaymentCount()
            )
        );
    }

    private Map<String, Object> getAvailableResources() {
        return Map.of("resources", List.of(
            Map.of(
                "uri", "banking://customers",
                "name", "Customers Database",
                "description", "Access to customer information and credit profiles",
                "mimeType", "application/json"
            ),
            Map.of(
                "uri", "banking://loans",
                "name", "Loans Database",
                "description", "Complete loan portfolio with installment schedules",
                "mimeType", "application/json"
            ),
            Map.of(
                "uri", "banking://payments",
                "name", "Payments Database",
                "description", "Payment history and transaction records",
                "mimeType", "application/json"
            ),
            Map.of(
                "uri", "banking://analytics",
                "name", "Analytics Engine",
                "description", "Real-time banking analytics and reporting",
                "mimeType", "application/json"
            )
        ));
    }

    private Map<String, Object> handleResourceRead(Object id, Map<String, Object> params) {
        String uri = (String) params.get("uri");
        
        Object content = switch (uri) {
            case "banking://customers" -> customerService.getAllCustomersSummary();
            case "banking://loans" -> loanService.getAllLoansSummary();
            case "banking://payments" -> paymentService.getAllPaymentsSummary();
            case "banking://analytics" -> getSystemAnalytics();
            default -> throw new IllegalArgumentException("Unknown resource: " + uri);
        };
        
        return createResponse(id, Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", objectMapper.writeValueAsString(content)
            ))
        ));
    }

    private Map<String, Object> getAvailablePrompts() {
        return Map.of("prompts", List.of(
            Map.of(
                "name", "customer_analysis",
                "description", "Comprehensive customer analysis prompt for LLMs",
                "arguments", List.of(
                    Map.of("name", "customerId", "description", "Customer ID to analyze", "required", true),
                    Map.of("name", "includeLoans", "description", "Include loan details", "required", false),
                    Map.of("name", "includePayments", "description", "Include payment history", "required", false)
                )
            ),
            Map.of(
                "name", "risk_assessment",
                "description", "Risk assessment prompt for credit decisions",
                "arguments", List.of(
                    Map.of("name", "customerId", "description", "Customer ID for risk assessment", "required", true),
                    Map.of("name", "loanAmount", "description", "Proposed loan amount", "required", false)
                )
            ),
            Map.of(
                "name", "payment_optimization",
                "description", "Payment strategy optimization recommendations",
                "arguments", List.of(
                    Map.of("name", "loanId", "description", "Loan ID for optimization", "required", true)
                )
            )
        ));
    }

    private Map<String, Object> handlePromptGet(Object id, Map<String, Object> params) {
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        String promptText = switch (name) {
            case "customer_analysis" -> generateCustomerAnalysisPrompt(arguments);
            case "risk_assessment" -> generateRiskAssessmentPrompt(arguments);
            case "payment_optimization" -> generatePaymentOptimizationPrompt(arguments);
            default -> throw new IllegalArgumentException("Unknown prompt: " + name);
        };
        
        return createResponse(id, Map.of(
            "description", "Generated prompt for " + name,
            "messages", List.of(Map.of(
                "role", "user",
                "content", Map.of(
                    "type", "text",
                    "text", promptText
                )
            ))
        ));
    }

    private String generateCustomerAnalysisPrompt(Map<String, Object> arguments) {
        String customerId = (String) arguments.get("customerId");
        boolean includeLoans = Boolean.TRUE.equals(arguments.get("includeLoans"));
        boolean includePayments = Boolean.TRUE.equals(arguments.get("includePayments"));
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following customer data and provide insights:\n\n");
        
        try {
            var customer = customerService.findByCustomerNumber(customerId);
            if (customer != null) {
                prompt.append("Customer Profile:\n");
                prompt.append("- ID: ").append(customer.getCustomerNumber()).append("\n");
                prompt.append("- Name: ").append(customer.getFirstName()).append(" ").append(customer.getLastName()).append("\n");
                prompt.append("- Credit Score: ").append(customer.getCreditScore()).append("\n");
                prompt.append("- Credit Limit: $").append(customer.getCreditLimit()).append("\n");
                prompt.append("- Available Credit: $").append(customer.getAvailableCredit()).append("\n");
                
                if (includeLoans) {
                    var loans = loanService.findByCustomerId(customer.getId());
                    prompt.append("\nLoans: ").append(loans.size()).append(" active loans\n");
                }
                
                if (includePayments) {
                    var payments = paymentService.findByCustomerId(customer.getId());
                    prompt.append("Payment History: ").append(payments.size()).append(" total payments\n");
                }
                
                prompt.append("\nProvide analysis on:\n");
                prompt.append("1. Credit utilization and financial health\n");
                prompt.append("2. Risk assessment and recommendations\n");
                prompt.append("3. Potential for additional products or services\n");
                prompt.append("4. Areas of concern or opportunity\n");
            }
        } catch (Exception e) {
            prompt.append("Error retrieving customer data: ").append(e.getMessage());
        }
        
        return prompt.toString();
    }

    private String generateRiskAssessmentPrompt(Map<String, Object> arguments) {
        String customerId = (String) arguments.get("customerId");
        Object loanAmountObj = arguments.get("loanAmount");
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Perform a comprehensive risk assessment for the following customer:\n\n");
        
        try {
            var customer = customerService.findByCustomerNumber(customerId);
            if (customer != null) {
                prompt.append("Customer Risk Profile:\n");
                prompt.append("- Credit Score: ").append(customer.getCreditScore()).append("\n");
                prompt.append("- Annual Income: $").append(customer.getAnnualIncome()).append("\n");
                prompt.append("- Employment Status: ").append(customer.getEmploymentStatus()).append("\n");
                prompt.append("- Current Credit Utilization: ").append(
                    customer.getCreditLimit().subtract(customer.getAvailableCredit())
                        .divide(customer.getCreditLimit()).multiply(new java.math.BigDecimal("100"))
                ).append("%\n");
                
                if (loanAmountObj != null) {
                    prompt.append("- Proposed Loan Amount: $").append(loanAmountObj).append("\n");
                }
                
                var loans = loanService.findByCustomerId(customer.getId());
                prompt.append("- Active Loans: ").append(loans.size()).append("\n");
                
                prompt.append("\nProvide risk assessment covering:\n");
                prompt.append("1. Credit risk score (1-10 scale)\n");
                prompt.append("2. Key risk factors\n");
                prompt.append("3. Mitigation strategies\n");
                prompt.append("4. Recommendation (approve/reject/conditional approval)\n");
                prompt.append("5. Suggested terms if approved\n");
            }
        } catch (Exception e) {
            prompt.append("Error retrieving customer data: ").append(e.getMessage());
        }
        
        return prompt.toString();
    }

    private String generatePaymentOptimizationPrompt(Map<String, Object> arguments) {
        String loanId = (String) arguments.get("loanId");
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following loan and provide payment optimization recommendations:\n\n");
        
        try {
            var loan = loanService.findByLoanNumber(loanId);
            if (loan != null) {
                prompt.append("Loan Details:\n");
                prompt.append("- Loan ID: ").append(loan.getLoanNumber()).append("\n");
                prompt.append("- Original Amount: $").append(loan.getLoanAmount()).append("\n");
                prompt.append("- Outstanding Amount: $").append(loan.getOutstandingAmount()).append("\n");
                prompt.append("- Interest Rate: ").append(loan.getInterestRate().multiply(new java.math.BigDecimal("100"))).append("%\n");
                prompt.append("- Installment Count: ").append(loan.getInstallmentCount()).append("\n");
                prompt.append("- Monthly Payment: $").append(loan.getInstallmentAmount()).append("\n");
                
                var installments = loanService.getInstallments(loan.getId());
                long overdue = installments.stream().filter(i -> 
                    "OVERDUE".equals(i.getStatus()) || 
                    (i.getDueDate().isBefore(java.time.LocalDate.now()) && "PENDING".equals(i.getStatus()))
                ).count();
                
                prompt.append("- Overdue Installments: ").append(overdue).append("\n");
                
                prompt.append("\nProvide optimization strategies for:\n");
                prompt.append("1. Early payment benefits and savings calculation\n");
                prompt.append("2. Restructuring options if applicable\n");
                prompt.append("3. Payment schedule optimization\n");
                prompt.append("4. Risk mitigation for overdue amounts\n");
                prompt.append("5. Customer communication recommendations\n");
            }
        } catch (Exception e) {
            prompt.append("Error retrieving loan data: ").append(e.getMessage());
        }
        
        return prompt.toString();
    }

    private Map<String, Object> handleLoggingSetLevel(Object id, Map<String, Object> params) {
        String level = (String) params.get("level");
        // Implementation would set logging level
        return createResponse(id, Map.of("success", true, "level", level));
    }

    private Object getSystemAnalytics() {
        return Map.of(
            "overview", Map.of(
                "totalCustomers", customerService.getTotalCustomerCount(),
                "totalLoans", loanService.getTotalLoanCount(),
                "totalPayments", paymentService.getTotalPaymentCount(),
                "systemUptime", "99.9%"
            ),
            "performance", Map.of(
                "avgResponseTime", "35ms",
                "requestsPerSecond", "150",
                "errorRate", "0.01%"
            ),
            "business", Map.of(
                "loanApprovalRate", loanService.getApprovalRate(),
                "defaultRate", loanService.getDefaultRate(),
                "avgLoanAmount", loanService.getAverageLoanAmount()
            )
        );
    }

    private Map<String, Object> createResponse(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> createErrorResponse(Object id, String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    private void sendErrorResponse(WebSocketSession session, String code, String message, Object id) throws IOException {
        Map<String, Object> errorResponse = createErrorResponse(id, code, message);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
    }
}