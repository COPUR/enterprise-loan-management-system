package com.loanmanagement.tdd;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.service.LoanFacadeService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for Service Layer - Validates core service methods work correctly
 */
public class TDDServiceLayerTest {
    
    @Test
    @DisplayName("Should create loan using LoanFacadeService with proper domain objects")
    void shouldCreateLoanUsingFacadeService() {
        // Given
        LoanFacadeService loanFacadeService = new LoanFacadeService();
        Money principalAmount = Money.of(Currency.getInstance("USD"), new BigDecimal("10000"));
        BigDecimal interestRate = new BigDecimal("5.5");
        Integer termMonths = 36;
        Long customerId = 1L;
        
        // When
        Loan loan = loanFacadeService.createSimpleLoan(customerId, principalAmount, interestRate, termMonths);
        
        // Then
        assertThat(loan).isNotNull();
        assertThat(loan.getPrincipalAmount()).isEqualTo(principalAmount);
        assertThat(loan.getInterestRate()).isEqualTo(interestRate);
        assertThat(loan.getTermMonths()).isEqualTo(termMonths);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(loan.getCustomerId()).isNotNull();
        assertThat(loan.getId()).isNotNull();
    }
    
    @Test
    @DisplayName("Should create domain events with proper structure")
    void shouldCreateDomainEventsWithProperStructure() {
        // Given
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        Money amount = Money.of(Currency.getInstance("USD"), new BigDecimal("10000"));
        
        // When - Create LoanApplicationSubmittedEvent
        var applicationEvent = com.loanmanagement.loan.domain.event.LoanApplicationSubmittedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .requestedAmount(amount)
                .loanPurpose(LoanPurpose.PERSONAL)
                .requestedTerms(LoanTerms.builder()
                        .interestRate(new BigDecimal("5.5"))
                        .termInMonths(36)
                        .paymentFrequency(PaymentFrequency.MONTHLY)
                        .build())
                .loanOfficerId(LoanOfficerId.generate())
                .applicationDate(LocalDateTime.now())
                .build();
        
        // Then
        assertThat(applicationEvent).isNotNull();
        assertThat(applicationEvent.getAggregateId()).isEqualTo(loanId.getValue());
        assertThat(applicationEvent.getEventType()).isEqualTo("LoanApplicationSubmittedEvent");
        assertThat(applicationEvent.getRequestedAmount()).isEqualTo(amount);
        
        // When - Create LoanApprovedEvent
        var approvedEvent = com.loanmanagement.loan.domain.event.LoanApprovedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .approvedAmount(amount)
                .approvedTerms(LoanTerms.builder()
                        .interestRate(new BigDecimal("5.5"))
                        .termInMonths(36)
                        .paymentFrequency(PaymentFrequency.MONTHLY)
                        .build())
                .conditions(java.util.List.of("Standard approval"))
                .approvalDate(LocalDateTime.now())
                .approvingOfficerId(LoanOfficerId.generate())
                .approvalExpirationDate(java.time.LocalDate.now().plusDays(30))
                .termsModified(false)
                .build();
        
        // Then
        assertThat(approvedEvent).isNotNull();
        assertThat(approvedEvent.getAggregateId()).isEqualTo(loanId.getValue());
        assertThat(approvedEvent.getEventType()).isEqualTo("LoanApprovedEvent");
        assertThat(approvedEvent.getApprovedAmount()).isEqualTo(amount);
    }
    
    @Test
    @DisplayName("Should validate all domain events implement DomainEvent interface")
    void shouldValidateAllDomainEventsImplementDomainEvent() {
        // Given
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        Money amount = Money.of(Currency.getInstance("USD"), new BigDecimal("10000"));
        
        // When & Then - Test LoanApplicationSubmittedEvent
        var applicationEvent = com.loanmanagement.loan.domain.event.LoanApplicationSubmittedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .requestedAmount(amount)
                .loanPurpose(LoanPurpose.PERSONAL)
                .requestedTerms(LoanTerms.builder()
                        .interestRate(new BigDecimal("5.5"))
                        .termInMonths(36)
                        .paymentFrequency(PaymentFrequency.MONTHLY)
                        .build())
                .loanOfficerId(LoanOfficerId.generate())
                .applicationDate(LocalDateTime.now())
                .build();
        
        assertThat(applicationEvent).isInstanceOf(com.loanmanagement.shared.domain.DomainEvent.class);
        
        // Test LoanApprovedEvent
        var approvedEvent = com.loanmanagement.loan.domain.event.LoanApprovedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .approvedAmount(amount)
                .approvedTerms(LoanTerms.builder()
                        .interestRate(new BigDecimal("5.5"))
                        .termInMonths(36)
                        .paymentFrequency(PaymentFrequency.MONTHLY)
                        .build())
                .conditions(java.util.List.of("Standard approval"))
                .approvalDate(LocalDateTime.now())
                .approvingOfficerId(LoanOfficerId.generate())
                .approvalExpirationDate(java.time.LocalDate.now().plusDays(30))
                .termsModified(false)
                .build();
        
        assertThat(approvedEvent).isInstanceOf(com.loanmanagement.shared.domain.DomainEvent.class);
        
        // Test LoanRejectedEvent
        var rejectedEvent = com.loanmanagement.loan.domain.event.LoanRejectedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .primaryReason("Credit score too low")
                .rejectionDetails(java.util.List.of("Credit score too low"))
                .appealable(true)
                .appealDeadline(java.time.LocalDate.now().plusDays(30))
                .rejectionDate(LocalDateTime.now())
                .rejectingOfficerId(LoanOfficerId.generate())
                .build();
        
        assertThat(rejectedEvent).isInstanceOf(com.loanmanagement.shared.domain.DomainEvent.class);
        
        // Test LoanDisbursedEvent
        var disbursedEvent = com.loanmanagement.loan.domain.event.LoanDisbursedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .occurredOn(LocalDateTime.now())
                .loanId(loanId)
                .customerId(customerId)
                .disbursedAmount(amount)
                .disbursementMethod(DisbursementMethod.ACH_TRANSFER)
                .accountNumber("1234567890")
                .routingNumber("123456789")
                .disbursementDate(LocalDateTime.now())
                .disbursedBy(LoanOfficerId.generate())
                .specialInstructions("Standard disbursement")
                .build();
        
        assertThat(disbursedEvent).isInstanceOf(com.loanmanagement.shared.domain.DomainEvent.class);
    }
}