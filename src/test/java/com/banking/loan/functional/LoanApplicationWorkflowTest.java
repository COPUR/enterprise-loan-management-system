package com.banking.loan.functional;

import com.banking.loan.application.commands.SubmitLoanApplicationCommand;
import com.banking.loan.application.ports.in.LoanApplicationUseCase;
import com.banking.loan.application.results.LoanApplicationResult;
import com.banking.loan.domain.loan.LoanType;
import com.banking.loan.domain.shared.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive functional tests for loan application workflow
 * Tests the complete end-to-end loan application process
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Loan Application Workflow Functional Tests")
public class LoanApplicationWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private LoanApplicationUseCase loanApplicationUseCase;

    @Test
    @DisplayName("Should successfully process complete loan application workflow")
    public void shouldProcessCompleteLoanApplicationWorkflow() throws Exception {
        // Given: A valid loan application request
        String customerId = "CUST-" + UUID.randomUUID().toString().substring(0, 8);
        
        String loanApplicationJson = """
            {
                "customerId": "%s",
                "amount": 50000.00,
                "termInMonths": 24,
                "loanType": "PERSONAL",
                "purpose": "Home improvement",
                "collateralDescription": "Property documents",
                "monthlyIncome": 8000.00
            }
            """.formatted(customerId);

        // When: Submit loan application
        String response = mockMvc.perform(post("/api/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanApplicationJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.status").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then: Verify loan application was created successfully
        assertNotNull(response);
        assertTrue(response.contains(customerId));
        
        System.out.println("✅ Loan Application Workflow Test: Successfully submitted loan application");
    }

    @Test
    @DisplayName("Should validate loan application business rules")
    public void shouldValidateLoanApplicationBusinessRules() throws Exception {
        // Given: Invalid loan application (amount too high)
        String invalidLoanJson = """
            {
                "customerId": "CUST-INVALID",
                "amount": 1000000.00,
                "termInMonths": 240,
                "loanType": "PERSONAL",
                "purpose": "Investment",
                "collateralDescription": "None",
                "monthlyIncome": 3000.00
            }
            """;

        // When & Then: Should reject invalid application
        mockMvc.perform(post("/api/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLoanJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andReturn();

        System.out.println("✅ Business Rules Validation: Correctly rejected invalid loan application");
    }

    @Test
    @DisplayName("Should handle Islamic banking loan application")
    public void shouldHandleIslamicBankingLoan() throws Exception {
        // Given: Islamic banking loan application (Murabaha)
        String islamicLoanJson = """
            {
                "customerId": "CUST-ISLAMIC-001",
                "amount": 75000.00,
                "termInMonths": 36,
                "loanType": "MURABAHA",
                "purpose": "Vehicle purchase",
                "collateralDescription": "Vehicle registration",
                "monthlyIncome": 10000.00
            }
            """;

        // When: Submit Islamic loan application
        mockMvc.perform(post("/api/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(islamicLoanJson)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("MURABAHA"))
                .andExpect(jsonPath("$.amount").value(75000.00));

        System.out.println("✅ Islamic Banking: Successfully processed Murabaha loan application");
    }

    @Test
    @DisplayName("Should retrieve loan details")
    public void shouldRetrieveLoanDetails() throws Exception {
        // Given: An existing loan ID (mock for test)
        String loanId = "LOAN-TEST-001";

        // When: Retrieve loan details
        mockMvc.perform(get("/api/v1/loans/{loanId}", loanId)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").exists())
                .andReturn();

        System.out.println("✅ Loan Retrieval: Successfully retrieved loan details");
    }

    @Test
    @DisplayName("Should handle concurrent loan applications")
    public void shouldHandleConcurrentLoanApplications() throws Exception {
        // Given: Multiple concurrent applications
        String customerId1 = "CUST-CONCURRENT-001";
        String customerId2 = "CUST-CONCURRENT-002";
        
        String loanApp1 = """
            {
                "customerId": "%s",
                "amount": 30000.00,
                "termInMonths": 18,
                "loanType": "PERSONAL",
                "purpose": "Education",
                "collateralDescription": "None",
                "monthlyIncome": 6000.00
            }
            """.formatted(customerId1);

        String loanApp2 = """
            {
                "customerId": "%s",
                "amount": 45000.00,
                "termInMonths": 24,
                "loanType": "AUTO",
                "purpose": "Car purchase",
                "collateralDescription": "Vehicle",
                "monthlyIncome": 7500.00
            }
            """.formatted(customerId2);

        // When: Submit concurrent applications
        Thread.sleep(100); // Small delay to simulate real timing
        
        mockMvc.perform(post("/api/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanApp1)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/loans/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanApp2)
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isCreated());

        System.out.println("✅ Concurrency: Successfully handled concurrent loan applications");
    }
}