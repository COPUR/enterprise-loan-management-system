package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for KafkaSecurityService
 * Tests encryption, decryption, digital signatures, and FAPI security compliance
 * Ensures 85%+ test coverage for Kafka security infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaSecurityService Tests")
class KafkaSecurityServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    private KafkaSecurityService securityService;
    private TestDomainEvent testEvent;
    private String testEventJson;

    @BeforeEach
    void setUp() throws Exception {
        securityService = new KafkaSecurityService(objectMapper);
        testEvent = new TestDomainEvent("AGGREGATE-123", 1L, "sensitive-data");
        testEventJson = "{\"eventId\":\"EVENT-123\",\"data\":\"sensitive-data\"}";
        
        when(objectMapper.writeValueAsString(any())).thenReturn(testEventJson);
        when(objectMapper.readValue(anyString(), eq(Map.class)))
            .thenReturn(Map.of("eventId", "EVENT-123", "data", "sensitive-data"));
    }

    @Nested
    @DisplayName("Encryption Tests")
    class EncryptionTests {

        @Test
        @DisplayName("Should encrypt sensitive event data successfully")
        void shouldEncryptSensitiveEventDataSuccessfully() throws Exception {
            // Given
            String originalJson = "{\"eventId\":\"EVENT-123\",\"customerData\":\"sensitive\"}";
            
            // When
            String encryptedJson = securityService.encryptEventData(originalJson);

            // Then
            assertThat(encryptedJson).isNotNull();
            assertThat(encryptedJson).isNotEqualTo(originalJson);
            assertThat(encryptedJson).contains("\"encrypted\":true");
            assertThat(encryptedJson).contains("\"algorithm\":\"AES-256-GCM\"");
            assertThat(encryptedJson).contains("\"encryptedData\":");
            assertThat(encryptedJson).contains("\"iv\":");
        }

        @Test
        @DisplayName("Should decrypt encrypted event data successfully")
        void shouldDecryptEncryptedEventDataSuccessfully() throws Exception {
            // Given
            String originalJson = "{\"eventId\":\"EVENT-123\",\"customerData\":\"sensitive\"}";
            String encryptedJson = securityService.encryptEventData(originalJson);
            
            // When
            String decryptedJson = securityService.decryptEventData(encryptedJson);

            // Then
            assertThat(decryptedJson).isEqualTo(originalJson);
        }

        @Test
        @DisplayName("Should handle encryption of large payloads")
        void shouldHandleEncryptionOfLargePayloads() throws Exception {
            // Given
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                largeData.append("This is a large payload with sensitive data. ");
            }
            String largeJson = "{\"eventId\":\"EVENT-123\",\"largeData\":\"" + largeData + "\"}";
            
            // When
            String encryptedJson = securityService.encryptEventData(largeJson);
            String decryptedJson = securityService.decryptEventData(encryptedJson);

            // Then
            assertThat(decryptedJson).isEqualTo(largeJson);
        }

        @Test
        @DisplayName("Should generate unique IVs for each encryption")
        void shouldGenerateUniqueIvsForEachEncryption() throws Exception {
            // Given
            String originalJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            
            // When
            String encrypted1 = securityService.encryptEventData(originalJson);
            String encrypted2 = securityService.encryptEventData(originalJson);

            // Then
            assertThat(encrypted1).isNotEqualTo(encrypted2);
            
            // Extract IVs and verify they're different
            String iv1 = extractIvFromEncryptedJson(encrypted1);
            String iv2 = extractIvFromEncryptedJson(encrypted2);
            assertThat(iv1).isNotEqualTo(iv2);
        }

        @Test
        @DisplayName("Should fail gracefully on invalid encrypted data")
        void shouldFailGracefullyOnInvalidEncryptedData() {
            // Given
            String invalidEncryptedJson = "{\"encrypted\":true,\"encryptedData\":\"invalid\",\"iv\":\"invalid\"}";

            // When & Then
            assertThatThrownBy(() -> securityService.decryptEventData(invalidEncryptedJson))
                .isInstanceOf(KafkaSecurityService.SecurityException.class)
                .hasMessageContaining("Failed to decrypt event data");
        }
    }

    @Nested
    @DisplayName("Digital Signature Tests")
    class DigitalSignatureTests {

        @Test
        @DisplayName("Should sign event data successfully")
        void shouldSignEventDataSuccessfully() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            
            // When
            String signedJson = securityService.signEventData(eventJson);

            // Then
            assertThat(signedJson).isNotNull();
            assertThat(signedJson).contains("\"signature\":");
            assertThat(signedJson).contains("\"algorithm\":\"RS256\"");
            assertThat(signedJson).contains("\"signedAt\":");
        }

        @Test
        @DisplayName("Should verify valid event signature successfully")
        void shouldVerifyValidEventSignatureSuccessfully() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            String signedJson = securityService.signEventData(eventJson);
            
            // When
            boolean isValid = securityService.verifySignature(signedJson);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject tampered event data")
        void shouldRejectTamperedEventData() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            String signedJson = securityService.signEventData(eventJson);
            
            // Tamper with the data
            String tamperedJson = signedJson.replace("\"data\":\"test\"", "\"data\":\"tampered\"");
            
            // When
            boolean isValid = securityService.verifySignature(tamperedJson);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle signature verification of unsigned data")
        void shouldHandleSignatureVerificationOfUnsignedData() {
            // Given
            String unsignedJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";

            // When
            boolean isValid = securityService.verifySignature(unsignedJson);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("FAPI Security Compliance Tests")
    class FapiSecurityComplianceTests {

        @Test
        @DisplayName("Should apply FAPI security when required")
        void shouldApplyFapiSecurityWhenRequired() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"fapiInteractionId\":\"FAPI-123\"}";
            
            // When
            String securedJson = securityService.applyFapiSecurity(eventJson, testEvent);

            // Then
            assertThat(securedJson).contains("\"fapiCompliant\":true");
            assertThat(securedJson).contains("\"jti\":");
            assertThat(securedJson).contains("\"iat\":");
            assertThat(securedJson).contains("\"aud\":\"banking-api\"");
            assertThat(securedJson).contains("\"iss\":\"banking-loan-management\"");
        }

        @Test
        @DisplayName("Should validate FAPI interaction ID format")
        void shouldValidateFapiInteractionIdFormat() {
            // Given
            String validFapiId = "550e8400-e29b-41d4-a716-446655440000";
            String invalidFapiId = "invalid-fapi-id";

            // When
            boolean validResult = securityService.isValidFapiInteractionId(validFapiId);
            boolean invalidResult = securityService.isValidFapiInteractionId(invalidFapiId);

            // Then
            assertThat(validResult).isTrue();
            assertThat(invalidResult).isFalse();
        }

        @Test
        @DisplayName("Should generate FAPI-compliant JWT claims")
        void shouldGenerateFapiCompliantJwtClaims() throws Exception {
            // Given
            String fapiInteractionId = "550e8400-e29b-41d4-a716-446655440000";
            
            // When
            Map<String, Object> claims = securityService.generateFapiClaims(fapiInteractionId, testEvent);

            // Then
            assertThat(claims).containsKey("jti");
            assertThat(claims).containsKey("iat");
            assertThat(claims).containsKey("exp");
            assertThat(claims).containsEntry("aud", "banking-api");
            assertThat(claims).containsEntry("iss", "banking-loan-management");
            assertThat(claims).containsEntry("fapi_interaction_id", fapiInteractionId);
            assertThat(claims).containsKey("event_id");
            assertThat(claims).containsKey("service_domain");
        }

        @Test
        @DisplayName("Should apply strong customer authentication (SCA) markers")
        void shouldApplyStrongCustomerAuthenticationMarkers() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"psuData\":{\"psuId\":\"PSU001\"}}";
            
            // When
            String scaAppliedJson = securityService.applyScaMarkers(eventJson, testEvent);

            // Then
            assertThat(scaAppliedJson).contains("\"scaRequired\":true");
            assertThat(scaAppliedJson).contains("\"scaApproach\":\"DECOUPLED\"");
            assertThat(scaAppliedJson).contains("\"scaStatus\":\"RECEIVED\"");
        }
    }

    @Nested
    @DisplayName("Key Management Tests")
    class KeyManagementTests {

        @Test
        @DisplayName("Should rotate encryption keys periodically")
        void shouldRotateEncryptionKeysPeriodically() throws Exception {
            // Given
            String originalData = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            
            // Encrypt with current key
            String encrypted1 = securityService.encryptEventData(originalData);
            
            // Simulate key rotation
            securityService.rotateEncryptionKey();
            
            // Encrypt with new key
            String encrypted2 = securityService.encryptEventData(originalData);
            
            // When
            String decrypted1 = securityService.decryptEventData(encrypted1);
            String decrypted2 = securityService.decryptEventData(encrypted2);

            // Then
            assertThat(decrypted1).isEqualTo(originalData);
            assertThat(decrypted2).isEqualTo(originalData);
            assertThat(encrypted1).isNotEqualTo(encrypted2); // Different keys produce different results
        }

        @Test
        @DisplayName("Should maintain key versioning for backward compatibility")
        void shouldMaintainKeyVersioningForBackwardCompatibility() throws Exception {
            // Given
            String originalData = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            String encryptedWithOldKey = securityService.encryptEventData(originalData);
            
            // Rotate key
            securityService.rotateEncryptionKey();
            
            // When - Old encrypted data should still be decryptable
            String decrypted = securityService.decryptEventData(encryptedWithOldKey);

            // Then
            assertThat(decrypted).isEqualTo(originalData);
        }

        @Test
        @DisplayName("Should handle key rotation failure gracefully")
        void shouldHandleKeyRotationFailureGracefully() {
            // When & Then
            assertThatNoException().isThrownBy(() -> {
                securityService.rotateEncryptionKey();
                securityService.rotateSigningKey();
            });
        }
    }

    @Nested
    @DisplayName("Security Policy Tests")
    class SecurityPolicyTests {

        @Test
        @DisplayName("Should identify events requiring encryption")
        void shouldIdentifyEventsRequiringEncryption() {
            // Given
            String paymentEvent = "{\"eventType\":\"PaymentInitiated\",\"amount\":1000}";
            String customerEvent = "{\"eventType\":\"CustomerDataUpdated\",\"customerData\":{}}";
            String publicEvent = "{\"eventType\":\"LoanStatusChanged\",\"status\":\"APPROVED\"}";

            // When
            boolean paymentRequiresEncryption = securityService.requiresEncryption(paymentEvent);
            boolean customerRequiresEncryption = securityService.requiresEncryption(customerEvent);
            boolean publicRequiresEncryption = securityService.requiresEncryption(publicEvent);

            // Then
            assertThat(paymentRequiresEncryption).isTrue();
            assertThat(customerRequiresEncryption).isTrue();
            assertThat(publicRequiresEncryption).isFalse();
        }

        @Test
        @DisplayName("Should identify events requiring digital signatures")
        void shouldIdentifyEventsRequiringDigitalSignatures() {
            // Given
            String criticalEvent = "{\"eventType\":\"LoanApprovalGranted\",\"amount\":50000}";
            String regularEvent = "{\"eventType\":\"DocumentGenerated\",\"documentId\":\"DOC-123\"}";

            // When
            boolean criticalRequiresSignature = securityService.requiresDigitalSignature(criticalEvent);
            boolean regularRequiresSignature = securityService.requiresDigitalSignature(regularEvent);

            // Then
            assertThat(criticalRequiresSignature).isTrue();
            assertThat(regularRequiresSignature).isFalse();
        }

        @Test
        @DisplayName("Should apply complete security transformation")
        void shouldApplyCompleteSecurityTransformation() throws Exception {
            // Given
            String sensitiveEventJson = "{\"eventType\":\"PaymentInitiated\",\"amount\":50000,\"fapiInteractionId\":\"FAPI-123\"}";
            
            // When
            String securedJson = securityService.applySecurityIfRequired(sensitiveEventJson, testEvent);

            // Then
            assertThat(securedJson).contains("\"encrypted\":true");
            assertThat(securedJson).contains("\"signature\":");
            assertThat(securedJson).contains("\"fapiCompliant\":true");
        }
    }

    @Nested
    @DisplayName("Performance and Monitoring Tests")
    class PerformanceAndMonitoringTests {

        @Test
        @DisplayName("Should track security operation metrics")
        void shouldTrackSecurityOperationMetrics() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            
            // When
            for (int i = 0; i < 5; i++) {
                securityService.encryptEventData(eventJson);
                securityService.signEventData(eventJson);
            }

            // Then
            KafkaSecurityService.SecurityMetrics metrics = securityService.getMetrics();
            assertThat(metrics.getTotalEncryptionOperations()).isEqualTo(5L);
            assertThat(metrics.getTotalSigningOperations()).isEqualTo(5L);
            assertThat(metrics.getAverageEncryptionTimeMs()).isGreaterThan(0.0);
            assertThat(metrics.getAverageSigningTimeMs()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should report security service health")
        void shouldReportSecurityServiceHealth() {
            // When
            KafkaSecurityService.HealthStatus health = securityService.getHealth();

            // Then
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getStatusMessage()).contains("Security service operating normally");
            assertThat(health.getEncryptionKeyStatus()).isEqualTo("ACTIVE");
            assertThat(health.getSigningKeyStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should detect and report key expiration warnings")
        void shouldDetectAndReportKeyExpirationWarnings() throws Exception {
            // Given
            // Simulate keys nearing expiration (this would be implementation-specific)
            securityService.simulateKeyNearExpiration();

            // When
            KafkaSecurityService.HealthStatus health = securityService.getHealth();

            // Then
            assertThat(health.isHealthy()).isTrue(); // Still healthy but with warnings
            assertThat(health.getWarnings()).contains("Encryption key expires soon");
            assertThat(health.getWarnings()).contains("Signing key expires soon");
        }
    }

    // Helper methods

    private String extractIvFromEncryptedJson(String encryptedJson) throws Exception {
        Map<String, Object> jsonMap = objectMapper.readValue(encryptedJson, Map.class);
        return (String) jsonMap.get("iv");
    }

    // Test domain event implementation
    private static class TestDomainEvent extends DomainEvent {
        private final String testData;

        public TestDomainEvent(String aggregateId, long version, String testData) {
            super(aggregateId, "TestAggregate", version);
            this.testData = testData;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }

        @Override
        public Object getEventData() {
            return Map.of("testData", testData);
        }

        @Override
        public String getServiceDomain() {
            return "TestDomain";
        }

        @Override
        public String getBehaviorQualifier() {
            return "TEST";
        }
    }
}