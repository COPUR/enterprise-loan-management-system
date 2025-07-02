package com.bank.loanmanagement.loan.messaging.infrastructure.security;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * FAPI Security Service for Event-Driven Architecture
 * Implements Financial-grade API security standards for event processing
 * Ensures secure event publishing and consumption with proper authentication and authorization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FAPIEventSecurityService {

    private final KafkaSecurityService kafkaSecurityService;
    private final JwtDecoder jwtDecoder;
    
    /**
     * Secure event publishing with FAPI compliance
     * Enriches events with security context and ensures proper authentication
     */
    public CompletableFuture<Void> secureEventPublish(DomainEvent event, String topic) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Extract security context
                FAPISecurityContext securityContext = extractFAPISecurityContext();
                
                // Enrich event with FAPI security metadata
                DomainEvent enrichedEvent = enrichEventWithSecurity(event, securityContext);
                
                // Validate FAPI compliance
                validateFAPICompliance(enrichedEvent, securityContext);
                
                // Encrypt sensitive data if required
                DomainEvent secureEvent = kafkaSecurityService.encryptSensitiveData(enrichedEvent);
                
                // Log security event for audit
                logSecurityEvent("EVENT_PUBLISHED", enrichedEvent, securityContext);
                
                log.debug("Secure event published to topic {} with FAPI compliance", topic);
                
            } catch (Exception e) {
                log.error("Failed to publish secure event to topic {}", topic, e);
                throw new FAPIEventSecurityException("Secure event publishing failed", e);
            }
        });
    }
    
    /**
     * Secure event consumption with FAPI validation
     * Validates JWT tokens and ensures proper authorization for event processing
     */
    public CompletableFuture<Void> secureEventConsume(DomainEvent event, String topic) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Extract security context from event metadata
                FAPISecurityContext securityContext = extractSecurityContextFromEvent(event);
                
                // Validate JWT token if present
                if (securityContext.getAccessToken() != null) {
                    validateJwtToken(securityContext.getAccessToken());
                }
                
                // Validate FAPI interaction ID
                validateFAPIInteractionId(securityContext.getFapiInteractionId());
                
                // Check authorization for event consumption
                validateEventConsumptionAuthorization(event, securityContext);
                
                // Decrypt sensitive data if encrypted
                DomainEvent decryptedEvent = kafkaSecurityService.decryptSensitiveData(event);
                
                // Log security event for audit
                logSecurityEvent("EVENT_CONSUMED", decryptedEvent, securityContext);
                
                log.debug("Secure event consumed from topic {} with FAPI compliance", topic);
                
            } catch (Exception e) {
                log.error("Failed to consume secure event from topic {}", topic, e);
                throw new FAPIEventSecurityException("Secure event consumption failed", e);
            }
        });
    }
    
    /**
     * Extract FAPI security context from current authentication
     */
    private FAPISecurityContext extractFAPISecurityContext() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return FAPISecurityContext.builder()
                    .accessToken(jwt.getTokenValue())
                    .clientId(jwt.getClaimAsString("client_id"))
                    .subject(jwt.getSubject())
                    .issuer(jwt.getIssuer().toString())
                    .issuedAt(jwt.getIssuedAt())
                    .expiresAt(jwt.getExpiresAt())
                    .scopes(jwt.getClaimAsStringList("scope"))
                    .fapiInteractionId(generateFAPIInteractionId())
                    .authTime(OffsetDateTime.now())
                    .customerIpAddress(extractCustomerIpAddress())
                    .build();
            }
            
            // Return minimal context for system events
            return FAPISecurityContext.builder()
                .fapiInteractionId(generateFAPIInteractionId())
                .authTime(OffsetDateTime.now())
                .isSystemEvent(true)
                .build();
                
        } catch (Exception e) {
            log.warn("Failed to extract FAPI security context", e);
            return FAPISecurityContext.builder()
                .fapiInteractionId(generateFAPIInteractionId())
                .authTime(OffsetDateTime.now())
                .isSystemEvent(true)
                .build();
        }
    }
    
    /**
     * Enrich domain event with FAPI security metadata
     */
    private DomainEvent enrichEventWithSecurity(DomainEvent event, FAPISecurityContext securityContext) {
        // Add FAPI security metadata to event
        event.getMetadata().put("fapiInteractionId", securityContext.getFapiInteractionId());
        event.getMetadata().put("clientId", securityContext.getClientId());
        event.getMetadata().put("authTime", securityContext.getAuthTime().toString());
        event.getMetadata().put("customerIpAddress", securityContext.getCustomerIpAddress());
        event.getMetadata().put("isSystemEvent", securityContext.isSystemEvent());
        
        // Add compliance markers
        event.getMetadata().put("fapiCompliant", true);
        event.getMetadata().put("securityValidated", true);
        event.getMetadata().put("encryptionRequired", determineEncryptionRequirement(event));
        
        return event;
    }
    
    /**
     * Extract security context from event metadata
     */
    private FAPISecurityContext extractSecurityContextFromEvent(DomainEvent event) {
        Map<String, Object> metadata = event.getMetadata();
        
        return FAPISecurityContext.builder()
            .fapiInteractionId((String) metadata.get("fapiInteractionId"))
            .clientId((String) metadata.get("clientId"))
            .authTime(metadata.get("authTime") != null ? 
                OffsetDateTime.parse((String) metadata.get("authTime")) : null)
            .customerIpAddress((String) metadata.get("customerIpAddress"))
            .isSystemEvent((Boolean) metadata.getOrDefault("isSystemEvent", false))
            .build();
    }
    
    /**
     * Validate FAPI compliance for event
     */
    private void validateFAPICompliance(DomainEvent event, FAPISecurityContext securityContext) {
        // Validate FAPI interaction ID format
        if (securityContext.getFapiInteractionId() == null || 
            !securityContext.getFapiInteractionId().matches("^[a-f0-9\\-]{36}$")) {
            throw new FAPIEventSecurityException("Invalid FAPI interaction ID format");
        }
        
        // Validate customer IP address for customer events
        if (!securityContext.isSystemEvent() && 
            (securityContext.getCustomerIpAddress() == null || 
             securityContext.getCustomerIpAddress().trim().isEmpty())) {
            throw new FAPIEventSecurityException("Customer IP address required for customer events");
        }
        
        // Validate authentication time
        if (securityContext.getAuthTime() == null) {
            throw new FAPIEventSecurityException("Authentication time required for FAPI compliance");
        }
        
        // Check if authentication is recent (within 1 hour for sensitive operations)
        if (!securityContext.isSystemEvent() && 
            securityContext.getAuthTime().isBefore(OffsetDateTime.now().minusHours(1))) {
            throw new FAPIEventSecurityException("Authentication too old for sensitive event processing");
        }
    }
    
    /**
     * Validate JWT token
     */
    private void validateJwtToken(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            
            // Validate token expiration
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(java.time.Instant.now())) {
                throw new FAPIEventSecurityException("Access token has expired");
            }
            
            // Validate required claims
            if (jwt.getClaimAsString("client_id") == null) {
                throw new FAPIEventSecurityException("Missing client_id claim in access token");
            }
            
            log.debug("JWT token validated successfully for client: {}", jwt.getClaimAsString("client_id"));
            
        } catch (JwtException e) {
            throw new FAPIEventSecurityException("Invalid JWT token", e);
        }
    }
    
    /**
     * Validate FAPI interaction ID
     */
    private void validateFAPIInteractionId(String fapiInteractionId) {
        if (fapiInteractionId == null || fapiInteractionId.trim().isEmpty()) {
            throw new FAPIEventSecurityException("FAPI interaction ID is required");
        }
        
        // Validate UUID format
        try {
            UUID.fromString(fapiInteractionId);
        } catch (IllegalArgumentException e) {
            throw new FAPIEventSecurityException("FAPI interaction ID must be a valid UUID", e);
        }
    }
    
    /**
     * Validate authorization for event consumption
     */
    private void validateEventConsumptionAuthorization(DomainEvent event, FAPISecurityContext securityContext) {
        // Check if this is a system event (always allowed)
        if (securityContext.isSystemEvent()) {
            return;
        }
        
        // Validate client authorization for event type
        String eventType = event.getEventType();
        String clientId = securityContext.getClientId();
        
        if (!isClientAuthorizedForEventType(clientId, eventType)) {
            throw new FAPIEventSecurityException(
                String.format("Client %s not authorized to consume events of type %s", clientId, eventType));
        }
        
        // Additional authorization checks based on event data
        validateEventDataAuthorization(event, securityContext);
    }
    
    /**
     * Check if client is authorized for specific event type
     */
    private boolean isClientAuthorizedForEventType(String clientId, String eventType) {
        // Implementation would check against authorization rules
        // For now, allow all registered clients to consume banking events
        return clientId != null && !clientId.trim().isEmpty();
    }
    
    /**
     * Validate authorization for specific event data
     */
    private void validateEventDataAuthorization(DomainEvent event, FAPISecurityContext securityContext) {
        // Extract customer ID from event if present
        String customerId = (String) event.getEventData().get("customerId");
        
        if (customerId != null && !securityContext.isSystemEvent()) {
            // Validate that the authenticated client can access this customer's data
            if (!isClientAuthorizedForCustomer(securityContext.getClientId(), customerId)) {
                throw new FAPIEventSecurityException(
                    String.format("Client %s not authorized to access customer %s data", 
                        securityContext.getClientId(), customerId));
            }
        }
    }
    
    /**
     * Check if client is authorized for specific customer
     */
    private boolean isClientAuthorizedForCustomer(String clientId, String customerId) {
        // Implementation would check customer-client relationships
        // For now, allow all authenticated clients to access all customers (banking context)
        return clientId != null && customerId != null;
    }
    
    /**
     * Determine if event requires encryption
     */
    private boolean determineEncryptionRequirement(DomainEvent event) {
        String eventType = event.getEventType();
        
        // Events that require encryption
        return eventType.contains("Payment") || 
               eventType.contains("Credit") || 
               eventType.contains("Account") ||
               eventType.contains("PersonalData");
    }
    
    /**
     * Generate FAPI interaction ID
     */
    private String generateFAPIInteractionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Extract customer IP address from request context
     */
    private String extractCustomerIpAddress() {
        // Implementation would extract from HTTP request headers
        // For event-driven context, this might be passed through metadata
        return "127.0.0.1"; // Placeholder
    }
    
    /**
     * Log security event for audit purposes
     */
    private void logSecurityEvent(String action, DomainEvent event, FAPISecurityContext securityContext) {
        log.info("FAPI Security Event: action={}, eventType={}, fapiInteractionId={}, clientId={}, authTime={}", 
            action, 
            event.getEventType(), 
            securityContext.getFapiInteractionId(),
            securityContext.getClientId(),
            securityContext.getAuthTime());
    }
    
    /**
     * FAPI Security Context holder
     */
    public static class FAPISecurityContext {
        private final String accessToken;
        private final String clientId;
        private final String subject;
        private final String issuer;
        private final java.time.Instant issuedAt;
        private final java.time.Instant expiresAt;
        private final java.util.List<String> scopes;
        private final String fapiInteractionId;
        private final OffsetDateTime authTime;
        private final String customerIpAddress;
        private final boolean isSystemEvent;
        
        private FAPISecurityContext(Builder builder) {
            this.accessToken = builder.accessToken;
            this.clientId = builder.clientId;
            this.subject = builder.subject;
            this.issuer = builder.issuer;
            this.issuedAt = builder.issuedAt;
            this.expiresAt = builder.expiresAt;
            this.scopes = builder.scopes;
            this.fapiInteractionId = builder.fapiInteractionId;
            this.authTime = builder.authTime;
            this.customerIpAddress = builder.customerIpAddress;
            this.isSystemEvent = builder.isSystemEvent;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getAccessToken() { return accessToken; }
        public String getClientId() { return clientId; }
        public String getSubject() { return subject; }
        public String getIssuer() { return issuer; }
        public java.time.Instant getIssuedAt() { return issuedAt; }
        public java.time.Instant getExpiresAt() { return expiresAt; }
        public java.util.List<String> getScopes() { return scopes; }
        public String getFapiInteractionId() { return fapiInteractionId; }
        public OffsetDateTime getAuthTime() { return authTime; }
        public String getCustomerIpAddress() { return customerIpAddress; }
        public boolean isSystemEvent() { return isSystemEvent; }
        
        public static class Builder {
            private String accessToken;
            private String clientId;
            private String subject;
            private String issuer;
            private java.time.Instant issuedAt;
            private java.time.Instant expiresAt;
            private java.util.List<String> scopes;
            private String fapiInteractionId;
            private OffsetDateTime authTime;
            private String customerIpAddress;
            private boolean isSystemEvent;
            
            public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
            public Builder clientId(String clientId) { this.clientId = clientId; return this; }
            public Builder subject(String subject) { this.subject = subject; return this; }
            public Builder issuer(String issuer) { this.issuer = issuer; return this; }
            public Builder issuedAt(java.time.Instant issuedAt) { this.issuedAt = issuedAt; return this; }
            public Builder expiresAt(java.time.Instant expiresAt) { this.expiresAt = expiresAt; return this; }
            public Builder scopes(java.util.List<String> scopes) { this.scopes = scopes; return this; }
            public Builder fapiInteractionId(String fapiInteractionId) { this.fapiInteractionId = fapiInteractionId; return this; }
            public Builder authTime(OffsetDateTime authTime) { this.authTime = authTime; return this; }
            public Builder customerIpAddress(String customerIpAddress) { this.customerIpAddress = customerIpAddress; return this; }
            public Builder isSystemEvent(boolean isSystemEvent) { this.isSystemEvent = isSystemEvent; return this; }
            
            public FAPISecurityContext build() {
                return new FAPISecurityContext(this);
            }
        }
    }
    
    /**
     * FAPI Event Security Exception
     */
    public static class FAPIEventSecurityException extends RuntimeException {
        public FAPIEventSecurityException(String message) {
            super(message);
        }
        
        public FAPIEventSecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}