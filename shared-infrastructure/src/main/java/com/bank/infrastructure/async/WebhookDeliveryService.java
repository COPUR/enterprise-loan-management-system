package com.bank.infrastructure.async;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Webhook Delivery Service for External System Integration
 * 
 * Provides reliable webhook delivery with:
 * - HMAC-SHA256 signature verification
 * - Exponential backoff retry logic
 * - Dead letter queue for failed deliveries
 * - Delivery status tracking
 * - Security headers and validation
 * - Async processing for performance
 */
@Service
@Transactional
public class WebhookDeliveryService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final WebhookConfigurationService configService;
    private final WebhookDeliveryRepository deliveryRepository;
    
    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(10);
    private static final Duration WEBHOOK_TIMEOUT = Duration.ofSeconds(30);
    
    // HMAC algorithm
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    public WebhookDeliveryService(WebClient.Builder webClientBuilder,
                                ObjectMapper objectMapper,
                                WebhookConfigurationService configService,
                                WebhookDeliveryRepository deliveryRepository) {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB limit
            .build();
        this.objectMapper = objectMapper;
        this.configService = configService;
        this.deliveryRepository = deliveryRepository;
    }
    
    /**
     * Deliver webhook event asynchronously
     */
    @Async("webhookExecutor")
    public CompletableFuture<WebhookDeliveryResult> deliverWebhookAsync(WebhookEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return deliverWebhook(event).block();
            } catch (Exception e) {
                return WebhookDeliveryResult.failed(event.getWebhookId(), e.getMessage());
            }
        });
    }
    
    /**
     * Deliver webhook event with retry logic
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 600000)
    )
    public Mono<WebhookDeliveryResult> deliverWebhook(WebhookEvent event) {
        String deliveryId = UUID.randomUUID().toString();
        
        return configService.getWebhookConfiguration(event.getTargetUrl())
            .flatMap(config -> {
                try {
                    // Create delivery record
                    WebhookDelivery delivery = createDeliveryRecord(deliveryId, event, config);
                    deliveryRepository.save(delivery);
                    
                    // Prepare webhook payload
                    WebhookPayload payload = createWebhookPayload(event);
                    String payloadJson = objectMapper.writeValueAsString(payload);
                    
                    // Generate signature
                    String signature = generateSignature(payloadJson, config.getSecret());
                    
                    // Send webhook
                    return sendWebhook(event.getTargetUrl(), payloadJson, signature, config)
                        .map(response -> {
                            // Update delivery status
                            delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
                            delivery.setDeliveredAt(Instant.now());
                            delivery.setHttpStatusCode(response.getStatusCode());
                            delivery.setResponseBody(response.getBody());
                            deliveryRepository.save(delivery);
                            
                            return WebhookDeliveryResult.success(deliveryId, response.getStatusCode());
                        })
                        .onErrorResume(error -> {
                            // Update delivery status on error
                            delivery.setStatus(WebhookDeliveryStatus.FAILED);
                            delivery.setFailedAt(Instant.now());
                            delivery.setErrorMessage(error.getMessage());
                            delivery.incrementRetryCount();
                            deliveryRepository.save(delivery);
                            
                            // Check if max retries reached
                            if (delivery.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                                return sendToDeadLetterQueue(event, error.getMessage())
                                    .then(Mono.just(WebhookDeliveryResult.failed(deliveryId, error.getMessage())));
                            }
                            
                            return Mono.error(error);
                        });
                        
                } catch (Exception e) {
                    return Mono.error(e);
                }
            })
            .retryWhen(createRetrySpec())
            .onErrorReturn(WebhookDeliveryResult.failed(deliveryId, "Max retries exceeded"));
    }
    
    /**
     * Send webhook HTTP request
     */
    private Mono<WebhookResponse> sendWebhook(String targetUrl, String payload, 
                                            String signature, WebhookConfiguration config) {
        return webClient.post()
            .uri(targetUrl)
            .header("Content-Type", "application/json")
            .header("X-Banking-Event-Type", extractEventType(payload))
            .header("X-Banking-Event-Id", extractEventId(payload))
            .header("X-Banking-Signature", signature)
            .header("X-Banking-Timestamp", Instant.now().toString())
            .header("X-Banking-Webhook-Id", config.getWebhookId())
            .header("User-Agent", "Banking-Platform-Webhook/1.0")
            .bodyValue(payload)
            .retrieve()
            .toEntity(String.class)
            .timeout(WEBHOOK_TIMEOUT)
            .map(response -> new WebhookResponse(
                response.getStatusCode().value(),
                response.getBody()
            ))
            .onErrorMap(this::mapWebhookError);
    }
    
    /**
     * Generate HMAC-SHA256 signature for webhook payload
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookException("Failed to generate webhook signature", e);
        }
    }
    
    /**
     * Create webhook payload from domain event
     */
    private WebhookPayload createWebhookPayload(WebhookEvent event) {
        return WebhookPayload.builder()
            .webhookId(UUID.randomUUID().toString())
            .eventType(event.getEventType())
            .data(event.getData())
            .timestamp(event.getTimestamp())
            .signature(null) // Will be set after payload generation
            .metadata(Map.of(
                "source", "banking-platform",
                "version", "1.0",
                "environment", getEnvironment()
            ))
            .build();
    }
    
    /**
     * Create delivery record for tracking
     */
    private WebhookDelivery createDeliveryRecord(String deliveryId, WebhookEvent event, 
                                               WebhookConfiguration config) {
        return WebhookDelivery.builder()
            .deliveryId(deliveryId)
            .webhookId(config.getWebhookId())
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .targetUrl(event.getTargetUrl())
            .status(WebhookDeliveryStatus.PENDING)
            .createdAt(Instant.now())
            .retryCount(0)
            .build();
    }
    
    /**
     * Create retry specification with exponential backoff
     */
    private RetryBackoffSpec createRetrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, INITIAL_BACKOFF)
            .maxBackoff(MAX_BACKOFF)
            .jitter(0.1)
            .filter(this::isRetryableError)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                new WebhookException("Webhook delivery failed after " + MAX_RETRY_ATTEMPTS + " attempts"));
    }
    
    /**
     * Check if error is retryable
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebhookException) {
            WebhookException webhookEx = (WebhookException) throwable;
            int statusCode = webhookEx.getStatusCode();
            
            // Retry on server errors and rate limiting
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        }
        
        // Retry on network errors
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof reactor.core.Exceptions.ReactiveException;
    }
    
    /**
     * Map various errors to WebhookException
     */
    private Throwable mapWebhookError(Throwable throwable) {
        if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            org.springframework.web.reactive.function.client.WebClientResponseException webEx = 
                (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
            return new WebhookException(
                "Webhook delivery failed: " + webEx.getMessage(),
                webEx.getStatusCode().value()
            );
        }
        
        return new WebhookException("Webhook delivery failed: " + throwable.getMessage());
    }
    
    /**
     * Send failed event to dead letter queue
     */
    private Mono<Void> sendToDeadLetterQueue(WebhookEvent event, String errorMessage) {
        // Implementation would send to DLQ (Kafka, SQS, etc.)
        // For now, just log the failure
        System.err.println("Webhook delivery failed permanently: " + event.getEventId() + " - " + errorMessage);
        return Mono.empty();
    }
    
    /**
     * Extract event type from payload for header
     */
    private String extractEventType(String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            return (String) payloadMap.get("eventType");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Extract event ID from payload for header
     */
    private String extractEventId(String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            return (String) payloadMap.get("webhookId");
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Get current environment
     */
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "development");
    }
    
    // DTOs and Value Objects
    
    @lombok.Builder
    @lombok.Data
    public static class WebhookEvent {
        private String webhookId;
        private String eventId;
        private String eventType;
        private String targetUrl;
        private Object data;
        private Instant timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class WebhookPayload {
        private String webhookId;
        private String eventType;
        private Object data;
        private Instant timestamp;
        private String signature;
        private Map<String, Object> metadata;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class WebhookResponse {
        private int statusCode;
        private String body;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class WebhookDelivery {
        private String deliveryId;
        private String webhookId;
        private String eventId;
        private String eventType;
        private String targetUrl;
        private WebhookDeliveryStatus status;
        private Instant createdAt;
        private Instant deliveredAt;
        private Instant failedAt;
        private int retryCount;
        private Integer httpStatusCode;
        private String responseBody;
        private String errorMessage;
        
        public void incrementRetryCount() {
            this.retryCount++;
        }
    }
    
    public enum WebhookDeliveryStatus {
        PENDING,
        DELIVERED,
        FAILED,
        DEAD_LETTER
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class WebhookDeliveryResult {
        private String deliveryId;
        private boolean success;
        private Integer statusCode;
        private String errorMessage;
        
        public static WebhookDeliveryResult success(String deliveryId, int statusCode) {
            return new WebhookDeliveryResult(deliveryId, true, statusCode, null);
        }
        
        public static WebhookDeliveryResult failed(String deliveryId, String errorMessage) {
            return new WebhookDeliveryResult(deliveryId, false, null, errorMessage);
        }
    }
    
    public static class WebhookException extends RuntimeException {
        private final int statusCode;
        
        public WebhookException(String message) {
            super(message);
            this.statusCode = 0;
        }
        
        public WebhookException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public WebhookException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = 0;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }
}

/**
 * Placeholder interfaces for dependencies
 */
interface WebhookConfigurationService {
    Mono<WebhookConfiguration> getWebhookConfiguration(String targetUrl);
}

interface WebhookDeliveryRepository {
    void save(WebhookDeliveryService.WebhookDelivery delivery);
}

@lombok.Builder
@lombok.Data
class WebhookConfiguration {
    private String webhookId;
    private String targetUrl;
    private String secret;
    private boolean active;
    private java.util.Set<String> eventTypes;
}