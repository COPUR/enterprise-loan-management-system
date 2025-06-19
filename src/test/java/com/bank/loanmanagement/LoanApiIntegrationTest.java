package com.bank.loanmanagement;

import com.bank.loanmanagement.domain.customer.CreditCustomer;
import com.bank.loanmanagement.domain.loan.CreditLoan;
import com.bank.loanmanagement.infrastructure.repository.CreditCustomerRepository;
import com.bank.loanmanagement.infrastructure.repository.CreditLoanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Loan API Integration Tests - Business Rules Validation")
public class LoanApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreditCustomerRepository customerRepository;

    @Autowired
    private CreditLoanRepository loanRepository;

    private CreditCustomer testCustomer;

    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = CreditCustomer.builder()
            .name("John")
            .surname("Doe")
            .creditLimit(BigDecimal.valueOf(100000))
            .usedCreditLimit(BigDecimal.ZERO)
            .build();
        testCustomer = customerRepository.save(testCustomer);
    }

    @Nested
    @DisplayName("Create Loan Endpoint Tests")
    class CreateLoanEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create loan successfully with valid parameters")
        void shouldCreateLoanSuccessfully() throws Exception {
            // Given
            Map<String, Object> loanRequest = new HashMap<>();
            loanRequest.put("customerId", testCustomer.getId());
            loanRequest.put("amount", 10000);
            loanRequest.put("interestRate", 0.2);
            loanRequest.put("numberOfInstallments", 12);

            // When & Then
            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(testCustomer.getId()))
                .andExpect(jsonPath("$.loanAmount").value(10000))
                .andExpect(jsonPath("$.interestRate").value(0.2))
                .andExpect(jsonPath("$.numberOfInstallments").value(12))
                .andExpect(jsonPath("$.totalAmount").value(12000)) // 10000 * 1.2
                .andExpect(jsonPath("$.installments").isArray())
                .andExpect(jsonPath("$.installments", hasSize(12)))
                .andExpect(jsonPath("$.isPaid").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reject loan with insufficient credit limit")
        void shouldRejectLoanWithInsufficientCreditLimit() throws Exception {
            // Given - Customer with limited credit
            testCustomer.setUsedCreditLimit(BigDecimal.valueOf(95000)); // Only 5000 available
            customerRepository.save(testCustomer);

            Map<String, Object> loanRequest = new HashMap<>();
            loanRequest.put("customerId", testCustomer.getId());
            loanRequest.put("amount", 10000); // More than available
            loanRequest.put("interestRate", 0.2);
            loanRequest.put("numberOfInstallments", 12);

            // When & Then
            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Customer does not have enough credit limit"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reject loan with invalid number of installments")
        void shouldRejectLoanWithInvalidInstallments() throws Exception {
            // Test various invalid installment numbers
            int[] invalidInstallments = {1, 3, 5, 7, 8, 10, 11, 13, 18, 36};

            for (int installments : invalidInstallments) {
                Map<String, Object> loanRequest = new HashMap<>();
                loanRequest.put("customerId", testCustomer.getId());
                loanRequest.put("amount", 10000);
                loanRequest.put("interestRate", 0.2);
                loanRequest.put("numberOfInstallments", installments);

                mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loanRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Number of installments can only be 6, 9, 12, or 24"));
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should accept loan with valid number of installments")
        void shouldAcceptLoanWithValidInstallments() throws Exception {
            // Test valid installment numbers
            int[] validInstallments = {6, 9, 12, 24};

            for (int installments : validInstallments) {
                Map<String, Object> loanRequest = new HashMap<>();
                loanRequest.put("customerId", testCustomer.getId());
                loanRequest.put("amount", 5000); // Small amount to avoid credit limit issues
                loanRequest.put("interestRate", 0.2);
                loanRequest.put("numberOfInstallments", installments);

                mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loanRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numberOfInstallments").value(installments));
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reject loan with interest rate outside valid range")
        void shouldRejectLoanWithInvalidInterestRate() throws Exception {
            // Test below minimum
            Map<String, Object> loanRequest1 = new HashMap<>();
            loanRequest1.put("customerId", testCustomer.getId());
            loanRequest1.put("amount", 10000);
            loanRequest1.put("interestRate", 0.05); // Below 0.1
            loanRequest1.put("numberOfInstallments", 12);

            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loanRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interest rate must be between 0.1 and 0.5"));

            // Test above maximum
            Map<String, Object> loanRequest2 = new HashMap<>();
            loanRequest2.put("customerId", testCustomer.getId());
            loanRequest2.put("amount", 10000);
            loanRequest2.put("interestRate", 0.6); // Above 0.5
            loanRequest2.put("numberOfInstallments", 12);

            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loanRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interest rate must be between 0.1 and 0.5"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create installments with correct due dates")
        void shouldCreateInstallmentsWithCorrectDueDates() throws Exception {
            // Given
            Map<String, Object> loanRequest = new HashMap<>();
            loanRequest.put("customerId", testCustomer.getId());
            loanRequest.put("amount", 6000);
            loanRequest.put("interestRate", 0.2);
            loanRequest.put("numberOfInstallments", 6);

            // When & Then
            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loanRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installments", hasSize(6)))
                .andExpect(jsonPath("$.installments[0].dueDate").value(LocalDate.now().plusMonths(1).withDayOfMonth(1).toString()))
                .andExpect(jsonPath("$.installments[1].dueDate").value(LocalDate.now().plusMonths(2).withDayOfMonth(1).toString()))
                .andExpect(jsonPath("$.installments[*].amount", everyItem(is(1200.0)))); // 7200 / 6 = 1200
        }
    }

    @Nested
    @DisplayName("List Loans Endpoint Tests")
    class ListLoansEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all loans for a customer")
        void shouldListAllLoansForCustomer() throws Exception {
            // Given - Create multiple loans
            createTestLoan(testCustomer.getId(), 10000, 0.2, 12);
            createTestLoan(testCustomer.getId(), 5000, 0.3, 6);

            // When & Then
            mockMvc.perform(get("/api/loans")
                    .param("customerId", testCustomer.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].customerId", everyItem(is(testCustomer.getId().intValue()))));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter loans by number of installments")
        void shouldFilterLoansByNumberOfInstallments() throws Exception {
            // Given
            createTestLoan(testCustomer.getId(), 10000, 0.2, 12);
            createTestLoan(testCustomer.getId(), 5000, 0.3, 6);
            createTestLoan(testCustomer.getId(), 8000, 0.25, 12);

            // When & Then
            mockMvc.perform(get("/api/loans")
                    .param("customerId", testCustomer.getId().toString())
                    .param("numberOfInstallments", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].numberOfInstallments", everyItem(is(12))));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter loans by paid status")
        void shouldFilterLoansByPaidStatus() throws Exception {
            // Given
            CreditLoan loan1 = createTestLoan(testCustomer.getId(), 10000, 0.2, 12);
            CreditLoan loan2 = createTestLoan(testCustomer.getId(), 5000, 0.3, 6);
            
            // Mark one loan as paid
            loan1.setIsPaid(true);
            loanRepository.save(loan1);

            // When & Then
            mockMvc.perform(get("/api/loans")
                    .param("customerId", testCustomer.getId().toString())
                    .param("isPaid", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isPaid").value(false));
        }
    }

    @Nested
    @DisplayName("List Installments Endpoint Tests")
    class ListInstallmentsEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all installments for a loan")
        void shouldListAllInstallmentsForLoan() throws Exception {
            // Given
            CreditLoan loan = createTestLoan(testCustomer.getId(), 12000, 0.2, 12);

            // When & Then
            mockMvc.perform(get("/api/loans/{loanId}/installments", loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[*].amount", everyItem(is(1200.0)))) // 14400 / 12 = 1200
                .andExpect(jsonPath("$[*].isPaid", everyItem(is(false))))
                .andExpect(jsonPath("$[*].paidAmount", everyItem(is(0))));
        }
    }

    @Nested
    @DisplayName("Pay Loan Endpoint Tests")
    class PayLoanEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should pay installments wholly based on payment amount")
        void shouldPayInstallmentsWholly() throws Exception {
            // Given
            CreditLoan loan = createTestLoan(testCustomer.getId(), 12000, 0.2, 12);
            BigDecimal installmentAmount = BigDecimal.valueOf(1200); // 14400 / 12

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", installmentAmount.multiply(BigDecimal.valueOf(2.5))); // 3000 - should pay 2 installments

            // When & Then
            mockMvc.perform(post("/api/loans/{loanId}/pay", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentsPaid").value(2))
                .andExpect(jsonPath("$.totalAmountSpent").value(2400))
                .andExpect(jsonPath("$.isLoanFullyPaid").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should pay earliest installments first")
        void shouldPayEarliestInstallmentsFirst() throws Exception {
            // Given
            CreditLoan loan = createTestLoan(testCustomer.getId(), 12000, 0.2, 12);
            
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", 3600); // Pay 3 installments

            // When
            mockMvc.perform(post("/api/loans/{loanId}/pay", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentsPaid").value(3));

            // Then - Verify first 3 installments are paid
            mockMvc.perform(get("/api/loans/{loanId}/installments", loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isPaid").value(true))
                .andExpect(jsonPath("$[1].isPaid").value(true))
                .andExpect(jsonPath("$[2].isPaid").value(true))
                .andExpect(jsonPath("$[3].isPaid").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should mark loan as fully paid when all installments are paid")
        void shouldMarkLoanAsFullyPaid() throws Exception {
            // Given
            CreditLoan loan = createTestLoan(testCustomer.getId(), 6000, 0.2, 6);
            
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", 7200); // Pay entire loan (6000 * 1.2)

            // When & Then
            mockMvc.perform(post("/api/loans/{loanId}/pay", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentsPaid").value(6))
                .andExpect(jsonPath("$.isLoanFullyPaid").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should not allow payment of installments due more than 3 months in future")
        void shouldNotAllowPaymentOfFutureInstallments() throws Exception {
            // Given - This test verifies the business rule but implementation depends on current date
            CreditLoan loan = createTestLoan(testCustomer.getId(), 24000, 0.2, 24);
            
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", 28800); // Try to pay entire loan

            // When & Then - Should only pay installments within 3 months
            mockMvc.perform(post("/api/loans/{loanId}/pay", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentsPaid").value(lessThanOrEqualTo(3)));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should require authentication for all endpoints")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/loans"))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should require ADMIN role for loan operations")
        void shouldRequireAdminRole() throws Exception {
            mockMvc.perform(get("/api/loans"))
                .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should allow ADMIN role access to all operations")
        void shouldAllowAdminAccess() throws Exception {
            mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk());
        }
    }

    private CreditLoan createTestLoan(Long customerId, int amount, double interestRate, int installments) {
        CreditLoan loan = CreditLoan.builder()
            .customerId(customerId)
            .loanAmount(BigDecimal.valueOf(amount))
            .interestRate(BigDecimal.valueOf(interestRate))
            .numberOfInstallments(installments)
            .createDate(LocalDate.now())
            .isPaid(false)
            .build();
        loan.generateInstallments();
        return loanRepository.save(loan);
    }
}