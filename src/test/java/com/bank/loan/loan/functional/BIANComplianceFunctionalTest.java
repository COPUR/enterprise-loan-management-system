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
 * BIAN (Banking Industry Architecture Network) compliance functional tests
 * Tests BIAN service domains and standard banking operations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BIAN Compliance Functional Tests")
public class BIANComplianceFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should handle BIAN Consumer Loan service domain")
    public void shouldHandleBIANConsumerLoan() throws Exception {
        // Given: BIAN Consumer Loan request
        String bianLoanJson = """
            {
                "customerReference": "CUST-BIAN-001",
                "loanAmount": {
                    "amount": 45000.00,
                    "currency": "USD"
                },
                "loanTerm": "24M",
                "loanType": "CONSUMER",
                "collateralType": "UNSECURED",
                "requestedDate": "2024-01-15"
            }
            """;

        // When: Submit BIAN compliant loan request
        mockMvc.perform(post("/api/bian/v1/consumer-loan/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bianLoanJson)
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value("CUST-BIAN-001"))
                .andExpect(jsonPath("$.loanAmount.amount").value(45000.00))
                .andReturn();

        System.out.println("✅ BIAN Consumer Loan: Successfully processed BIAN consumer loan request");
    }

    @Test
    @DisplayName("Should handle BIAN Payment Initiation service domain")
    public void shouldHandleBIANPaymentInitiation() throws Exception {
        // Given: BIAN Payment Initiation request
        String bianPaymentJson = """
            {
                "paymentTransactionReference": "TXN-BIAN-001",
                "payerReference": "PAYER-001",
                "payeeReference": "PAYEE-001",
                "amount": {
                    "amount": 2500.00,
                    "currency": "USD"
                },
                "paymentMethod": "ELECTRONIC_TRANSFER",
                "executionDate": "2024-01-15"
            }
            """;

        // When: Initiate BIAN payment
        mockMvc.perform(post("/api/bian/v1/payment-initiation/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bianPaymentJson)
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTransactionReference").value("TXN-BIAN-001"))
                .andReturn();

        System.out.println("✅ BIAN Payment Initiation: Successfully initiated BIAN payment");
    }

    @Test
    @DisplayName("Should retrieve BIAN service domain information")
    public void shouldRetrieveBIANServiceDomainInfo() throws Exception {
        // When: Retrieve BIAN service domains
        mockMvc.perform(get("/api/bian/v1/service-domains")
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        System.out.println("✅ BIAN Service Domains: Successfully retrieved BIAN service domain information");
    }

    @Test
    @DisplayName("Should handle BIAN Credit Risk Assessment")
    public void shouldHandleBIANCreditRiskAssessment() throws Exception {
        // Given: BIAN Credit Risk Assessment request
        String riskAssessmentJson = """
            {
                "customerReference": "CUST-RISK-001",
                "assessmentType": "COMPREHENSIVE",
                "loanAmount": {
                    "amount": 75000.00,
                    "currency": "USD"
                },
                "assessmentCriteria": [
                    "CREDIT_HISTORY",
                    "INCOME_VERIFICATION",
                    "DEBT_TO_INCOME_RATIO"
                ]
            }
            """;

        // When: Perform BIAN credit risk assessment
        mockMvc.perform(post("/api/bian/v1/credit-risk-assessment/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(riskAssessmentJson)
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value("CUST-RISK-001"))
                .andReturn();

        System.out.println("✅ BIAN Credit Risk: Successfully performed BIAN credit risk assessment");
    }

    @Test
    @DisplayName("Should validate BIAN request structure")
    public void shouldValidateBIANRequestStructure() throws Exception {
        // Given: Invalid BIAN request (missing required fields)
        String invalidBianJson = """
            {
                "invalidField": "invalid_value"
            }
            """;

        // When & Then: Should reject invalid BIAN request
        mockMvc.perform(post("/api/bian/v1/consumer-loan/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBianJson)
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isBadRequest())
                .andReturn();

        System.out.println("✅ BIAN Validation: Correctly validated BIAN request structure");
    }

    @Test
    @DisplayName("Should handle BIAN Account Information Service")
    public void shouldHandleBIANAccountInformation() throws Exception {
        // Given: Account information request
        String accountId = "ACC-BIAN-001";

        // When: Retrieve BIAN account information
        mockMvc.perform(get("/api/bian/v1/account-information/{accountId}", accountId)
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        System.out.println("✅ BIAN Account Info: Successfully retrieved BIAN account information");
    }

    @Test
    @DisplayName("Should ensure BIAN compliance across multiple service domains")
    public void shouldEnsureBIANComplianceAcrossServiceDomains() throws Exception {
        // Test multiple BIAN service domains in sequence
        
        // 1. Customer Relationship Management
        mockMvc.perform(get("/api/bian/v1/customer-relationship-management/customers")
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk());

        // 2. Product Inventory Management
        mockMvc.perform(get("/api/bian/v1/product-inventory/loan-products")
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk());

        // 3. Regulatory Compliance
        mockMvc.perform(get("/api/bian/v1/regulatory-compliance/status")
                .header("Authorization", "Bearer bian-token"))
                .andExpect(status().isOk());

        System.out.println("✅ BIAN Multi-Domain: Successfully verified BIAN compliance across service domains");
    }
}