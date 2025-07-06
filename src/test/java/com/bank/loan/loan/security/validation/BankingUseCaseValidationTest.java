package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive Banking Use Case Validation with FAPI 2.0 + DPoP
 * Tests all major banking workflows with the new security implementation
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Banking Use Case Validation with FAPI 2.0 + DPoP")
class BankingUseCaseValidationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private DPoPClientLibrary.DPoPHttpClient dpopClient;
    private JWK dpopKeyPair;
    private String accessToken;
    private String customerId;
    private String loanId;
    private String paymentId;

    @BeforeEach
    void setUp() throws Exception {
        this.objectMapper = new ObjectMapper();
        this.dpopKeyPair = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(dpopKeyPair);
        this.accessToken = authenticateWithDPoP();
    }

    @Test
    @Order(1)
    @DisplayName("Use Case 1: Customer Onboarding with FAPI 2.0 + DPoP")
    void testCustomerOnboardingFlow() throws Exception {
        // Step 1: Create new customer with personal information
        Map<String, Object> customerRequest = createCustomerRequest();
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
                .andExpect(jsonPath("$.customerId", notNullValue()))
                .andExpect(jsonPath("$.personalInfo.firstName", is("John")))
                .andExpect(jsonPath("$.personalInfo.lastName", is("Smith")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.creditLimit.amount", is(50000.0)))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> customerResponse = objectMapper.readValue(response, Map.class);
        this.customerId = (String) customerResponse.get("customerId");
        
        // Step 2: Verify customer was created with proper security audit trail
        verifyCustomerCreationAuditLog();
        
        // Step 3: Test customer data retrieval with DPoP
        dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/customers/" + customerId, accessToken);
        
        mockMvc.perform(get("/api/v1/customers/" + customerId)
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.personalInfo.email", is("john.smith@example.com")));
    }

    @Test
    @Order(2)
    @DisplayName("Use Case 2: Loan Application Process with FAPI 2.0 + DPoP")
    void testLoanApplicationProcess() throws Exception {
        // Ensure customer exists
        if (customerId == null) {
            testCustomerOnboardingFlow();
        }
        
        // Step 1: Create loan application
        Map<String, Object> loanRequest = createLoanApplicationRequest(customerId);
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
                .andExpect(jsonPath("$.loanId", notNullValue()))
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.amount", is(100000.0)))
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
                .andExpect(jsonPath("$.monthlyPayment", notNullValue()))
                .andExpect(jsonPath("$.approvalWorkflow.currentStep", is("CREDIT_ASSESSMENT")))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> loanResponse = objectMapper.readValue(response, Map.class);
        this.loanId = (String) loanResponse.get("loanId");
        
        // Step 2: Check loan status
        testLoanStatusInquiry();
        
        // Step 3: Simulate credit assessment completion
        simulateCreditAssessment();
        
        // Step 4: Approve loan (requires loan officer role)
        approveLoan();
        
        // Step 5: Verify loan approval audit trail
        verifyLoanApprovalAuditLog();
    }

    @Test
    @Order(3)
    @DisplayName("Use Case 3: Loan Status and Installment Management")
    void testLoanStatusInquiry() throws Exception {
        if (loanId == null) {
            testLoanApplicationProcess();
        }
        
        // Get loan details
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans/" + loanId, accessToken);
        
        mockMvc.perform(get("/api/v1/loans/" + loanId)
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId", is(loanId)))
                .andExpect(jsonPath("$.installmentCount", is(360)))
                .andExpect(jsonPath("$.interestRate", is(3.5)));
        
        // Get loan installments
        dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans/" + loanId + "/installments", accessToken);
        
        mockMvc.perform(get("/api/v1/loans/" + loanId + "/installments")
                .param("status", "PENDING")
                .param("limit", "12")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId", is(loanId)))
                .andExpect(jsonPath("$.installments", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.summary.totalInstallments", is(360)))
                .andExpect(jsonPath("$.summary.nextDueDate", notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("Use Case 4: Payment Processing with DPoP Security")
    void testPaymentProcessing() throws Exception {
        if (loanId == null) {
            testLoanApplicationProcess();
        }
        
        // Step 1: Process regular monthly payment
        Map<String, Object> paymentRequest = createPaymentRequest(loanId);
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        String response = mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpected(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.loanId", is(loanId)))
                .andExpect(jsonPath("$.amount", is(449.04)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.allocation.principalAmount", notNullValue()))
                .andExpect(jsonPath("$.allocation.interestAmount", notNullValue()))
                .andExpect(jsonPath("$.remainingBalance", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        
        Map<String, Object> paymentResponse = objectMapper.readValue(response, Map.class);
        this.paymentId = (String) paymentResponse.get("paymentId");
        
        // Step 2: Get payment receipt
        dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/payments/" + paymentId + "/receipt", accessToken);
        
        mockMvc.perform(get("/api/v1/payments/" + paymentId + "/receipt")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.receiptNumber", notNullValue()));
        
        // Step 3: Test idempotency - retry same payment with same idempotency key
        testPaymentIdempotency(paymentRequest);
    }

    @Test
    @Order(5)
    @DisplayName("Use Case 5: Early Payment Processing")
    void testEarlyPaymentProcessing() throws Exception {
        if (loanId == null) {
            testLoanApplicationProcess();
        }
        
        // Process early payment with discount calculation
        Map<String, Object> earlyPaymentRequest = createEarlyPaymentRequest(loanId);
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", generateIdempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(earlyPaymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentType", is("EARLY_PAYMENT")))
                .andExpect(jsonPath("$.allocation.discountAmount", notNullValue()));
    }

    @Test
    @Order(6)
    @DisplayName("Use Case 6: Customer Portfolio Management")
    void testCustomerPortfolioManagement() throws Exception {
        if (customerId == null) {
            testCustomerOnboardingFlow();
        }
        
        // Get customer's loan portfolio
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/customers/" + customerId + "/loans", accessToken);
        
        mockMvc.perform(get("/api/v1/customers/" + customerId + "/loans")
                .param("status", "ACTIVE")
                .param("limit", "10")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.loans", isA(List.class)))
                .andExpect(jsonPath("$.portfolio.totalOutstanding", notNullValue()))
                .andExpected(jsonPath("$.portfolio.monthlyPayment", notNullValue()));
        
        // Get customer's payment history
        dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/customers/" + customerId + "/payments", accessToken);
        
        mockMvc.perform(get("/api/v1/customers/" + customerId + "/payments")
                .param("from", "2024-01-01")
                .param("to", "2024-12-31")
                .param("limit", "50")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.payments", isA(List.class)));
    }

    @Test
    @Order(7)
    @DisplayName("Use Case 7: Administrative Operations with Enhanced Security")
    void testAdministrativeOperations() throws Exception {
        // Test admin-level operations that require higher privileges
        
        // Get system-wide loan statistics
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/admin/loan-statistics", accessToken);
        
        mockMvc.perform(get("/api/v1/admin/loan-statistics")
                .param("period", "monthly")
                .param("year", "2024")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans", notNullValue()))
                .andExpect(jsonPath("$.totalAmount", notNullValue()))
                .andExpect(jsonPath("$.averageInterestRate", notNullValue()));
        
        // Test compliance reporting
        dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/admin/compliance-report", accessToken);
        
        mockMvc.perform(get("/api/v1/admin/compliance-report")
                .param("reportType", "FAPI_COMPLIANCE")
                .param("from", "2024-01-01")
                .param("to", "2024-01-31")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType", is("FAPI_COMPLIANCE")))
                .andExpect(jsonPath("$.securityProfile", is("FAPI 2.0 + DPoP")))
                .andExpect(jsonPath("$.complianceScore", notNullValue()));
    }

    @Test
    @Order(8)
    @DisplayName("Use Case 8: Security Audit and Monitoring")
    void testSecurityAuditAndMonitoring() throws Exception {
        // Test security audit log access
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/audit/logs", accessToken);
        
        mockMvc.perform(get("/api/v1/audit/logs")
                .param("user", "john.smith@banking.local")
                .param("action", "LOAN_APPROVED")
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2024-01-31T23:59:59Z")
                .param("limit", "100")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogs", isA(List.class)))
                .andExpect(jsonPath("$.pagination.total", notNullValue()));
        
        // Test security event reporting
        Map<String, Object> securityEvent = createSecurityEventReport();
        dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/security/events", accessToken);
        
        mockMvc.perform(post("/api/v1/security/events")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(securityEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.eventType", is("SUSPICIOUS_ACTIVITY")))
                .andExpect(jsonPath("$.status", is("UNDER_INVESTIGATION")));
    }

    @Test
    @Order(9)
    @DisplayName("Use Case 9: Multi-Client DPoP Validation")
    void testMultiClientDPoPValidation() throws Exception {
        // Test that DPoP proofs are client-specific and cannot be reused
        
        // Generate second DPoP key pair for different client
        JWK secondDPoPKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient secondDPoPClient = new DPoPClientLibrary.DPoPHttpClient(secondDPoPKey);
        
        // Try to use second client's DPoP proof with first client's token
        String wrongDPoPProof = secondDPoPClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/loans", accessToken);
        
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", wrongDPoPProof) // Wrong DPoP proof
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_dpop_proof")))
                .andExpect(jsonPath("$.error_description", containsString("key mismatch")));
    }

    @Test
    @Order(10)
    @DisplayName("Use Case 10: Performance and Scalability Validation")
    void testPerformanceAndScalability() throws Exception {
        // Test multiple concurrent DPoP requests
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/customers/" + customerId, accessToken);
            
            mockMvc.perform(get("/api/v1/customers/" + customerId)
                    .header("Authorization", "DPoP " + accessToken)
                    .header("DPoP", dpopProof)
                    .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                    .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                    .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Verify performance is within acceptable limits (should be under 5 seconds for 10 requests)
        assert totalTime < 5000 : "DPoP validation performance is too slow: " + totalTime + "ms";
    }

    // Helper Methods
    private String authenticateWithDPoP() {
        // Return mock DPoP-bound access token
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9.mock.dpop.token";
    }

    private Map<String, Object> createCustomerRequest() {
        Map<String, Object> customerRequest = new HashMap<>();
        customerRequest.put("personalInfo", Map.of(
            "firstName", "John",
            "lastName", "Smith",
            "dateOfBirth", "1980-01-15",
            "email", "john.smith@example.com",
            "phone", "+1-555-0123"
        ));
        customerRequest.put("address", Map.of(
            "street", "123 Main Street",
            "city", "New York",
            "state", "NY",
            "zipCode", "10001",
            "country", "US"
        ));
        customerRequest.put("identification", Map.of(
            "type", "SSN",
            "number", "123-45-6789",
            "issuingCountry", "US"
        ));
        return customerRequest;
    }

    private Map<String, Object> createLoanApplicationRequest(String customerId) {
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", customerId);
        loanRequest.put("amount", 100000.00);
        loanRequest.put("currency", "USD");
        loanRequest.put("purpose", "Home Purchase");
        loanRequest.put("installmentCount", 360);
        loanRequest.put("interestRate", 3.5);
        loanRequest.put("startDate", "2024-02-01");
        loanRequest.put("loanType", "MORTGAGE");
        loanRequest.put("collateral", Map.of(
            "type", "REAL_ESTATE",
            "value", 350000.00,
            "description", "Single family home at 123 Oak Street"
        ));
        return loanRequest;
    }

    private Map<String, Object> createPaymentRequest(String loanId) {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("loanId", loanId);
        paymentRequest.put("amount", 449.04);
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", Map.of(
            "type", "BANK_TRANSFER",
            "accountNumber", "****1234",
            "routingNumber", "021000021"
        ));
        paymentRequest.put("paymentDate", LocalDate.now().toString());
        paymentRequest.put("notes", "Regular monthly payment");
        return paymentRequest;
    }

    private Map<String, Object> createEarlyPaymentRequest(String loanId) {
        Map<String, Object> earlyPaymentRequest = new HashMap<>();
        earlyPaymentRequest.put("loanId", loanId);
        earlyPaymentRequest.put("amount", 5000.00);
        earlyPaymentRequest.put("currency", "USD");
        earlyPaymentRequest.put("paymentType", "EARLY_PAYMENT");
        earlyPaymentRequest.put("paymentMethod", Map.of(
            "type", "BANK_TRANSFER",
            "accountNumber", "****1234"
        ));
        earlyPaymentRequest.put("applyEarlyPaymentDiscount", true);
        return earlyPaymentRequest;
    }

    private Map<String, Object> createSecurityEventReport() {
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "SUSPICIOUS_ACTIVITY");
        securityEvent.put("severity", "HIGH");
        securityEvent.put("description", "Multiple failed DPoP validation attempts");
        securityEvent.put("userId", "john.smith@banking.local");
        securityEvent.put("ipAddress", "192.168.1.100");
        securityEvent.put("details", Map.of(
            "failedAttempts", 5,
            "timeWindow", "5 minutes",
            "dpopErrors", List.of("invalid_signature", "replay_attack")
        ));
        return securityEvent;
    }

    private void simulateCreditAssessment() throws Exception {
        // Simulate credit assessment completion
        Map<String, Object> assessmentUpdate = Map.of(
            "step", "CREDIT_ASSESSMENT",
            "status", "COMPLETED",
            "score", 750,
            "recommendation", "APPROVE"
        );
        
        String dpopProof = dpopClient.getDPoPHeader("PUT", "https://localhost:8080/api/v1/loans/" + loanId + "/assessment", accessToken);
        
        mockMvc.perform(put("/api/v1/loans/" + loanId + "/assessment")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assessmentUpdate)))
                .andExpect(status().isOk());
    }

    private void approveLoan() throws Exception {
        Map<String, Object> approvalRequest = Map.of(
            "approvalNotes", "Credit assessment completed successfully. Customer meets all criteria.",
            "conditions", List.of("Provide proof of insurance", "Submit final employment verification")
        );
        
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    private void testPaymentIdempotency(Map<String, Object> paymentRequest) throws Exception {
        String idempotencyKey = generateIdempotencyKey();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        // First request with idempotency key
        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated());
        
        // Second request with same idempotency key should return same result
        dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/payments", accessToken);
        
        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk()) // Should return 200 instead of 201 for idempotent request
                .andExpect(jsonPath("$.paymentId", is(paymentId)));
    }

    private void verifyCustomerCreationAuditLog() throws Exception {
        // Verify audit log entry was created for customer creation
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/audit/logs", accessToken);
        
        mockMvc.perform(get("/api/v1/audit/logs")
                .param("action", "CUSTOMER_CREATED")
                .param("resourceId", customerId)
                .param("limit", "1")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogs", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.auditLogs[0].action", is("CUSTOMER_CREATED")))
                .andExpect(jsonPath("$.auditLogs[0].resourceId", is(customerId)));
    }

    private void verifyLoanApprovalAuditLog() throws Exception {
        // Verify audit log entry was created for loan approval
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://localhost:8080/api/v1/audit/logs", accessToken);
        
        mockMvc.perform(get("/api/v1/audit/logs")
                .param("action", "LOAN_APPROVED")
                .param("resourceId", loanId)
                .param("limit", "1")
                .header("Authorization", "DPoP " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", generateFAPIInteractionId())
                .header("X-FAPI-Auth-Date", getCurrentFAPIDate())
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogs", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.auditLogs[0].action", is("LOAN_APPROVED")))
                .andExpect(jsonPath("$.auditLogs[0].resourceId", is(loanId)));
    }

    // Utility methods
    private String generateFAPIInteractionId() {
        return UUID.randomUUID().toString();
    }

    private String getCurrentFAPIDate() {
        return java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}