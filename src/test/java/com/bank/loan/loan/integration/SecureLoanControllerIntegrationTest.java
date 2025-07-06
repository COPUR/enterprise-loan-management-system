package com.bank.loan.loan.integration;

import com.bank.loan.loan.api.controller.SecureLoanController;
import com.bank.loan.loan.dto.LoanApplicationRequest;
import com.bank.loan.loan.dto.PaymentRequest;
import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.bank.loan.loan.service.AuditService;
import com.bank.loan.loan.service.LoanService;
import com.bank.loan.loan.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for SecureLoanController
 * 
 * Tests FAPI 2.0 + DPoP compliance for the secure loan controller including:
 * - FAPI security headers validation
 * - DPoP token binding requirements
 * - Proper authorization and audit logging
 * - Banking compliance validation
 */
@WebMvcTest(SecureLoanController.class)
@ActiveProfiles("test")
@DisplayName("SecureLoanController Integration Tests")
class SecureLoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuditService auditService;

    private JWK testDPoPKey;
    private DPoPClientLibrary.DPoPHttpClient dpopClient;

    @BeforeEach
    void setUp() throws Exception {
        this.testDPoPKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(testDPoPKey);
    }

    @Test
    @WithMockUser(roles = "LOAN_OFFICER")
    @DisplayName("Should successfully create loan with valid FAPI headers")
    void shouldCreateLoanWithValidFAPIHeaders() throws Exception {
        // Arrange
        LoanApplicationRequest request = createValidLoanRequest();
        String fiapiInteractionId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-FAPI-Auth-Date", "Mon, 06 Jan 2025 12:00:00 GMT")
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-FAPI-Interaction-ID", fiapiInteractionId))
                .andExpect(header().string("X-Idempotency-Key", idempotencyKey));
    }

    @Test
    @WithMockUser(roles = "LOAN_OFFICER")
    @DisplayName("Should reject loan creation without FAPI Interaction ID")
    void shouldRejectLoanCreationWithoutFAPIInteractionId() throws Exception {
        // Arrange
        LoanApplicationRequest request = createValidLoanRequest();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpected(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    @WithMockUser(roles = "LOAN_OFFICER")
    @DisplayName("Should reject loan creation without idempotency key")
    void shouldRejectLoanCreationWithoutIdempotencyKey() throws Exception {
        // Arrange
        LoanApplicationRequest request = createValidLoanRequest();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should allow customer to view own loans")
    void shouldAllowCustomerToViewOwnLoans() throws Exception {
        // Arrange
        String fiapiInteractionId = UUID.randomUUID().toString();
        String customerId = "CUST001";

        // Act & Assert
        mockMvc.perform(get("/api/v1/loans")
                .param("customerId", customerId)
                .header("Authorization", "DPoP test-token")
                .header("X-FAPI-Interaction-ID", fiapiInteractionId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-FAPI-Interaction-ID", fiapiInteractionId));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should process payment with valid FAPI headers and DPoP proof")
    void shouldProcessPaymentWithValidHeaders() throws Exception {
        // Arrange
        PaymentRequest paymentRequest = createValidPaymentRequest();
        String fiapiInteractionId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        String loanId = "LOAN001";
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans/" + loanId + "/payments", "test-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans/{loanId}/payments", loanId)
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-FAPI-Auth-Date", "Mon, 06 Jan 2025 12:00:00 GMT")
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-FAPI-Interaction-ID", fiapiInteractionId))
                .andExpect(header().string("X-Idempotency-Key", idempotencyKey));
    }

    @Test
    @WithMockUser(roles = "SENIOR_LOAN_OFFICER")
    @DisplayName("Should approve loan with proper authorization")
    void shouldApproveLoanWithProperAuthorization() throws Exception {
        // Arrange
        String loanId = "LOAN001";
        String fiapiInteractionId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans/" + loanId + "/approve", null);
        
        var approvalRequest = objectMapper.createObjectNode();
        approvalRequest.put("approvalNotes", "Loan approved after thorough review");
        approvalRequest.put("conditions", "Standard terms and conditions apply");

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans/{loanId}/approve", loanId)
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-FAPI-Auth-Date", "Mon, 06 Jan 2025 12:00:00 GMT")
                .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-FAPI-Interaction-ID", fiapiInteractionId));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should reject loan approval by customer")
    void shouldRejectLoanApprovalByCustomer() throws Exception {
        // Arrange
        String loanId = "LOAN001";
        String fiapiInteractionId = UUID.randomUUID().toString();
        var approvalRequest = objectMapper.createObjectNode();
        approvalRequest.put("approvalNotes", "Attempting unauthorized approval");

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans/{loanId}/approve", loanId)
                .header("Authorization", "DPoP test-token")
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should get payment history with pagination")
    void shouldGetPaymentHistoryWithPagination() throws Exception {
        // Arrange
        String loanId = "LOAN001";
        String fiapiInteractionId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/api/v1/loans/{loanId}/payments", loanId)
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "DPoP test-token")
                .header("X-FAPI-Interaction-ID", fiapiInteractionId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-FAPI-Interaction-ID", fiapiInteractionId));
    }

    @Test
    @DisplayName("Should handle invalid FAPI Customer IP Address format")
    void shouldHandleInvalidFAPICustomerIPAddress() throws Exception {
        // Arrange
        LoanApplicationRequest request = createValidLoanRequest();
        String fiapiInteractionId = UUID.randomUUID().toString();
        String dpopProof = dpopClient.getDPoPHeader("POST", "https://localhost:8080/api/v1/loans", null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/loans")
                .header("Authorization", "DPoP test-token")
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-FAPI-Customer-IP-Address", "invalid-ip-format")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpected(jsonPath("$.error").value("invalid_request"));
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private LoanApplicationRequest createValidLoanRequest() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setCustomerId("CUST001");
        request.setAmount(new BigDecimal("50000.00"));
        request.setCurrency("USD");
        request.setInterestRate(5.25);
        request.setInstallmentCount(60);
        request.setLoanType("PERSONAL");
        request.setPurpose("Home Improvement");
        return request;
    }

    private PaymentRequest createValidPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("1088.00"));
        request.setCurrency("USD");
        request.setPaymentType("REGULAR");
        request.setPaymentChannel("ONLINE");
        request.setNotes("Regular monthly payment");
        
        PaymentRequest.PaymentMethod paymentMethod = new PaymentRequest.PaymentMethod();
        paymentMethod.setType("BANK_TRANSFER");
        paymentMethod.setAccountNumber("****1234");
        paymentMethod.setRoutingNumber("021000021");
        paymentMethod.setBankName("Test Bank");
        request.setPaymentMethod(paymentMethod);
        
        return request;
    }
}