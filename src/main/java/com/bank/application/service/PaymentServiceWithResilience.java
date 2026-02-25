package com.bank.application.service;

import com.bank.domain.payment.Payment;
import com.bank.domain.payment.PaymentRepository;
import com.bank.infrastructure.resilience.service.ResilientServiceTemplate;
import com.bank.infrastructure.resilience.service.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Example of how to use the resilience framework in a real service.
 * This demonstrates best practices for implementing resilient service calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceWithResilience {
    
    private final ResilientServiceTemplate resilientTemplate;
    private final PaymentRepository paymentRepository;
    private final ExternalPaymentGateway paymentGateway;
    private final FraudDetectionService fraudDetectionService;
    private final NotificationService notificationService;
    
    /**
     * Process payment with full resilience protection
     */
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing payment for amount: {}", request.getAmount());
        
        // Step 1: Fraud detection with circuit breaker
        boolean fraudCheckPassed = resilientTemplate.forService("fraud-detection")
                .withTimeout(Duration.ofSeconds(5))
                .withFallback(throwable -> {
                    log.warn("Fraud detection service unavailable, applying default rules");
                    return applyDefaultFraudRules(request);
                })
                .execute(() -> fraudDetectionService.checkForFraud(request));
        
        if (!fraudCheckPassed) {
            log.warn("Payment blocked due to fraud detection");
            return PaymentResult.blocked("Fraud detected");
        }
        
        // Step 2: Process payment through external gateway with resilience
        ExternalPaymentResult externalResult = resilientTemplate.forService("payment-gateway")
                .withTimeout(Duration.ofSeconds(30))
                .withMaxRetries(2)
                .withHealthCheck(() -> checkPaymentGatewayHealth())
                .withFallback(throwable -> {
                    log.error("Payment gateway failed, using fallback", throwable);
                    return handlePaymentGatewayFailure(request);
                })
                .execute(() -> paymentGateway.processPayment(request));
        
        // Step 3: Save payment record
        Payment payment = createPaymentRecord(request, externalResult);
        Payment savedPayment = resilientTemplate.forService("database")
                .withTimeout(Duration.ofSeconds(5))
                .critical() // Database operations are critical
                .withFallback(throwable -> {
                    log.error("Failed to save payment, queueing for retry", throwable);
                    queuePaymentForRetry(payment);
                    return payment;
                })
                .execute(() -> paymentRepository.save(payment));
        
        // Step 4: Send notification (non-critical, fire-and-forget)
        resilientTemplate.forService("notification")
                .withTimeout(Duration.ofSeconds(10))
                .withFallback(throwable -> {
                    log.warn("Notification failed, will retry later", throwable);
                    return null;
                })
                .executeAsync(() -> sendPaymentNotification(savedPayment));
        
        return PaymentResult.success(savedPayment.getId(), externalResult.getTransactionId());
    }
    
    /**
     * Batch payment processing with bulkhead isolation
     */
    public CompletableFuture<BatchPaymentResult> processBatchPayments(BatchPaymentRequest batchRequest) {
        log.info("Processing batch of {} payments", batchRequest.getPayments().size());
        
        return resilientTemplate.executeBatch(
                "payment-batch",
                () -> {
                    BatchPaymentResult result = new BatchPaymentResult();
                    
                    for (PaymentRequest payment : batchRequest.getPayments()) {
                        try {
                            PaymentResult paymentResult = processPayment(payment);
                            result.addResult(payment.getId(), paymentResult);
                        } catch (Exception e) {
                            log.error("Failed to process payment in batch: {}", payment.getId(), e);
                            result.addFailure(payment.getId(), e.getMessage());
                        }
                    }
                    
                    return result;
                }
        );
    }
    
    /**
     * Get payment status with resilience
     */
    public PaymentStatus getPaymentStatus(String paymentId) {
        return resilientTemplate.forService("payment-status")
                .withTimeout(Duration.ofSeconds(3))
                .withFallback(throwable -> {
                    log.warn("Cannot retrieve payment status, returning cached value");
                    return getCachedPaymentStatus(paymentId);
                })
                .execute(() -> {
                    Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
                    
                    // Check external status if payment is pending
                    if (payment.isPending()) {
                        ExternalPaymentStatus externalStatus = paymentGateway
                                .checkStatus(payment.getExternalTransactionId());
                        return mapToPaymentStatus(externalStatus);
                    }
                    
                    return mapToPaymentStatus(payment);
                });
    }
    
    // Fallback methods
    
    private boolean applyDefaultFraudRules(PaymentRequest request) {
        // Simple fraud rules when service is unavailable
        if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            log.warn("Payment exceeds threshold without fraud check");
            return false;
        }
        return true;
    }
    
    private ExternalPaymentResult handlePaymentGatewayFailure(PaymentRequest request) {
        // Queue for offline processing
        log.info("Queueing payment for offline processing: {}", request.getId());
        return ExternalPaymentResult.queued(request.getId());
    }
    
    private HealthStatus checkPaymentGatewayHealth() {
        try {
            boolean healthy = paymentGateway.ping();
            return healthy ? HealthStatus.healthy() : 
                    HealthStatus.unhealthy("Payment gateway not responding");
        } catch (Exception e) {
            return HealthStatus.unhealthy("Health check failed: " + e.getMessage());
        }
    }
    
    private void queuePaymentForRetry(Payment payment) {
        // Implementation for queueing failed payments
        log.info("Payment queued for retry: {}", payment.getId());
    }
    
    private CompletableFuture<Void> sendPaymentNotification(Payment payment) {
        return CompletableFuture.runAsync(() -> {
            notificationService.sendPaymentConfirmation(
                    payment.getCustomerId(),
                    payment.getAmount(),
                    payment.getId()
            );
        });
    }
    
    private PaymentStatus getCachedPaymentStatus(String paymentId) {
        // Return cached or default status
        return PaymentStatus.unknown();
    }
    
    private Payment createPaymentRecord(PaymentRequest request, ExternalPaymentResult externalResult) {
        return Payment.builder()
                .customerId(UUID.fromString(request.getCustomerId()))
                .amount(request.getAmount())
                .externalTransactionId(externalResult.getTransactionId())
                .status(mapExternalStatus(externalResult.getStatus()))
                .build();
    }
    
    private PaymentStatus mapToPaymentStatus(Payment payment) {
        return PaymentStatus.builder()
                .paymentId(payment.getId().toString())
                .status(payment.getStatus().toString())
                .amount(payment.getAmount())
                .lastUpdated(payment.getUpdatedAt())
                .build();
    }
    
    private PaymentStatus mapToPaymentStatus(ExternalPaymentStatus externalStatus) {
        return PaymentStatus.builder()
                .status(externalStatus.getStatus())
                .externalReference(externalStatus.getReference())
                .build();
    }
    
    private com.bank.domain.payment.PaymentStatus mapExternalStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> com.bank.domain.payment.PaymentStatus.COMPLETED;
            case "PENDING" -> com.bank.domain.payment.PaymentStatus.PROCESSING;
            case "FAILED" -> com.bank.domain.payment.PaymentStatus.FAILED;
            default -> com.bank.domain.payment.PaymentStatus.PENDING;
        };
    }
}

// Supporting classes for the example

@lombok.Data
@lombok.Builder
class PaymentRequest {
    private String id;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String description;
}

@lombok.Data
@lombok.Builder
class PaymentResult {
    private boolean success;
    private String paymentId;
    private String externalTransactionId;
    private String message;
    
    public static PaymentResult success(UUID paymentId, String externalTransactionId) {
        return PaymentResult.builder()
                .success(true)
                .paymentId(paymentId.toString())
                .externalTransactionId(externalTransactionId)
                .build();
    }
    
    public static PaymentResult blocked(String reason) {
        return PaymentResult.builder()
                .success(false)
                .message(reason)
                .build();
    }
}

@lombok.Data
class BatchPaymentRequest {
    private List<PaymentRequest> payments;
}

@lombok.Data
class BatchPaymentResult {
    private final Map<String, PaymentResult> results = new HashMap<>();
    private final List<String> failures = new ArrayList<>();
    
    public void addResult(String paymentId, PaymentResult result) {
        results.put(paymentId, result);
    }
    
    public void addFailure(String paymentId, String reason) {
        failures.add(paymentId + ": " + reason);
    }
}

@lombok.Data
@lombok.Builder
class PaymentStatus {
    private String paymentId;
    private String status;
    private BigDecimal amount;
    private String externalReference;
    private Instant lastUpdated;
    
    public static PaymentStatus unknown() {
        return PaymentStatus.builder()
                .status("UNKNOWN")
                .build();
    }
}

interface ExternalPaymentGateway {
    ExternalPaymentResult processPayment(PaymentRequest request);
    ExternalPaymentStatus checkStatus(String transactionId);
    boolean ping();
}

@lombok.Data
@lombok.Builder
class ExternalPaymentResult {
    private String transactionId;
    private String status;
    private String message;
    
    public static ExternalPaymentResult queued(String requestId) {
        return ExternalPaymentResult.builder()
                .transactionId("QUEUED-" + requestId)
                .status("QUEUED")
                .message("Payment queued for offline processing")
                .build();
    }
}

@lombok.Data
class ExternalPaymentStatus {
    private String status;
    private String reference;
}

interface FraudDetectionService {
    boolean checkForFraud(PaymentRequest request);
}

interface NotificationService {
    void sendPaymentConfirmation(UUID customerId, BigDecimal amount, UUID paymentId);
}

class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String paymentId) {
        super("Payment not found: " + paymentId);
    }
}