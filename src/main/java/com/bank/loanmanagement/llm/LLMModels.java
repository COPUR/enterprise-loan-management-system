package com.bank.loanmanagement.llm;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * LLM Chatbot Data Models
 * Defines conversation structures for banking AI assistants
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String customerId;
    private String conversationId;
    private String sessionId;
    private Map<String, String> context;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String responseType;
    private Map<String, Object> data;
    private List<String> suggestions;
    private String conversationId;
    private LocalDateTime timestamp;
    private boolean requiresAuthentication;
    private List<String> requiredActions;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankingContext {
    private String systemName;
    private List<String> capabilities;
    private List<String> supportedOperations;
    private String complianceLevel;
    private List<String> securityFeatures;
    private Map<String, Object> customerContext;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatIntent {
    private ChatIntentType type;
    private double extractedAmount;
    private int extractedTerm;
    private double extractedRate;
    private String extractedLoanId;
    private String extractedPaymentMethod;
    private Map<String, Object> parameters;
}

public enum ChatIntentType {
    ACCOUNT_INQUIRY,
    LOAN_APPLICATION,
    PAYMENT_PROCESSING,
    LOAN_CALCULATION,
    CREDIT_CHECK,
    DOCUMENT_UPLOAD,
    GENERAL_INQUIRY,
    AUTHENTICATION_REQUIRED
}