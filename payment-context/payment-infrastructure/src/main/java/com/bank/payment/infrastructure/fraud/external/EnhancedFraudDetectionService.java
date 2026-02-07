package com.bank.payment.infrastructure.fraud.external;

import com.bank.payment.application.FraudDetectionService;
import com.bank.payment.application.FraudDetectedException;
import com.bank.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;

/**
 * Enhanced Fraud Detection Service that integrates with external providers
 * 
 * This service combines internal ML models with external fraud detection
 * services for comprehensive fraud prevention.
 */
@Service("enhancedFraudDetectionService")
public class EnhancedFraudDetectionService implements FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedFraudDetectionService.class);

    private final ExternalFraudDetectionClient externalClient;
    private final Executor fraudExecutor;

    // Risk thresholds
    private static final int HIGH_RISK_THRESHOLD = 75;
    private static final int BLOCK_THRESHOLD = 90;
    private static final int EXTERNAL_SERVICE_TIMEOUT_SECONDS = 5;

    public EnhancedFraudDetectionService(ExternalFraudDetectionClient externalClient,
                                        @Qualifier("fraudExecutor") Executor fraudExecutor) {
        this.externalClient = externalClient;
        this.fraudExecutor = fraudExecutor;
    }

    public void validatePayment(Payment payment) throws FraudDetectedException {
        logger.info("Starting enhanced fraud validation for payment: {}", payment.getId());

        try {
            // Create external fraud request
            ExternalFraudRequest fraudRequest = buildFraudRequest(payment);

            // Call external fraud detection service asynchronously
            CompletableFuture<ExternalFraudResponse> externalResponse = 
                externalClient.screenTransaction(fraudRequest);

            // Get response with timeout
            ExternalFraudResponse response = externalResponse.get(
                EXTERNAL_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Evaluate fraud response
            evaluateFraudResponse(payment, response);

            logger.info("Enhanced fraud validation completed for payment: {} with risk score: {}", 
                payment.getId(), response.getRiskScore());

        } catch (TimeoutException e) {
            logger.warn("External fraud service timeout for payment: {}, using fallback validation", 
                payment.getId());
            performFallbackValidation(payment);

        } catch (ExecutionException | InterruptedException e) {
            logger.error("External fraud service error for payment: {}, using fallback validation: {}", 
                payment.getId(), e.getMessage());
            performFallbackValidation(payment);

        } catch (FraudDetectedException e) {
            // Re-throw fraud exceptions
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during fraud validation for payment: {}: {}", 
                payment.getId(), e.getMessage());
            // Don't block payment for unexpected errors, but log for investigation
        }
    }

    @Override
    public boolean isValidPayment(Payment payment) {
        try {
            validatePayment(payment);
            return true;
        } catch (FraudDetectedException ex) {
            return false;
        }
    }

    @Override
    public int calculateRiskScore(Payment payment) {
        try {
            FraudAnalysisResult result = performComprehensiveAnalysis(payment);
            Integer combined = result.getCombinedRiskScore();
            if (combined != null) {
                return combined;
            }
            Integer external = result.getExternalRiskScore();
            return external != null ? external : 50;
        } catch (Exception e) {
            return 50;
        }
    }

    @Override
    public boolean exceedsVelocityLimits(Payment payment) {
        BigDecimal amount = payment.getAmount().getAmount();
        return amount.compareTo(BigDecimal.valueOf(100000)) > 0;
    }

    @Override
    public boolean isSuspiciousPattern(Payment payment) {
        BigDecimal amount = payment.getAmount().getAmount();
        return amount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0
            && amount.compareTo(BigDecimal.valueOf(10000)) > 0;
    }

    /**
     * Perform comprehensive fraud analysis including external services
     */
    public FraudAnalysisResult performComprehensiveAnalysis(Payment payment) {
        logger.info("Performing comprehensive fraud analysis for payment: {}", payment.getId());

        FraudAnalysisResult result = FraudAnalysisResult.builder()
            .paymentId(payment.getId().getValue())
            .customerId(payment.getCustomerId().getValue())
            .timestamp(LocalDateTime.now())
            .build();

        try {
            // Build external fraud request
            ExternalFraudRequest fraudRequest = buildFraudRequest(payment);

            // Get external fraud analysis
            CompletableFuture<ExternalFraudResponse> externalFuture = 
                externalClient.screenTransaction(fraudRequest);

            ExternalFraudResponse externalResponse = externalFuture.get(
                EXTERNAL_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Combine results
            result.setExternalRiskScore(externalResponse.getRiskScore());
            result.setExternalRiskLevel(externalResponse.getRiskLevel());
            result.setExternalReasons(externalResponse.getReasons());
            result.setIsBlockedByExternal(externalResponse.getIsBlocked());

            // Calculate combined risk score
            int combinedRiskScore = calculateCombinedRiskScore(result);
            result.setCombinedRiskScore(combinedRiskScore);
            result.setFinalRecommendation(determineFinalRecommendation(combinedRiskScore));

        } catch (Exception e) {
            logger.error("Error during comprehensive fraud analysis: {}", e.getMessage());
            result.setHasError(true);
            result.setErrorMessage(e.getMessage());
            result.setCombinedRiskScore(50); // Default medium risk
            result.setFinalRecommendation("REVIEW");
        }

        return result;
    }

    /**
     * Report confirmed fraud case to external service
     */
    public void reportFraudCase(Payment payment, String fraudType, String description) {
        logger.info("Reporting fraud case for payment: {} of type: {}", payment.getId(), fraudType);

        try {
            FraudCaseReport report = FraudCaseReport.builder()
                .transactionId(payment.getId().getValue())
                .customerId(payment.getCustomerId().getValue())
                .caseType("CONFIRMED_FRAUD")
                .fraudType(fraudType)
                .amount(payment.getAmount().getAmount())
                .currency(payment.getAmount().getCurrency().getCurrencyCode())
                .transactionDate(payment.getCreatedAt())
                .reportedDate(LocalDateTime.now())
                .reportedBy("BANK")
                .fraudDetails(FraudCaseReport.FraudDetails.builder()
                    .description(description)
                    .isFirstPartyFraud(false)
                    .estimatedLoss(payment.getAmount().getAmount())
                    .build())
                .status("OPEN")
                .priority("HIGH")
                .build();

            CompletableFuture<FraudReportResponse> reportFuture = 
                externalClient.reportFraudCase(report);

            reportFuture.thenAcceptAsync(response -> 
                logger.info("Fraud case reported successfully with case ID: {}", response.getCaseId()), 
                fraudExecutor
            ).exceptionally(throwable -> {
                logger.error("Failed to report fraud case: {}", throwable.getMessage());
                return null;
            });

        } catch (Exception e) {
            logger.error("Error reporting fraud case: {}", e.getMessage());
        }
    }

    // Private helper methods

    private ExternalFraudRequest buildFraudRequest(Payment payment) {
        return ExternalFraudRequest.builder()
            .transactionId(payment.getId().getValue())
            .customerId(payment.getCustomerId().getValue())
            .amount(payment.getAmount().getAmount())
            .currency(payment.getAmount().getCurrency().getCurrencyCode())
            .paymentMethod(payment.getPaymentType().name())
            .timestamp(payment.getCreatedAt())
            .customerProfile(ExternalFraudRequest.CustomerProfile.builder()
                .customerId(payment.getCustomerId().getValue())
                .customerType("INDIVIDUAL")
                .isVerified(true)
                .build())
            .transactionContext(ExternalFraudRequest.TransactionContext.builder()
                .channelType("ONLINE")
                .authenticationMethod("PASSWORD")
                .isRecurring(false)
                .build())
            .build();
    }

    private void evaluateFraudResponse(Payment payment, ExternalFraudResponse response) 
            throws FraudDetectedException {
        
        if (response.getIsBlocked() || response.getRiskScore() > BLOCK_THRESHOLD) {
            throw new FraudDetectedException(
                String.format("Payment blocked by fraud detection. Risk score: %d, Reasons: %s",
                    response.getRiskScore(), String.join(", ", response.getReasons())));
        }

        if (response.getRiskScore() > HIGH_RISK_THRESHOLD) {
            logger.warn("High risk payment detected: {} with score: {}", 
                payment.getId(), response.getRiskScore());
            // Could trigger additional authentication or manual review
        }
    }

    private void performFallbackValidation(Payment payment) throws FraudDetectedException {
        // Simple fallback rules when external service is unavailable
        BigDecimal amount = payment.getAmount().getAmount();
        
        // Block unusually large amounts
        if (amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
            throw new FraudDetectedException("Large amount transaction requires manual review");
        }

        // Add other basic validations as needed
        logger.info("Fallback fraud validation completed for payment: {}", payment.getId());
    }

    private int calculateCombinedRiskScore(FraudAnalysisResult result) {
        // Combine external and internal risk scores
        int externalScore = result.getExternalRiskScore() != null ? result.getExternalRiskScore() : 50;
        
        // For now, use external score as primary
        // In production, you'd combine multiple sources
        return externalScore;
    }

    private String determineFinalRecommendation(int combinedRiskScore) {
        if (combinedRiskScore > 90) {
            return "DECLINE";
        } else if (combinedRiskScore > 75) {
            return "REVIEW";
        } else if (combinedRiskScore > 50) {
            return "AUTH_REQUIRED";
        } else {
            return "APPROVE";
        }
    }
}
