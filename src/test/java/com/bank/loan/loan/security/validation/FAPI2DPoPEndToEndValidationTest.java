package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.par.service.PARService;
import com.bank.loan.loan.security.par.model.PARResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End Validation Test for FAPI 2.0 + DPoP Implementation
 * Tests complete authentication flow from PAR to API access
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FAPI 2.0 + DPoP End-to-End Validation")
class FAPI2DPoPEndToEndValidationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private DPoPClientLibrary.DPoPHttpClient dpopClient;
    private JWK dpopKeyPair;
    private String clientId = "test-banking-app";
    private String redirectUri = "https://app.banking.com/callback";
    
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() throws Exception {
        this.objectMapper = new ObjectMapper();
        
        // Generate DPoP key pair for testing
        this.dpopKeyPair = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(dpopKeyPair);
    }

    @Test
    @DisplayName("Complete FAPI 2.0 + DPoP Authentication Flow")
    void testCompleteAuthenticationFlow() throws Exception {
        // Step 1: Create PAR Request
        String requestUri = createPARRequest();
        
        // Step 2: Authorization Request (simulated)
        String authorizationCode = simulateAuthorizationRequest(requestUri);
        
        // Step 3: Token Exchange with DPoP
        String accessToken = exchangeCodeForToken(authorizationCode);
        
        // Step 4: API Access with DPoP-bound token
        testAPIAccessWithDPoPToken(accessToken);
        
        // Step 5: Refresh Token Flow
        testRefreshTokenFlow(accessToken);
    }

    @Test
    @DisplayName("PAR Request with DPoP JKT Validation")
    void testPARRequestWithDPoPJKT() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String codeChallenge = generatePKCECodeChallenge();
        String clientAssertion = generateClientAssertion();
        
        Map<String, String> parParams = new HashMap<>();
        parParams.put("client_id", clientId);
        parParams.put("redirect_uri", redirectUri);
        parParams.put("response_type", "code");
        parParams.put("scope", "openid banking-loans banking-payments");
        parParams.put("code_challenge", codeChallenge);
        parParams.put("code_challenge_method", "S256");
        parParams.put("dpop_jkt", jktThumbprint);
        parParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parParams.put("client_assertion", clientAssertion);
        
        mockMvc.perform(post("/oauth2/par")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(convertMapToMultiValueMap(parParams)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.request_uri", startsWith("urn:ietf:params:oauth:request_uri:")))
                .andExpect(jsonPath("$.expires_in", is(300)));
    }

    @Test
    @DisplayName("Token Exchange with Valid DPoP Proof")
    void testTokenExchangeWithDPoP() throws Exception {
        String authorizationCode = "test_auth_code_12345";
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/oauth2/token", null);
        String clientAssertion = generateClientAssertion();
        String codeVerifier = generatePKCECodeVerifier();
        
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("grant_type", "authorization_code");
        tokenParams.put("code", authorizationCode);
        tokenParams.put("redirect_uri", redirectUri);
        tokenParams.put("code_verifier", codeVerifier);
        tokenParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        tokenParams.put("client_assertion", clientAssertion);
        
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("DPoP", dpopProof)
                .params(convertMapToMultiValueMap(tokenParams)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type", is("DPoP")))
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.expires_in", is(300)))
                .andExpect(jsonPath("$.scope", containsString("banking-loans")));
    }

    @Test
    @DisplayName("API Access with DPoP-bound Token")
    void testAPIAccessWithDPoPBoundToken() throws Exception {
        String accessToken = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9..."; // Mock DPoP-bound token
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", accessToken);
        
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012")
                .header("X-FAPI-Auth-Date", "Tue, 15 Jan 2024 10:30:00 GMT")
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-FAPI-Interaction-ID"));
    }

    @Test
    @DisplayName("Banking Use Case: Loan Application Flow")
    void testLoanApplicationFlow() throws Exception {
        String accessToken = authenticateAndGetToken();
        
        // Create customer first
        String customerId = createCustomerWithDPoP(accessToken);
        
        // Create loan application
        String loanId = createLoanApplicationWithDPoP(accessToken, customerId);
        
        // Approve loan (requires higher privileges)
        approveLoanWithDPoP(accessToken, loanId);
        
        // Check loan status
        checkLoanStatusWithDPoP(accessToken, loanId);
        
        // Process payment
        processLoanPaymentWithDPoP(accessToken, loanId);
    }

    @Test
    @DisplayName("Banking Use Case: Payment Processing Flow")
    void testPaymentProcessingFlow() throws Exception {
        String accessToken = authenticateAndGetToken();
        String loanId = "loan_test_12345";
        
        // Get loan details
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans/" + loanId, accessToken);
        
        mockMvc.perform(get("/api/v1/loans/" + loanId)
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk());
        
        // Process payment
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("loanId", loanId);
        paymentRequest.put("amount", 449.04);
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", Map.of("type", "BANK_TRANSFER", "accountNumber", "****1234"));
        
        dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Security Validation: DPoP Replay Attack Prevention")
    void testDPoPReplayAttackPrevention() throws Exception {
        String accessToken = authenticateAndGetToken();
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", accessToken);
        
        // First request should succeed
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk());
        
        // Second request with same DPoP proof should fail (replay attack)
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof) // Same proof - should be rejected
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_dpop_proof")))
                .andExpect(jsonPath("$.error_description", containsString("replay")));
    }

    @Test
    @DisplayName("Security Validation: Invalid DPoP Proof Rejection")
    void testInvalidDPoPProofRejection() throws Exception {
        String accessToken = authenticateAndGetToken();
        String invalidDPoPProof = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRwb3Arand0In0.invalid.proof";
        
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", invalidDPoPProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpected(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_dpop_proof")));
    }

    @Test
    @DisplayName("Legacy Flow Rejection: Hybrid Flow Not Allowed")
    void testHybridFlowRejection() throws Exception {
        String clientAssertion = generateClientAssertion();
        String codeChallenge = generatePKCECodeChallenge();
        
        Map<String, String> parParams = new HashMap<>();
        parParams.put("client_id", clientId);
        parParams.put("redirect_uri", redirectUri);
        parParams.put("response_type", "code id_token"); // Hybrid flow - should be rejected
        parParams.put("scope", "openid banking-loans");
        parParams.put("code_challenge", codeChallenge);
        parParams.put("code_challenge_method", "S256");
        parParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parParams.put("client_assertion", clientAssertion);
        
        mockMvc.perform(post("/oauth2/par")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(convertMapToMultiValueMap(parParams)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("unsupported_response_type")))
                .andExpect(jsonPath("$.error_description", containsString("Hybrid flows")));
    }

    @Test
    @DisplayName("Legacy Flow Rejection: Implicit Flow Not Allowed")
    void testImplicitFlowRejection() throws Exception {
        String clientAssertion = generateClientAssertion();
        
        Map<String, String> parParams = new HashMap<>();
        parParams.put("client_id", clientId);
        parParams.put("redirect_uri", redirectUri);
        parParams.put("response_type", "token"); // Implicit flow - should be rejected
        parParams.put("scope", "openid banking-loans");
        parParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parParams.put("client_assertion", clientAssertion);
        
        mockMvc.perform(post("/oauth2/par")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(convertMapToMultiValueMap(parParams)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("unsupported_response_type")))
                .andExpect(jsonPath("$.error_description", containsString("implicit flows")));
    }

    // Helper methods
    private String createPARRequest() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String codeChallenge = generatePKCECodeChallenge();
        String clientAssertion = generateClientAssertion();
        
        Map<String, String> parParams = new HashMap<>();
        parParams.put("client_id", clientId);
        parParams.put("redirect_uri", redirectUri);
        parParams.put("response_type", "code");
        parParams.put("scope", "openid banking-loans banking-payments");
        parParams.put("code_challenge", codeChallenge);
        parParams.put("code_challenge_method", "S256");
        parParams.put("dpop_jkt", jktThumbprint);
        parParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parParams.put("client_assertion", clientAssertion);
        
        String response = mockMvc.perform(post("/oauth2/par")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(convertMapToMultiValueMap(parParams)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> parResponse = objectMapper.readValue(response, Map.class);
        return (String) parResponse.get("request_uri");
    }

    private String simulateAuthorizationRequest(String requestUri) {
        // In real scenario, this would involve user authentication and consent
        return "auth_code_" + System.currentTimeMillis();
    }

    private String exchangeCodeForToken(String authorizationCode) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/oauth2/token", null);
        String clientAssertion = generateClientAssertion();
        String codeVerifier = generatePKCECodeVerifier();
        
        Map<String, String> tokenParams = new HashMap<>();
        tokenParams.put("grant_type", "authorization_code");
        tokenParams.put("code", authorizationCode);
        tokenParams.put("redirect_uri", redirectUri);
        tokenParams.put("code_verifier", codeVerifier);
        tokenParams.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        tokenParams.put("client_assertion", clientAssertion);
        
        String response = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("DPoP", dpopProof)
                .params(convertMapToMultiValueMap(tokenParams)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> tokenResponse = objectMapper.readValue(response, Map.class);
        return (String) tokenResponse.get("access_token");
    }

    private void testAPIAccessWithDPoPToken(String accessToken) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", accessToken);
        
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk());
    }

    private void testRefreshTokenFlow(String accessToken) throws Exception {
        // Implementation for refresh token testing
        // This would test the refresh token flow with DPoP
    }

    private String authenticateAndGetToken() throws Exception {
        // Simplified authentication for testing
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9.mock.token";
    }

    private String createCustomerWithDPoP(String accessToken) throws Exception {
        Map<String, Object> customerRequest = new HashMap<>();
        customerRequest.put("personalInfo", Map.of(
            "firstName", "John",
            "lastName", "Smith",
            "email", "john.smith@example.com"
        ));
        
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/customers", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> customerResponse = objectMapper.readValue(response, Map.class);
        return (String) customerResponse.get("customerId");
    }

    private String createLoanApplicationWithDPoP(String accessToken, String customerId) throws Exception {
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", customerId);
        loanRequest.put("amount", 100000.00);
        loanRequest.put("currency", "USD");
        loanRequest.put("purpose", "Home Purchase");
        loanRequest.put("installmentCount", 360);
        loanRequest.put("interestRate", 3.5);
        
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> loanResponse = objectMapper.readValue(response, Map.class);
        return (String) loanResponse.get("loanId");
    }

    private void approveLoanWithDPoP(String accessToken, String loanId) throws Exception {
        Map<String, Object> approvalRequest = new HashMap<>();
        approvalRequest.put("approvalNotes", "Credit assessment completed successfully");
        
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans/" + loanId + "/approve", accessToken);
        
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/approve")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk());
    }

    private void checkLoanStatusWithDPoP(String accessToken, String loanId) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans/" + loanId, accessToken);
        
        mockMvc.perform(get("/api/v1/loans/" + loanId)
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId", is(loanId)));
    }

    private void processLoanPaymentWithDPoP(String accessToken, String loanId) throws Exception {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("loanId", loanId);
        paymentRequest.put("amount", 449.04);
        paymentRequest.put("currency", "USD");
        
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated());
    }

    // Utility methods
    private String generatePKCECodeChallenge() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generatePKCECodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateClientAssertion() {
        // Implementation would generate a proper private_key_jwt
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9.mock.client.assertion";
    }

    private String generateFAPIInteractionId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String getCurrentFAPIDate() {
        return java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private String generateIdempotencyKey() {
        return java.util.UUID.randomUUID().toString();
    }

    private org.springframework.util.MultiValueMap<String, String> convertMapToMultiValueMap(Map<String, String> map) {
        org.springframework.util.MultiValueMap<String, String> multiValueMap = new org.springframework.util.LinkedMultiValueMap<>();
        map.forEach(multiValueMap::add);
        return multiValueMap;
    }
}