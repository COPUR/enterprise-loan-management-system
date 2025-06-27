package com.banking.loan.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive functional tests for payment processing workflow
 * Tests payment processing, history, and early payment calculations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment Processing Workflow Functional Tests")
public class PaymentProcessingWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully process loan payment")
    public void shouldProcessLoanPayment() throws Exception {
        // Given: A valid payment request
        String loanId = "LOAN-PAYMENT-001";
        String paymentJson = """
            {
                "amount": 2500.00,
                "paymentMethod": "BANK_TRANSFER",
                "paymentReference": "PAY-%s",
                "notes": "Monthly installment payment",
                "paymentChannel": "ONLINE_BANKING"
            }
            """.formatted(UUID.randomUUID().toString().substring(0, 8));

        // When: Process payment
        mockMvc.perform(post("/api/v1/loans/{loanId}/payments", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        System.out.println("✅ Payment Processing: Successfully processed loan payment");
    }

    @Test
    @DisplayName("Should retrieve payment history")
    public void shouldRetrievePaymentHistory() throws Exception {
        // Given: A loan with payment history
        String loanId = "LOAN-HISTORY-001";

        // When: Retrieve payment history
        mockMvc.perform(get("/api/v1/loans/{loanId}/payments", loanId)
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        System.out.println("✅ Payment History: Successfully retrieved payment history");
    }

    @Test
    @DisplayName("Should calculate early payment options")
    public void shouldCalculateEarlyPaymentOptions() throws Exception {
        // Given: A loan eligible for early payment
        String loanId = "LOAN-EARLY-001";

        // When: Calculate early payment options
        mockMvc.perform(get("/api/v1/loans/{loanId}/early-payment", loanId)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        System.out.println("✅ Early Payment: Successfully calculated early payment options");
    }

    @Test
    @DisplayName("Should validate payment business rules")
    public void shouldValidatePaymentBusinessRules() throws Exception {
        // Given: Invalid payment (negative amount)
        String loanId = "LOAN-INVALID-001";
        String invalidPaymentJson = """
            {
                "amount": -100.00,
                "paymentMethod": "CASH",
                "paymentReference": "INVALID-REF",
                "notes": "Invalid payment",
                "paymentChannel": "BRANCH"
            }
            """;

        // When & Then: Should reject invalid payment
        mockMvc.perform(post("/api/v1/loans/{loanId}/payments", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPaymentJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andReturn();

        System.out.println("✅ Payment Validation: Correctly rejected invalid payment");
    }

    @Test
    @DisplayName("Should handle different payment methods")
    public void shouldHandleDifferentPaymentMethods() throws Exception {
        // Given: Various payment methods
        String loanId = "LOAN-METHODS-001";
        
        String[] paymentMethods = {"BANK_TRANSFER", "CREDIT_CARD", "DEBIT_CARD", "ONLINE_BANKING"};
        
        for (String method : paymentMethods) {
            String paymentJson = """
                {
                    "amount": 1000.00,
                    "paymentMethod": "%s",
                    "paymentReference": "REF-%s",
                    "notes": "Payment via %s",
                    "paymentChannel": "DIGITAL"
                }
                """.formatted(method, UUID.randomUUID().toString().substring(0, 6), method);

            // When: Process payment with different methods
            mockMvc.perform(post("/api/v1/loans/{loanId}/payments", loanId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentJson)
                    .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        System.out.println("✅ Payment Methods: Successfully handled all payment methods");
    }

    @Test
    @DisplayName("Should handle Islamic banking payment compliance")
    public void shouldHandleIslamicPaymentCompliance() throws Exception {
        // Given: Islamic banking compliant payment
        String loanId = "LOAN-ISLAMIC-001";
        String islamicPaymentJson = """
            {
                "amount": 3000.00,
                "paymentMethod": "ISLAMIC_BANK_TRANSFER",
                "paymentReference": "ISLAMIC-PAY-001",
                "notes": "Sharia compliant payment",
                "paymentChannel": "ISLAMIC_BANKING"
            }
            """;

        // When: Process Islamic compliant payment
        mockMvc.perform(post("/api/v1/loans/{loanId}/payments", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(islamicPaymentJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✅ Islamic Payment: Successfully processed Sharia compliant payment");
    }
}