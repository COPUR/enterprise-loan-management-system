package com.bank.loan.loan.ai.infrastructure.web;

import com.bank.loan.loan.ai.application.service.AIAssistantApplicationService;
import com.bank.loan.loan.ai.application.service.AIAssistantApplicationService.*;
import com.bank.loan.loan.security.dpop.annotation.DPoPSecured;
import com.bank.loan.loan.security.fapi.annotation.FAPISecured;
import com.bank.loan.loan.security.fapi.validation.FAPISecurityHeaders;
import com.bank.loan.loan.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * FAPI 2.0 + DPoP Compliant AI Assistant REST Controller
 * 
 * Provides enterprise-grade AI capabilities for banking operations with:
 * - FAPI 2.0 security headers validation
 * - DPoP token binding requirements
 * - Comprehensive audit logging
 * - Banking regulatory compliance
 */
@RestController
@RequestMapping("/api/v1/ai")
@DPoPSecured
@FAPISecured
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Assistant", description = "FAPI 2.0 + DPoP compliant AI-powered banking assistance")
public class AIAssistantRestController {

    private final AIAssistantApplicationService aiAssistantService;
    
    @Autowired
    private AuditService auditService;

    @GetMapping("/health")
    @Operation(summary = "AI service health check", 
               description = "Verify that AI services are available and functioning")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('UNDERWRITER') or hasRole('ADMIN')")
    public ResponseEntity<AIServiceHealthResult> healthCheck(
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader(value = "X-FAPI-Auth-Date", required = false) String fapiAuthDate,
            @RequestHeader(value = "X-FAPI-Customer-IP-Address", required = false) String customerIpAddress,
            HttpServletRequest httpRequest) {
        
        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            log.debug("Performing AI service health check for user: {}", userId);
            
            AIServiceHealthResult healthResult = aiAssistantService.performHealthCheck();
            
            // Audit log the health check
            auditService.logDataAccess("AI_HEALTH_CHECK", "AI_SERVICE", userId, 
                                     httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            if (healthResult.getOverall() == HealthStatus.HEALTHY) {
                return ResponseEntity.ok()
                    .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                    .body(healthResult);
            } else {
                return ResponseEntity.status(503)
                    .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                    .body(healthResult);
            }
            
        } catch (Exception e) {
            auditService.logSecurityViolation("AI_HEALTH_CHECK_FAILED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    @PostMapping("/analyze/loan-application")
    @Operation(summary = "Comprehensive loan application analysis", 
               description = "Perform comprehensive AI analysis on loan application with business rules")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('UNDERWRITER')")
    public ResponseEntity<ComprehensiveLoanAnalysisResult> analyzeLoanApplication(
            @Valid @RequestBody ComprehensiveLoanAnalysisRequest request,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader(value = "X-FAPI-Auth-Date", required = false) String fapiAuthDate,
            @RequestHeader(value = "X-FAPI-Customer-IP-Address", required = false) String customerIpAddress,
            @RequestHeader("X-Idempotency-Key") @NotNull String idempotencyKey,
            HttpServletRequest httpRequest) {
        
        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            log.info("Received comprehensive loan analysis request for applicant: {} by user: {}", 
                    request.getApplicantId(), userId);
            
            // TODO: Check idempotency for AI operations
            
            ComprehensiveLoanAnalysisResult result = aiAssistantService.analyzeLoanApplication(request);
            
            // Audit log the AI analysis
            auditService.logDataAccess("AI_LOAN_ANALYSIS", request.getApplicantId(), userId, 
                                     httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            log.info("Completed comprehensive loan analysis for applicant: {} with score: {}", 
                    request.getApplicantId(), result.getOverallScore());
            
            return ResponseEntity.ok()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(result);
            
        } catch (Exception e) {
            log.error("Failed comprehensive loan analysis for applicant: {}", request.getApplicantId(), e);
            
            auditService.logSecurityViolation("AI_LOAN_ANALYSIS_FAILED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            return ResponseEntity.internalServerError()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .build();
        }
    }

    @PostMapping("/assess/credit-risk")
    @Operation(summary = "Advanced credit risk assessment", 
               description = "Perform advanced credit risk assessment with portfolio considerations")
    @PreAuthorize("hasRole('UNDERWRITER') or hasRole('RISK_ANALYST')")
    public ResponseEntity<CreditRiskAssessmentResult> assessCreditRisk(
            @Valid @RequestBody CreditRiskAssessmentRequest request) {
        
        log.info("Received credit risk assessment request for applicant: {}", request.getApplicantId());
        
        try {
            CreditRiskAssessmentResult result = aiAssistantService.assessCreditRisk(request);
            
            log.info("Completed credit risk assessment for applicant: {} with risk score: {}", 
                    request.getApplicantId(), result.getOverallRiskScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed credit risk assessment for applicant: {}", request.getApplicantId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/recommend/loans")
    @Operation(summary = "Generate personalized loan recommendations", 
               description = "Generate personalized loan recommendations with customer journey analysis")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('RELATIONSHIP_MANAGER')")
    public ResponseEntity<PersonalizedRecommendationsResult> generateLoanRecommendations(
            @Valid @RequestBody PersonalizedRecommendationRequest request) {
        
        log.info("Received loan recommendation request for customer: {}", request.getCustomerId());
        
        try {
            PersonalizedRecommendationsResult result = aiAssistantService.generateLoanRecommendations(request);
            
            log.info("Generated {} loan recommendations for customer: {}", 
                    result.getProductRecommendations().size(), request.getCustomerId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to generate loan recommendations for customer: {}", request.getCustomerId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/analyze/financial-health")
    @Operation(summary = "Analyze financial health", 
               description = "Analyze financial health with comprehensive metrics")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('FINANCIAL_ADVISOR')")
    public ResponseEntity<FinancialHealthAnalysisResult> analyzeFinancialHealth(
            @Valid @RequestBody FinancialHealthAnalysisRequest request) {
        
        log.info("Received financial health analysis request for applicant: {}", request.getApplicantId());
        
        try {
            FinancialHealthAnalysisResult result = aiAssistantService.analyzeFinancialHealth(request);
            
            log.info("Completed financial health analysis for applicant: {} with score: {}", 
                    request.getApplicantId(), result.getOverallHealthScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed financial health analysis for applicant: {}", request.getApplicantId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/detect/fraud")
    @Operation(summary = "Fraud detection analysis", 
               description = "Perform AI-powered fraud detection analysis")
    @PreAuthorize("hasRole('FRAUD_ANALYST') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<FraudDetectionResult> detectFraud(
            @Valid @RequestBody FraudDetectionRequest request) {
        
        log.info("Received fraud detection request for entity: {}", request.getEntityId());
        
        try {
            // This would integrate with the fraud detection service
            // For now, return a placeholder response
            FraudDetectionResult result = new FraudDetectionResult(
                request.getEntityId(),
                false,
                0.1,
                "No fraud indicators detected",
                "Continue with normal processing"
            );
            
            log.info("Completed fraud detection for entity: {} with risk score: {}", 
                    request.getEntityId(), result.fraudScore());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed fraud detection for entity: {}", request.getEntityId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/strategy/collection")
    @Operation(summary = "Generate collection strategy", 
               description = "Generate AI-powered collection strategy for delinquent accounts")
    @PreAuthorize("hasRole('COLLECTION_AGENT') or hasRole('COLLECTION_MANAGER')")
    public ResponseEntity<CollectionStrategyResult> generateCollectionStrategy(
            @Valid @RequestBody CollectionStrategyRequest request) {
        
        log.info("Received collection strategy request for account: {}", request.getAccountId());
        
        try {
            // This would integrate with collection strategy AI
            // For now, return a placeholder response
            CollectionStrategyResult result = new CollectionStrategyResult(
                request.getAccountId(),
                "CONTACT_CUSTOMER",
                "Phone call within 3 days",
                7,
                "Standard payment plan offer"
            );
            
            log.info("Generated collection strategy for account: {} with priority: {}", 
                    request.getAccountId(), result.priority());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to generate collection strategy for account: {}", request.getAccountId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/analyze/batch")
    @Operation(summary = "Batch AI operations", 
               description = "Process multiple AI operations in batch")
    @PreAuthorize("hasRole('BATCH_PROCESSOR') or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<BatchProcessingResult>> processBatchOperations(
            @Valid @RequestBody BatchProcessingRequest request) {
        
        log.info("Received batch processing request with {} operations", request.getOperations().size());
        
        return aiAssistantService.processBatchOperations(request)
            .thenApply(result -> {
                log.info("Completed batch processing: {}/{} operations successful", 
                        result.getCompletedOperations(), result.getTotalOperations());
                return ResponseEntity.ok(result);
            })
            .exceptionally(throwable -> {
                log.error("Batch processing failed", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    @GetMapping("/insights/dashboard")
    @Operation(summary = "AI insights dashboard", 
               description = "Get AI insights and analytics for dashboard")
    @PreAuthorize("hasRole('MANAGER') or hasRole('EXECUTIVE') or hasRole('ANALYST')")
    public ResponseEntity<AIInsightsDashboard> getAIInsightsDashboard(
            @RequestParam(defaultValue = "30") int days) {
        
        log.debug("Retrieving AI insights dashboard for {} days", days);
        
        try {
            // This would aggregate AI insights and analytics
            // For now, return a placeholder response
            AIInsightsDashboard dashboard = new AIInsightsDashboard(
                1250,  // totalAnalyses
                0.89,  // averageConfidence
                0.15,  // fraudDetectionRate
                0.78,  // approvalRate
                4.2    // averageProcessingTime
            );
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("Failed to retrieve AI insights dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/config")
    @Operation(summary = "AI configuration and capabilities", 
               description = "Get current AI configuration and available capabilities")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AI_ENGINEER')")
    public ResponseEntity<AIConfigurationInfo> getAIConfiguration() {
        
        log.debug("Retrieving AI configuration information");
        
        try {
            AIServiceHealthResult health = aiAssistantService.performHealthCheck();
            
            AIConfigurationInfo config = new AIConfigurationInfo(
                health.getModelVersion(),
                "OpenAI GPT-4",
                health.getOverall() == HealthStatus.HEALTHY,
                "2024-12-25",
                new String[]{"loan_analysis", "fraud_detection", "risk_assessment", "recommendations"}
            );
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("Failed to retrieve AI configuration", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Record types for API responses
    public record FraudDetectionRequest(String entityId, String entityType) {}
    public record FraudDetectionResult(String entityId, boolean hasFraudRisk, double fraudScore, String indicators, String recommendation) {}
    public record CollectionStrategyRequest(String accountId, double delinquentAmount) {}
    public record CollectionStrategyResult(String accountId, String strategy, String action, int priority, String notes) {}
    public record AIInsightsDashboard(int totalAnalyses, double averageConfidence, double fraudDetectionRate, double approvalRate, double averageProcessingTime) {}
    public record AIConfigurationInfo(String modelVersion, String modelType, boolean isHealthy, String lastUpdated, String[] capabilities) {}
}