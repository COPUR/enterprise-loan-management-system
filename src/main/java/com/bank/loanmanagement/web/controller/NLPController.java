package com.bank.loanmanagement.web.controller;

import com.bank.loanmanagement.application.service.NLPApplicationService;
import com.bank.loanmanagement.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Natural Language Processing
 * Provides AI-powered endpoints for converting user prompts to banking requests
 */
@RestController
@RequestMapping("/api/ai/nlp")
@CrossOrigin(origins = "*")
public class NLPController {

    private final NLPApplicationService nlpService;

    public NLPController(NLPApplicationService nlpService) {
        this.nlpService = nlpService;
    }

    /**
     * Convert natural language prompt to structured loan request
     * POST /api/ai/nlp/convert-prompt-to-loan
     */
    @PostMapping("/convert-prompt-to-loan")
    public ResponseEntity<?> convertPromptToLoanRequest(@RequestBody PromptRequest request) {
        try {
            LoanRequestConversionResult result = nlpService.convertPromptToLoanRequest(request.getPrompt());
            
            return ResponseEntity.ok(new PromptToLoanResponse(
                    result.isSuccess(),
                    result.getOriginalPrompt(),
                    result.getConvertedRequest(),
                    result.getValidationResult(),
                    result.getConfidence(),
                    result.getError(),
                    LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "CONVERSION_FAILED",
                    "Failed to convert prompt to loan request: " + e.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * Analyze user intent from natural language input
     * POST /api/ai/nlp/analyze-intent
     */
    @PostMapping("/analyze-intent")
    public ResponseEntity<?> analyzeUserIntent(@RequestBody UserInputRequest request) {
        try {
            UserIntentAnalysisResult result = nlpService.analyzeUserIntent(request.getUserInput());
            
            return ResponseEntity.ok(new IntentAnalysisResponse(
                    result.isSuccess(),
                    result.getIntentAnalysis(),
                    result.getFinancialParameters(),
                    result.getRequestAssessment(),
                    result.getError(),
                    LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "INTENT_ANALYSIS_FAILED",
                    "Failed to analyze user intent: " + e.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    /**
     * Process natural language banking request end-to-end
     * POST /api/ai/nlp/process-request
     */
    @PostMapping("/process-request")
    public ResponseEntity<?> processNaturalLanguageRequest(@RequestBody BankingRequestInput request) {
        try {
            BankingRequestProcessingResult result = nlpService.processNaturalLanguageRequest(request.getUserInput());
            
            return ResponseEntity.ok(new BankingRequestProcessingResponse(
                    result.isSuccess(),
                    result.getUserInput(),
                    result.getIntentAnalysis(),
                    result.getRequestAssessment(),
                    result.getLoanConversion(),
                    result.getRecommendedAction(),
                    result.getError(),
                    LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(
                    "REQUEST_PROCESSING_FAILED",
                    "Failed to process banking request: " + e.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }

    // Request DTOs
    public static class PromptRequest {
        private String prompt;
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    public static class UserInputRequest {
        private String userInput;
        
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
    }

    public static class BankingRequestInput {
        private String userInput;
        
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
    }

    // Response DTOs
    public static class PromptToLoanResponse {
        private boolean success;
        private String originalPrompt;
        private LoanRequest convertedRequest;
        private ValidationResult validationResult;
        private double confidence;
        private String error;
        private LocalDateTime timestamp;

        public PromptToLoanResponse(boolean success, String originalPrompt, LoanRequest convertedRequest, 
                                  ValidationResult validationResult, double confidence, String error, LocalDateTime timestamp) {
            this.success = success;
            this.originalPrompt = originalPrompt;
            this.convertedRequest = convertedRequest;
            this.validationResult = validationResult;
            this.confidence = confidence;
            this.error = error;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getOriginalPrompt() { return originalPrompt; }
        public LoanRequest getConvertedRequest() { return convertedRequest; }
        public ValidationResult getValidationResult() { return validationResult; }
        public double getConfidence() { return confidence; }
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class IntentAnalysisResponse {
        private boolean success;
        private UserIntentAnalysis intentAnalysis;
        private FinancialParameters financialParameters;
        private RequestAssessment requestAssessment;
        private String error;
        private LocalDateTime timestamp;

        public IntentAnalysisResponse(boolean success, UserIntentAnalysis intentAnalysis, 
                                    FinancialParameters financialParameters, RequestAssessment requestAssessment,
                                    String error, LocalDateTime timestamp) {
            this.success = success;
            this.intentAnalysis = intentAnalysis;
            this.financialParameters = financialParameters;
            this.requestAssessment = requestAssessment;
            this.error = error;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public UserIntentAnalysis getIntentAnalysis() { return intentAnalysis; }
        public FinancialParameters getFinancialParameters() { return financialParameters; }
        public RequestAssessment getRequestAssessment() { return requestAssessment; }
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class BankingRequestProcessingResponse {
        private boolean success;
        private String userInput;
        private UserIntentAnalysis intentAnalysis;
        private RequestAssessment requestAssessment;
        private LoanRequestConversionResult loanConversion;
        private String recommendedAction;
        private String error;
        private LocalDateTime timestamp;

        public BankingRequestProcessingResponse(boolean success, String userInput, UserIntentAnalysis intentAnalysis, 
                                              RequestAssessment requestAssessment, LoanRequestConversionResult loanConversion,
                                              String recommendedAction, String error, LocalDateTime timestamp) {
            this.success = success;
            this.userInput = userInput;
            this.intentAnalysis = intentAnalysis;
            this.requestAssessment = requestAssessment;
            this.loanConversion = loanConversion;
            this.recommendedAction = recommendedAction;
            this.error = error;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getUserInput() { return userInput; }
        public UserIntentAnalysis getIntentAnalysis() { return intentAnalysis; }
        public RequestAssessment getRequestAssessment() { return requestAssessment; }
        public LoanRequestConversionResult getLoanConversion() { return loanConversion; }
        public String getRecommendedAction() { return recommendedAction; }
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse(String errorCode, String message, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}