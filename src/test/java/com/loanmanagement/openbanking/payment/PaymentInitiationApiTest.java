package com.loanmanagement.openbanking.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.openbanking.payment.model.PaymentInitiationRequest;
import com.loanmanagement.openbanking.payment.model.PaymentInitiationResponse;
import com.loanmanagement.openbanking.payment.model.PaymentStatus;
import com.loanmanagement.openbanking.payment.service.PaymentInitiationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD tests for Payment Initiation API following Open Banking standards
 * These tests will fail initially and drive the implementation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment Initiation API Tests")
class PaymentInitiationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentInitiationService paymentInitiationService;

    private PaymentInitiationRequest samplePaymentRequest;
    private PaymentInitiationResponse samplePaymentResponse;

    @BeforeEach
    void setUp() {
        samplePaymentRequest = PaymentInitiationRequest.builder()
            .instructionIdentification("INSTR-001")
            .endToEndIdentification("E2E-001")
            .instructedAmount(PaymentInitiationRequest.Amount.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build())
            .debtorAccount(PaymentInitiationRequest.Account.builder()
                .schemeName("UK.OBIE.IBAN")
                .identification("GB33BUKB20201555555555")
                .name("John Doe")
                .build())
            .creditorAccount(PaymentInitiationRequest.Account.builder()
                .schemeName("UK.OBIE.IBAN")
                .identification("GB33BUKB20201666666666")
                .name("Jane Smith")
                .build())
            .remittanceInformation(PaymentInitiationRequest.RemittanceInformation.builder()
                .unstructured("Loan payment")
                .build())
            .build();

        samplePaymentResponse = PaymentInitiationResponse.builder()
            .paymentId(UUID.randomUUID().toString())
            .status(PaymentStatus.PENDING)
            .creationDateTime(LocalDateTime.now())
            .statusUpdateDateTime(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should fail - Payment initiation endpoint not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailCreatePaymentInitiation() throws Exception {
        // This test will fail initially as the endpoint doesn't exist
        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePaymentRequest))
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.PaymentId").exists())
            .andExpect(jsonPath("$.Data.Status").value("AwaitingAuthorisation"));
    }

    @Test
    @DisplayName("Should fail - Payment consent creation endpoint not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailCreatePaymentConsent() throws Exception {
        // This test will fail initially as the endpoint doesn't exist
        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payment-consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePaymentRequest))
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.ConsentId").exists())
            .andExpect(jsonPath("$.Data.Status").value("AwaitingAuthorisation"));
    }

    @Test
    @DisplayName("Should fail - Get payment status endpoint not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailGetPaymentStatus() throws Exception {
        // Given
        String paymentId = UUID.randomUUID().toString();
        when(paymentInitiationService.getPaymentStatus(paymentId))
            .thenReturn(samplePaymentResponse);

        // This test will fail initially as the endpoint doesn't exist
        mockMvc.perform(get("/open-banking/v3.1/pisp/domestic-payments/{paymentId}", paymentId)
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Data.PaymentId").value(paymentId))
            .andExpect(jsonPath("$.Data.Status").exists());
    }

    @Test
    @DisplayName("Should fail - FAPI headers validation not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailValidateFapiHeaders() throws Exception {
        // Test missing required FAPI headers
        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePaymentRequest))
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.Errors[0].ErrorCode").value("UK.OBIE.Header.Missing"))
            .andExpect(jsonPath("$.Errors[0].Message").value("Missing required FAPI headers"));
    }

    @Test
    @DisplayName("Should fail - Payment amount validation not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailValidatePaymentAmount() throws Exception {
        // Given - invalid payment amount
        PaymentInitiationRequest invalidRequest = samplePaymentRequest.toBuilder()
            .instructedAmount(PaymentInitiationRequest.Amount.builder()
                .amount(new BigDecimal("-100.00")) // Negative amount
                .currency("USD")
                .build())
            .build();

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.Errors[0].ErrorCode").value("UK.OBIE.Field.Invalid"))
            .andExpect(jsonPath("$.Errors[0].Message").value("Amount must be positive"));
    }

    @Test
    @DisplayName("Should fail - Account validation not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailValidateAccount() throws Exception {
        // Given - invalid account format
        PaymentInitiationRequest invalidRequest = samplePaymentRequest.toBuilder()
            .debtorAccount(PaymentInitiationRequest.Account.builder()
                .schemeName("UK.OBIE.IBAN")
                .identification("INVALID-IBAN")
                .name("John Doe")
                .build())
            .build();

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.Errors[0].ErrorCode").value("UK.OBIE.Field.Invalid"))
            .andExpect(jsonPath("$.Errors[0].Message").value("Invalid account identification"));
    }

    @Test
    @DisplayName("Should fail - Payment idempotency not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailHandleIdempotentRequests() throws Exception {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        when(paymentInitiationService.createPayment(any(), anyString()))
            .thenReturn(samplePaymentResponse);

        // First request
        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePaymentRequest))
                .header("x-idempotency-key", idempotencyKey)
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated());

        // Second identical request should return same result
        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePaymentRequest))
                .header("x-idempotency-key", idempotencyKey)
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.PaymentId").value(samplePaymentResponse.getPaymentId()));
    }

    @Test
    @DisplayName("Should fail - Payment consent authorization not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailAuthorizePaymentConsent() throws Exception {
        // Given
        String consentId = UUID.randomUUID().toString();

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payment-consents/{consentId}/authorize", consentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"authorisationCode\": \"AUTH123\"}")
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Data.Status").value("Authorised"));
    }

    @Test
    @DisplayName("Should fail - Payment notification webhook not implemented")
    void shouldFailHandlePaymentStatusNotification() throws Exception {
        // Given
        String notificationPayload = """
            {
                "eventType": "payment.status.changed",
                "paymentId": "%s",
                "status": "Completed",
                "timestamp": "2023-10-15T10:30:00Z"
            }
            """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/open-banking/webhooks/payment-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(notificationPayload)
                .header("x-webhook-signature", "signature-hash"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should fail - Bulk payment initiation not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailCreateBulkPayment() throws Exception {
        // Given - bulk payment request
        String bulkPaymentRequest = """
            {
                "Data": {
                    "Initiation": {
                        "InstructionIdentification": "BULK-001",
                        "RequestedExecutionDateTime": "2023-10-15T10:30:00Z",
                        "Payments": [
                            {
                                "InstructionIdentification": "INSTR-001",
                                "EndToEndIdentification": "E2E-001",
                                "InstructedAmount": {
                                    "Amount": "100.00",
                                    "Currency": "USD"
                                },
                                "CreditorAccount": {
                                    "SchemeName": "UK.OBIE.IBAN",
                                    "Identification": "GB33BUKB20201666666666",
                                    "Name": "Jane Smith"
                                }
                            }
                        ]
                    }
                }
            }
            """;

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payment-bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkPaymentRequest)
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.BulkPaymentId").exists())
            .andExpect(jsonPath("$.Data.Status").value("AwaitingAuthorisation"));
    }

    @Test
    @DisplayName("Should fail - Payment scheduling not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailCreateScheduledPayment() throws Exception {
        // Given - scheduled payment request
        PaymentInitiationRequest scheduledRequest = samplePaymentRequest.toBuilder()
            .requestedExecutionDateTime(LocalDateTime.now().plusDays(7))
            .build();

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-scheduled-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scheduledRequest))
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.ScheduledPaymentId").exists())
            .andExpect(jsonPath("$.Data.Status").value("AwaitingAuthorisation"));
    }

    @Test
    @DisplayName("Should fail - Payment refund not implemented")
    @WithMockUser(roles = "PAYMENT_INITIATION")
    void shouldFailCreatePaymentRefund() throws Exception {
        // Given
        String paymentId = UUID.randomUUID().toString();
        String refundRequest = """
            {
                "Data": {
                    "Refund": {
                        "RefundAmount": {
                            "Amount": "50.00",
                            "Currency": "USD"
                        },
                        "RefundReason": "Partial refund requested by customer"
                    }
                }
            }
            """;

        mockMvc.perform(post("/open-banking/v3.1/pisp/domestic-payments/{paymentId}/refund", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundRequest)
                .header("x-fapi-auth-date", "Sun, 10 Sep 2017 19:43:31 UTC")
                .header("x-fapi-customer-ip-address", "104.25.212.99")
                .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.Data.RefundId").exists())
            .andExpect(jsonPath("$.Data.Status").value("Pending"));
    }
}