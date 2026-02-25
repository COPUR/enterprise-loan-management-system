package com.loanmanagement.shared.application.service;

import com.loanmanagement.shared.application.port.out.*;

/**
 * Example Framework-Agnostic Service
 * Demonstrates how to use dependency inversion abstractions
 * This service does not depend on any specific framework
 */
public class ExternalCreditCheckService {
    
    private final HttpClientPort httpClient;
    private final ValidationPort validator;
    private final TimeProvider timeProvider;
    private final LoggingPort logger;
    
    public ExternalCreditCheckService(
            HttpClientPort httpClient,
            ValidationPort validator,
            TimeProvider timeProvider,
            LoggingFactory loggingFactory) {
        this.httpClient = httpClient;
        this.validator = validator;
        this.timeProvider = timeProvider;
        this.logger = loggingFactory.getLogger(ExternalCreditCheckService.class);
    }
    
    /**
     * Check credit score from external service
     */
    public CreditCheckResult checkCreditScore(CreditCheckRequest request) {
        logger.info("Checking credit score for customer: {}", request.customerId());
        
        // Validate request
        ValidationPort.ValidationResult validationResult = validator.validate(request);
        if (!validationResult.valid()) {
            logger.warn("Invalid credit check request: {}", validationResult.getViolationMessages());
            return CreditCheckResult.invalid(validationResult.getViolationMessages());
        }
        
        try {
            // Make HTTP call to external service
            HttpClientPort.HttpResponse<ExternalCreditResponse> response = httpClient.post(
                    "https://api.credit-bureau.com/check",
                    mapToExternalRequest(request),
                    ExternalCreditResponse.class
            );
            
            if (response.isSuccessful()) {
                logger.info("Credit check successful for customer: {}", request.customerId());
                return mapToResult(response.body(), request);
            } else {
                logger.error("Credit check failed with status: {}", response.statusCode());
                return CreditCheckResult.error("External service unavailable");
            }
            
        } catch (Exception e) {
            logger.error("Error during credit check", e);
            return CreditCheckResult.error("Service communication failed");
        }
    }
    
    private ExternalCreditRequest mapToExternalRequest(CreditCheckRequest request) {
        return new ExternalCreditRequest(
                request.customerId().toString(),
                request.ssn(),
                request.fullName()
        );
    }
    
    private CreditCheckResult mapToResult(ExternalCreditResponse response, CreditCheckRequest request) {
        return new CreditCheckResult(
                true,
                response.creditScore(),
                response.riskLevel(),
                request.customerId(),
                timeProvider.now(),
                null
        );
    }
    
    // DTOs
    public record CreditCheckRequest(
            Long customerId,
            String ssn,
            String fullName
    ) {}
    
    public record CreditCheckResult(
            boolean successful,
            Integer creditScore,
            String riskLevel,
            Long customerId,
            java.time.LocalDateTime checkedAt,
            String errorMessage
    ) {
        public static CreditCheckResult invalid(String message) {
            return new CreditCheckResult(false, null, null, null, null, message);
        }
        
        public static CreditCheckResult error(String message) {
            return new CreditCheckResult(false, null, null, null, null, message);
        }
    }
    
    // External service DTOs
    private record ExternalCreditRequest(
            String customerId,
            String ssn,
            String name
    ) {}
    
    private record ExternalCreditResponse(
            Integer creditScore,
            String riskLevel
    ) {}
}