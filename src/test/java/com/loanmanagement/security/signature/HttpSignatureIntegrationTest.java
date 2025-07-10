package com.loanmanagement.security.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.security.signature.exception.InvalidSignatureException;
import com.loanmanagement.security.signature.exception.MissingSignatureHeaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration test for HTTP Message Signature validation
 * Tests the complete signature validation workflow
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HTTP Signature Integration Tests")
class HttpSignatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignatureKeyResolver keyResolver;

    @Autowired
    private SignatureComponentsExtractor componentsExtractor;

    private KeyPair keyPair;
    private String keyId = "test-key-123";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        // Mock key resolver to return our test public key
        when(keyResolver.resolvePublicKey(keyId)).thenReturn(Optional.of(keyPair.getPublic()));
        when(keyResolver.resolvePublicKey(anyString())).thenReturn(Optional.empty());
        when(keyResolver.resolvePublicKey(keyId)).thenReturn(Optional.of(keyPair.getPublic()));
    }

    @Test
    @DisplayName("Should allow request with valid signature")
    void shouldAllowRequestWithValidSignature() throws Exception {
        // Given
        String requestBody = "{\"amount\":10000,\"currency\":\"USD\"}";
        String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String host = "localhost";
        String digest = "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=";

        // Build signature string
        String signatureString = String.format(
            "(request-target): post /api/v1/loans\nhost: %s\ndate: %s\ncontent-length: %d\ndigest: %s",
            host, currentDate, requestBody.length(), digest
        );

        // Create signature
        String signature = componentsExtractor.createSignature(
            signatureString, keyPair.getPrivate(), "SHA256withRSA"
        );

        // Build signature header
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date content-length digest\",signature=\"%s\"",
            keyId, signature
        );

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .header("Signature", signatureHeader)
                .header("Host", host)
                .header("Date", currentDate)
                .header("Content-Length", String.valueOf(requestBody.length()))
                .header("Digest", digest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(request().attribute("signature.validated", true));
    }

    @Test
    @DisplayName("Should reject request without signature header")
    void shouldRejectRequestWithoutSignatureHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value(
                    containsString("Missing signature header")));
    }

    @Test
    @DisplayName("Should reject request with invalid signature")
    void shouldRejectRequestWithInvalidSignature() throws Exception {
        // Given
        String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String invalidSignatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"invalidSignature\"",
            keyId
        );

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .header("Signature", invalidSignatureHeader)
                .header("Host", "localhost")
                .header("Date", currentDate)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @DisplayName("Should reject request with missing required headers")
    void shouldRejectRequestWithMissingRequiredHeaders() throws Exception {
        // Given - missing Date header
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host\",signature=\"someSignature\"",
            keyId
        );

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .header("Signature", signatureHeader)
                .header("Host", "localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                    containsString("Date header is required")));
    }

    @Test
    @DisplayName("Should reject request with old timestamp")
    void shouldRejectRequestWithOldTimestamp() throws Exception {
        // Given - timestamp 10 minutes ago
        String oldDate = ZonedDateTime.now().minusMinutes(10)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
        
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"someSignature\"",
            keyId
        );

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .header("Signature", signatureHeader)
                .header("Host", "localhost")
                .header("Date", oldDate)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                    containsString("timestamp is too old")));
    }

    @Test
    @DisplayName("Should allow GET request without body signature")
    void shouldAllowGetRequestWithoutBodySignature() throws Exception {
        // Given
        String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String host = "localhost";

        // Build signature string for GET request
        String signatureString = String.format(
            "(request-target): get /api/v1/loans\nhost: %s\ndate: %s",
            host, currentDate
        );

        // Create signature
        String signature = componentsExtractor.createSignature(
            signatureString, keyPair.getPrivate(), "SHA256withRSA"
        );

        // Build signature header
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"%s\"",
            keyId, signature
        );

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .header("Signature", signatureHeader)
                .header("Host", host)
                .header("Date", currentDate))
                .andExpect(status().isOk())
                .andExpect(request().attribute("signature.validated", true));
    }

    @Test
    @DisplayName("Should allow request to excluded path without signature")
    void shouldAllowRequestToExcludedPathWithoutSignature() throws Exception {
        // When & Then
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject request with unresolvable key ID")
    void shouldRejectRequestWithUnresolvableKeyId() throws Exception {
        // Given
        String unknownKeyId = "unknown-key";
        String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"someSignature\"",
            unknownKeyId
        );

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .header("Signature", signatureHeader)
                .header("Host", "localhost")
                .header("Date", currentDate)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                    containsString("Cannot resolve public key")));
    }

    @Test
    @DisplayName("Should handle signature validation with query parameters")
    void shouldHandleSignatureValidationWithQueryParameters() throws Exception {
        // Given
        String currentDate = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String host = "localhost";

        // Build signature string with query parameters
        String signatureString = String.format(
            "(request-target): get /api/v1/loans?status=active&limit=10\nhost: %s\ndate: %s",
            host, currentDate
        );

        // Create signature
        String signature = componentsExtractor.createSignature(
            signatureString, keyPair.getPrivate(), "SHA256withRSA"
        );

        // Build signature header
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date\",signature=\"%s\"",
            keyId, signature
        );

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .param("status", "active")
                .param("limit", "10")
                .header("Signature", signatureHeader)
                .header("Host", host)
                .header("Date", currentDate))
                .andExpect(status().isOk())
                .andExpect(request().attribute("signature.validated", true));
    }
}