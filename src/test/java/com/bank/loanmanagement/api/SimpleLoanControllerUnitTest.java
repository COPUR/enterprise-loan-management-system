package com.bank.loanmanagement.api;

import com.bank.loanmanagement.api.controller.SimpleLoanController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for SimpleLoanController
 * Tests core functionality without Spring context
 */
public class SimpleLoanControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private SimpleLoanController controller;

    @BeforeEach
    public void setUp() {
        controller = new SimpleLoanController();
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Unit Test: Create Loan - Should return valid response structure")
    public void testCreateLoanBasic() throws Exception {
        // Given: Valid loan request
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("customerId", "CUST001");
        loanRequest.put("amount", 10000);
        loanRequest.put("interestRate", 0.05);
        loanRequest.put("numberOfInstallments", 12);

        // When & Then: Create loan endpoint should respond
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Unit Test: Get Loans by Customer - Should handle GET request")
    public void testGetLoansByCustomerBasic() throws Exception {
        // When & Then: Get loans endpoint should respond
        mockMvc.perform(get("/api/v1/loans")
                .param("customerId", "CUST002"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Unit Test: Get Installments - Should handle GET request")
    public void testGetInstallmentsBasic() throws Exception {
        // When & Then: Get installments endpoint should return 404 (not implemented yet)
        mockMvc.perform(get("/api/v1/loans/LOAN001/installments"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unit Test: Pay Installment - Should handle POST request")
    public void testPayInstallmentBasic() throws Exception {
        // Given: Payment request
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("amount", 1000.0);

        // When & Then: Pay installment endpoint should return 404 (not implemented yet)
        mockMvc.perform(post("/api/v1/loans/LOAN001/installments/1/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isNotFound());
    }
}