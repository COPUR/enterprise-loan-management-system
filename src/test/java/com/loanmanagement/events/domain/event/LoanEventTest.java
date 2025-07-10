package com.loanmanagement.events.domain.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TDD Tests for Loan Domain Events
 * Testing the event classes that need to be fixed
 */
class LoanEventTest {

    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
    }

    @Test
    void shouldCreateLoanApplicationSubmittedEvent() {
        // When
        LoanApplicationSubmittedEvent event = new LoanApplicationSubmittedEvent(
            123L, 456L, new BigDecimal("50000"), 24, "Home Purchase"
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanApplicationSubmitted", event.getEventType());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredOn());
    }

    @Test
    void shouldCreateLoanApprovedEvent() {
        // When
        LoanApprovedEvent event = new LoanApprovedEvent(
            123L, 456L, new BigDecimal("50000"), "APPROVED", "John Doe"
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanApproved", event.getEventType());
    }

    @Test
    void shouldCreateLoanRejectedEvent() {
        // When
        LoanRejectedEvent event = new LoanRejectedEvent(
            123L, 456L, "INSUFFICIENT_INCOME", "John Doe"
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanRejected", event.getEventType());
    }

    @Test
    void shouldCreateLoanDisbursedEvent() {
        // When
        LoanDisbursedEvent event = new LoanDisbursedEvent(
            123L, 456L, new BigDecimal("50000"), testTime, "BANK_TRANSFER"
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanDisbursed", event.getEventType());
    }

    @Test
    void shouldCreateLoanRestructuredEvent() {
        // When
        LoanRestructuredEvent event = new LoanRestructuredEvent(
            123L, 456L, new BigDecimal("4.5"), 36, "System", "Customer Request", testTime
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanRestructured", event.getEventType());
    }

    @Test
    void shouldCreateLoanDefaultedEvent() {
        // When
        LoanDefaultedEvent event = new LoanDefaultedEvent(
            123L, 456L, new BigDecimal("25000"), "System", "90 days overdue", testTime
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanDefaulted", event.getEventType());
    }

    @Test
    void shouldCreateLoanPaidOffEvent() {
        // Given
        com.loanmanagement.shared.domain.model.Money totalAmount = 
            new com.loanmanagement.shared.domain.model.Money(new BigDecimal("50000"), "USD");
        
        // When
        LoanPaidOffEvent event = new LoanPaidOffEvent(
            123L, 456L, totalAmount, "Customer", testTime, "Full payment received"
        );
        
        // Then
        assertNotNull(event);
        assertEquals("123", event.getAggregateId());
        assertEquals("LoanPaidOff", event.getEventType());
    }
}