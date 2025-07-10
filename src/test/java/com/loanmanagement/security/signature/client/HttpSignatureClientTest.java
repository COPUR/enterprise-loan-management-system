package com.loanmanagement.security.signature.client;

import com.loanmanagement.security.signature.DigestCalculator;
import com.loanmanagement.security.signature.SignatureComponentsExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test suite for HttpSignatureClient
 * Tests client SDK functionality for signature generation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HTTP Signature Client Tests")
class HttpSignatureClientTest {

    @Mock
    private SignatureComponentsExtractor componentsExtractor;

    @Mock
    private DigestCalculator digestCalculator;

    private HttpSignatureClient signatureClient;
    private PrivateKey privateKey;
    private String keyId = "test-key-123";

    @BeforeEach
    void setUp() throws Exception {
        signatureClient = new HttpSignatureClient(componentsExtractor, digestCalculator);

        // Generate test key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        privateKey = keyPair.getPrivate();

        // Mock dependencies
        when(componentsExtractor.createSignature(anyString(), any(PrivateKey.class), anyString()))
            .thenReturn("mockSignature");
        when(digestCalculator.calculateSha256Digest(anyString()))
            .thenReturn("SHA-256=mockDigest");
    }

    @Test
    @DisplayName("Should generate signature headers for GET request")
    void shouldGenerateSignatureHeadersForGetRequest() {
        // Given
        SignatureRequest request = signatureClient.createGetRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans"
        );

        // When
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);

        // Then
        assertNotNull(headers);
        assertTrue(headers.containsKey("Date"));
        assertTrue(headers.containsKey("Host"));
        assertTrue(headers.containsKey("Signature"));
        assertEquals("api.example.com", headers.get("Host"));
        
        // GET request should not have digest or content-length
        assertFalse(headers.containsKey("Digest"));
        assertFalse(headers.containsKey("Content-Length"));
    }

    @Test
    @DisplayName("Should generate signature headers for POST request with body")
    void shouldGenerateSignatureHeadersForPostRequestWithBody() {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        SignatureRequest request = signatureClient.createPostRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans", requestBody
        );

        // When
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);

        // Then
        assertNotNull(headers);
        assertTrue(headers.containsKey("Date"));
        assertTrue(headers.containsKey("Host"));
        assertTrue(headers.containsKey("Signature"));
        assertTrue(headers.containsKey("Digest"));
        assertTrue(headers.containsKey("Content-Length"));
        
        assertEquals("api.example.com", headers.get("Host"));
        assertEquals("SHA-256=mockDigest", headers.get("Digest"));
        assertEquals(String.valueOf(requestBody.length()), headers.get("Content-Length"));
    }

    @Test
    @DisplayName("Should generate signature headers for DELETE request")
    void shouldGenerateSignatureHeadersForDeleteRequest() {
        // Given
        SignatureRequest request = signatureClient.createDeleteRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans/123"
        );

        // When
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);

        // Then
        assertNotNull(headers);
        assertTrue(headers.containsKey("Date"));
        assertTrue(headers.containsKey("Host"));
        assertTrue(headers.containsKey("Signature"));
        assertEquals("api.example.com", headers.get("Host"));
        
        // DELETE request should not have digest or content-length
        assertFalse(headers.containsKey("Digest"));
        assertFalse(headers.containsKey("Content-Length"));
    }

    @Test
    @DisplayName("Should validate headers for GET request successfully")
    void shouldValidateHeadersForGetRequestSuccessfully() {
        // Given
        SignatureRequest request = signatureClient.createGetRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans"
        );

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> signatureClient.validateHeaders(request));
    }

    @Test
    @DisplayName("Should validate headers for POST request successfully")
    void shouldValidateHeadersForPostRequestSuccessfully() {
        // Given
        SignatureRequest request = signatureClient.createPostRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans", "{\"test\":\"data\"}"
        );

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> signatureClient.validateHeaders(request));
    }

    @Test
    @DisplayName("Should throw exception for missing required headers")
    void shouldThrowExceptionForMissingRequiredHeaders() {
        // Given
        SignatureRequest request = SignatureRequest.builder()
            .keyId(keyId)
            .privateKey(privateKey)
            .algorithm("rsa-sha256")
            .javaAlgorithm("SHA256withRSA")
            .method("GET")
            .host("api.example.com")
            .path("/api/v1/loans")
            .headers(new String[]{"host"}) // Missing (request-target) and date
            .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> signatureClient.validateHeaders(request)
        );
        assertTrue(exception.getMessage().contains("Missing required headers"));
    }

    @Test
    @DisplayName("Should throw exception for POST request missing body headers")
    void shouldThrowExceptionForPostRequestMissingBodyHeaders() {
        // Given
        SignatureRequest request = SignatureRequest.builder()
            .keyId(keyId)
            .privateKey(privateKey)
            .algorithm("rsa-sha256")
            .javaAlgorithm("SHA256withRSA")
            .method("POST")
            .host("api.example.com")
            .path("/api/v1/loans")
            .requestBody("{\"test\":\"data\"}")
            .headers(new String[]{"(request-target)", "host", "date"}) // Missing content-length and digest
            .build();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> signatureClient.validateHeaders(request)
        );
        assertTrue(exception.getMessage().contains("Missing required headers for body requests"));
    }

    @Test
    @DisplayName("Should handle exception during signature generation")
    void shouldHandleExceptionDuringSignatureGeneration() throws Exception {
        // Given
        when(componentsExtractor.createSignature(anyString(), any(PrivateKey.class), anyString()))
            .thenThrow(new RuntimeException("Signature creation failed"));

        SignatureRequest request = signatureClient.createGetRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans"
        );

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class, 
            () -> signatureClient.generateSignatureHeaders(request)
        );
        assertTrue(exception.getMessage().contains("Signature generation failed"));
    }

    @Test
    @DisplayName("Should create proper signature header format")
    void shouldCreateProperSignatureHeaderFormat() {
        // Given
        SignatureRequest request = signatureClient.createGetRequest(
            keyId, privateKey, "api.example.com", "/api/v1/loans"
        );

        // When
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);

        // Then
        String signatureHeader = headers.get("Signature");
        assertNotNull(signatureHeader);
        assertTrue(signatureHeader.contains("keyId=\"" + keyId + "\""));
        assertTrue(signatureHeader.contains("algorithm=\"rsa-sha256\""));
        assertTrue(signatureHeader.contains("headers=\"(request-target) host date\""));
        assertTrue(signatureHeader.contains("signature=\"mockSignature\""));
    }

    @Test
    @DisplayName("Should handle query parameters in signature")
    void shouldHandleQueryParametersInSignature() {
        // Given
        SignatureRequest request = SignatureRequest.builder()
            .keyId(keyId)
            .privateKey(privateKey)
            .algorithm("rsa-sha256")
            .javaAlgorithm("SHA256withRSA")
            .method("GET")
            .host("api.example.com")
            .path("/api/v1/loans")
            .queryString("status=active&limit=10")
            .headers(new String[]{"(request-target)", "host", "date"})
            .build();

        // When
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);

        // Then
        assertNotNull(headers);
        assertTrue(headers.containsKey("Signature"));
    }
}