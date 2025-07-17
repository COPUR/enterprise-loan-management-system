package com.bank.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for FAPI Security Validator
 * 
 * Tests FAPI 1.0 Advanced Security Requirements:
 * - SR-001: FAPI header validation (X-FAPI-Interaction-ID, X-FAPI-Auth-Date, X-FAPI-Customer-IP-Address)
 * - SR-002: Request signature validation (JWS with HMAC-SHA256)
 * - SR-003: Response signature generation
 * - SR-004: TLS validation for FAPI endpoints
 * - SR-005: FAPI-compliant error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FAPI Security Validator Tests")
class FAPISecurityValidatorTest {
    
    private FAPISecurityValidator fapiSecurityValidator;
    private static final String VALID_SIGNING_KEY = "test-signing-key-for-hmac-256";
    
    @BeforeEach
    void setUp() {
        fapiSecurityValidator = new FAPISecurityValidator(VALID_SIGNING_KEY);
    }
    
    @Test
    @DisplayName("SR-001: Should validate FAPI interaction ID successfully")
    void shouldValidateFAPIInteractionIDSuccessfully() {
        // Given
        String validInteractionId = "550e8400-e29b-41d4-a716-446655440000";
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateInteractionId(validInteractionId);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-001: Should reject invalid FAPI interaction ID")
    void shouldRejectInvalidFAPIInteractionID() {
        // Given
        String invalidInteractionId = "invalid-id";
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateInteractionId(invalidInteractionId);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("Invalid X-FAPI-Interaction-ID format");
    }
    
    @Test
    @DisplayName("SR-001: Should reject null FAPI interaction ID")
    void shouldRejectNullFAPIInteractionID() {
        // Given
        String nullInteractionId = null;
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateInteractionId(nullInteractionId);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("X-FAPI-Interaction-ID is required");
    }
    
    @Test
    @DisplayName("SR-001: Should validate FAPI auth date successfully")
    void shouldValidateFAPIAuthDateSuccessfully() {
        // Given
        String validAuthDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateAuthDate(validAuthDate);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-001: Should reject invalid FAPI auth date format")
    void shouldRejectInvalidFAPIAuthDateFormat() {
        // Given
        String invalidAuthDate = "2024-01-01 10:00:00";
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateAuthDate(invalidAuthDate);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("Invalid X-FAPI-Auth-Date format");
    }
    
    @Test
    @DisplayName("SR-001: Should reject expired FAPI auth date")
    void shouldRejectExpiredFAPIAuthDate() {
        // Given
        String expiredAuthDate = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateAuthDate(expiredAuthDate);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("X-FAPI-Auth-Date is too old");
    }
    
    @Test
    @DisplayName("SR-001: Should validate customer IP address successfully")
    void shouldValidateCustomerIPAddressSuccessfully() {
        // Given
        String validCustomerIP = "192.168.1.100";
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateCustomerIP(validCustomerIP);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-001: Should reject invalid customer IP address")
    void shouldRejectInvalidCustomerIPAddress() {
        // Given
        String invalidCustomerIP = "invalid.ip.address";
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateCustomerIP(invalidCustomerIP);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("Invalid X-FAPI-Customer-IP-Address format");
    }
    
    @Test
    @DisplayName("SR-001: Should validate all FAPI headers successfully")
    void shouldValidateAllFAPIHeadersSuccessfully() {
        // Given
        String validAuthDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String validCustomerIP = "192.168.1.100";
        String validInteractionId = "550e8400-e29b-41d4-a716-446655440000";
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateFAPIHeaders(validAuthDate, validCustomerIP, validInteractionId);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-002: Should validate request signature successfully")
    void shouldValidateRequestSignatureSuccessfully() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000.00);
        request.put("currency", "USD");
        request.put("customerId", "CUST-12345");
        
        String validSignature = fapiSecurityValidator.generateHMACSignature(request);
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateRequestSignature(request, validSignature);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-002: Should reject invalid request signature")
    void shouldRejectInvalidRequestSignature() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000.00);
        request.put("currency", "USD");
        request.put("customerId", "CUST-12345");
        
        String invalidSignature = "invalid-signature";
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateRequestSignature(request, invalidSignature);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("Invalid request signature");
    }
    
    @Test
    @DisplayName("SR-002: Should reject null request signature")
    void shouldRejectNullRequestSignature() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000.00);
        
        String nullSignature = null;
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateRequestSignature(request, nullSignature);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("JWS signature required for request");
    }
    
    @Test
    @DisplayName("SR-003: Should generate HMAC signature successfully")
    void shouldGenerateHMACSignatureSuccessfully() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000.00);
        request.put("currency", "USD");
        request.put("customerId", "CUST-12345");
        
        // When
        String signature = fapiSecurityValidator.generateHMACSignature(request);
        
        // Then
        assertThat(signature).isNotNull();
        assertThat(signature).isNotEmpty();
        assertThat(signature).matches("^[A-Za-z0-9+/=]+$"); // Base64 pattern
    }
    
    @Test
    @DisplayName("SR-003: Should generate consistent signatures for same request")
    void shouldGenerateConsistentSignaturesForSameRequest() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000.00);
        request.put("currency", "USD");
        request.put("customerId", "CUST-12345");
        
        // When
        String signature1 = fapiSecurityValidator.generateHMACSignature(request);
        String signature2 = fapiSecurityValidator.generateHMACSignature(request);
        
        // Then
        assertThat(signature1).isEqualTo(signature2);
    }
    
    @Test
    @DisplayName("SR-003: Should generate different signatures for different requests")
    void shouldGenerateDifferentSignaturesForDifferentRequests() {
        // Given
        Map<String, Object> request1 = new HashMap<>();
        request1.put("amount", 1000.00);
        request1.put("currency", "USD");
        
        Map<String, Object> request2 = new HashMap<>();
        request2.put("amount", 2000.00);
        request2.put("currency", "EUR");
        
        // When
        String signature1 = fapiSecurityValidator.generateHMACSignature(request1);
        String signature2 = fapiSecurityValidator.generateHMACSignature(request2);
        
        // Then
        assertThat(signature1).isNotEqualTo(signature2);
    }
    
    @Test
    @DisplayName("SR-004: Should validate TLS requirement for FAPI endpoints")
    void shouldValidateTLSRequirementForFAPIEndpoints() {
        // Given
        String fapiEndpoint = "/fapi/v1/accounts";
        boolean isSecureConnection = true;
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateTLSForFAPIEndpoint(fapiEndpoint, isSecureConnection);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-004: Should reject non-TLS connections for FAPI endpoints")
    void shouldRejectNonTLSConnectionsForFAPIEndpoints() {
        // Given
        String fapiEndpoint = "/fapi/v1/accounts";
        boolean isSecureConnection = false;
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateTLSForFAPIEndpoint(fapiEndpoint, isSecureConnection);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("FAPI endpoints require HTTPS/TLS 1.2+");
    }
    
    @Test
    @DisplayName("SR-004: Should allow non-TLS connections for non-FAPI endpoints")
    void shouldAllowNonTLSConnectionsForNonFAPIEndpoints() {
        // Given
        String nonFAPIEndpoint = "/api/v1/health";
        boolean isSecureConnection = false;
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateTLSForFAPIEndpoint(nonFAPIEndpoint, isSecureConnection);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-005: Should create FAPI-compliant error response")
    void shouldCreateFAPICompliantErrorResponse() {
        // Given
        String errorCode = "invalid_request";
        String errorDescription = "The request is missing required parameters";
        String interactionId = "550e8400-e29b-41d4-a716-446655440000";
        
        // When
        Map<String, Object> errorResponse = fapiSecurityValidator.createFAPIErrorResponse(
            errorCode, errorDescription, interactionId);
        
        // Then
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.get("error")).isEqualTo(errorCode);
        assertThat(errorResponse.get("error_description")).isEqualTo(errorDescription);
        assertThat(errorResponse.get("x_fapi_interaction_id")).isEqualTo(interactionId);
        assertThat(errorResponse.get("timestamp")).isNotNull();
    }
    
    @Test
    @DisplayName("SR-002: Should handle malformed JSON in request signature validation")
    void shouldHandleMalformedJSONInRequestSignatureValidation() {
        // Given
        Object malformedRequest = new Object() {
            public String toString() {
                throw new RuntimeException("JSON serialization failed");
            }
        };
        String signature = "some-signature";
        
        // When & Then
        assertThatThrownBy(() -> {
            fapiSecurityValidator.validateRequestSignature(malformedRequest, signature);
        }).isInstanceOf(FAPISecurityException.class)
          .hasMessageContaining("Failed to generate HMAC signature");
    }
    
    @Test
    @DisplayName("SR-001: Should validate IPv6 customer IP address")
    void shouldValidateIPv6CustomerIPAddress() {
        // Given
        String validIPv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        
        // When & Then
        assertThatCode(() -> {
            fapiSecurityValidator.validateCustomerIP(validIPv6);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("SR-003: Should sign response successfully")
    void shouldSignResponseSuccessfully() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", "ACC-12345");
        response.put("balance", 5000.00);
        response.put("currency", "USD");
        
        // When
        String signature = fapiSecurityValidator.signResponse(response);
        
        // Then
        assertThat(signature).isNotNull();
        assertThat(signature).isNotEmpty();
        assertThat(signature).matches("^[A-Za-z0-9+/=]+$"); // Base64 pattern
    }
}