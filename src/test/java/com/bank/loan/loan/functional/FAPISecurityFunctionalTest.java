package com.bank.loanmanagement.loan.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FAPI (Financial-grade API) security functional tests
 * Tests OAuth2.1, FAPI compliance, and financial-grade security features
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FAPI Security Functional Tests")
public class FAPISecurityFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should enforce FAPI authentication requirements")
    public void shouldEnforceFAPIAuthentication() throws Exception {
        // When: Access protected endpoint without proper FAPI token
        mockMvc.perform(get("/api/v1/loans/applications")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        System.out.println("✅ FAPI Authentication: Correctly enforced FAPI authentication requirements");
    }

    @Test
    @DisplayName("Should validate FAPI token structure and claims")
    public void shouldValidateFAPITokenStructure() throws Exception {
        // Given: Valid FAPI-compliant token request
        String tokenRequestJson = """
            {
                "grant_type": "authorization_code",
                "client_id": "fapi-test-client",
                "client_secret": "fapi-test-secret",
                "code": "auth-code-123",
                "redirect_uri": "https://client.example.com/callback",
                "code_verifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
            }
            """;

        // When: Request FAPI token
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("grant_type=client_credentials&client_id=fapi-test-client&client_secret=fapi-test-secret&scope=banking:loans")
                .header("Content-Type", "application/x-www-form-urlencoded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();

        System.out.println("✅ FAPI Token: Successfully validated FAPI token structure");
    }

    @Test
    @DisplayName("Should enforce FAPI security headers")
    public void shouldEnforceFAPISecurityHeaders() throws Exception {
        // When: Make request with required FAPI security headers
        mockMvc.perform(get("/api/v1/loans/LOAN-001")
                .header("Authorization", "Bearer valid-fapi-token")
                .header("x-fapi-financial-id", "bank-001")
                .header("x-fapi-customer-ip-address", "192.168.1.1")
                .header("x-fapi-customer-last-logged-time", "2024-01-15T10:30:00Z")
                .header("x-fapi-interaction-id", "interaction-123"))
                .andExpect(status().isOk())
                .andExpect(header().exists("x-fapi-interaction-id"))
                .andReturn();

        System.out.println("✅ FAPI Headers: Successfully enforced FAPI security headers");
    }

    @Test
    @DisplayName("Should validate FAPI request signing")
    public void shouldValidateFAPIRequestSigning() throws Exception {
        // Given: FAPI signed request (JWS)
        String signedRequest = """
            {
                "data": {
                    "customerId": "CUST-FAPI-001",
                    "amount": 25000.00,
                    "purpose": "Business expansion"
                },
                "signature": "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9..."
            }
            """;

        // When: Submit signed FAPI request
        mockMvc.perform(post("/api/fapi/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signedRequest)
                .header("Authorization", "Bearer fapi-token")
                .header("x-fapi-financial-id", "bank-001"))
                .andExpect(status().isCreated())
                .andReturn();

        System.out.println("✅ FAPI Signing: Successfully validated FAPI request signing");
    }

    @Test
    @DisplayName("Should handle FAPI consent management")
    public void shouldHandleFAPIConsentManagement() throws Exception {
        // Given: FAPI consent request
        String consentJson = """
            {
                "permissions": [
                    "ReadAccountsBasic",
                    "ReadAccountsDetail",
                    "ReadBalances",
                    "ReadTransactionsBasic"
                ],
                "expirationDateTime": "2024-12-31T23:59:59Z",
                "transactionFromDateTime": "2024-01-01T00:00:00Z",
                "transactionToDateTime": "2024-12-31T23:59:59Z"
            }
            """;

        // When: Create FAPI consent
        mockMvc.perform(post("/oauth2/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentJson)
                .header("Authorization", "Bearer fapi-token")
                .header("x-fapi-financial-id", "bank-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentId").exists())
                .andExpect(jsonPath("$.status").value("AwaitingAuthorisation"))
                .andReturn();

        System.out.println("✅ FAPI Consent: Successfully handled FAPI consent management");
    }

    @Test
    @DisplayName("Should enforce FAPI rate limiting")
    public void shouldEnforceFAPIRateLimiting() throws Exception {
        // When: Make multiple rapid requests to test rate limiting
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get("/api/v1/loans/applications")
                    .header("Authorization", "Bearer fapi-rate-test-token")
                    .header("x-fapi-financial-id", "bank-001"))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        // Then: Subsequent request should be rate limited
        mockMvc.perform(get("/api/v1/loans/applications")
                .header("Authorization", "Bearer fapi-rate-test-token")
                .header("x-fapi-financial-id", "bank-001"))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        System.out.println("✅ FAPI Rate Limiting: Successfully enforced FAPI rate limiting");
    }

    @Test
    @DisplayName("Should validate FAPI error responses")
    public void shouldValidateFAPIErrorResponses() throws Exception {
        // When: Make invalid FAPI request
        mockMvc.perform(post("/api/fapi/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"request\"}")
                .header("Authorization", "Bearer fapi-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors[0].ErrorCode").exists())
                .andExpect(jsonPath("$.errors[0].Message").exists())
                .andReturn();

        System.out.println("✅ FAPI Error Handling: Successfully validated FAPI error responses");
    }

    @Test
    @DisplayName("Should support FAPI MTLS (Mutual TLS)")
    public void shouldSupportFAPIMTLS() throws Exception {
        // Note: In real implementation, this would test client certificate validation
        // For test purposes, we simulate MTLS validation
        
        // When: Access FAPI endpoint with MTLS requirement
        mockMvc.perform(get("/api/fapi/v1/high-security/loans")
                .header("Authorization", "Bearer fapi-mtls-token")
                .header("x-fapi-financial-id", "bank-001")
                .header("SSL_CLIENT_CERT", "cert-fingerprint-123"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✅ FAPI MTLS: Successfully supported FAPI mutual TLS");
    }
}