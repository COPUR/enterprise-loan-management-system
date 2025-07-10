package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for missing Loan domain model methods
 * 
 * This test drives the implementation of missing methods identified by compilation errors
 */
@DisplayName("Loan Domain Methods TDD Test")
class LoanDomainMethodsTest {
    
    private Loan loan;
    private LoanId loanId;
    private CustomerId customerId;
    private Money principalAmount;
    private LoanTerms terms;
    private LoanOfficerId officerId;
    
    @BeforeEach
    void setUp() {
        loanId = LoanId.generate();
        customerId = CustomerId.generate();
        principalAmount = Money.of("USD", new BigDecimal("10000.00"));
        terms = LoanTerms.builder()
                .termInMonths(12)
                .interestRate(new BigDecimal("5.0"))
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        officerId = LoanOfficerId.generate();
        
        loan = Loan.createApplication(loanId, customerId, principalAmount, 
                                    LoanPurpose.HOME_IMPROVEMENT, terms, officerId);
    }
    
    @Test
    @DisplayName("Should have canBeCompleted method")
    void shouldHaveCanBeCompletedMethod() {
        // Given - a loan in PENDING status
        
        // When & Then - method should exist and return false for pending loan
        assertThat(loan.canBeCompleted()).isFalse();
        
        // When - loan is approved and activated with zero balance
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(principalAmount)
                .approvedTerms(terms)
                .conditions(java.util.List.of("Standard approval"))
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();
                
        loan.approve(conditions, officerId);
        
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber("123456789")
                .routingNumber("987654321")
                .disbursementMethod(DisbursementMethod.BANK_TRANSFER)
                .disbursementDate(java.time.LocalDate.now())
                .build();
                
        loan.disburse(instructions, officerId);
        
        // Still can't be completed with outstanding balance
        assertThat(loan.canBeCompleted()).isFalse();
    }
    
    @Test
    @DisplayName("Should have markAsCompleted method")
    void shouldHaveMarkAsCompletedMethod() {
        // Given - an active loan
        prepareActiveLoan();
        
        // When - trying to mark as completed with outstanding balance
        // Then - should throw exception
        assertThatThrownBy(() -> loan.markAsCompleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be completed");
    }
    
    @Test
    @DisplayName("Should have getTotalAmount method")
    void shouldHaveGetTotalAmountMethod() {
        // When & Then - method should exist and return principal amount
        assertThat(loan.getTotalAmount()).isEqualTo(principalAmount);
    }
    
    @Test
    @DisplayName("Should have canBeRestructured method")
    void shouldHaveCanBeRestructuredMethod() {
        // Given - a pending loan
        
        // When & Then - pending loan cannot be restructured
        assertThat(loan.canBeRestructured()).isFalse();
        
        // When - loan becomes active
        prepareActiveLoan();
        
        // Then - active loan can be restructured
        assertThat(loan.canBeRestructured()).isTrue();
    }
    
    @Test
    @DisplayName("Should have canBeActivated method")
    void shouldHaveCanBeActivatedMethod() {
        // Given - a pending loan
        
        // When & Then - pending loan cannot be activated
        assertThat(loan.canBeActivated()).isFalse();
        
        // When - loan is approved
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(principalAmount)
                .approvedTerms(terms)
                .conditions(java.util.List.of("Standard approval"))
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();
                
        loan.approve(conditions, officerId);
        
        // Then - approved loan can be activated
        assertThat(loan.canBeActivated()).isTrue();
    }
    
    @Test
    @DisplayName("Should have getApplicationDate method")
    void shouldHaveGetApplicationDateMethod() {
        // When & Then - method should exist and return creation date
        assertThat(loan.getApplicationDate()).isNotNull();
        assertThat(loan.getApplicationDate()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should have getInterestRate method")
    void shouldHaveGetInterestRateMethod() {
        // When & Then - method should exist and return current terms interest rate
        assertThat(loan.getInterestRate()).isEqualTo(new BigDecimal("5.0"));
    }
    
    @Test
    @DisplayName("Should have getTermMonths method")
    void shouldHaveGetTermMonthsMethod() {
        // When & Then - method should exist and return current terms months
        assertThat(loan.getTermMonths()).isEqualTo(12);
    }
    
    private void prepareActiveLoan() {
        // Approve loan
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(principalAmount)
                .approvedTerms(terms)
                .conditions(java.util.List.of("Standard approval"))
                .expirationDate(LocalDateTime.now().plusDays(30))
                .build();
                
        loan.approve(conditions, officerId);
        
        // Disburse loan
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber("123456789")
                .routingNumber("987654321")
                .disbursementMethod(DisbursementMethod.BANK_TRANSFER)
                .disbursementDate(java.time.LocalDate.now())
                .build();
                
        loan.disburse(instructions, officerId);
    }
}