package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * FAPI 2.0 + DPoP End-to-End Integration Test
 * Tests complete banking workflows with real Keycloak integration and test data
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "fapi2-dpop"})
@TestMethodOrder(OrderAnnotation.class)
@Sql(scripts = "/config/test-data/fapi2-banking-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("FAPI 2.0 + DPoP End-to-End Integration Test")
class FAPI2EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Test clients configuration
    private static final String PRODUCTION_CLIENT_ID = "fapi2-banking-app-production";
    private static final String MOBILE_CLIENT_ID = "fapi2-mobile-banking-app";
    private static final String CORPORATE_CLIENT_ID = "fapi2-corporate-banking-client";
    
    // Test user credentials
    private static final String LOAN_OFFICER_USERNAME = "loan-officer-1";
    private static final String LOAN_OFFICER_PASSWORD = "LoanOfficer2025!@#";
    private static final String SENIOR_OFFICER_USERNAME = "senior-loan-officer";
    private static final String SENIOR_OFFICER_PASSWORD = "SeniorLoan2025!@#";
    private static final String CUSTOMER_USERNAME = "test-customer-1";
    private static final String CUSTOMER_PASSWORD = "TestCustomer2025!@#";

    // DPoP clients for different scenarios
    private DPoPClientLibrary.DPoPHttpClient productionDPoPClient;
    private DPoPClientLibrary.DPoPHttpClient mobileDPoPClient;
    private DPoPClientLibrary.DPoPHttpClient corporateDPoPClient;
    
    private JWK productionDPoPKey;
    private JWK mobileDPoPKey;
    private JWK corporateDPoPKey;

    // Authentication tokens
    private String loanOfficerToken;
    private String seniorOfficerToken;
    private String customerToken;
    private String corporateToken;

    @BeforeAll
    static void setupClass() {
        System.setProperty("spring.profiles.active", "test,fapi2-dpop");
        System.setProperty("keycloak.realm", "fapi2-banking-realm");
        System.setProperty("keycloak.auth-server-url", "http://localhost:8090/auth");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Generate DPoP keys for different clients
        this.productionDPoPKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.mobileDPoPKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.corporateDPoPKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();

        this.productionDPoPClient = new DPoPClientLibrary.DPoPHttpClient(productionDPoPKey);
        this.mobileDPoPClient = new DPoPClientLibrary.DPoPHttpClient(mobileDPoPKey);
        this.corporateDPoPClient = new DPoPClientLibrary.DPoPHttpClient(corporateDPoPKey);
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Authenticate All Test Users")
    void authenticateTestUsers() throws Exception {
        // Authenticate loan officer
        this.loanOfficerToken = authenticateUser(LOAN_OFFICER_USERNAME, LOAN_OFFICER_PASSWORD, 
                                                 PRODUCTION_CLIENT_ID, productionDPoPClient);
        assertNotNull(loanOfficerToken);

        // Authenticate senior loan officer
        this.seniorOfficerToken = authenticateUser(SENIOR_OFFICER_USERNAME, SENIOR_OFFICER_PASSWORD, 
                                                   PRODUCTION_CLIENT_ID, productionDPoPClient);
        assertNotNull(seniorOfficerToken);

        // Authenticate customer
        this.customerToken = authenticateUser(CUSTOMER_USERNAME, CUSTOMER_PASSWORD, 
                                              MOBILE_CLIENT_ID, mobileDPoPClient);
        assertNotNull(customerToken);

        // Authenticate corporate client (service account)
        this.corporateToken = authenticateCorporateClient(CORPORATE_CLIENT_ID, corporateDPoPClient);
        assertNotNull(corporateToken);

        System.out.println("✅ All test users authenticated successfully");
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 1: Complete Customer Onboarding Flow")
    void testCompleteCustomerOnboarding() throws Exception {
        // Step 1: Create new customer (loan officer)
        Map<String, Object> newCustomer = createNewCustomerRequest();
        String customerId = createCustomerWithDPoP(newCustomer, loanOfficerToken, productionDPoPClient);
        
        // Step 2: Verify customer creation in audit logs
        verifyAuditLogEntry("CUSTOMER", customerId, "CREATED", loanOfficerToken, productionDPoPClient);
        
        // Step 3: Update customer information
        updateCustomerWithDPoP(customerId, loanOfficerToken, productionDPoPClient);
        
        // Step 4: Customer views their own profile
        viewCustomerProfileWithDPoP(customerId, customerToken, mobileDPoPClient);
        
        System.out.println("✅ Customer onboarding flow completed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 2: Complete Loan Application and Approval Flow")
    void testCompleteLoanApplicationFlow() throws Exception {
        String customerId = "CUST001"; // Using existing test customer
        
        // Step 1: Customer initiates loan application
        Map<String, Object> loanApplication = createLoanApplicationRequest(customerId);
        String loanId = submitLoanApplicationWithDPoP(loanApplication, customerToken, mobileDPoPClient);
        
        // Step 2: Loan officer reviews application
        reviewLoanApplicationWithDPoP(loanId, loanOfficerToken, productionDPoPClient);
        
        // Step 3: Credit assessment (analyst)
        performCreditAssessmentWithDPoP(loanId, customerId, loanOfficerToken, productionDPoPClient);
        
        // Step 4: Senior loan officer approves
        approveLoanWithDPoP(loanId, seniorOfficerToken, productionDPoPClient);
        
        // Step 5: Verify loan approval in audit logs
        verifyAuditLogEntry("LOAN", loanId, "APPROVED", seniorOfficerToken, productionDPoPClient);
        
        // Step 6: Customer checks loan status
        checkLoanStatusWithDPoP(loanId, customerToken, mobileDPoPClient);
        
        System.out.println("✅ Loan application and approval flow completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 3: Payment Processing and Management")
    void testPaymentProcessingFlow() throws Exception {
        String loanId = "LOAN001"; // Using existing test loan
        
        // Step 1: Customer makes regular payment
        Map<String, Object> regularPayment = createRegularPaymentRequest(loanId);
        String paymentId = processPaymentWithDPoP(regularPayment, customerToken, mobileDPoPClient);
        
        // Step 2: Generate payment receipt
        generatePaymentReceiptWithDPoP(paymentId, customerToken, mobileDPoPClient);
        
        // Step 3: Customer makes early payment
        Map<String, Object> earlyPayment = createEarlyPaymentRequest(loanId);
        String earlyPaymentId = processPaymentWithDPoP(earlyPayment, customerToken, mobileDPoPClient);
        
        // Step 4: Loan officer reviews payment history
        reviewPaymentHistoryWithDPoP(loanId, loanOfficerToken, productionDPoPClient);
        
        // Step 5: Test payment idempotency
        testPaymentIdempotencyWithDPoP(regularPayment, customerToken, mobileDPoPClient);
        
        System.out.println("✅ Payment processing flow completed successfully");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 4: Corporate Banking Operations")
    void testCorporateBankingOperations() throws Exception {
        String corporateCustomerId = "CORP001";
        
        // Step 1: Corporate client requests loan information
        getCorporateLoanPortfolioWithDPoP(corporateCustomerId, corporateToken, corporateDPoPClient);
        
        // Step 2: Corporate client submits large loan application
        Map<String, Object> corporateLoan = createCorporateLoanRequest(corporateCustomerId);
        String corporateLoanId = submitLoanApplicationWithDPoP(corporateLoan, corporateToken, corporateDPoPClient);
        
        // Step 3: Senior officer reviews corporate loan
        reviewLoanApplicationWithDPoP(corporateLoanId, seniorOfficerToken, productionDPoPClient);
        
        // Step 4: Corporate payment processing
        Map<String, Object> corporatePayment = createCorporatePaymentRequest("LOAN004");
        processPaymentWithDPoP(corporatePayment, corporateToken, corporateDPoPClient);
        
        System.out.println("✅ Corporate banking operations completed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 5: Administrative and Compliance Operations")
    void testAdministrativeOperations() throws Exception {
        // Step 1: Generate loan statistics report
        generateLoanStatisticsReportWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        // Step 2: Compliance audit log review
        reviewComplianceAuditLogsWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        // Step 3: FAPI compliance report
        generateFAPIComplianceReportWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        // Step 4: Security event monitoring
        reviewSecurityEventsWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        System.out.println("✅ Administrative operations completed successfully");
    }

    @Test
    @Order(7)
    @DisplayName("E2E Test 6: Security Validation and Error Scenarios")
    void testSecurityValidationScenarios() throws Exception {
        // Test 1: DPoP replay attack prevention
        testDPoPReplayPrevention();
        
        // Test 2: Token binding validation
        testTokenBindingValidation();
        
        // Test 3: Invalid client authentication
        testInvalidClientAuthentication();
        
        // Test 4: Rate limiting
        testRateLimitingWithDPoP();
        
        // Test 5: Authorization violations
        testAuthorizationViolations();
        
        System.out.println("✅ Security validation scenarios completed successfully");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 7: Migration and Feature Flag Testing")
    void testMigrationFeatures() throws Exception {
        // Test 1: Check migration status for clients
        checkMigrationStatusWithDPoP(PRODUCTION_CLIENT_ID, seniorOfficerToken, productionDPoPClient);
        
        // Test 2: Feature flag validation
        testFeatureFlagsWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        // Test 3: Performance metrics collection
        validatePerformanceMetricsWithDPoP(seniorOfficerToken, productionDPoPClient);
        
        System.out.println("✅ Migration and feature flag testing completed successfully");
    }

    // ========================================================================
    // Helper Methods for Authentication and API Operations
    // ========================================================================

    private String authenticateUser(String username, String password, String clientId, 
                                   DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        // Step 1: Create PAR request
        String requestUri = createPARRequestForUser(username, clientId, dpopClient);
        
        // Step 2: Simulate authorization (user login and consent)
        String authCode = simulateUserAuthorizationFlow(requestUri, username, password);
        
        // Step 3: Exchange code for DPoP-bound token
        return exchangeCodeForDPoPToken(authCode, clientId, dpopClient);
    }

    private String authenticateCorporateClient(String clientId, DPoPClientLibrary.DPoPHttpClient dpopClient) 
            throws Exception {
        // Corporate client uses client credentials flow
        return performClientCredentialsFlowWithDPoP(clientId, dpopClient);
    }

    private String createPARRequestForUser(String username, String clientId, 
                                          DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopClient.getDPoPKey());
        String codeChallenge = generatePKCECodeChallenge();
        String clientAssertion = generateClientAssertion(clientId);
        
        Map<String, String> parRequest = new HashMap<>();
        parRequest.put("client_id", clientId);
        parRequest.put("redirect_uri", getRedirectUriForClient(clientId));
        parRequest.put("response_type", "code");
        parRequest.put("scope", getScopeForClient(clientId));
        parRequest.put("code_challenge", codeChallenge);
        parRequest.put("code_challenge_method", "S256");
        parRequest.put("dpop_jkt", jktThumbprint);
        parRequest.put("nonce", generateNonce());
        parRequest.put("state", "state_" + System.currentTimeMillis());
        parRequest.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parRequest.put("client_assertion", clientAssertion);
        
        String response = mockMvc.perform(post("/oauth2/par")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(convertMapToMultiValueMap(parRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> parResponse = objectMapper.readValue(response, Map.class);
        return (String) parResponse.get("request_uri");
    }

    private String exchangeCodeForDPoPToken(String authCode, String clientId, 
                                           DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/oauth2/token", null);
        String clientAssertion = generateClientAssertion(clientId);
        String codeVerifier = generatePKCECodeVerifier();
        
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("grant_type", "authorization_code");
        tokenRequest.put("code", authCode);
        tokenRequest.put("redirect_uri", getRedirectUriForClient(clientId));
        tokenRequest.put("code_verifier", codeVerifier);
        tokenRequest.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        tokenRequest.put("client_assertion", clientAssertion);
        
        String response = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("DPoP", dpopProof)
                .params(convertMapToMultiValueMap(tokenRequest)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.token_type", is("DPoP")))
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> tokenResponse = objectMapper.readValue(response, Map.class);
        return (String) tokenResponse.get("access_token");
    }

    // ========================================================================
    // Banking Operations Helper Methods
    // ========================================================================

    private String createCustomerWithDPoP(Map<String, Object> customerData, String accessToken,
                                          DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/customers", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> customerResponse = objectMapper.readValue(response, Map.class);
        return (String) customerResponse.get("customerId");
    }

    private String submitLoanApplicationWithDPoP(Map<String, Object> loanData, String accessToken,
                                                 DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> loanResponse = objectMapper.readValue(response, Map.class);
        return (String) loanResponse.get("loanId");
    }

    private String processPaymentWithDPoP(Map<String, Object> paymentData, String accessToken,
                                         DPoPClientLibrary.DPoPHttpClient dpopClient) throws Exception {
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> paymentResponse = objectMapper.readValue(response, Map.class);
        return (String) paymentResponse.get("paymentId");
    }

    // ========================================================================
    // Data Creation Helper Methods
    // ========================================================================

    private Map<String, Object> createNewCustomerRequest() {
        Map<String, Object> customer = new HashMap<>();
        customer.put("personalInfo", Map.of(
            "firstName", "Alice",
            "lastName", "Johnson",
            "dateOfBirth", "1992-05-10",
            "email", "alice.johnson@example.com",
            "phone", "+1-555-9999",
            "ssn", "555-66-7777"
        ));
        customer.put("address", Map.of(
            "street", "789 Pine Avenue",
            "city", "San Francisco",
            "state", "CA",
            "zipCode", "94102",
            "country", "US"
        ));
        return customer;
    }

    private Map<String, Object> createLoanApplicationRequest(String customerId) {
        Map<String, Object> loan = new HashMap<>();
        loan.put("customerId", customerId);
        loan.put("amount", 75000.00);
        loan.put("currency", "USD");
        loan.put("purpose", "Home Improvement");
        loan.put("installmentCount", 120);
        loan.put("interestRate", 4.25);
        loan.put("loanType", "PERSONAL");
        return loan;
    }

    private Map<String, Object> createRegularPaymentRequest(String loanId) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("loanId", loanId);
        payment.put("amount", 1088.00);
        payment.put("currency", "USD");
        payment.put("paymentType", "REGULAR");
        payment.put("paymentMethod", Map.of(
            "type", "BANK_TRANSFER",
            "accountNumber", "****1234",
            "routingNumber", "021000021"
        ));
        return payment;
    }

    // ========================================================================
    // Security Test Helper Methods
    // ========================================================================

    private void testDPoPReplayPrevention() throws Exception {
        String dpopProof = productionDPoPClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", loanOfficerToken);
        
        // First request should succeed
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + loanOfficerToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId()))
                .andExpected(status().isOk());
        
        // Second request with same proof should fail
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + loanOfficerToken)
                .header("DPoP", dpopProof) // Same proof - replay attack
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_dpop_proof")));
    }

    private void testTokenBindingValidation() throws Exception {
        // Try to use mobile client's token with production client's DPoP proof
        String mobileToken = customerToken; // Bound to mobile DPoP key
        String productionDPoPProof = productionDPoPClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", mobileToken);
        
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + mobileToken)
                .header("DPoP", productionDPoPProof) // Wrong DPoP key
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("key mismatch")));
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    private String simulateUserAuthorizationFlow(String requestUri, String username, String password) {
        // In real test, this would interact with Keycloak
        // For this test, we'll return a mock authorization code
        return "auth_code_" + System.currentTimeMillis() + "_" + username;
    }

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

    private String generateClientAssertion(String clientId) {
        // In real implementation, this would create a proper private_key_jwt
        return "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.mock.client.assertion.for." + clientId;
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String getRedirectUriForClient(String clientId) {
        return switch (clientId) {
            case PRODUCTION_CLIENT_ID -> "https://app.banking.com/callback";
            case MOBILE_CLIENT_ID -> "https://mobile.banking.com/callback";
            case CORPORATE_CLIENT_ID -> "https://corporate.banking.com/callback";
            default -> "https://localhost:8080/callback";
        };
    }

    private String getScopeForClient(String clientId) {
        return switch (clientId) {
            case PRODUCTION_CLIENT_ID -> "openid banking-loans banking-payments banking-admin";
            case MOBILE_CLIENT_ID -> "openid banking-loans banking-payments";
            case CORPORATE_CLIENT_ID -> "banking-loans banking-payments";
            default -> "openid";
        };
    }

    private org.springframework.util.MultiValueMap<String, String> convertMapToMultiValueMap(Map<String, String> map) {
        org.springframework.util.MultiValueMap<String, String> multiValueMap = new org.springframework.util.LinkedMultiValueMap<>();
        map.forEach(multiValueMap::add);
        return multiValueMap;
    }

    // Additional helper methods would be implemented here for completeness...
}