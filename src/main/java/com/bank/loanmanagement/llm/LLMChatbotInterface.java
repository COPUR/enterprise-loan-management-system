package com.bank.loanmanagement.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.bank.loanmanagement.mcp.MCPBankingServer;
import com.bank.loanmanagement.mcp.MCPToolRequest;
import com.bank.loanmanagement.mcp.MCPToolResponse;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * LLM Chatbot Interface for Banking System
 * Provides conversational AI capabilities with access to real banking data
 * Integrates with MCP server for structured banking operations
 * 
 * Supported LLM Providers:
 * - OpenAI GPT-4/ChatGPT
 * - Claude (Anthropic)
 * - Google Bard/Gemini
 * - Custom enterprise LLMs
 */
@RestController
@RequestMapping("/llm/v1")
public class LLMChatbotInterface {

    @Autowired
    private MCPBankingServer mcpServer;

    /**
     * Chat Endpoint for Banking Assistant
     * Processes natural language queries and provides banking assistance
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> processChat(@RequestBody ChatRequest request) {
        try {
            String userMessage = request.getMessage();
            String customerId = request.getCustomerId();
            
            // Analyze user intent
            ChatIntent intent = analyzeIntent(userMessage);
            
            // Process based on intent
            ChatResponse response = switch (intent.getType()) {
                case ACCOUNT_INQUIRY -> handleAccountInquiry(customerId, intent);
                case LOAN_APPLICATION -> handleLoanApplication(customerId, intent);
                case PAYMENT_PROCESSING -> handlePaymentProcessing(customerId, intent);
                case LOAN_CALCULATION -> handleLoanCalculation(intent);
                case GENERAL_INQUIRY -> handleGeneralInquiry(userMessage);
                default -> createDefaultResponse();
            };
            
            response.setConversationId(request.getConversationId());
            response.setTimestamp(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.ok(createErrorResponse("I apologize, but I encountered an issue processing your request. Please try again or contact customer support."));
        }
    }

    /**
     * Banking Assistant Context
     * Provides LLMs with current banking context and capabilities
     */
    @GetMapping("/context")
    public ResponseEntity<BankingContext> getBankingContext(@RequestParam(required = false) String customerId) {
        try {
            BankingContext context = BankingContext.builder()
                    .systemName("Enterprise Loan Management System")
                    .capabilities(List.of(
                        "Account balance inquiries",
                        "Loan application processing",
                        "Payment processing and history",
                        "EMI calculations",
                        "Credit score information",
                        "Loan eligibility assessment"
                    ))
                    .supportedOperations(List.of(
                        "get_customer_profile",
                        "calculate_loan_eligibility", 
                        "process_payment",
                        "get_portfolio_analytics"
                    ))
                    .complianceLevel("FAPI 1.0 Advanced")
                    .securityFeatures(List.of(
                        "OAuth2 with PKCE",
                        "Request/Response signing",
                        "MTLS authentication",
                        "Audit logging"
                    ))
                    .build();
            
            // Add customer-specific context if provided
            if (customerId != null) {
                context.setCustomerContext(getCustomerContext(customerId));
            }
            
            return ResponseEntity.ok(context);
            
        } catch (Exception e) {
            return ResponseEntity.ok(BankingContext.builder()
                    .systemName("Enterprise Loan Management System")
                    .capabilities(List.of("Basic banking assistance"))
                    .build());
        }
    }

    /**
     * Handle Account Inquiry
     */
    private ChatResponse handleAccountInquiry(String customerId, ChatIntent intent) {
        try {
            // Get customer profile via MCP
            MCPToolRequest mcpRequest = MCPToolRequest.builder()
                    .name("get_customer_profile")
                    .arguments(Map.of(
                        "customerId", customerId,
                        "includeHistory", true
                    ))
                    .build();
            
            MCPToolResponse mcpResponse = mcpServer.getCustomerProfile(mcpRequest);
            
            if (mcpResponse.isError()) {
                return createErrorResponse("I'm unable to retrieve your account information at the moment. Please try again later.");
            }
            
            Map<String, Object> profileData = mcpResponse.getContent().get(0).getData();
            
            String responseMessage = formatAccountInquiryResponse(profileData, intent);
            
            return ChatResponse.builder()
                    .message(responseMessage)
                    .responseType("account_info")
                    .data(profileData)
                    .suggestions(List.of(
                        "Check my loan status",
                        "Make a payment", 
                        "Apply for a new loan",
                        "Calculate EMI for different amounts"
                    ))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("I encountered an issue retrieving your account information. Please contact customer support if this persists.");
        }
    }

    /**
     * Handle Loan Application
     */
    private ChatResponse handleLoanApplication(String customerId, ChatIntent intent) {
        try {
            double requestedAmount = intent.getExtractedAmount();
            int termMonths = intent.getExtractedTerm();
            
            // Calculate eligibility via MCP
            MCPToolRequest mcpRequest = MCPToolRequest.builder()
                    .name("calculate_loan_eligibility")
                    .arguments(Map.of(
                        "customerId", customerId,
                        "requestedAmount", requestedAmount,
                        "termMonths", termMonths
                    ))
                    .build();
            
            MCPToolResponse mcpResponse = mcpServer.calculateLoanEligibility(mcpRequest);
            
            if (mcpResponse.isError()) {
                return createErrorResponse("I'm unable to process your loan application request at the moment. Please try again later.");
            }
            
            Map<String, Object> eligibilityData = mcpResponse.getContent().get(0).getData();
            
            String responseMessage = formatLoanApplicationResponse(eligibilityData);
            
            return ChatResponse.builder()
                    .message(responseMessage)
                    .responseType("loan_eligibility")
                    .data(eligibilityData)
                    .suggestions(generateLoanSuggestions(eligibilityData))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("I encountered an issue processing your loan application. Please provide the loan amount and term, or contact customer support.");
        }
    }

    /**
     * Handle Payment Processing
     */
    private ChatResponse handlePaymentProcessing(String customerId, ChatIntent intent) {
        try {
            String loanId = intent.getExtractedLoanId();
            double amount = intent.getExtractedAmount();
            
            if (loanId == null || amount == 0) {
                return createErrorResponse("To process a payment, please provide your loan ID and payment amount. For example: 'I want to pay $500 for loan 123'");
            }
            
            // Process payment via MCP
            MCPToolRequest mcpRequest = MCPToolRequest.builder()
                    .name("process_payment")
                    .arguments(Map.of(
                        "loanId", loanId,
                        "amount", amount,
                        "paymentMethod", "BANK_TRANSFER"
                    ))
                    .build();
            
            MCPToolResponse mcpResponse = mcpServer.processPayment(mcpRequest);
            
            if (mcpResponse.isError()) {
                return createErrorResponse("I'm unable to process your payment at the moment. Please try again later or use our online payment portal.");
            }
            
            Map<String, Object> paymentData = mcpResponse.getContent().get(0).getData();
            
            String responseMessage = formatPaymentResponse(paymentData);
            
            return ChatResponse.builder()
                    .message(responseMessage)
                    .responseType("payment_confirmation")
                    .data(paymentData)
                    .suggestions(List.of(
                        "Check my remaining balance",
                        "View payment history",
                        "Set up automatic payments",
                        "Download payment receipt"
                    ))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("I encountered an issue processing your payment. Please verify your loan ID and amount, or contact customer support.");
        }
    }

    /**
     * Handle Loan Calculation
     */
    private ChatResponse handleLoanCalculation(ChatIntent intent) {
        try {
            double principal = intent.getExtractedAmount();
            int termMonths = intent.getExtractedTerm();
            double interestRate = intent.getExtractedRate() != 0 ? intent.getExtractedRate() : 0.15; // Default rate
            
            // Calculate EMI
            double monthlyRate = interestRate / 12;
            double emi = principal * (monthlyRate * Math.pow(1 + monthlyRate, termMonths)) / 
                        (Math.pow(1 + monthlyRate, termMonths) - 1);
            double totalAmount = emi * termMonths;
            double totalInterest = totalAmount - principal;
            
            Map<String, Object> calculationData = Map.of(
                "principal", principal,
                "interestRate", interestRate * 100,
                "termMonths", termMonths,
                "monthlyEMI", Math.round(emi * 100.0) / 100.0,
                "totalAmount", Math.round(totalAmount * 100.0) / 100.0,
                "totalInterest", Math.round(totalInterest * 100.0) / 100.0
            );
            
            String responseMessage = String.format(
                "Here's your loan calculation:\n\n" +
                "💰 Loan Amount: $%.2f\n" +
                "📊 Interest Rate: %.1f%% per annum\n" +
                "📅 Loan Term: %d months\n\n" +
                "📋 Results:\n" +
                "• Monthly EMI: $%.2f\n" +
                "• Total Amount Payable: $%.2f\n" +
                "• Total Interest: $%.2f\n\n" +
                "Would you like to apply for this loan or explore different terms?",
                principal, interestRate * 100, termMonths, emi, totalAmount, totalInterest
            );
            
            return ChatResponse.builder()
                    .message(responseMessage)
                    .responseType("loan_calculation")
                    .data(calculationData)
                    .suggestions(List.of(
                        "Apply for this loan",
                        "Try different loan terms",
                        "Check my eligibility",
                        "Compare interest rates"
                    ))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("I encountered an issue with the loan calculation. Please provide the loan amount and term. For example: 'Calculate EMI for $50,000 loan for 24 months'");
        }
    }

    /**
     * Handle General Inquiry
     */
    private ChatResponse handleGeneralInquiry(String message) {
        // Simple keyword-based responses for common banking questions
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("interest rate") || lowerMessage.contains("rates")) {
            return createGeneralResponse(
                "Our interest rates range from 8% to 20% per annum, depending on your credit score and loan amount. " +
                "Customers with credit scores above 750 get our best rates starting at 8%. " +
                "Would you like me to check your personalized rate?",
                List.of("Check my interest rate", "Apply for a loan", "Improve my credit score")
            );
        }
        
        if (lowerMessage.contains("credit score") || lowerMessage.contains("credit")) {
            return createGeneralResponse(
                "Your credit score is a key factor in loan approval and interest rates. " +
                "We work with customers across all credit ranges. " +
                "Would you like me to check your current credit score and loan eligibility?",
                List.of("Check my credit score", "Improve credit score tips", "Apply for a loan")
            );
        }
        
        if (lowerMessage.contains("documents") || lowerMessage.contains("requirements")) {
            return createGeneralResponse(
                "For loan applications, you'll typically need:\n" +
                "• Valid government-issued ID\n" +
                "• Proof of income (pay stubs, tax returns)\n" +
                "• Bank statements (last 3 months)\n" +
                "• Employment verification\n\n" +
                "The exact requirements may vary based on loan type and amount.",
                List.of("Start loan application", "Upload documents", "Speak to loan officer")
            );
        }
        
        return createDefaultResponse();
    }

    /**
     * Analyze user intent from natural language
     */
    private ChatIntent analyzeIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Account inquiry patterns
        if (lowerMessage.contains("balance") || lowerMessage.contains("account") || 
            lowerMessage.contains("profile") || lowerMessage.contains("information")) {
            return ChatIntent.builder()
                    .type(ChatIntentType.ACCOUNT_INQUIRY)
                    .build();
        }
        
        // Payment processing patterns
        if (lowerMessage.contains("pay") || lowerMessage.contains("payment") || 
            lowerMessage.contains("make a payment")) {
            return ChatIntent.builder()
                    .type(ChatIntentType.PAYMENT_PROCESSING)
                    .extractedAmount(extractAmount(message))
                    .extractedLoanId(extractLoanId(message))
                    .build();
        }
        
        // Loan application patterns
        if (lowerMessage.contains("apply") || lowerMessage.contains("loan application") || 
            lowerMessage.contains("new loan")) {
            return ChatIntent.builder()
                    .type(ChatIntentType.LOAN_APPLICATION)
                    .extractedAmount(extractAmount(message))
                    .extractedTerm(extractTerm(message))
                    .build();
        }
        
        // Calculation patterns
        if (lowerMessage.contains("calculate") || lowerMessage.contains("emi") || 
            lowerMessage.contains("monthly payment")) {
            return ChatIntent.builder()
                    .type(ChatIntentType.LOAN_CALCULATION)
                    .extractedAmount(extractAmount(message))
                    .extractedTerm(extractTerm(message))
                    .extractedRate(extractRate(message))
                    .build();
        }
        
        return ChatIntent.builder()
                .type(ChatIntentType.GENERAL_INQUIRY)
                .build();
    }

    // Helper methods for data extraction and formatting

    private double extractAmount(String message) {
        // Extract dollar amounts like $50,000 or 50000
        String pattern = "\\$?([0-9,]+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(message);
        if (m.find()) {
            return Double.parseDouble(m.group(1).replace(",", ""));
        }
        return 0;
    }

    private int extractTerm(String message) {
        // Extract terms like "24 months" or "2 years"
        if (message.contains("month")) {
            String pattern = "(\\d+)\\s*month";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(message.toLowerCase());
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        if (message.contains("year")) {
            String pattern = "(\\d+)\\s*year";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(message.toLowerCase());
            if (m.find()) {
                return Integer.parseInt(m.group(1)) * 12;
            }
        }
        return 24; // Default to 24 months
    }

    private double extractRate(String message) {
        // Extract rates like "15%" or "0.15"
        String pattern = "(\\d+(?:\\.\\d+)?)%?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(message);
        if (m.find()) {
            double rate = Double.parseDouble(m.group(1));
            return rate > 1 ? rate / 100 : rate; // Convert percentage to decimal
        }
        return 0;
    }

    private String extractLoanId(String message) {
        // Extract loan IDs like "loan 123" or "ID 456"
        String pattern = "(?:loan|id)\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(message);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String formatAccountInquiryResponse(Map<String, Object> profileData, ChatIntent intent) {
        return String.format(
            "Hello %s! Here's your account summary:\n\n" +
            "📊 Credit Score: %d\n" +
            "📧 Email: %s\n" +
            "📅 Member Since: %s\n" +
            "💰 Total Loan Amount: $%.2f\n" +
            "💳 Total Payments: $%.2f\n\n" +
            "Your account is in good standing. How can I assist you today?",
            profileData.get("name"),
            profileData.get("creditScore"),
            profileData.get("email"),
            profileData.get("memberSince"),
            profileData.get("totalLoansAmount"),
            profileData.get("totalPaymentsAmount")
        );
    }

    private String formatLoanApplicationResponse(Map<String, Object> eligibilityData) {
        boolean eligible = (Boolean) eligibilityData.get("eligible");
        
        if (eligible) {
            return String.format(
                "Great news! You're eligible for this loan.\n\n" +
                "💰 Requested Amount: $%.2f\n" +
                "✅ Approved Amount: $%.2f\n" +
                "📊 Interest Rate: %.1f%% per annum\n" +
                "📅 Term: %d months\n" +
                "💳 Monthly Payment: $%.2f\n\n" +
                "Reason: %s\n\n" +
                "Would you like to proceed with the application?",
                eligibilityData.get("requestedAmount"),
                eligibilityData.get("approvedAmount"),
                (Double) eligibilityData.get("interestRate") * 100,
                eligibilityData.get("termMonths"),
                eligibilityData.get("monthlyPayment"),
                eligibilityData.get("reasoning")
            );
        } else {
            return String.format(
                "I understand you're interested in a loan. Based on our initial assessment:\n\n" +
                "💰 Requested Amount: $%.2f\n" +
                "📊 Current Credit Score: %d\n" +
                "⚠️ Status: %s\n\n" +
                "Reason: %s\n\n" +
                "Don't worry! I can help you explore alternatives or discuss ways to improve your eligibility.",
                eligibilityData.get("requestedAmount"),
                eligibilityData.get("creditScore"),
                eligible ? "Approved" : "Under Review",
                eligibilityData.get("reasoning")
            );
        }
    }

    private String formatPaymentResponse(Map<String, Object> paymentData) {
        return String.format(
            "✅ Payment Processed Successfully!\n\n" +
            "💰 Payment Amount: $%.2f\n" +
            "🔢 Reference Number: %s\n" +
            "📅 Payment Date: %s\n" +
            "💳 Remaining Balance: $%.2f\n" +
            "📋 Loan Status: %s\n\n" +
            "Thank you for your payment! Your account has been updated.",
            paymentData.get("amount"),
            paymentData.get("referenceNumber"),
            paymentData.get("paymentDate"),
            paymentData.get("remainingBalance"),
            paymentData.get("loanStatus")
        );
    }

    private List<String> generateLoanSuggestions(Map<String, Object> eligibilityData) {
        boolean eligible = (Boolean) eligibilityData.get("eligible");
        
        if (eligible) {
            return List.of(
                "Proceed with application",
                "Explore different terms",
                "Set up automatic payments",
                "Download loan documents"
            );
        } else {
            return List.of(
                "Check alternative loan options", 
                "Get tips to improve credit score",
                "Speak with loan advisor",
                "Calculate smaller loan amount"
            );
        }
    }

    private Map<String, Object> getCustomerContext(String customerId) {
        try {
            MCPToolRequest mcpRequest = MCPToolRequest.builder()
                    .name("get_customer_profile")
                    .arguments(Map.of("customerId", customerId, "includeHistory", false))
                    .build();
            
            MCPToolResponse mcpResponse = mcpServer.getCustomerProfile(mcpRequest);
            
            if (!mcpResponse.isError()) {
                return mcpResponse.getContent().get(0).getData();
            }
        } catch (Exception e) {
            // Fall back to basic context
        }
        
        return Map.of("status", "authenticated");
    }

    private ChatResponse createGeneralResponse(String message, List<String> suggestions) {
        return ChatResponse.builder()
                .message(message)
                .responseType("general_info")
                .suggestions(suggestions)
                .build();
    }

    private ChatResponse createDefaultResponse() {
        return ChatResponse.builder()
                .message("I'm here to help with your banking needs! I can assist you with:\n\n" +
                        "• Checking account balances and information\n" +
                        "• Loan applications and eligibility\n" +
                        "• Processing payments\n" +
                        "• Calculating EMIs and loan terms\n" +
                        "• General banking questions\n\n" +
                        "What would you like help with today?")
                .responseType("welcome")
                .suggestions(List.of(
                    "Check my account balance",
                    "Apply for a loan",
                    "Make a payment", 
                    "Calculate loan EMI"
                ))
                .build();
    }

    private ChatResponse createErrorResponse(String message) {
        return ChatResponse.builder()
                .message(message)
                .responseType("error")
                .suggestions(List.of(
                    "Try again",
                    "Contact customer support",
                    "Return to main menu"
                ))
                .build();
    }
}