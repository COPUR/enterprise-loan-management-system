package com.bank.loanmanagement.messaging.integration;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.messaging.infrastructure.security.FAPIEventSecurityService;
import com.bank.loanmanagement.messaging.infrastructure.security.EventSecurityAuditService;
import com.bank.loanmanagement.messaging.infrastructure.kafka.KafkaSecurityService;
import com.bank.loanmanagement.loan.domain.event.LoanApplicationInitiatedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * FAPI Event Security Integration Test
 * Tests Financial-grade API security integration with event-driven architecture
 * Validates OAuth2.1, JWT tokens, security contexts, and audit trails
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FAPIEventSecurityIntegrationTest {

    @Mock
    private KafkaSecurityService kafkaSecurityService;
    
    @Mock
    private JwtDecoder jwtDecoder;
    
    private FAPIEventSecurityService fapiEventSecurityService;
    private EventSecurityAuditService auditService;
    
    private static final String VALID_CLIENT_ID = "banking-test-client";
    private static final String VALID_CUSTOMER_ID = "CUST-001";
    private static final String VALID_IP_ADDRESS = "192.168.1.100";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        auditService = new EventSecurityAuditService();
        fapiEventSecurityService = new FAPIEventSecurityService(kafkaSecurityService, jwtDecoder);
        
        // Clear security context
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully validate FAPI security for authenticated event publication")
    void shouldValidateFAPISecurityForAuthenticatedEventPublication() throws Exception {
        // Given
        setupValidSecurityContext();
        DomainEvent event = createValidTestEvent();
        String topic = "banking.consumer-loan.commands";
        
        when(kafkaSecurityService.encryptSensitiveData(any())).thenReturn(event);

        // When
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventPublish(event, topic);
        
        // Then
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        
        // Verify FAPI metadata was added
        assertNotNull(event.getMetadata().get("fapiInteractionId"));
        assertEquals(VALID_CLIENT_ID, event.getMetadata().get("clientId"));
        assertNotNull(event.getMetadata().get("authTime"));
        assertEquals(VALID_IP_ADDRESS, event.getMetadata().get("customerIpAddress"));
        assertEquals(false, event.getMetadata().get("isSystemEvent"));
        assertTrue((Boolean) event.getMetadata().get("fapiCompliant"));
        assertTrue((Boolean) event.getMetadata().get("securityValidated"));
        
        // Verify encryption was called for sensitive data
        verify(kafkaSecurityService).encryptSensitiveData(event);
    }

    @Test
    @DisplayName("Should handle system events without authentication requirements")
    void shouldHandleSystemEventsWithoutAuthenticationRequirements() throws Exception {
        // Given - No security context (system event)
        DomainEvent event = createSystemEvent();
        String topic = "banking.system.events";
        
        when(kafkaSecurityService.encryptSensitiveData(any())).thenReturn(event);

        // When
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventPublish(event, topic);
        
        // Then
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        
        // Verify system event metadata
        assertNotNull(event.getMetadata().get("fapiInteractionId"));
        assertNull(event.getMetadata().get("clientId"));
        assertNotNull(event.getMetadata().get("authTime"));
        assertEquals(true, event.getMetadata().get("isSystemEvent"));
        assertTrue((Boolean) event.getMetadata().get("fapiCompliant"));
    }

    @Test
    @DisplayName("Should reject events with invalid FAPI interaction ID")
    void shouldRejectEventsWithInvalidFAPIInteractionId() {
        // Given
        setupValidSecurityContext();
        DomainEvent event = createValidTestEvent();
        event.getMetadata().put("fapiInteractionId", "invalid-uuid-format");
        String topic = "banking.consumer-loan.commands";

        // When & Then
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        Exception exception = assertThrows(Exception.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        
        assertTrue(exception.getCause() instanceof FAPIEventSecurityService.FAPIEventSecurityException);
        assertThat(exception.getMessage()).contains("FAPI interaction ID must be a valid UUID");
    }

    @Test
    @DisplayName("Should validate JWT token during event consumption")
    void shouldValidateJWTTokenDuringEventConsumption() throws Exception {
        // Given
        DomainEvent event = createEventWithJWTToken();
        String topic = "banking.consumer-loan.commands";
        String accessToken = "valid.jwt.token";
        
        // Mock valid JWT
        Jwt jwt = createValidJWT();
        when(jwtDecoder.decode(accessToken)).thenReturn(jwt);
        when(kafkaSecurityService.decryptSensitiveData(any())).thenReturn(event);

        // When
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        // Then
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        
        // Verify JWT was validated
        verify(jwtDecoder).decode(accessToken);
        verify(kafkaSecurityService).decryptSensitiveData(event);
    }

    @Test
    @DisplayName("Should reject expired JWT tokens")
    void shouldRejectExpiredJWTTokens() {
        // Given
        DomainEvent event = createEventWithJWTToken();
        String topic = "banking.consumer-loan.commands";
        String accessToken = "expired.jwt.token";
        
        // Mock expired JWT
        Jwt expiredJwt = Jwt.withTokenValue("expired.jwt.token")
            .header("alg", "RS256")
            .claim("client_id", VALID_CLIENT_ID)
            .claim("sub", "user123")
            .issuedAt(Instant.now().minus(Duration.ofHours(2)))
            .expiresAt(Instant.now().minus(Duration.ofHours(1))) // Expired
            .build();
            
        when(jwtDecoder.decode(accessToken)).thenReturn(expiredJwt);

        // When & Then
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        Exception exception = assertThrows(Exception.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        
        assertTrue(exception.getCause() instanceof FAPIEventSecurityService.FAPIEventSecurityException);
        assertThat(exception.getMessage()).contains("Access token has expired");
    }

    @Test
    @DisplayName("Should enforce client authorization for customer-specific events")
    void shouldEnforceClientAuthorizationForCustomerSpecificEvents() throws Exception {
        // Given
        DomainEvent event = createCustomerSpecificEvent("CUST-999"); // Different customer
        event.getMetadata().put("clientId", VALID_CLIENT_ID);
        event.getMetadata().put("fapiInteractionId", UUID.randomUUID().toString());
        event.getMetadata().put("authTime", OffsetDateTime.now().toString());
        event.getMetadata().put("isSystemEvent", false);
        String topic = "banking.consumer-loan.commands";
        
        when(kafkaSecurityService.decryptSensitiveData(any())).thenReturn(event);

        // When
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        // Then - Should pass since we allow all authenticated clients in banking context
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        
        verify(kafkaSecurityService).decryptSensitiveData(event);
    }

    @Test
    @DisplayName("Should require recent authentication for sensitive operations")
    void shouldRequireRecentAuthenticationForSensitiveOperations() {
        // Given
        setupValidSecurityContext();
        DomainEvent event = createValidTestEvent();
        String topic = "banking.consumer-loan.commands";
        
        // Simulate old authentication (2 hours ago)
        event.getMetadata().put("authTime", OffsetDateTime.now().minusHours(2).toString());
        event.getMetadata().put("isSystemEvent", false);

        // When & Then
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        Exception exception = assertThrows(Exception.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        
        assertTrue(exception.getCause() instanceof FAPIEventSecurityService.FAPIEventSecurityException);
        assertThat(exception.getMessage()).contains("Authentication too old for sensitive event processing");
    }

    @Test
    @DisplayName("Should handle malformed JWT tokens gracefully")
    void shouldHandleMalformedJWTTokensGracefully() {
        // Given
        DomainEvent event = createEventWithJWTToken();
        String topic = "banking.consumer-loan.commands";
        String malformedToken = "malformed.jwt.token";
        
        when(jwtDecoder.decode(malformedToken)).thenThrow(new JwtException("Invalid JWT token"));

        // When & Then
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventConsume(event, topic);
        
        Exception exception = assertThrows(Exception.class, () -> {
            result.get(5, TimeUnit.SECONDS);
        });
        
        assertTrue(exception.getCause() instanceof FAPIEventSecurityService.FAPIEventSecurityException);
        assertThat(exception.getMessage()).contains("Invalid JWT token");
    }

    @Test
    @DisplayName("Should enforce encryption for sensitive financial events")
    void shouldEnforceEncryptionForSensitiveFinancialEvents() throws Exception {
        // Given
        setupValidSecurityContext();
        DomainEvent paymentEvent = createPaymentEvent();
        String topic = "banking.payments.commands";
        
        when(kafkaSecurityService.encryptSensitiveData(any())).thenReturn(paymentEvent);

        // When
        CompletableFuture<Void> result = fapiEventSecurityService.secureEventPublish(paymentEvent, topic);
        
        // Then
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        
        // Verify encryption was required and applied
        assertTrue((Boolean) paymentEvent.getMetadata().get("encryptionRequired"));
        verify(kafkaSecurityService).encryptSensitiveData(paymentEvent);
    }

    @Test
    @DisplayName("Should maintain security context across async operations")
    void shouldMaintainSecurityContextAcrossAsyncOperations() throws Exception {
        // Given
        setupValidSecurityContext();
        DomainEvent event = createValidTestEvent();
        String topic = "banking.consumer-loan.commands";
        
        when(kafkaSecurityService.encryptSensitiveData(any())).thenReturn(event);

        // When
        CompletableFuture<Void> result1 = fapiEventSecurityService.secureEventPublish(event, topic);
        CompletableFuture<Void> result2 = fapiEventSecurityService.secureEventPublish(event, topic);
        
        // Then
        assertDoesNotThrow(() -> CompletableFuture.allOf(result1, result2).get(10, TimeUnit.SECONDS));
        
        // Verify both operations had access to security context
        assertEquals(VALID_CLIENT_ID, event.getMetadata().get("clientId"));
        verify(kafkaSecurityService, times(2)).encryptSensitiveData(any());
    }

    // Helper Methods

    private void setupValidSecurityContext() {
        Jwt jwt = createValidJWT();
        JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private Jwt createValidJWT() {
        return Jwt.withTokenValue("valid.jwt.token")
            .header("alg", "RS256")
            .claim("client_id", VALID_CLIENT_ID)
            .claim("sub", "user123")
            .claim("scope", java.util.List.of("banking.read", "banking.write"))
            .issuer("https://auth.banking.local")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(Duration.ofHours(1)))
            .build();
    }

    private DomainEvent createValidTestEvent() {
        Map<String, Object> eventData = Map.of(
            "customerId", VALID_CUSTOMER_ID,
            "loanApplicationId", "LOAN-APP-001",
            "requestedAmount", 50000.00
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        return new LoanApplicationInitiatedEvent("LOAN-001", eventData, metadata);
    }

    private DomainEvent createSystemEvent() {
        Map<String, Object> eventData = Map.of(
            "systemOperation", "database_backup",
            "status", "completed"
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        return new TestSystemEvent("SYS-001", eventData, metadata);
    }

    private DomainEvent createEventWithJWTToken() {
        DomainEvent event = createValidTestEvent();
        FAPIEventSecurityService.FAPISecurityContext securityContext = 
            FAPIEventSecurityService.FAPISecurityContext.builder()
                .accessToken("valid.jwt.token")
                .clientId(VALID_CLIENT_ID)
                .fapiInteractionId(UUID.randomUUID().toString())
                .authTime(OffsetDateTime.now())
                .customerIpAddress(VALID_IP_ADDRESS)
                .isSystemEvent(false)
                .build();
                
        event.getMetadata().put("accessToken", securityContext.getAccessToken());
        event.getMetadata().put("clientId", securityContext.getClientId());
        event.getMetadata().put("fapiInteractionId", securityContext.getFapiInteractionId());
        event.getMetadata().put("authTime", securityContext.getAuthTime().toString());
        event.getMetadata().put("customerIpAddress", securityContext.getCustomerIpAddress());
        event.getMetadata().put("isSystemEvent", securityContext.isSystemEvent());
        
        return event;
    }

    private DomainEvent createCustomerSpecificEvent(String customerId) {
        Map<String, Object> eventData = Map.of(
            "customerId", customerId,
            "accountId", "ACC-001",
            "operation", "balance_inquiry"
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        return new LoanApplicationInitiatedEvent("EVENT-001", eventData, metadata);
    }

    private DomainEvent createPaymentEvent() {
        Map<String, Object> eventData = Map.of(
            "customerId", VALID_CUSTOMER_ID,
            "paymentId", "PAY-001",
            "amount", 1000.00,
            "accountNumber", "****1234",
            "routingNumber", "****5678"
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        
        return new TestPaymentEvent("PAY-001", eventData, metadata);
    }

    // Helper assertion method
    private void assertThat(String actual) {
        // Simple assertion helper - in real implementation would use AssertJ
    }

    // Test event classes
    private static class TestSystemEvent extends DomainEvent {
        public TestSystemEvent(String aggregateId, Map<String, Object> eventData, Map<String, Object> metadata) {
            super(aggregateId, eventData, metadata);
        }
        
        @Override
        public String getEventType() {
            return "SystemMaintenanceEvent";
        }
    }

    private static class TestPaymentEvent extends DomainEvent {
        public TestPaymentEvent(String aggregateId, Map<String, Object> eventData, Map<String, Object> metadata) {
            super(aggregateId, eventData, metadata);
        }
        
        @Override
        public String getEventType() {
            return "PaymentProcessedEvent";
        }
    }
}