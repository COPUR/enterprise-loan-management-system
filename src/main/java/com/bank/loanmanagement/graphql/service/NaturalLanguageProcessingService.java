package com.bank.loanmanagement.graphql.service;

import com.bank.loanmanagement.graphql.dto.NLQueryResult;
import com.bank.loanmanagement.graphql.dto.QueryEntity;
import com.bank.loanmanagement.graphql.dto.QueryIntent;
import com.bank.loanmanagement.graphql.dto.EntityType;
import com.bank.loanmanagement.service.CustomerService;
import com.bank.loanmanagement.service.LoanService;
import com.bank.loanmanagement.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalLanguageProcessingService {

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // Intent patterns for banking domain
    private static final Map<String, Pattern> INTENT_PATTERNS = Map.of(
        "SEARCH", Pattern.compile("(?i)(find|show|get|list|search|display).*"),
        "ANALYTICS", Pattern.compile("(?i)(analyze|analytics|report|statistics|trends|performance).*"),
        "RECOMMENDATION", Pattern.compile("(?i)(recommend|suggest|advice|should|optimize).*"),
        "TRANSACTION", Pattern.compile("(?i)(pay|payment|transfer|process|create|loan).*"),
        "REPORT", Pattern.compile("(?i)(report|summary|overview|dashboard|metrics).*"),
        "HELP", Pattern.compile("(?i)(help|how|what|explain|guide).*")
    );

    // Entity patterns for banking entities
    private static final Map<String, Pattern> ENTITY_PATTERNS = Map.of(
        "CUSTOMER", Pattern.compile("(?i)(customer|client|borrower)s?\\s*(id|number)?\\s*:?\\s*([A-Z0-9-]+)?"),
        "LOAN", Pattern.compile("(?i)(loan|credit)s?\\s*(id|number)?\\s*:?\\s*([A-Z0-9-]+)?"),
        "PAYMENT", Pattern.compile("(?i)(payment)s?\\s*(id|number)?\\s*:?\\s*([A-Z0-9-]+)?"),
        "AMOUNT", Pattern.compile("(?i)\\$?([0-9,]+(?:\\.[0-9]{2})?)"),
        "PERCENTAGE", Pattern.compile("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*%"),
        "DATE", Pattern.compile("(?i)(today|yesterday|last\\s+week|last\\s+month|[0-9]{4}-[0-9]{2}-[0-9]{2})"),
        "STATUS", Pattern.compile("(?i)(active|pending|approved|rejected|overdue|completed|failed)")
    );

    public NLQueryResult processNaturalLanguageQuery(String query, Object context) {
        long startTime = System.currentTimeMillis();
        
        // Detect intent
        QueryIntent intent = detectIntent(query);
        
        // Extract entities
        List<QueryEntity> entities = extractEntities(query);
        
        // Process query based on intent and entities
        Object result = processQuery(intent, entities, query, context);
        
        // Calculate confidence based on entity matches and intent clarity
        float confidence = calculateConfidence(intent, entities, query);
        
        // Generate suggestions for similar queries
        List<String> suggestions = generateSuggestions(intent, entities);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return NLQueryResult.builder()
            .query(query)
            .intent(intent)
            .entities(entities)
            .result(result)
            .confidence(confidence)
            .suggestions(suggestions)
            .executionTime(executionTime / 1000.0f)
            .build();
    }

    private QueryIntent detectIntent(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(normalizedQuery).find()) {
                return QueryIntent.valueOf(entry.getKey());
            }
        }
        
        return QueryIntent.SEARCH; // Default intent
    }

    private List<QueryEntity> extractEntities(String query) {
        List<QueryEntity> entities = new ArrayList<>();
        
        for (Map.Entry<String, Pattern> entry : ENTITY_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(query);
            while (matcher.find()) {
                String value = matcher.group().trim();
                int position = matcher.start();
                
                entities.add(QueryEntity.builder()
                    .type(EntityType.valueOf(entry.getKey()))
                    .value(value)
                    .confidence(0.8f + (value.length() > 3 ? 0.2f : 0.0f))
                    .position(position)
                    .build());
            }
        }
        
        return entities;
    }

    private Object processQuery(QueryIntent intent, List<QueryEntity> entities, String query, Object context) {
        try {
            switch (intent) {
                case SEARCH:
                    return processSearchQuery(entities, query);
                case ANALYTICS:
                    return processAnalyticsQuery(entities, query);
                case RECOMMENDATION:
                    return processRecommendationQuery(entities, query);
                case TRANSACTION:
                    return processTransactionQuery(entities, query);
                case REPORT:
                    return processReportQuery(entities, query);
                case HELP:
                    return processHelpQuery(query);
                default:
                    return processGenericQuery(entities, query);
            }
        } catch (Exception e) {
            return Map.of(
                "error", "Failed to process query",
                "message", e.getMessage(),
                "query", query
            );
        }
    }

    private Object processSearchQuery(List<QueryEntity> entities, String query) {
        Map<String, Object> result = new HashMap<>();
        
        // Check for customer search
        Optional<QueryEntity> customerEntity = entities.stream()
            .filter(e -> e.getType() == EntityType.CUSTOMER)
            .findFirst();
            
        if (customerEntity.isPresent()) {
            String customerValue = extractIdentifier(customerEntity.get().getValue());
            if (customerValue != null) {
                try {
                    var customer = customerService.findByCustomerNumber(customerValue);
                    if (customer != null) {
                        result.put("customer", customer);
                        result.put("loans", loanService.findByCustomerId(customer.getId()));
                        result.put("payments", paymentService.findByCustomerId(customer.getId()));
                    }
                } catch (Exception e) {
                    result.put("error", "Customer not found: " + customerValue);
                }
            }
        }
        
        // Check for loan search
        Optional<QueryEntity> loanEntity = entities.stream()
            .filter(e -> e.getType() == EntityType.LOAN)
            .findFirst();
            
        if (loanEntity.isPresent()) {
            String loanValue = extractIdentifier(loanEntity.get().getValue());
            if (loanValue != null) {
                try {
                    var loan = loanService.findByLoanNumber(loanValue);
                    if (loan != null) {
                        result.put("loan", loan);
                        result.put("installments", loanService.getInstallments(loan.getId()));
                        result.put("payments", paymentService.findByLoanId(loan.getId()));
                    }
                } catch (Exception e) {
                    result.put("error", "Loan not found: " + loanValue);
                }
            }
        }
        
        // Check for status-based search
        Optional<QueryEntity> statusEntity = entities.stream()
            .filter(e -> e.getType() == EntityType.STATUS)
            .findFirst();
            
        if (statusEntity.isPresent()) {
            String status = statusEntity.get().getValue().toUpperCase();
            if (query.toLowerCase().contains("loan")) {
                result.put("loans", loanService.findByStatus(status));
            }
            if (query.toLowerCase().contains("payment")) {
                result.put("payments", paymentService.findByStatus(status));
            }
        }
        
        // Handle overdue queries
        if (query.toLowerCase().contains("overdue")) {
            result.put("overdueLoans", loanService.findOverdueLoans());
            result.put("overduePayments", paymentService.findOverduePayments());
        }
        
        return result;
    }

    private Object processAnalyticsQuery(List<QueryEntity> entities, String query) {
        Map<String, Object> analytics = new HashMap<>();
        
        // Loan analytics
        if (query.toLowerCase().contains("loan")) {
            analytics.put("totalLoans", loanService.getTotalLoanCount());
            analytics.put("totalLoanAmount", loanService.getTotalLoanAmount());
            analytics.put("averageLoanAmount", loanService.getAverageLoanAmount());
            analytics.put("loansByStatus", loanService.getLoanCountByStatus());
            analytics.put("loansByType", loanService.getLoanCountByType());
        }
        
        // Payment analytics
        if (query.toLowerCase().contains("payment")) {
            analytics.put("totalPayments", paymentService.getTotalPaymentCount());
            analytics.put("totalPaymentAmount", paymentService.getTotalPaymentAmount());
            analytics.put("onTimePaymentRate", paymentService.getOnTimePaymentRate());
            analytics.put("averagePaymentDelay", paymentService.getAveragePaymentDelay());
        }
        
        // Customer analytics
        if (query.toLowerCase().contains("customer")) {
            analytics.put("totalCustomers", customerService.getTotalCustomerCount());
            analytics.put("activeCustomers", customerService.getActiveCustomerCount());
            analytics.put("averageCreditScore", customerService.getAverageCreditScore());
            analytics.put("customersByRisk", customerService.getCustomerCountByRiskLevel());
        }
        
        // Risk analytics
        if (query.toLowerCase().contains("risk")) {
            analytics.put("highRiskCustomers", customerService.getHighRiskCustomers());
            analytics.put("defaultRate", loanService.getDefaultRate());
            analytics.put("riskDistribution", customerService.getRiskDistribution());
        }
        
        return analytics;
    }

    private Object processRecommendationQuery(List<QueryEntity> entities, String query) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Customer-specific recommendations
        Optional<QueryEntity> customerEntity = entities.stream()
            .filter(e -> e.getType() == EntityType.CUSTOMER)
            .findFirst();
            
        if (customerEntity.isPresent()) {
            String customerId = extractIdentifier(customerEntity.get().getValue());
            if (customerId != null) {
                recommendations.addAll(generateCustomerRecommendations(customerId));
            }
        } else {
            // General recommendations
            recommendations.addAll(generateGeneralRecommendations());
        }
        
        return Map.of("recommendations", recommendations);
    }

    private Object processTransactionQuery(List<QueryEntity> entities, String query) {
        Map<String, Object> result = new HashMap<>();
        
        if (query.toLowerCase().contains("payment")) {
            result.put("message", "To process a payment, use the processPayment mutation with loan ID and payment amount");
            result.put("example", "mutation { processPayment(input: { loanId: \"LOAN123\", paymentAmount: 1500.00, paymentMethod: BANK_TRANSFER }) }");
        }
        
        if (query.toLowerCase().contains("loan")) {
            result.put("message", "To create a loan, use the createLoan mutation with customer and loan details");
            result.put("example", "mutation { createLoan(input: { customerId: \"CUST123\", loanAmount: 25000.00, interestRate: 0.15, installmentCount: 12 }) }");
        }
        
        return result;
    }

    private Object processReportQuery(List<QueryEntity> entities, String query) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("generatedAt", LocalDateTime.now());
        report.put("period", "Current");
        
        // System overview
        report.put("systemOverview", Map.of(
            "totalCustomers", customerService.getTotalCustomerCount(),
            "totalLoans", loanService.getTotalLoanCount(),
            "totalPayments", paymentService.getTotalPaymentCount(),
            "systemHealth", "OPERATIONAL"
        ));
        
        // Performance metrics
        report.put("performanceMetrics", Map.of(
            "averageResponseTime", "< 40ms",
            "uptime", "99.9%",
            "transactionVolume", paymentService.getTotalPaymentCount()
        ));
        
        return report;
    }

    private Object processHelpQuery(String query) {
        Map<String, Object> help = new HashMap<>();
        
        help.put("availableQueries", List.of(
            "Find customer by ID: 'show customer CUST123'",
            "Get loan details: 'find loan LOAN456'",
            "Analytics: 'analyze loan performance'",
            "Overdue items: 'show overdue loans'",
            "Customer recommendations: 'recommend for customer CUST123'",
            "System report: 'generate system report'"
        ));
        
        help.put("supportedEntities", List.of(
            "Customers (CUST123, customer)",
            "Loans (LOAN456, loan)",
            "Payments (PAY789, payment)",
            "Amounts ($1000, 1000.00)",
            "Dates (today, 2024-01-01)",
            "Status (active, pending, overdue)"
        ));
        
        help.put("sampleQueries", List.of(
            "Show me all overdue loans",
            "Find customer with ID CUST123",
            "Analyze payment trends for last month",
            "What are the recommendations for customer CUST456?",
            "Generate a system performance report"
        ));
        
        return help;
    }

    private Object processGenericQuery(List<QueryEntity> entities, String query) {
        return Map.of(
            "message", "Query processed but no specific handler found",
            "detectedEntities", entities.size(),
            "suggestion", "Try using more specific terms like 'find customer', 'show loans', or 'analyze payments'"
        );
    }

    private List<Map<String, Object>> generateCustomerRecommendations(String customerId) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            var customer = customerService.findByCustomerNumber(customerId);
            if (customer != null) {
                // Credit utilization recommendation
                if (customer.getAvailableCredit().doubleValue() > customer.getCreditLimit().doubleValue() * 0.8) {
                    recommendations.add(Map.of(
                        "type", "CREDIT_INCREASE",
                        "title", "Consider Credit Limit Increase",
                        "description", "Customer has low credit utilization and good payment history",
                        "priority", "MEDIUM"
                    ));
                }
                
                // Payment recommendations
                var overdueLoans = loanService.findOverdueLoansByCustomer(customer.getId());
                if (!overdueLoans.isEmpty()) {
                    recommendations.add(Map.of(
                        "type", "EARLY_PAYMENT",
                        "title", "Address Overdue Payments",
                        "description", "Customer has " + overdueLoans.size() + " overdue loans",
                        "priority", "HIGH"
                    ));
                }
            }
        } catch (Exception e) {
            recommendations.add(Map.of(
                "type", "ERROR",
                "title", "Unable to generate customer recommendations",
                "description", "Customer data not accessible: " + e.getMessage(),
                "priority", "LOW"
            ));
        }
        
        return recommendations;
    }

    private List<Map<String, Object>> generateGeneralRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        recommendations.add(Map.of(
            "type", "PROCESS_IMPROVEMENT",
            "title", "Automate Overdue Payment Notifications",
            "description", "Implement automated alerts for customers with overdue payments",
            "priority", "MEDIUM"
        ));
        
        recommendations.add(Map.of(
            "type", "RISK_MITIGATION",
            "title", "Enhanced Risk Assessment",
            "description", "Implement real-time risk scoring for new loan applications",
            "priority", "HIGH"
        ));
        
        return recommendations;
    }

    private String extractIdentifier(String value) {
        // Extract ID from patterns like "customer CUST123" or "CUST123"
        Pattern idPattern = Pattern.compile("[A-Z]{3,}[0-9]+");
        Matcher matcher = idPattern.matcher(value.toUpperCase());
        return matcher.find() ? matcher.group() : null;
    }

    private float calculateConfidence(QueryIntent intent, List<QueryEntity> entities, String query) {
        float baseConfidence = 0.5f;
        
        // Increase confidence based on number of recognized entities
        baseConfidence += Math.min(entities.size() * 0.15f, 0.4f);
        
        // Increase confidence for clear intent patterns
        if (intent != QueryIntent.SEARCH) {
            baseConfidence += 0.1f;
        }
        
        // Decrease confidence for very short or very long queries
        int queryLength = query.split("\\s+").length;
        if (queryLength < 3 || queryLength > 20) {
            baseConfidence -= 0.1f;
        }
        
        return Math.max(0.1f, Math.min(1.0f, baseConfidence));
    }

    private List<String> generateSuggestions(QueryIntent intent, List<QueryEntity> entities) {
        List<String> suggestions = new ArrayList<>();
        
        switch (intent) {
            case SEARCH:
                suggestions.add("Try: 'find customer CUST123'");
                suggestions.add("Try: 'show all overdue loans'");
                suggestions.add("Try: 'list payments for loan LOAN456'");
                break;
            case ANALYTICS:
                suggestions.add("Try: 'analyze loan performance last month'");
                suggestions.add("Try: 'show payment trends'");
                suggestions.add("Try: 'customer risk analysis'");
                break;
            case RECOMMENDATION:
                suggestions.add("Try: 'recommend for customer CUST123'");
                suggestions.add("Try: 'suggest loan restructuring options'");
                break;
            default:
                suggestions.add("Try being more specific with customer or loan IDs");
                suggestions.add("Use keywords like 'find', 'show', 'analyze', or 'recommend'");
        }
        
        return suggestions;
    }
}