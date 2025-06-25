package com.bank.loanmanagement.api;

import com.bank.loanmanagement.LoanManagementApplication;
import com.bank.loanmanagement.api.controller.SimpleLoanController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for SimpleLoanController
 * Tests all 4 core business requirements from Orange Solution case study
 */
@SpringBootTest(classes = LoanManagementApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("simple")
public class SimpleLoanControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Business Requirement 1: Create Loan - Should create loan with proper calculation")
    public void testCreateLoan() throws Exception {
        setUp();
        
        // Given: Valid loan request
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST001");
        loanRequest.put("amount", 10000);
        loanRequest.put("interestRate", 0.05);
        loanRequest.put("numberOfInstallments", 12);

        // When: Create loan
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                // Then: Should return 201 Created with loan details
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId", notNullValue()))
                .andExpect(jsonPath("$.customerId", is("CUST001")))
                .andExpect(jsonPath("$.amount", is(10000)))
                .andExpect(jsonPath("$.interestRate", is(0.05)))
                .andExpect(jsonPath("$.numberOfInstallments", is(12)))
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.monthlyPayment", notNullValue()))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("Business Requirement 1: Create Loan - Should validate required fields")
    public void testCreateLoanValidation() throws Exception {
        setUp();
        
        // Given: Invalid loan request (missing customerId)
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("amount", 10000);
        invalidRequest.put("interestRate", 0.05);

        // When: Attempt to create loan
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Missing required fields")));
    }

    @Test
    @DisplayName("Business Requirement 2: List Loans by Customer - Should return customer loans")
    public void testGetLoansByCustomer() throws Exception {
        setUp();
        
        // Given: Create a loan first
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST002");
        loanRequest.put("amount", 5000);
        loanRequest.put("interestRate", 0.04);
        loanRequest.put("numberOfInstallments", 6);

        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated());

        // When: Get loans by customer
        mockMvc.perform(get("/api/v1/loans")
                .param("customerId", "CUST002"))
                // Then: Should return customer's loans
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].customerId", is("CUST002")))
                .andExpect(jsonPath("$[0].amount", is(5000)));
    }

    @Test
    @DisplayName("Business Requirement 2: List Loans by Customer - Should return empty array for unknown customer")
    public void testGetLoansForUnknownCustomer() throws Exception {
        setUp();
        
        // When: Get loans for non-existent customer
        mockMvc.perform(get("/api/v1/loans")
                .param("customerId", "UNKNOWN_CUSTOMER"))
                // Then: Should return empty array
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Business Requirement 3: List Installments by Loan - Should return loan installments")
    public void testGetInstallmentsByLoan() throws Exception {
        setUp();
        
        // Given: Create a loan first
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST003");
        loanRequest.put("amount", 12000);
        loanRequest.put("interestRate", 0.06);
        loanRequest.put("numberOfInstallments", 12);

        String response = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> createdLoan = objectMapper.readValue(response, Map.class);
        String loanId = (String) createdLoan.get("loanId");

        // When: Get installments for the loan
        mockMvc.perform(get("/api/v1/loans/{loanId}/installments", loanId))
                // Then: Should return all installments
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].installmentNumber", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[0].amount", notNullValue()))
                .andExpect(jsonPath("$[0].principalAmount", notNullValue()))
                .andExpect(jsonPath("$[0].interestAmount", notNullValue()))
                .andExpect(jsonPath("$[0].dueDate", notNullValue()))
                .andExpect(jsonPath("$[11].installmentNumber", is(12)))
                .andExpect(jsonPath("$[11].remainingBalance", is(0.0)));
    }

    @Test
    @DisplayName("Business Requirement 3: List Installments - Should return 404 for non-existent loan")
    public void testGetInstallmentsForNonExistentLoan() throws Exception {
        setUp();
        
        // When: Get installments for non-existent loan
        mockMvc.perform(get("/api/v1/loans/NON_EXISTENT_LOAN/installments"))
                // Then: Should return 404 Not Found
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Business Requirement 4: Pay Installment - Should process payment successfully")
    public void testPayInstallment() throws Exception {
        setUp();
        
        // Given: Create a loan first
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST004");
        loanRequest.put("amount", 6000);
        loanRequest.put("interestRate", 0.05);
        loanRequest.put("numberOfInstallments", 6);

        String loanResponse = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> createdLoan = objectMapper.readValue(loanResponse, Map.class);
        String loanId = (String) createdLoan.get("loanId");

        // Given: Payment request
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", 1000.0);

        // When: Pay first installment
        mockMvc.perform(post("/api/v1/loans/{loanId}/installments/1/pay", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                // Then: Should return payment confirmation
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId", is(loanId)))
                .andExpect(jsonPath("$.installmentNumber", is(1)))
                .andExpect(jsonPath("$.amount", is(1000.0)))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.paidAt", notNullValue()));

        // Verify installment is marked as paid
        mockMvc.perform(get("/api/v1/loans/{loanId}/installments", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("PAID")))
                .andExpect(jsonPath("$[0].paidAmount", is(1000.0)))
                .andExpect(jsonPath("$[0].paidAt", notNullValue()));
    }

    @Test
    @DisplayName("Business Requirement 4: Pay Installment - Should prevent double payment")
    public void testPreventDoublePayment() throws Exception {
        setUp();
        
        // Given: Create a loan and pay first installment
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST005");
        loanRequest.put("amount", 3000);
        loanRequest.put("interestRate", 0.04);
        loanRequest.put("numberOfInstallments", 3);

        String loanResponse = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> createdLoan = objectMapper.readValue(loanResponse, Map.class);
        String loanId = (String) createdLoan.get("loanId");

        // Pay first installment
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", 1000.0);

        mockMvc.perform(post("/api/v1/loans/{loanId}/installments/1/pay", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        // When: Attempt to pay the same installment again
        mockMvc.perform(post("/api/v1/loans/{loanId}/installments/1/pay", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                // Then: Should return error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("already paid")));
    }

    @Test
    @DisplayName("Business Requirement 4: Pay Installment - Should validate installment number")
    public void testPayInvalidInstallmentNumber() throws Exception {
        setUp();
        
        // Given: Create a loan
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST006");
        loanRequest.put("amount", 2000);
        loanRequest.put("interestRate", 0.03);
        loanRequest.put("numberOfInstallments", 2);

        String loanResponse = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> createdLoan = objectMapper.readValue(loanResponse, Map.class);
        String loanId = (String) createdLoan.get("loanId");

        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", 1000.0);

        // When: Attempt to pay invalid installment number
        mockMvc.perform(post("/api/v1/loans/{loanId}/installments/99/pay", loanId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                // Then: Should return error
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Invalid installment number")));
    }
}