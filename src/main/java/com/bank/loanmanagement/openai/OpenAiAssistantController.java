package com.bank.loanmanagement.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RestController
@RequestMapping("/api/assistant")
public class OpenAiAssistantController {
    
    @Autowired
    private OpenAiAssistantService assistantService;
    
    // GraphQL Query Mappings
    @QueryMapping
    public CompletableFuture<Map<String, Object>> assistantRiskAnalysis(
            @Argument String customerId) {
        return assistantService.analyzeCustomerRisk(customerId);
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> assistantLoanEligibility(
            @Argument String customerId,
            @Argument Double loanAmount,
            @Argument Integer installmentCount) {
        return assistantService.evaluateLoanEligibility(customerId, loanAmount, installmentCount);
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> assistantPaymentOptimization(
            @Argument String loanId,
            @Argument Double paymentAmount) {
        return assistantService.optimizePaymentStrategy(loanId, paymentAmount);
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> assistantBankingInsights(
            @Argument String period) {
        return assistantService.getBankingInsights(period);
    }
    
    @QueryMapping
    public Map<String, Object> assistantStatus() {
        return assistantService.getAssistantStatus();
    }
    
    // GraphQL Mutation Mappings
    @MutationMapping
    public CompletableFuture<String> processBankingQuery(
            @Argument String query,
            @Argument String customerId) {
        return assistantService.processBankingQuery(query, customerId);
    }
    
    // REST API Endpoints
    @PostMapping("/query")
    public ResponseEntity<CompletableFuture<String>> handleBankingQuery(
            @RequestBody Map<String, String> request) {
        
        String query = request.get("query");
        String customerId = request.get("customerId");
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        CompletableFuture<String> response = assistantService.processBankingQuery(query, customerId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/risk-analysis")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> analyzeRisk(
            @RequestBody Map<String, String> request) {
        
        String customerId = request.get("customerId");
        
        if (customerId == null || customerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        CompletableFuture<Map<String, Object>> analysis = assistantService.analyzeCustomerRisk(customerId);
        return ResponseEntity.ok(analysis);
    }
    
    @PostMapping("/loan-eligibility")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> evaluateEligibility(
            @RequestBody Map<String, Object> request) {
        
        String customerId = (String) request.get("customerId");
        Double loanAmount = (Double) request.get("loanAmount");
        Integer installmentCount = (Integer) request.get("installmentCount");
        
        if (customerId == null || loanAmount == null || installmentCount == null) {
            return ResponseEntity.badRequest().build();
        }
        
        CompletableFuture<Map<String, Object>> eligibility = assistantService.evaluateLoanEligibility(
            customerId, loanAmount, installmentCount);
        return ResponseEntity.ok(eligibility);
    }
    
    @PostMapping("/payment-optimization")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> optimizePayment(
            @RequestBody Map<String, Object> request) {
        
        String loanId = (String) request.get("loanId");
        Double paymentAmount = (Double) request.get("paymentAmount");
        
        if (loanId == null || paymentAmount == null) {
            return ResponseEntity.badRequest().build();
        }
        
        CompletableFuture<Map<String, Object>> optimization = assistantService.optimizePaymentStrategy(
            loanId, paymentAmount);
        return ResponseEntity.ok(optimization);
    }
    
    @GetMapping("/insights")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> getBankingInsights(
            @RequestParam(defaultValue = "LAST_30_DAYS") String period) {
        
        CompletableFuture<Map<String, Object>> insights = assistantService.getBankingInsights(period);
        return ResponseEntity.ok(insights);
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAssistantStatus() {
        Map<String, Object> status = assistantService.getAssistantStatus();
        return ResponseEntity.ok(status);
    }
}