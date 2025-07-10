package com.loanmanagement.shared.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for FAPI Security Features
 * These tests are designed to FAIL initially and drive the implementation
 * of FAPI-compliant security features from backup-src
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://test-auth-server.example.com/.well-known/jwks.json"
})
@DisplayName("FAPI Security - TDD Tests")
class FAPISecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Should enforce FAPI security headers on protected endpoints")
    void shouldEnforceFAPISecurityHeaders() throws Exception {
        // When: Make request to protected endpoint
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token"))
                
                // Then: Response includes FAPI-required security headers
                .andExpect(status().isOk())
                .andExpect(header().exists("X-FAPI-Interaction-ID"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("Should generate unique FAPI Interaction ID for each request")
    void shouldGenerateUniqueFAPIInteractionIdForEachRequest() throws Exception {
        // When: Make multiple requests
        String interactionId1 = mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("X-FAPI-Interaction-ID");

        String interactionId2 = mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("X-FAPI-Interaction-ID");

        // Then: Each request has a unique interaction ID
        assertThat(interactionId1).isNotNull();
        assertThat(interactionId2).isNotNull();
        assertThat(interactionId1).isNotEqualTo(interactionId2);
    }

    @Test
    @DisplayName("Should validate JWT tokens with FAPI compliance")
    void shouldValidateJWTTokensWithFAPICompliance() throws Exception {
        // Given: Invalid JWT token
        String invalidToken = "invalid.jwt.token";
        
        // When: Make request with invalid token
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer " + invalidToken))
                
                // Then: Should return 401 with FAPI-compliant error response
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-FAPI-Interaction-ID"))
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").exists())
                .andExpect(jsonPath("$.fapi_compliance").value("FAPI token validation failed"));
    }

    @Test
    @DisplayName("Should enforce strong JWT signature algorithms")
    void shouldEnforceStrongJWTSignatureAlgorithms() throws Exception {
        // Given: JWT token with weak signature algorithm (HS256)
        String weakToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.weak-signature";
        
        // When: Make request with weak token
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer " + weakToken))
                
                // Then: Should reject weak signature algorithms
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Weak signature algorithm not allowed for FAPI"));
    }

    @Test
    @DisplayName("Should enforce rate limiting with FAPI-compliant responses")
    void shouldEnforceRateLimitingWithFAPICompliantResponses() throws Exception {
        // Given: Multiple rapid requests from same client
        String clientIp = "192.168.1.100";
        
        // When: Make requests exceeding rate limit
        for (int i = 0; i < 65; i++) { // Exceed 60 requests per minute limit
            mockMvc.perform(get("/api/customers")
                    .header("Authorization", "Bearer valid-jwt-token")
                    .header("X-Forwarded-For", clientIp));
        }
        
        // Then: Should return 429 with FAPI-compliant rate limit response
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("X-Forwarded-For", clientIp))
                
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(jsonPath("$.error").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.error_description").value("Too many requests. Please retry after 60 seconds."))
                .andExpect(jsonPath("$.fapi_compliance").value("FAPI rate limiting enforced"));
    }

    @Test
    @DisplayName("Should include rate limit headers in successful responses")
    void shouldIncludeRateLimitHeadersInSuccessfulResponses() throws Exception {
        // When: Make successful request
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token"))
                
                // Then: Response includes rate limit headers
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Should validate FAPI-specific request headers")
    void shouldValidateFAPISpecificRequestHeaders() throws Exception {
        // When: Make request with FAPI headers
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("X-FAPI-Auth-Date", "2023-12-07T10:40:52Z")
                .header("X-FAPI-Customer-IP-Address", "203.0.113.1")
                .header("X-FAPI-Interaction-ID", "test-interaction-id"))
                
                // Then: Headers should be accepted and processed
                .andExpect(status().isOk())
                .andExpect(header().exists("X-FAPI-Interaction-ID"));
    }

    @Test
    @DisplayName("Should enforce CORS policy for FAPI compliance")
    void shouldEnforceCORSPolicyForFAPICompliance() throws Exception {
        // When: Make CORS preflight request
        mockMvc.perform(options("/api/customers")
                .header("Origin", "https://authorized-client.bank.com")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,X-FAPI-Interaction-ID"))
                
                // Then: Should return FAPI-compliant CORS headers
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://authorized-client.bank.com"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    @DisplayName("Should reject requests from unauthorized origins")
    void shouldRejectRequestsFromUnauthorizedOrigins() throws Exception {
        // When: Make request from unauthorized origin
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("Origin", "https://malicious-site.com"))
                
                // Then: Should reject the request
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should enforce TLS requirements for FAPI endpoints")
    void shouldEnforceTLSRequirementsForFAPIEndpoints() throws Exception {
        // When: Make request without secure connection (simulated)
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("X-Forwarded-Proto", "http")) // Simulate non-HTTPS request
                
                // Then: Should enforce HTTPS requirement
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("insecure_transport"))
                .andExpect(jsonPath("$.error_description").value("FAPI requires secure transport (HTTPS)"));
    }

    @Test
    @DisplayName("Should validate request signature for critical operations")
    void shouldValidateRequestSignatureForCriticalOperations() throws Exception {
        // Given: Request with JWS signature header
        String jwsSignature = "eyJhbGciOiJSUzI1NiIsImtpZCI6InRlc3Qta2V5In0..signature";
        String requestBody = "{\"amount\": 10000, \"customerId\": 12345}";
        
        // When: Make signed request to critical endpoint
        mockMvc.perform(post("/api/loans")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("X-JWS-Signature", jwsSignature)
                .header("Content-Type", "application/json")
                .content(requestBody))
                
                // Then: Should validate signature and process request
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-FAPI-Interaction-ID"));
    }

    @Test
    @DisplayName("Should reject critical operations without valid signatures")
    void shouldRejectCriticalOperationsWithoutValidSignatures() throws Exception {
        // Given: Request without signature to critical endpoint
        String requestBody = "{\"amount\": 10000, \"customerId\": 12345}";
        
        // When: Make unsigned request to critical endpoint
        mockMvc.perform(post("/api/loans")
                .header("Authorization", "Bearer valid-jwt-token")
                .header("Content-Type", "application/json")
                .content(requestBody))
                
                // Then: Should reject unsigned critical operation
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("missing_signature"))
                .andExpect(jsonPath("$.error_description").value("FAPI requires signed requests for critical operations"));
    }

    @Test
    @DisplayName("Should implement proper error handling with FAPI compliance")
    void shouldImplementProperErrorHandlingWithFAPICompliance() throws Exception {
        // When: Trigger server error
        mockMvc.perform(get("/api/customers/trigger-error")
                .header("Authorization", "Bearer valid-jwt-token"))
                
                // Then: Should return FAPI-compliant error response
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-FAPI-Interaction-ID"))
                .andExpect(jsonPath("$.error").value("server_error"))
                .andExpect(jsonPath("$.error_description").exists())
                .andExpect(jsonPath("$.fapi_interaction_id").exists());
    }
}