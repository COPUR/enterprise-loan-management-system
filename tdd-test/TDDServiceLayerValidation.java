package com.loanmanagement.tdd;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.service.LoanFacadeService;
import com.loanmanagement.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TDD Validation for Service Layer - Validates core service methods work correctly
 */
public class TDDServiceLayerValidation {
    
    public static void main(String[] args) {
        System.out.println("=== TDD Service Layer Validation ===\n");
        
        try {
            testLoanFacadeService();
            testDomainEvents();
            testDomainEventInterface();
            System.out.println("✅ All service layer tests passed!");
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void testLoanFacadeService() {
        System.out.println("Testing LoanFacadeService...");
        
        // Given
        LoanFacadeService loanFacadeService = new LoanFacadeService();
        Money principalAmount = Money.of("USD", new BigDecimal("10000"));
        BigDecimal interestRate = new BigDecimal("5.5");
        Integer termMonths = 36;
        Long customerId = 1L;
        
        // When
        Loan loan = loanFacadeService.createSimpleLoan(customerId, principalAmount, interestRate, termMonths);
        
        // Then
        assert loan != null : "Loan should not be null";
        assert loan.getPrincipalAmount().equals(principalAmount) : "Principal amount should match";
        assert loan.getInterestRate().equals(interestRate) : "Interest rate should match";
        assert loan.getTermMonths().equals(termMonths) : "Term months should match";
        assert loan.getStatus() == LoanStatus.PENDING : "Status should be PENDING";
        assert loan.getCustomerId() != null : "Customer ID should not be null";
        assert loan.getId() != null : "Loan ID should not be null";
        
        System.out.println("✅ LoanFacadeService test passed");
    }
    
    public static void testDomainEvents() {
        System.out.println("Testing Domain Events...");
        
        // Given
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        Money amount = Money.of("USD", new BigDecimal("10000"));
        
        // Test LoanApplicationSubmittedEvent
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
        
        assert applicationEvent != null : "Application event should not be null";
        assert applicationEvent.getAggregateId().equals(loanId.getValue()) : "Aggregate ID should match";
        assert applicationEvent.getEventType().equals("LoanApplicationSubmittedEvent") : "Event type should match";
        assert applicationEvent.getRequestedAmount().equals(amount) : "Requested amount should match";
        
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
        
        assert approvedEvent != null : "Approved event should not be null";
        assert approvedEvent.getAggregateId().equals(loanId.getValue()) : "Aggregate ID should match";
        assert approvedEvent.getEventType().equals("LoanApprovedEvent") : "Event type should match";
        assert approvedEvent.getApprovedAmount().equals(amount) : "Approved amount should match";
        
        System.out.println("✅ Domain Events test passed");
    }
    
    public static void testDomainEventInterface() {
        System.out.println("Testing DomainEvent Interface Implementation...");
        
        // Given
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.of("customer-123");
        Money amount = Money.of("USD", new BigDecimal("10000"));
        
        // Test LoanApplicationSubmittedEvent implements DomainEvent
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
        
        assert applicationEvent instanceof com.loanmanagement.shared.domain.DomainEvent : "Should implement DomainEvent";
        
        // Test LoanApprovedEvent implements DomainEvent
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
        
        assert approvedEvent instanceof com.loanmanagement.shared.domain.DomainEvent : "Should implement DomainEvent";
        
        // Test LoanRejectedEvent implements DomainEvent
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
        
        assert rejectedEvent instanceof com.loanmanagement.shared.domain.DomainEvent : "Should implement DomainEvent";
        
        System.out.println("✅ DomainEvent Interface test passed");
    }
}