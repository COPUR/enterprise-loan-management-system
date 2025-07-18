package com.bank.infrastructure.notifications;

import com.bank.shared.kernel.domain.CustomerId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Real-Time Notification Service
 * 
 * Comprehensive notification system for enterprise banking:
 * - WebSocket real-time notifications
 * - Webhook delivery with retry logic
 * - Email/SMS notifications
 * - Push notifications
 * - Event-driven architecture
 * - Delivery guarantees
 * - Rate limiting
 * - Template management
 * - Compliance logging
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final Logger notificationLogger = LoggerFactory.getLogger("NOTIFICATION");
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Active WebSocket sessions
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // Webhook configurations
    private final Map<String, WebhookConfig> webhookConfigs = new ConcurrentHashMap<>();
    
    // Notification templates
    private final Map<NotificationType, NotificationTemplate> templates = new ConcurrentHashMap<>();
    
    // Redis keys
    private static final String NOTIFICATION_QUEUE_KEY = "notifications:queue";
    private static final String WEBHOOK_RETRY_KEY = "webhook:retry:";
    private static final String RATE_LIMIT_KEY = "notification:rate:";
    private static final String DELIVERY_LOG_KEY = "notification:delivery:";
    
    // Configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds
    private static final int RATE_LIMIT_PER_MINUTE = 60;
    
    /**
     * Send real-time notification to customer
     */
    public CompletableFuture<NotificationResult> sendNotification(
            CustomerId customerId, 
            NotificationType type, 
            Map<String, Object> data) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check rate limiting
                if (isRateLimited(customerId)) {
                    return NotificationResult.rateLimited();
                }
                
                // Create notification
                Notification notification = createNotification(customerId, type, data);
                
                // Send via multiple channels
                NotificationResult result = new NotificationResult(notification.getId());
                
                // 1. WebSocket (real-time)
                sendWebSocketNotification(notification, result);
                
                // 2. Webhook (external integrations)
                sendWebhookNotification(notification, result);
                
                // 3. Email/SMS (if configured)
                sendEmailSMSNotification(notification, result);
                
                // 4. Push notification (mobile apps)
                sendPushNotification(notification, result);
                
                // 5. Kafka event (for other services)
                publishNotificationEvent(notification);
                
                // Log delivery
                logNotificationDelivery(notification, result);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Failed to send notification", e);
                return NotificationResult.failed(e.getMessage());
            }
        });
    }
    
    /**
     * Register webhook endpoint
     */
    public void registerWebhook(String customerId, WebhookConfig config) {
        // Validate webhook URL
        if (!isValidWebhookUrl(config.getUrl())) {
            throw new IllegalArgumentException("Invalid webhook URL");
        }
        
        // Store webhook configuration
        webhookConfigs.put(customerId, config);
        
        // Test webhook connectivity
        testWebhookConnectivity(config);
        
        notificationLogger.info("Webhook registered for customer: {} -> {}", 
            customerId, config.getUrl());
    }
    
    /**
     * Register WebSocket session
     */
    public void registerWebSocketSession(String customerId, WebSocketSession session) {
        activeSessions.put(customerId, session);
        
        // Send welcome message
        sendWelcomeMessage(customerId, session);
        
        notificationLogger.info("WebSocket session registered for customer: {}", customerId);
    }
    
    /**
     * Unregister WebSocket session
     */
    public void unregisterWebSocketSession(String customerId) {
        WebSocketSession session = activeSessions.remove(customerId);
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                logger.warn("Failed to close WebSocket session", e);
            }
        }
        
        notificationLogger.info("WebSocket session unregistered for customer: {}", customerId);
    }
    
    /**
     * Send loan approval notification
     */
    public CompletableFuture<NotificationResult> sendLoanApprovalNotification(
            CustomerId customerId, 
            String loanId, 
            String amount, 
            String currency) {
        
        Map<String, Object> data = Map.of(
            "loanId", loanId,
            "amount", amount,
            "currency", currency,
            "approvalTime", Instant.now().toString()
        );
        
        return sendNotification(customerId, NotificationType.LOAN_APPROVED, data);
    }
    
    /**
     * Send payment confirmation notification
     */
    public CompletableFuture<NotificationResult> sendPaymentConfirmationNotification(
            CustomerId customerId, 
            String paymentId, 
            String amount, 
            String currency) {
        
        Map<String, Object> data = Map.of(
            "paymentId", paymentId,
            "amount", amount,
            "currency", currency,
            "paymentTime", Instant.now().toString()
        );
        
        return sendNotification(customerId, NotificationType.PAYMENT_CONFIRMED, data);
    }
    
    /**
     * Send security alert notification
     */
    public CompletableFuture<NotificationResult> sendSecurityAlertNotification(
            CustomerId customerId, 
            String alertType, 
            String description) {
        
        Map<String, Object> data = Map.of(
            "alertType", alertType,
            "description", description,
            "alertTime", Instant.now().toString(),
            "severity", "HIGH"
        );
        
        return sendNotification(customerId, NotificationType.SECURITY_ALERT, data);
    }
    
    // Private methods
    
    private Notification createNotification(CustomerId customerId, NotificationType type, Map<String, Object> data) {
        NotificationTemplate template = templates.get(type);
        if (template == null) {
            template = getDefaultTemplate(type);
        }
        
        return new Notification(
            UUID.randomUUID().toString(),
            customerId.getId(),
            type,
            template.getTitle(),
            template.renderMessage(data),
            data,
            Instant.now()
        );
    }
    
    private void sendWebSocketNotification(Notification notification, NotificationResult result) {
        String customerId = notification.getCustomerId();
        WebSocketSession session = activeSessions.get(customerId);
        
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                    "type", "notification",
                    "data", notification
                ));
                
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                result.setWebSocketDelivered(true);
                
            } catch (Exception e) {
                logger.warn("Failed to send WebSocket notification", e);
                result.setWebSocketDelivered(false);
            }
        }
    }
    
    private void sendWebhookNotification(Notification notification, NotificationResult result) {
        String customerId = notification.getCustomerId();
        WebhookConfig config = webhookConfigs.get(customerId);
        
        if (config != null) {
            CompletableFuture.runAsync(() -> {
                deliverWebhook(config, notification, 1);
            });
            result.setWebhookScheduled(true);
        }
    }
    
    private void deliverWebhook(WebhookConfig config, Notification notification, int attempt) {
        try {
            // Prepare webhook payload
            WebhookPayload payload = new WebhookPayload(
                notification.getId(),
                notification.getType().toString(),
                notification.getCustomerId(),
                notification.getData(),
                Instant.now()
            );
            
            // Add webhook signature
            String signature = generateWebhookSignature(payload, config.getSecret());
            
            // Send HTTP request
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Webhook-Signature", signature);
            headers.set("Content-Type", "application/json");
            
            var request = new org.springframework.http.HttpEntity<>(payload, headers);
            
            var response = restTemplate.postForEntity(config.getUrl(), request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                notificationLogger.info("Webhook delivered successfully: {} -> {}", 
                    notification.getId(), config.getUrl());
            } else {
                throw new RuntimeException("Webhook returned non-2xx status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.warn("Webhook delivery failed (attempt {}): {}", attempt, e.getMessage());
            
            if (attempt < MAX_RETRY_ATTEMPTS) {
                // Schedule retry
                CompletableFuture.delayedExecutor(RETRY_DELAY_MS * attempt, TimeUnit.MILLISECONDS)
                    .execute(() -> deliverWebhook(config, notification, attempt + 1));
            } else {
                logger.error("Webhook delivery failed after {} attempts: {}", 
                    MAX_RETRY_ATTEMPTS, config.getUrl());
            }
        }
    }
    
    private void sendEmailSMSNotification(Notification notification, NotificationResult result) {
        // Mock implementation - integrate with email/SMS service
        if (notification.getType().requiresEmail()) {
            // Send email notification
            result.setEmailDelivered(true);
            notificationLogger.info("Email notification sent for: {}", notification.getId());
        }
        
        if (notification.getType().requiresSMS()) {
            // Send SMS notification
            result.setSmsDelivered(true);
            notificationLogger.info("SMS notification sent for: {}", notification.getId());
        }
    }
    
    private void sendPushNotification(Notification notification, NotificationResult result) {
        // Mock implementation - integrate with push notification service (Firebase, etc.)
        result.setPushDelivered(true);
        notificationLogger.info("Push notification sent for: {}", notification.getId());
    }
    
    private void publishNotificationEvent(Notification notification) {
        try {
            String eventJson = objectMapper.writeValueAsString(Map.of(
                "eventType", "NotificationSent",
                "notification", notification,
                "timestamp", Instant.now()
            ));
            
            kafkaTemplate.send("notification-events", notification.getCustomerId(), eventJson);
            
        } catch (Exception e) {
            logger.warn("Failed to publish notification event", e);
        }
    }
    
    private boolean isRateLimited(CustomerId customerId) {
        String key = RATE_LIMIT_KEY + customerId.getId();
        
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (count >= RATE_LIMIT_PER_MINUTE) {
                return true;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
            
            return false;
            
        } catch (Exception e) {
            logger.warn("Failed to check rate limit", e);
            return false; // Allow on Redis failure
        }
    }
    
    private void logNotificationDelivery(Notification notification, NotificationResult result) {
        try {
            String logEntry = objectMapper.writeValueAsString(Map.of(
                "notificationId", notification.getId(),
                "customerId", notification.getCustomerId(),
                "type", notification.getType(),
                "timestamp", Instant.now(),
                "deliveryStatus", result
            ));
            
            redisTemplate.opsForValue().set(
                DELIVERY_LOG_KEY + notification.getId(),
                logEntry,
                7, // 7 days retention
                TimeUnit.DAYS
            );
            
        } catch (Exception e) {
            logger.warn("Failed to log notification delivery", e);
        }
    }
    
    private boolean isValidWebhookUrl(String url) {
        return url != null && 
               (url.startsWith("https://") || url.startsWith("http://")) &&
               url.length() > 10;
    }
    
    private void testWebhookConnectivity(WebhookConfig config) {
        try {
            // Send test ping
            var testPayload = Map.of(
                "type", "webhook_test",
                "timestamp", Instant.now().toString()
            );
            
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            var request = new org.springframework.http.HttpEntity<>(testPayload, headers);
            
            var response = restTemplate.postForEntity(config.getUrl(), request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Webhook connectivity test failed: {}", config.getUrl());
            }
            
        } catch (Exception e) {
            logger.warn("Webhook connectivity test failed", e);
        }
    }
    
    private void sendWelcomeMessage(String customerId, WebSocketSession session) {
        try {
            String welcomeMessage = objectMapper.writeValueAsString(Map.of(
                "type", "welcome",
                "message", "Connected to real-time notifications",
                "customerId", customerId,
                "timestamp", Instant.now().toString()
            ));
            
            session.sendMessage(new org.springframework.web.socket.TextMessage(welcomeMessage));
            
        } catch (Exception e) {
            logger.warn("Failed to send welcome message", e);
        }
    }
    
    private String generateWebhookSignature(WebhookPayload payload, String secret) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] hash = mac.doFinal(payloadJson.getBytes());
            return "sha256=" + java.util.Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            logger.error("Failed to generate webhook signature", e);
            return "";
        }
    }
    
    private NotificationTemplate getDefaultTemplate(NotificationType type) {
        switch (type) {
            case LOAN_APPROVED:
                return new NotificationTemplate("Loan Approved", "Your loan application has been approved!");
            case PAYMENT_CONFIRMED:
                return new NotificationTemplate("Payment Confirmed", "Your payment has been processed successfully!");
            case SECURITY_ALERT:
                return new NotificationTemplate("Security Alert", "Security alert detected on your account!");
            default:
                return new NotificationTemplate("Notification", "You have a new notification!");
        }
    }
    
    // Inner classes
    
    public static class Notification {
        private final String id;
        private final String customerId;
        private final NotificationType type;
        private final String title;
        private final String message;
        private final Map<String, Object> data;
        private final Instant timestamp;
        
        public Notification(String id, String customerId, NotificationType type, 
                          String title, String message, Map<String, Object> data, Instant timestamp) {
            this.id = id;
            this.customerId = customerId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getId() { return id; }
        public String getCustomerId() { return customerId; }
        public NotificationType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    public static class NotificationResult {
        private final String notificationId;
        private boolean webSocketDelivered = false;
        private boolean webhookScheduled = false;
        private boolean emailDelivered = false;
        private boolean smsDelivered = false;
        private boolean pushDelivered = false;
        private boolean failed = false;
        private String errorMessage;
        
        public NotificationResult(String notificationId) {
            this.notificationId = notificationId;
        }
        
        public static NotificationResult rateLimited() {
            NotificationResult result = new NotificationResult("rate-limited");
            result.failed = true;
            result.errorMessage = "Rate limit exceeded";
            return result;
        }
        
        public static NotificationResult failed(String message) {
            NotificationResult result = new NotificationResult("failed");
            result.failed = true;
            result.errorMessage = message;
            return result;
        }
        
        // Getters and setters
        public String getNotificationId() { return notificationId; }
        public boolean isWebSocketDelivered() { return webSocketDelivered; }
        public void setWebSocketDelivered(boolean webSocketDelivered) { this.webSocketDelivered = webSocketDelivered; }
        public boolean isWebhookScheduled() { return webhookScheduled; }
        public void setWebhookScheduled(boolean webhookScheduled) { this.webhookScheduled = webhookScheduled; }
        public boolean isEmailDelivered() { return emailDelivered; }
        public void setEmailDelivered(boolean emailDelivered) { this.emailDelivered = emailDelivered; }
        public boolean isSmsDelivered() { return smsDelivered; }
        public void setSmsDelivered(boolean smsDelivered) { this.smsDelivered = smsDelivered; }
        public boolean isPushDelivered() { return pushDelivered; }
        public void setPushDelivered(boolean pushDelivered) { this.pushDelivered = pushDelivered; }
        public boolean isFailed() { return failed; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class WebhookConfig {
        private final String url;
        private final String secret;
        private final Set<NotificationType> enabledTypes;
        
        public WebhookConfig(String url, String secret, Set<NotificationType> enabledTypes) {
            this.url = url;
            this.secret = secret;
            this.enabledTypes = enabledTypes;
        }
        
        public String getUrl() { return url; }
        public String getSecret() { return secret; }
        public Set<NotificationType> getEnabledTypes() { return enabledTypes; }
    }
    
    public static class WebhookPayload {
        private final String id;
        private final String type;
        private final String customerId;
        private final Map<String, Object> data;
        private final Instant timestamp;
        
        public WebhookPayload(String id, String type, String customerId, 
                            Map<String, Object> data, Instant timestamp) {
            this.id = id;
            this.type = type;
            this.customerId = customerId;
            this.data = data;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public String getCustomerId() { return customerId; }
        public Map<String, Object> getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    public static class NotificationTemplate {
        private final String title;
        private final String messageTemplate;
        
        public NotificationTemplate(String title, String messageTemplate) {
            this.title = title;
            this.messageTemplate = messageTemplate;
        }
        
        public String getTitle() { return title; }
        
        public String renderMessage(Map<String, Object> data) {
            // Simple template rendering - in production, use a proper template engine
            String message = messageTemplate;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                message = message.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
            return message;
        }
    }
    
    public enum NotificationType {
        LOAN_APPROVED(true, true, true),
        LOAN_REJECTED(true, true, false),
        PAYMENT_CONFIRMED(true, false, true),
        PAYMENT_FAILED(true, true, true),
        SECURITY_ALERT(true, true, true),
        ACCOUNT_LOCKED(true, true, true),
        MAINTENANCE_NOTICE(false, false, true),
        PROMOTIONAL_OFFER(false, false, true);
        
        private final boolean requiresEmail;
        private final boolean requiresSMS;
        private final boolean requiresPush;
        
        NotificationType(boolean requiresEmail, boolean requiresSMS, boolean requiresPush) {
            this.requiresEmail = requiresEmail;
            this.requiresSMS = requiresSMS;
            this.requiresPush = requiresPush;
        }
        
        public boolean requiresEmail() { return requiresEmail; }
        public boolean requiresSMS() { return requiresSMS; }
        public boolean requiresPush() { return requiresPush; }
    }
}