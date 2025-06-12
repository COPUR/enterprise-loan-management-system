package com.bank.loanmanagement.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.PaymentService;
import com.bank.loanmanagement.domain.Customer;
import com.bank.loanmanagement.domain.Loan;
import com.bank.loanmanagement.domain.Payment;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Model Context Protocol (MCP) Server for Banking System
 * Provides structured banking data and operations for LLM integration
 * Enables chatbots to access real banking information with proper context
 * 
 * MCP Features:
 * - Structured banking data retrieval
 * - Real-time customer information
 * - Loan status and calculations
 * - Payment history and processing
 * - Financial analytics for AI models
 */
@RestController
@RequestMapping("/mcp/v1")
@Service
public class MCPBankingServer {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private PaymentService paymentService;

    /**
     * MCP Resource: Banking System Capabilities
     * Provides LLMs with available banking operations and data sources
     */
    @GetMapping("/resources")
    public MCPResourcesResponse getResources() {
        List<MCPResource> resources = List.of(
            MCPResource.builder()
                    .uri("banking://customers")
                    .name("Customer Management")
                    .description("Access customer profiles, credit scores, and account information")
                    .mimeType("application/json")
                    .capabilities(List.of("read", "query", "analytics"))
                    .build(),
            
            MCPResource.builder()
                    .uri("banking://loans")
                    .name("Loan Portfolio")
                    .description("Loan applications, approvals, calculations, and status tracking")
                    .mimeType("application/json")
                    .capabilities(List.of("read", "create", "update", "calculate"))
                    .build(),
            
            MCPResource.builder()
                    .uri("banking://payments")
                    .name("Payment Processing")
                    .description("Payment history, processing, and transaction analytics")
                    .mimeType("application/json")
                    .capabilities(List.of("read", "create", "analytics"))
                    .build(),
            
            MCPResource.builder()
                    .uri("banking://analytics")
                    .name("Financial Analytics")
                    .description("Risk assessment, portfolio analysis, and performance metrics")
                    .mimeType("application/json")
                    .capabilities(List.of("read", "analyze", "predict"))
                    .build()
        );
        
        return MCPResourcesResponse.builder()
                .resources(resources)
                .build();
    }

    /**
     * MCP Tools: Available Banking Operations
     * Defines tools that LLMs can use to interact with banking system
     */
    @GetMapping("/tools")
    public MCPToolsResponse getTools() {
        List<MCPTool> tools = List.of(
            MCPTool.builder()
                    .name("get_customer_profile")
                    .description("Retrieve complete customer profile including credit score and loan history")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "customerId", Map.of("type", "string", "description", "Customer ID or email"),
                            "includeHistory", Map.of("type", "boolean", "description", "Include loan and payment history")
                        ),
                        "required", List.of("customerId")
                    ))
                    .build(),
            
            MCPTool.builder()
                    .name("calculate_loan_eligibility")
                    .description("Calculate loan eligibility and personalized rates based on customer profile")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "customerId", Map.of("type", "string", "description", "Customer ID"),
                            "requestedAmount", Map.of("type", "number", "description", "Requested loan amount"),
                            "termMonths", Map.of("type", "number", "description", "Loan term in months")
                        ),
                        "required", List.of("customerId", "requestedAmount", "termMonths")
                    ))
                    .build(),
            
            MCPTool.builder()
                    .name("process_payment")
                    .description("Process loan payment and update account balance")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "loanId", Map.of("type", "string", "description", "Loan ID"),
                            "amount", Map.of("type", "number", "description", "Payment amount"),
                            "paymentMethod", Map.of("type", "string", "description", "Payment method (BANK_TRANSFER, ACH, etc)")
                        ),
                        "required", List.of("loanId", "amount")
                    ))
                    .build(),
            
            MCPTool.builder()
                    .name("get_portfolio_analytics")
                    .description("Generate portfolio analytics and risk assessment reports")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "timeframe", Map.of("type", "string", "description", "Analysis timeframe (30d, 90d, 1y)"),
                            "includeRisk", Map.of("type", "boolean", "description", "Include risk analysis")
                        )
                    ))
                    .build()
        );
        
        return MCPToolsResponse.builder()
                .tools(tools)
                .build();
    }

    /**
     * MCP Tool Execution: Get Customer Profile
     * Provides comprehensive customer information for LLM context
     */
    @PostMapping("/tools/get_customer_profile")
    public MCPToolResponse getCustomerProfile(@RequestBody MCPToolRequest request) {
        try {
            Map<String, Object> args = request.getArguments();
            String customerId = (String) args.get("customerId");
            boolean includeHistory = Boolean.TRUE.equals(args.get("includeHistory"));
            
            Customer customer;
            if (customerId.contains("@")) {
                customer = customerService.findByEmail(customerId);
            } else {
                customer = customerService.findById(Long.valueOf(customerId));
            }
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("customerId", customer.getId());
            profile.put("name", customer.getName());
            profile.put("email", customer.getEmail());
            profile.put("phone", customer.getPhone());
            profile.put("creditScore", customer.getCreditScore());
            profile.put("accountStatus", "ACTIVE");
            profile.put("memberSince", customer.getCreatedAt());
            
            if (includeHistory) {
                List<Loan> loans = loanService.findByCustomerId(customer.getId());
                List<Payment> payments = paymentService.findByCustomerId(customer.getId());
                
                profile.put("loanHistory", loans.stream()
                    .map(this::mapLoanToMCP)
                    .collect(Collectors.toList()));
                    
                profile.put("paymentHistory", payments.stream()
                    .map(this::mapPaymentToMCP)
                    .collect(Collectors.toList()));
                    
                profile.put("totalLoansAmount", loans.stream()
                    .mapToDouble(Loan::getAmount)
                    .sum());
                    
                profile.put("totalPaymentsAmount", payments.stream()
                    .mapToDouble(Payment::getAmount)
                    .sum());
            }
            
            return MCPToolResponse.builder()
                    .isError(false)
                    .content(List.of(MCPContent.builder()
                            .type("application/json")
                            .data(profile)
                            .build()))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("Failed to retrieve customer profile: " + e.getMessage());
        }
    }

    /**
     * MCP Tool Execution: Calculate Loan Eligibility
     * Provides AI-driven loan eligibility assessment
     */
    @PostMapping("/tools/calculate_loan_eligibility")
    public MCPToolResponse calculateLoanEligibility(@RequestBody MCPToolRequest request) {
        try {
            Map<String, Object> args = request.getArguments();
            String customerId = (String) args.get("customerId");
            double requestedAmount = ((Number) args.get("requestedAmount")).doubleValue();
            int termMonths = ((Number) args.get("termMonths")).intValue();
            
            Customer customer = customerService.findById(Long.valueOf(customerId));
            
            // Calculate eligibility based on credit score and history
            boolean eligible = isEligibleForLoan(customer, requestedAmount);
            double personalizedRate = calculatePersonalizedRate(customer);
            double maxAmount = calculateMaxLoanAmount(customer);
            double monthlyPayment = calculateMonthlyPayment(requestedAmount, personalizedRate, termMonths);
            
            Map<String, Object> eligibility = Map.of(
                "eligible", eligible,
                "customerId", customerId,
                "requestedAmount", requestedAmount,
                "approvedAmount", eligible ? Math.min(requestedAmount, maxAmount) : 0,
                "interestRate", personalizedRate,
                "termMonths", termMonths,
                "monthlyPayment", monthlyPayment,
                "totalInterest", (monthlyPayment * termMonths) - requestedAmount,
                "creditScore", customer.getCreditScore(),
                "riskLevel", calculateRiskLevel(customer),
                "reasoning", generateEligibilityReasoning(customer, eligible, requestedAmount)
            );
            
            return MCPToolResponse.builder()
                    .isError(false)
                    .content(List.of(MCPContent.builder()
                            .type("application/json")
                            .data(eligibility)
                            .build()))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("Failed to calculate loan eligibility: " + e.getMessage());
        }
    }

    /**
     * MCP Tool Execution: Process Payment
     * Handles payment processing with real-time updates
     */
    @PostMapping("/tools/process_payment")
    public MCPToolResponse processPayment(@RequestBody MCPToolRequest request) {
        try {
            Map<String, Object> args = request.getArguments();
            String loanId = (String) args.get("loanId");
            double amount = ((Number) args.get("amount")).doubleValue();
            String paymentMethod = (String) args.getOrDefault("paymentMethod", "BANK_TRANSFER");
            
            // Process payment through service
            Payment payment = Payment.builder()
                    .loanId(Long.valueOf(loanId))
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .referenceNumber("MCP-" + System.currentTimeMillis())
                    .status("COMPLETED")
                    .build();
            
            Payment processedPayment = paymentService.processPayment(payment);
            Loan updatedLoan = loanService.findById(Long.valueOf(loanId));
            
            Map<String, Object> result = Map.of(
                "paymentId", processedPayment.getId(),
                "amount", amount,
                "status", "SUCCESS",
                "referenceNumber", processedPayment.getReferenceNumber(),
                "paymentDate", processedPayment.getCreatedAt(),
                "remainingBalance", updatedLoan.getAmount() - (updatedLoan.getPaidAmount() != null ? updatedLoan.getPaidAmount() : 0),
                "nextPaymentDue", calculateNextPaymentDate(updatedLoan),
                "loanStatus", updatedLoan.getStatus()
            );
            
            return MCPToolResponse.builder()
                    .isError(false)
                    .content(List.of(MCPContent.builder()
                            .type("application/json")
                            .data(result)
                            .build()))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * MCP Tool Execution: Get Portfolio Analytics
     * Provides comprehensive portfolio analysis for AI insights
     */
    @PostMapping("/tools/get_portfolio_analytics")
    public MCPToolResponse getPortfolioAnalytics(@RequestBody MCPToolRequest request) {
        try {
            Map<String, Object> args = request.getArguments();
            String timeframe = (String) args.getOrDefault("timeframe", "30d");
            boolean includeRisk = Boolean.TRUE.equals(args.get("includeRisk"));
            
            List<Customer> customers = customerService.findAll();
            List<Loan> loans = loanService.findAll();
            List<Payment> payments = paymentService.findAll();
            
            Map<String, Object> analytics = new HashMap<>();
            
            // Basic portfolio metrics
            analytics.put("totalCustomers", customers.size());
            analytics.put("totalLoans", loans.size());
            analytics.put("totalLoanAmount", loans.stream().mapToDouble(Loan::getAmount).sum());
            analytics.put("totalPayments", payments.size());
            analytics.put("totalPaymentAmount", payments.stream().mapToDouble(Payment::getAmount).sum());
            
            // Performance metrics
            analytics.put("averageCreditScore", customers.stream()
                .mapToInt(Customer::getCreditScore)
                .average()
                .orElse(0));
                
            analytics.put("approvalRate", loans.stream()
                .mapToDouble(loan -> "APPROVED".equals(loan.getStatus()) ? 1.0 : 0.0)
                .average()
                .orElse(0) * 100);
            
            // Risk analytics (if requested)
            if (includeRisk) {
                analytics.put("riskDistribution", calculateRiskDistribution(customers));
                analytics.put("defaultRisk", calculatePortfolioDefaultRisk(loans, payments));
                analytics.put("concentrationRisk", calculateConcentrationRisk(loans));
            }
            
            analytics.put("generatedAt", LocalDateTime.now());
            analytics.put("timeframe", timeframe);
            
            return MCPToolResponse.builder()
                    .isError(false)
                    .content(List.of(MCPContent.builder()
                            .type("application/json")
                            .data(analytics)
                            .build()))
                    .build();
                    
        } catch (Exception e) {
            return createErrorResponse("Failed to generate portfolio analytics: " + e.getMessage());
        }
    }

    // Helper methods for banking calculations and analytics

    private Map<String, Object> mapLoanToMCP(Loan loan) {
        return Map.of(
            "loanId", loan.getId(),
            "amount", loan.getAmount(),
            "interestRate", loan.getInterestRate(),
            "termMonths", loan.getTermMonths(),
            "status", loan.getStatus(),
            "purpose", loan.getPurpose() != null ? loan.getPurpose() : "",
            "createdAt", loan.getCreatedAt()
        );
    }

    private Map<String, Object> mapPaymentToMCP(Payment payment) {
        return Map.of(
            "paymentId", payment.getId(),
            "loanId", payment.getLoanId(),
            "amount", payment.getAmount(),
            "paymentMethod", payment.getPaymentMethod(),
            "status", payment.getStatus(),
            "paymentDate", payment.getCreatedAt()
        );
    }

    private boolean isEligibleForLoan(Customer customer, double amount) {
        return customer.getCreditScore() >= 600 && amount <= calculateMaxLoanAmount(customer);
    }

    private double calculatePersonalizedRate(Customer customer) {
        if (customer.getCreditScore() >= 750) return 0.08;
        if (customer.getCreditScore() >= 700) return 0.12;
        if (customer.getCreditScore() >= 650) return 0.16;
        return 0.20;
    }

    private double calculateMaxLoanAmount(Customer customer) {
        return customer.getCreditScore() * 1000;
    }

    private double calculateMonthlyPayment(double principal, double rate, int months) {
        double monthlyRate = rate / 12;
        return principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / 
               (Math.pow(1 + monthlyRate, months) - 1);
    }

    private String calculateRiskLevel(Customer customer) {
        if (customer.getCreditScore() >= 750) return "LOW";
        if (customer.getCreditScore() >= 650) return "MEDIUM";
        return "HIGH";
    }

    private String generateEligibilityReasoning(Customer customer, boolean eligible, double amount) {
        if (eligible) {
            return String.format("Approved based on credit score %d and loan amount $%.2f within limits", 
                customer.getCreditScore(), amount);
        } else {
            return String.format("Declined: Credit score %d below minimum or amount $%.2f exceeds limit", 
                customer.getCreditScore(), amount);
        }
    }

    private LocalDateTime calculateNextPaymentDate(Loan loan) {
        return LocalDateTime.now().plusMonths(1);
    }

    private Map<String, Object> calculateRiskDistribution(List<Customer> customers) {
        long low = customers.stream().filter(c -> c.getCreditScore() >= 750).count();
        long medium = customers.stream().filter(c -> c.getCreditScore() >= 650 && c.getCreditScore() < 750).count();
        long high = customers.stream().filter(c -> c.getCreditScore() < 650).count();
        
        return Map.of("LOW", low, "MEDIUM", medium, "HIGH", high);
    }

    private double calculatePortfolioDefaultRisk(List<Loan> loans, List<Payment> payments) {
        // Simplified default risk calculation
        long overdue = loans.stream()
            .filter(loan -> "APPROVED".equals(loan.getStatus()))
            .filter(loan -> isOverdue(loan, payments))
            .count();
        
        return loans.isEmpty() ? 0 : (double) overdue / loans.size() * 100;
    }

    private boolean isOverdue(Loan loan, List<Payment> payments) {
        // Simplified overdue check
        return payments.stream()
            .filter(p -> p.getLoanId().equals(loan.getId()))
            .mapToDouble(Payment::getAmount)
            .sum() < (loan.getAmount() * 0.1); // Less than 10% paid
    }

    private Map<String, Object> calculateConcentrationRisk(List<Loan> loans) {
        double maxSingleLoan = loans.stream().mapToDouble(Loan::getAmount).max().orElse(0);
        double totalPortfolio = loans.stream().mapToDouble(Loan::getAmount).sum();
        
        return Map.of(
            "maxSingleLoanPercentage", totalPortfolio > 0 ? (maxSingleLoan / totalPortfolio * 100) : 0,
            "concentrationLevel", maxSingleLoan / totalPortfolio > 0.2 ? "HIGH" : "ACCEPTABLE"
        );
    }

    private MCPToolResponse createErrorResponse(String message) {
        return MCPToolResponse.builder()
                .isError(true)
                .content(List.of(MCPContent.builder()
                        .type("text/plain")
                        .data(Map.of("error", message))
                        .build()))
                .build();
    }
}