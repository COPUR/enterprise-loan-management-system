package com.bank.loan.loan.functional;

import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DPoP Security Functional Tests")
class DPoPSecurityFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    private DPoPTestKeyGenerator keyGenerator;
    private ECKey testKeyPair;
    private String mockAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        mockAccessToken = "mock.access.token";

        // Mock JWT decoder
        String jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
        Jwt mockJwt = Jwt.withTokenValue(mockAccessToken)
                .header("alg", "PS256")
                .header("typ", "JWT")
                .claim("iss", "https://auth.example.com")
                .claim("sub", "user123")
                .claim("aud", "https://api.example.com")
                .claim("exp", Instant.now().plusSeconds(300))
                .claim("iat", Instant.now())
                .claim("jti", "token123")
                .claim("cnf", Map.of("jkt", jktThumbprint))
                .claim("scope", "loans payments")
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(mockJwt);
    }

    @Nested
    @DisplayName("Complete Loan Application Flow with DPoP")
    class CompleteLoanApplicationFlowTests {

        @Test
        @DisplayName("Should complete entire loan application process with DPoP validation")
        void shouldCompleteLoanApplicationWithDPoPValidation() throws Exception {
            // Step 1: Create loan application
            String dpopProofCreate = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "POST", "http://localhost/api/v1/loans", mockAccessToken);

            String loanApplication = """
                {
                    "customerId": "CUST123",
                    "loanType": "PERSONAL",
                    "requestedAmount": 50000.00,
                    "termInMonths": 24,
                    "purpose": "Home renovation",
                    "annualIncome": 75000.00,
                    "employmentStatus": "FULL_TIME"
                }
                """;

            mockMvc.perform(post("/api/v1/loans")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofCreate)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loanApplication))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));

            // Step 2: Get loan status
            String dpopProofGet = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/loans/LOAN123", mockAccessToken);

            mockMvc.perform(get("/api/v1/loans/LOAN123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofGet)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789013")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanId").value("LOAN123"))
                    .andExpect(jsonPath("$.status").exists());

            // Step 3: Submit additional documents
            String dpopProofUpdate = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "PUT", "http://localhost/api/v1/loans/LOAN123/documents", mockAccessToken);

            String documentSubmission = """
                {
                    "documents": [
                        {
                            "type": "INCOME_VERIFICATION",
                            "documentId": "DOC123",
                            "fileName": "pay_stub.pdf"
                        }
                    ]
                }
                """;

            mockMvc.perform(put("/api/v1/loans/LOAN123/documents")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofUpdate)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789014")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(documentSubmission))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject loan application with missing DPoP proof")
        void shouldRejectLoanApplicationWithMissingDPoPProof() throws Exception {
            String loanApplication = """
                {
                    "customerId": "CUST123",
                    "loanType": "PERSONAL",
                    "requestedAmount": 50000.00,
                    "termInMonths": 24
                }
                """;

            mockMvc.perform(post("/api/v1/loans")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loanApplication))
                    .andExpect(status().isUnauthorized())
                    .andExpected(header().string("WWW-Authenticate", containsString("DPoP")));
        }
    }

    @Nested
    @DisplayName("Payment Processing Flow with DPoP")
    class PaymentProcessingFlowTests {

        @Test
        @DisplayName("Should complete payment processing with DPoP validation")
        void shouldCompletePaymentProcessingWithDPoPValidation() throws Exception {
            // Step 1: Initiate payment
            String dpopProofInitiate = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "POST", "http://localhost/api/v1/payments", mockAccessToken);

            String paymentRequest = """
                {
                    "fromAccountId": "ACC123",
                    "toAccountId": "ACC456",
                    "amount": 1000.00,
                    "currency": "USD",
                    "description": "Loan payment",
                    "reference": "LOAN123-PAYMENT-001"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofInitiate)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789015")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(paymentRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").exists())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            // Step 2: Confirm payment
            String dpopProofConfirm = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "PUT", "http://localhost/api/v1/payments/PAY123/confirm", mockAccessToken);

            String confirmationRequest = """
                {
                    "confirmationCode": "123456",
                    "authorizedAmount": 1000.00
                }
                """;

            mockMvc.perform(put("/api/v1/payments/PAY123/confirm")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofConfirm)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789016")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmationRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            // Step 3: Get payment status
            String dpopProofStatus = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/payments/PAY123", mockAccessToken);

            mockMvc.perform(get("/api/v1/payments/PAY123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofStatus)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789017")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("PAY123"));
        }

        @Test
        @DisplayName("Should validate DPoP proof binding throughout payment flow")
        void shouldValidateDPoPProofBindingThroughoutPaymentFlow() throws Exception {
            ECKey differentKeyPair = keyGenerator.generateECKey();

            // Step 1: Initiate payment with first key
            String dpopProofInitiate = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "POST", "http://localhost/api/v1/payments", mockAccessToken);

            String paymentRequest = """
                {
                    "fromAccountId": "ACC123",
                    "toAccountId": "ACC456",
                    "amount": 1000.00,
                    "currency": "USD"
                }
                """;

            mockMvc.perform(post("/api/v1/payments")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofInitiate)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789018")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(paymentRequest))
                    .andExpect(status().isCreated());

            // Step 2: Try to confirm payment with different key (should fail)
            String dpopProofConfirmWrong = keyGenerator.createDPoPProofWithAccessTokenHash(
                    differentKeyPair, "PUT", "http://localhost/api/v1/payments/PAY123/confirm", mockAccessToken);

            mockMvc.perform(put("/api/v1/payments/PAY123/confirm")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofConfirmWrong)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789019")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"confirmationCode\": \"123456\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", containsString("invalid_dpop_proof")));
        }
    }

    @Nested
    @DisplayName("Customer Account Management with DPoP")
    class CustomerAccountManagementTests {

        @Test
        @DisplayName("Should manage customer accounts with DPoP validation")
        void shouldManageCustomerAccountsWithDPoPValidation() throws Exception {
            // Step 1: Get customer profile
            String dpopProofGet = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/customers/CUST123", mockAccessToken);

            mockMvc.perform(get("/api/v1/customers/CUST123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofGet)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789020")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value("CUST123"));

            // Step 2: Update customer profile
            String dpopProofUpdate = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "PUT", "http://localhost/api/v1/customers/CUST123", mockAccessToken);

            String customerUpdate = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe.updated@example.com",
                    "phoneNumber": "+1234567890"
                }
                """;

            mockMvc.perform(put("/api/v1/customers/CUST123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofUpdate)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789021")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customerUpdate))
                    .andExpect(status().isOk());

            // Step 3: Get customer accounts
            String dpopProofAccounts = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/customers/CUST123/accounts", mockAccessToken);

            mockMvc.perform(get("/api/v1/customers/CUST123/accounts")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofAccounts)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789022")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accounts").isArray());
        }
    }

    @Nested
    @DisplayName("DPoP Security Edge Cases")
    class DPoPSecurityEdgeCasesTests {

        @Test
        @DisplayName("Should handle concurrent requests with same DPoP key")
        void shouldHandleConcurrentRequestsWithSameDPoPKey() throws Exception {
            String dpopProof1 = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/loans", mockAccessToken);
            String dpopProof2 = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/payments", mockAccessToken);

            // Both requests should succeed (different URIs, different JTIs)
            mockMvc.perform(get("/api/v1/loans")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProof1)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789023")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/payments")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProof2)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789024")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject requests with expired DPoP proof")
        void shouldRejectRequestsWithExpiredDPoPProof() throws Exception {
            long expiredTimestamp = Instant.now().minusSeconds(300).getEpochSecond();
            String expiredDPoPProof = keyGenerator.createDPoPProofWithTimestamp(
                    testKeyPair, "GET", "http://localhost/api/v1/loans", expiredTimestamp);

            mockMvc.perform(get("/api/v1/loans")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", expiredDPoPProof)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789025")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", containsString("invalid_dpop_proof")));
        }

        @Test
        @DisplayName("Should validate DPoP proof across different HTTP methods")
        void shouldValidateDPoPProofAcrossDifferentHttpMethods() throws Exception {
            // GET request
            String dpopProofGet = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/loans/LOAN123", mockAccessToken);

            mockMvc.perform(get("/api/v1/loans/LOAN123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofGet)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789026")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isOk());

            // DELETE request
            String dpopProofDelete = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "DELETE", "http://localhost/api/v1/loans/LOAN123", mockAccessToken);

            mockMvc.perform(delete("/api/v1/loans/LOAN123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofDelete)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789027")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isNoContent());

            // Wrong method should fail
            String dpopProofWrongMethod = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "POST", "http://localhost/api/v1/loans/LOAN123", mockAccessToken);

            mockMvc.perform(get("/api/v1/loans/LOAN123")
                            .header("Authorization", "Bearer " + mockAccessToken)
                            .header("DPoP", dpopProofWrongMethod)
                            .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789028")
                            .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                            .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DPoP Performance Tests")
    class DPoPPerformanceTests {

        @Test
        @DisplayName("Should handle high volume of DPoP requests efficiently")
        void shouldHandleHighVolumeOfDPoPRequestsEfficiently() throws Exception {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 100; i++) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", "http://localhost/api/v1/loans", mockAccessToken);

                mockMvc.perform(get("/api/v1/loans")
                                .header("Authorization", "Bearer " + mockAccessToken)
                                .header("DPoP", dpopProof)
                                .header("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-12345678902" + i)
                                .header("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z")
                                .header("X-FAPI-Customer-IP-Address", "192.168.1.100"))
                        .andExpected(status().isOk());
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Should complete 100 requests within reasonable time (less than 5 seconds)
            assertThat(totalTime).isLessThan(5000);
        }
    }
}