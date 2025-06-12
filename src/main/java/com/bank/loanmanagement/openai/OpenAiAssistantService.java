package com.bank.loanmanagement.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OpenAiAssistantService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiAssistantService.class);
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenAiAssistantService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public CompletableFuture<String> processBankingQuery(String userQuery, String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing banking query for customer: {}", customerId);
                
                // Execute Python banking assistant
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "python3", 
                    "src/main/python/banking_assistant.py"
                );
                
                // Set environment variables
                Map<String, String> env = processBuilder.environment();
                env.put("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY"));
                env.put("BANKING_QUERY", userQuery);
                env.put("CUSTOMER_ID", customerId != null ? customerId : "");
                
                Process process = processBuilder.start();
                
                // Send query to assistant
                try (var writer = process.outputWriter()) {
                    writer.write(userQuery + "\n");
                    writer.flush();
                }
                
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.inputStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                // Wait for process completion with timeout
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return "Request timeout - please try again with a simpler query";
                }
                
                String result = response.toString().trim();
                return result.isEmpty() ? "No response from banking assistant" : result;
                
            } catch (Exception e) {
                logger.error("Error processing banking query", e);
                return "Error processing your banking request: " + e.getMessage();
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> analyzeCustomerRisk(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = String.format(
                    "Generate a comprehensive risk assessment report for customer ID %s", 
                    customerId
                );
                
                String response = processBankingQuery(query, customerId).get(30, TimeUnit.SECONDS);
                
                Map<String, Object> result = new HashMap<>();
                result.put("customerId", customerId);
                result.put("analysis", response);
                result.put("timestamp", new Date());
                result.put("type", "RISK_ASSESSMENT");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error analyzing customer risk", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Failed to analyze customer risk: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> evaluateLoanEligibility(
            String customerId, 
            double loanAmount, 
            int installmentCount) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = String.format(
                    "Analyze loan eligibility for customer %s requesting $%.2f with %d installments",
                    customerId, loanAmount, installmentCount
                );
                
                String response = processBankingQuery(query, customerId).get(30, TimeUnit.SECONDS);
                
                Map<String, Object> result = new HashMap<>();
                result.put("customerId", customerId);
                result.put("requestedAmount", loanAmount);
                result.put("installmentCount", installmentCount);
                result.put("eligibilityAnalysis", response);
                result.put("timestamp", new Date());
                result.put("type", "LOAN_ELIGIBILITY");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error evaluating loan eligibility", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Failed to evaluate loan eligibility: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> optimizePaymentStrategy(
            String loanId, 
            double paymentAmount) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = String.format(
                    "Calculate optimal payment strategy for loan %s with payment amount $%.2f",
                    loanId, paymentAmount
                );
                
                String response = processBankingQuery(query, null).get(30, TimeUnit.SECONDS);
                
                Map<String, Object> result = new HashMap<>();
                result.put("loanId", loanId);
                result.put("paymentAmount", paymentAmount);
                result.put("optimizationStrategy", response);
                result.put("timestamp", new Date());
                result.put("type", "PAYMENT_OPTIMIZATION");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error optimizing payment strategy", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Failed to optimize payment strategy: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> getBankingInsights(String period) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = String.format(
                    "Provide comprehensive banking analytics and insights for the %s period",
                    period != null ? period : "last 30 days"
                );
                
                String response = processBankingQuery(query, null).get(30, TimeUnit.SECONDS);
                
                Map<String, Object> result = new HashMap<>();
                result.put("period", period);
                result.put("insights", response);
                result.put("timestamp", new Date());
                result.put("type", "BANKING_INSIGHTS");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error getting banking insights", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Failed to get banking insights: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    public boolean isConfigured() {
        return openAiApiKey != null && !openAiApiKey.trim().isEmpty();
    }
    
    public Map<String, Object> getAssistantStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isConfigured());
        status.put("service", "OpenAI Assistant Integration");
        status.put("capabilities", Arrays.asList(
            "Customer Risk Analysis",
            "Loan Eligibility Assessment", 
            "Payment Optimization",
            "Banking Analytics",
            "Natural Language Processing",
            "Regulatory Compliance Guidance"
        ));
        status.put("timestamp", new Date());
        return status;
    }
}