package com.bank.loan.application;

import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.loan.domain.*;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Loan Management Service
 * 
 * Tests Functional Requirements:
 * - FR-005: Loan Application & Origination
 * - FR-006: Loan Approval Workflow
 * - FR-007: Loan Disbursement
 * - FR-008: Loan Payment Processing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Management Service Tests")
class LoanManagementServiceTest {
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private CustomerCreditService customerCreditService;
    
    private LoanManagementService loanService;
    
    @BeforeEach
    void setUp() {
        loanService = new LoanManagementService(loanRepository, customerCreditService);
    }
    
    @Test
    @DisplayName("FR-005: Should create new loan application with valid data")
    void shouldCreateNewLoanApplicationWithValidData() {
        // Given
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-12345678",
            BigDecimal.valueOf(100000),
            "USD",
            BigDecimal.valueOf(5.25),
            24 // 2 years
        );
        
        LoanId expectedLoanId = LoanId.generate();
        Loan expectedLoan = Loan.create(
            expectedLoanId,
            CustomerId.of(request.customerId()),
            request.getPrincipalAsMoney(),
            InterestRate.of(request.annualInterestRate()),
            LoanTerm.ofMonths(request.termInMonths())
        );
        
        when(customerCreditService.hasAvailableCredit(
            CustomerId.of(request.customerId()), 
            request.getPrincipalAsMoney())).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenReturn(expectedLoan);
        
        // When
        LoanResponse response = loanService.createLoanApplication(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo("CUST-12345678");
        assertThat(response.principalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(response.annualInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(5.25));
        assertThat(response.termInMonths()).isEqualTo(24);
        assertThat(response.status()).isEqualTo("CREATED");
        
        verify(customerCreditService).hasAvailableCredit(
            CustomerId.of(request.customerId()), 
            request.getPrincipalAsMoney());
        verify(loanRepository).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-005: Should reject loan application when customer has insufficient credit")
    void shouldRejectLoanApplicationWhenInsufficientCredit() {
        // Given
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-12345678",
            BigDecimal.valueOf(500000), // Large amount
            "USD",
            BigDecimal.valueOf(5.25),
            24
        );
        
        when(customerCreditService.hasAvailableCredit(
            CustomerId.of(request.customerId()), 
            request.getPrincipalAsMoney())).thenReturn(false);
        when(customerCreditService.getAvailableCredit(
            CustomerId.of(request.customerId()))).thenReturn(Money.usd(BigDecimal.valueOf(100000)));
        
        // When & Then
        assertThatThrownBy(() -> loanService.createLoanApplication(request))
            .isInstanceOf(InsufficientCreditException.class)
            .hasMessageContaining("insufficient credit");
        
        verify(customerCreditService).hasAvailableCredit(
            CustomerId.of(request.customerId()), 
            request.getPrincipalAsMoney());
        verify(loanRepository, never()).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-005: Should reject loan application with invalid data")
    void shouldRejectLoanApplicationWithInvalidData() {
        // Given
        CreateLoanRequest invalidRequest = new CreateLoanRequest(
            "CUST-12345678",
            BigDecimal.valueOf(-10000), // Negative amount
            "USD",
            BigDecimal.valueOf(5.25),
            24
        );
        
        // When & Then
        assertThatThrownBy(() -> loanService.createLoanApplication(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class);
        
        verify(loanRepository, never()).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-006: Should approve loan application")
    void shouldApproveLoanApplication() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(50000)),
            InterestRate.of(BigDecimal.valueOf(4.5)),
            LoanTerm.ofMonths(36)
        );
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LoanResponse response = loanService.approveLoan(loanId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.approvalDate()).isEqualTo(LocalDate.now());
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-006: Should reject loan application with reason")
    void shouldRejectLoanApplicationWithReason() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(50000)),
            InterestRate.of(BigDecimal.valueOf(4.5)),
            LoanTerm.ofMonths(36)
        );
        
        String rejectionReason = "Insufficient credit history";
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LoanResponse response = loanService.rejectLoan(loanId.getValue(), rejectionReason);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("REJECTED");
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-006: Should throw exception when loan not found for approval")
    void shouldThrowExceptionWhenLoanNotFoundForApproval() {
        // Given
        LoanId nonExistentId = LoanId.of("LOAN-99999999");
        when(loanRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> loanService.approveLoan(nonExistentId.getValue()))
            .isInstanceOf(LoanNotFoundException.class)
            .hasMessage("Loan not found with ID: LOAN-99999999");
        
        verify(loanRepository).findById(nonExistentId);
    }
    
    @Test
    @DisplayName("FR-007: Should disburse approved loan")
    void shouldDisburseApprovedLoan() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(75000)),
            InterestRate.of(BigDecimal.valueOf(3.75)),
            LoanTerm.ofMonths(60)
        );
        loan.approve(); // Set to approved status
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerCreditService.reserveCredit(
            CustomerId.of("CUST-12345678"), 
            Money.usd(BigDecimal.valueOf(75000)))).thenReturn(true);
        
        // When
        LoanResponse response = loanService.disburseLoan(loanId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("DISBURSED");
        assertThat(response.disbursementDate()).isEqualTo(LocalDate.now());
        assertThat(response.maturityDate()).isEqualTo(LocalDate.now().plusMonths(60));
        assertThat(response.outstandingBalance()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository).save(any(Loan.class));
        verify(customerCreditService).reserveCredit(
            CustomerId.of("CUST-12345678"), 
            Money.usd(BigDecimal.valueOf(75000)));
    }
    
    @Test
    @DisplayName("FR-007: Should reject disbursement when loan not approved")
    void shouldRejectDisbursementWhenLoanNotApproved() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(75000)),
            InterestRate.of(BigDecimal.valueOf(3.75)),
            LoanTerm.ofMonths(60)
        );
        // Loan is in CREATED status, not approved
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // When & Then
        assertThatThrownBy(() -> loanService.disburseLoan(loanId.getValue()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be disbursed");
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository, never()).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-008: Should process loan payment")
    void shouldProcessLoanPayment() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(60000)),
            InterestRate.of(BigDecimal.valueOf(4.0)),
            LoanTerm.ofMonths(48)
        );
        loan.approve();
        loan.disburse();
        
        Money paymentAmount = Money.usd(BigDecimal.valueOf(2000));
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        LoanResponse response = loanService.makePayment(loanId.getValue(), paymentAmount);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.outstandingBalance()).isEqualByComparingTo(BigDecimal.valueOf(58000));
        assertThat(response.status()).isEqualTo("DISBURSED"); // Still active
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-008: Should mark loan as fully paid when balance reaches zero")
    void shouldMarkLoanAsFullyPaidWhenBalanceReachesZero() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(5000)),
            InterestRate.of(BigDecimal.valueOf(4.0)),
            LoanTerm.ofMonths(12)
        );
        loan.approve();
        loan.disburse();
        
        Money fullPayment = Money.usd(BigDecimal.valueOf(5000)); // Pay full amount
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerCreditService.releaseCredit(
            CustomerId.of("CUST-12345678"), 
            Money.usd(BigDecimal.valueOf(5000)))).thenReturn(true);
        
        // When
        LoanResponse response = loanService.makePayment(loanId.getValue(), fullPayment);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.outstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.status()).isEqualTo("FULLY_PAID");
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository).save(any(Loan.class));
        verify(customerCreditService).releaseCredit(
            CustomerId.of("CUST-12345678"), 
            Money.usd(BigDecimal.valueOf(5000)));
    }
    
    @Test
    @DisplayName("FR-008: Should reject payment exceeding outstanding balance")
    void shouldRejectPaymentExceedingOutstandingBalance() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(10000)),
            InterestRate.of(BigDecimal.valueOf(4.0)),
            LoanTerm.ofMonths(24)
        );
        loan.approve();
        loan.disburse();
        
        Money excessivePayment = Money.usd(BigDecimal.valueOf(15000)); // More than balance
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // When & Then
        assertThatThrownBy(() -> loanService.makePayment(loanId.getValue(), excessivePayment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot exceed outstanding balance");
        
        verify(loanRepository).findById(loanId);
        verify(loanRepository, never()).save(any(Loan.class));
    }
    
    @Test
    @DisplayName("FR-008: Should calculate monthly payment correctly")
    void shouldCalculateMonthlyPaymentCorrectly() {
        // Given
        LoanId loanId = LoanId.of("LOAN-12345678");
        Loan loan = Loan.create(
            loanId,
            CustomerId.of("CUST-12345678"),
            Money.usd(BigDecimal.valueOf(100000)),
            InterestRate.of(BigDecimal.valueOf(6.0)), // 6% annual
            LoanTerm.ofMonths(360) // 30 years
        );
        
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        
        // When
        LoanResponse response = loanService.findLoanById(loanId.getValue());
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.monthlyPayment()).isGreaterThan(BigDecimal.valueOf(599));
        assertThat(response.monthlyPayment()).isLessThan(BigDecimal.valueOf(601)); // ~$599.55
        
        verify(loanRepository).findById(loanId);
    }
}