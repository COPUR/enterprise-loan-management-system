package com.amanahfi.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for FAPI 2.0 Security Configuration
 * 
 * Tests compliance with Financial-grade API (FAPI) 2.0 Security Profile:
 * - OAuth 2.1 with PKCE
 * - DPoP (Demonstration of Proof of Possession) tokens
 * - TLS 1.2+ enforcement
 * - Security headers (HSTS, CSP, etc.)
 * - Rate limiting and DDoS protection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("FAPI 2.0 Security Configuration Tests")
class Fapi2SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should reject requests without Bearer token")
    void shouldRejectRequestsWithoutBearerToken() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/customers")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().exists("WWW-Authenticate")
            .expectHeader().valueMatches("WWW-Authenticate", "Bearer.*");
    }

    @Test
    @DisplayName("Should enforce HTTPS redirect for financial data")
    void shouldEnforceHttpsRedirectForFinancialData() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/accounts")
            .exchange()
            .expectStatus().isUnauthorized() // First auth, but verifies endpoint exists
            .expectHeader().exists("Strict-Transport-Security")
            .expectHeader().valueMatches("Strict-Transport-Security", "max-age=31536000.*");
    }

    @Test
    @DisplayName("Should include security headers for FAPI 2.0 compliance")
    void shouldIncludeSecurityHeadersForFapi2Compliance() {
        // When & Then
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Content-Type-Options")
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().exists("X-Frame-Options")
            .expectHeader().valueEquals("X-Frame-Options", "DENY")
            .expectHeader().exists("X-XSS-Protection")
            .expectHeader().valueEquals("X-XSS-Protection", "1; mode=block")
            .expectHeader().exists("Content-Security-Policy");
    }

    @Test
    @DisplayName("Should validate DPoP token binding")
    void shouldValidateDPoPTokenBinding() {
        // Given
        String invalidDPopToken = "invalid-dpop-token";
        String bearerToken = "Bearer test-token";

        // When & Then
        webTestClient.get()
            .uri("/api/v1/payments")
            .header("Authorization", bearerToken)
            .header("DPoP", invalidDPopToken)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().exists("DPoP-Nonce"); // Server should provide nonce for retry
    }

    @Test
    @DisplayName("Should enforce rate limiting per client")
    void shouldEnforceRateLimitingPerClient() {
        // Given
        String clientId = "test-client-123";
        
        // When - Make multiple requests rapidly
        for (int i = 0; i < 15; i++) { // Exceed typical rate limit of 10/minute
            webTestClient.get()
                .uri("/api/v1/customers")
                .header("X-Client-ID", clientId)
                .exchange();
        }

        // Then - Should eventually get rate limited
        webTestClient.get()
            .uri("/api/v1/customers")
            .header("X-Client-ID", clientId)
            .exchange()
            .expectStatus().isEqualTo(429) // Too Many Requests
            .expectHeader().exists("X-RateLimit-Remaining")
            .expectHeader().exists("X-RateLimit-Reset");
    }

    @Test
    @DisplayName("Should validate OAuth 2.1 PKCE parameters")
    void shouldValidateOAuth21PkceParameters() {
        // When & Then - OAuth authorization endpoint
        webTestClient.get()
            .uri("/oauth2/authorize?response_type=code&client_id=test&redirect_uri=https://example.com")
            .exchange()
            .expectStatus().is4xxClientError() // Should require PKCE parameters
            .expectBody()
            .jsonPath("$.error").isEqualTo("invalid_request")
            .jsonPath("$.error_description").value(desc -> 
                assertThat(desc.toString()).contains("code_challenge"));
    }

    @Test
    @DisplayName("Should validate JWT access token structure")
    void shouldValidateJwtAccessTokenStructure() {
        // Given - Mock JWT token without required claims
        String invalidJwt = "Bearer eyJhbGciOiJSUzI1NiJ9.invalid.signature";

        // When & Then
        webTestClient.get()
            .uri("/api/v1/accounts")
            .header("Authorization", invalidJwt)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("invalid_token");
    }

    @Test
    @DisplayName("Should enforce Islamic banking compliance headers")
    void shouldEnforceIslamicBankingComplianceHeaders() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/murabaha/contracts")
            .exchange()
            .expectStatus().isUnauthorized() // Auth required, but validates headers
            .expectHeader().exists("X-Islamic-Banking")
            .expectHeader().valueEquals("X-Islamic-Banking", "true")
            .expectHeader().exists("X-Sharia-Compliant")
            .expectHeader().valueEquals("X-Sharia-Compliant", "true");
    }

    @Test
    @DisplayName("Should validate UAE regulatory compliance")
    void shouldValidateUaeRegulatoryCompliance() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/compliance/checks")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().exists("X-Regulatory-Compliance")
            .expectHeader().valueMatches("X-Regulatory-Compliance", ".*CBUAE.*VARA.*");
    }

    @Test
    @DisplayName("Should handle CORS for Islamic banking frontend")
    void shouldHandleCorsForIslamicBankingFrontend() {
        // When & Then - Preflight request
        webTestClient.options()
            .uri("/api/v1/customers")
            .header("Origin", "https://amanahfi.ae")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Authorization,DPoP")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("Access-Control-Allow-Origin")
            .expectHeader().exists("Access-Control-Allow-Methods")
            .expectHeader().exists("Access-Control-Allow-Headers")
            .expectHeader().valueMatches("Access-Control-Allow-Headers", ".*DPoP.*");
    }

    @Test
    @DisplayName("Should track audit events for compliance")
    void shouldTrackAuditEventsForCompliance() {
        // Given
        String clientId = "audit-test-client";
        
        // When
        webTestClient.get()
            .uri("/api/v1/payments")
            .header("X-Client-ID", clientId)
            .exchange()
            .expectStatus().isUnauthorized(); // Expected

        // Then - Audit event should be recorded (would verify via logs/events in real implementation)
        // For now, just verify the endpoint responds consistently
        webTestClient.get()
            .uri("/actuator/auditevents")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Should validate minimum TLS version")
    void shouldValidateMinimumTlsVersion() {
        // This test verifies server configuration
        // In a real environment, would test with different TLS versions
        
        // When & Then - Health check should be available over secure connection
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("Strict-Transport-Security");
    }

    @Test
    @DisplayName("Should provide proper error responses for FAPI compliance")
    void shouldProvideProperErrorResponsesForFapiCompliance() {
        // When & Then - Invalid content type
        webTestClient.post()
            .uri("/api/v1/accounts")
            .header("Content-Type", "text/plain") // Invalid for financial API
            .exchange()
            .expectStatus().is4xxClientError()
            .expectHeader().valueEquals("Content-Type", "application/json")
            .expectBody()
            .jsonPath("$.error").exists()
            .jsonPath("$.error_description").exists()
            .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("Should validate request signing for high-value transactions")
    void shouldValidateRequestSigningForHighValueTransactions() {
        // Given
        String unsignedHighValueRequest = """
            {
                "amount": 50000.00,
                "currency": "AED",
                "recipient": "ACC-987654321"
            }
            """;

        // When & Then
        webTestClient.post()
            .uri("/api/v1/payments/high-value")
            .header("Content-Type", "application/json")
            .bodyValue(unsignedHighValueRequest)
            .exchange()
            .expectStatus().isUnauthorized() // Should require request signing
            .expectHeader().exists("X-Request-Signature-Required")
            .expectHeader().valueEquals("X-Request-Signature-Required", "true");
    }
}