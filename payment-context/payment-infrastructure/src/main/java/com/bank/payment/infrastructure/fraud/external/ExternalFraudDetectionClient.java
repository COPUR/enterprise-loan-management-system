package com.bank.payment.infrastructure.fraud.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * External Fraud Detection Service Client
 * 
 * Integrates with third-party fraud detection services like:
 * - FICO Falcon Fraud Manager
 * - SAS Fraud Management
 * - IBM Safer Payments
 * - NICE Actimize
 */
@Component
public class ExternalFraudDetectionClient {

    private static final Logger logger = LoggerFactory.getLogger(ExternalFraudDetectionClient.class);

    private final RestTemplate restTemplate;
    
    @Value("${fraud.detection.primary.url:https://api.frauddetection.com/v1}")
    private String primaryFraudServiceUrl;
    
    @Value("${fraud.detection.secondary.url:https://backup.frauddetection.com/v1}")
    private String secondaryFraudServiceUrl;
    
    @Value("${fraud.detection.api.key:demo-key}")
    private String apiKey;
    
    @Value("${fraud.detection.timeout.seconds:5}")
    private int timeoutSeconds;
    
    @Value("${fraud.detection.enabled:true}")
    private boolean fraudDetectionEnabled;

    public ExternalFraudDetectionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Perform real-time fraud screening with external service
     */
    public CompletableFuture<ExternalFraudResponse> screenTransaction(ExternalFraudRequest request) {
        if (!fraudDetectionEnabled) {
            logger.info("External fraud detection disabled, using mock response");
            return CompletableFuture.completedFuture(createMockResponse(request));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try primary service first
                ExternalFraudResponse response = callFraudService(primaryFraudServiceUrl, request);
                logger.info("Fraud screening completed for transaction {} with score: {}", 
                    request.getTransactionId(), response.getRiskScore());
                return response;
                
            } catch (Exception e) {
                logger.warn("Primary fraud service failed, trying secondary: {}", e.getMessage());
                
                try {
                    // Fallback to secondary service
                    ExternalFraudResponse response = callFraudService(secondaryFraudServiceUrl, request);
                    logger.info("Fraud screening completed via secondary service for transaction {} with score: {}", 
                        request.getTransactionId(), response.getRiskScore());
                    return response;
                    
                } catch (Exception secondaryError) {
                    logger.error("Both fraud services failed, using fallback logic: {}", secondaryError.getMessage());
                    return createFallbackResponse(request);
                }
            }
        });
    }

    /**
     * Batch fraud screening for multiple transactions
     */
    public CompletableFuture<Map<String, ExternalFraudResponse>> screenTransactionBatch(
            Map<String, ExternalFraudRequest> requests) {
        
        if (!fraudDetectionEnabled) {
            Map<String, ExternalFraudResponse> mockResponses = new HashMap<>();
            requests.forEach((id, req) -> mockResponses.put(id, createMockResponse(req)));
            return CompletableFuture.completedFuture(mockResponses);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, ExternalFraudResponse> responses = new HashMap<>();
            
            try {
                BatchFraudRequest batchRequest = new BatchFraudRequest(requests);
                BatchFraudResponse batchResponse = callBatchFraudService(primaryFraudServiceUrl, batchRequest);
                Map<String, ExternalFraudResponse> batchResponses = batchResponse.getResponses();
                if (batchResponses != null) {
                    responses.putAll(batchResponses);
                }
                
            } catch (Exception e) {
                logger.error("Batch fraud screening failed: {}", e.getMessage());
                // Process individually as fallback
                requests.forEach((id, req) -> {
                    try {
                        responses.put(id, screenTransaction(req).get(timeoutSeconds, TimeUnit.SECONDS));
                    } catch (Exception individualError) {
                        logger.warn("Individual fraud screening failed for {}: {}", id, individualError.getMessage());
                        responses.put(id, createFallbackResponse(req));
                    }
                });
            }
            
            return responses;
        });
    }

    /**
     * Update customer risk profile with external service
     */
    public CompletableFuture<CustomerRiskProfileResponse> updateCustomerRiskProfile(
            String customerId, CustomerRiskUpdateRequest updateRequest) {
        
        if (!fraudDetectionEnabled) {
            return CompletableFuture.completedFuture(
                new CustomerRiskProfileResponse(customerId, "MEDIUM", 45, LocalDateTime.now()));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = createHeaders();
                HttpEntity<CustomerRiskUpdateRequest> entity = new HttpEntity<>(updateRequest, headers);
                
                ResponseEntity<CustomerRiskProfileResponse> response = restTemplate.exchange(
                    primaryFraudServiceUrl + "/customers/" + customerId + "/risk-profile",
                    HttpMethod.PUT,
                    entity,
                    CustomerRiskProfileResponse.class
                );
                
                return response.getBody();
                
            } catch (Exception e) {
                logger.error("Customer risk profile update failed for {}: {}", customerId, e.getMessage());
                return new CustomerRiskProfileResponse(customerId, "MEDIUM", 50, LocalDateTime.now());
            }
        });
    }

    /**
     * Report fraud case to external service
     */
    public CompletableFuture<FraudReportResponse> reportFraudCase(FraudCaseReport fraudReport) {
        if (!fraudDetectionEnabled) {
            FraudReportResponse response = new FraudReportResponse();
            response.setTransactionId(fraudReport.getTransactionId());
            response.setStatus("RECEIVED");
            response.setCaseId("MOCK-CASE-" + System.currentTimeMillis());
            response.setTimestamp(LocalDateTime.now());
            return CompletableFuture.completedFuture(response);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = createHeaders();
                HttpEntity<FraudCaseReport> entity = new HttpEntity<>(fraudReport, headers);
                
                ResponseEntity<FraudReportResponse> response = restTemplate.exchange(
                    primaryFraudServiceUrl + "/fraud-cases",
                    HttpMethod.POST,
                    entity,
                    FraudReportResponse.class
                );
                
                logger.info("Fraud case reported successfully: {}", response.getBody().getCaseId());
                return response.getBody();
                
            } catch (Exception e) {
                logger.error("Fraud case reporting failed: {}", e.getMessage());
                FraudReportResponse response = new FraudReportResponse();
                response.setTransactionId(fraudReport.getTransactionId());
                response.setStatus("FAILED");
                response.setTimestamp(LocalDateTime.now());
                response.setMessage(e.getMessage());
                return response;
            }
        });
    }

    // Private helper methods

    private ExternalFraudResponse callFraudService(String serviceUrl, ExternalFraudRequest request) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<ExternalFraudRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<ExternalFraudResponse> response = restTemplate.exchange(
                serviceUrl + "/screen",
                HttpMethod.POST,
                entity,
                ExternalFraudResponse.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            logger.error("Fraud service call failed: {}", e.getMessage());
            throw new FraudServiceException("External fraud service unavailable", e);
        }
    }

    private BatchFraudResponse callBatchFraudService(String serviceUrl, BatchFraudRequest request) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<BatchFraudRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<BatchFraudResponse> response = restTemplate.exchange(
                serviceUrl + "/screen/batch",
                HttpMethod.POST,
                entity,
                BatchFraudResponse.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            logger.error("Batch fraud service call failed: {}", e.getMessage());
            throw new FraudServiceException("External batch fraud service unavailable", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("X-API-Version", "1.0");
        headers.set("X-Client-ID", "enterprise-banking-platform");
        return headers;
    }

    private ExternalFraudResponse createMockResponse(ExternalFraudRequest request) {
        // Simple mock logic based on transaction amount
        int riskScore = calculateMockRiskScore(request);
        String riskLevel = riskScore > 75 ? "HIGH" : riskScore > 50 ? "MEDIUM" : "LOW";
        boolean isBlocked = riskScore > 90;
        
        return ExternalFraudResponse.builder()
            .transactionId(request.getTransactionId())
            .riskScore(riskScore)
            .riskLevel(riskLevel)
            .isBlocked(isBlocked)
            .reasons(generateMockReasons(riskScore))
            .processingTime(150L) // Mock processing time
            .timestamp(LocalDateTime.now())
            .build();
    }

    private ExternalFraudResponse createFallbackResponse(ExternalFraudRequest request) {
        // Conservative fallback - medium risk for all transactions
        return ExternalFraudResponse.builder()
            .transactionId(request.getTransactionId())
            .riskScore(50)
            .riskLevel("MEDIUM")
            .isBlocked(false)
            .reasons(java.util.Arrays.asList("FALLBACK_MODE"))
            .processingTime(10L)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private int calculateMockRiskScore(ExternalFraudRequest request) {
        int score = 20; // Base score
        
        // Increase score based on amount
        if (request.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            score += 30;
        } else if (request.getAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            score += 15;
        }
        
        // Increase score for off-hours transactions
        int hour = LocalDateTime.now().getHour();
        if (hour < 6 || hour > 22) {
            score += 20;
        }
        
        // Add some randomness
        score += (int) (Math.random() * 25);
        
        return Math.min(100, score);
    }

    private java.util.List<String> generateMockReasons(int riskScore) {
        java.util.List<String> reasons = new java.util.ArrayList<>();
        
        if (riskScore > 75) {
            reasons.add("HIGH_AMOUNT");
            reasons.add("UNUSUAL_TIMING");
        } else if (riskScore > 50) {
            reasons.add("MODERATE_AMOUNT");
        } else {
            reasons.add("LOW_RISK_PROFILE");
        }
        
        return reasons;
    }

    // Exception class for fraud service errors
    public static class FraudServiceException extends RuntimeException {
        public FraudServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
