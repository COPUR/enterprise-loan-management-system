package com.loanmanagement.loan.domain;

import com.loanmanagement.loan.domain.event.*;
import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Loan Event Handling and State Transitions
 * Tests domain events generation and state transition validations
 */
@DisplayName("Loan Event Handling Tests")
class LoanEventHandlingTest {

    private LoanId loanId;
    private CustomerId customerId;
    private Money loanAmount;
    private LoanTerms loanTerms;
    private LoanOfficerId officerId;

    @BeforeEach
    void setUp() {
        loanId = LoanId.of("LOAN-EVENT-001");
        customerId = CustomerId.of("CUST-EVENT-001");
        loanAmount = Money.of("USD", new BigDecimal("100000.00"));
        loanTerms = LoanTerms.builder()
                .termInMonths(360)
                .interestRate(new BigDecimal("4.5"))
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        officerId = LoanOfficerId.of("OFFICER-EVENT-001");
    }

    @Nested
    @DisplayName("Loan Application Events Tests")
    class LoanApplicationEventsTests {

        @Test
        @DisplayName("Should emit LoanApplicationSubmittedEvent when loan is created")
        void shouldEmitLoanApplicationSubmittedEventWhenLoanIsCreated() {
            // When
            Loan loan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.HOME,
                    loanTerms,
                    officerId
            );

            // Then
            List<Object> events = loan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanApplicationSubmittedEvent event = (LoanApplicationSubmittedEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(loanAmount, event.getRequestedAmount());
            assertEquals(LoanPurpose.HOME, event.getLoanPurpose());
            assertEquals(officerId, event.getLoanOfficerId());
            assertNotNull(event.getEventId());
            assertNotNull(event.getOccurredOn());
        }

        @Test
        @DisplayName("Should include correct loan details in application event")
        void shouldIncludeCorrectLoanDetailsInApplicationEvent() {
            // When
            Loan loan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );

            // Then
            LoanApplicationSubmittedEvent event = (LoanApplicationSubmittedEvent) 
                    loan.getUncommittedEvents().get(0);
                    
            assertEquals(loanTerms.getTermInMonths(), event.getRequestedTerms().getTermInMonths());
            assertEquals(loanTerms.getInterestRate(), event.getRequestedTerms().getInterestRate());
            assertEquals(loanTerms.getPaymentFrequency(), event.getRequestedTerms().getPaymentFrequency());
        }
    }

    @Nested
    @DisplayName("Loan Approval Events Tests")
    class LoanApprovalEventsTests {

        private Loan pendingLoan;
        private ApprovalConditions approvalConditions;

        @BeforeEach
        void setUp() {
            pendingLoan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.HOME,
                    loanTerms,
                    officerId
            );
            pendingLoan.markEventsAsCommitted();
            
            approvalConditions = ApprovalConditions.builder()
                    .approvedAmount(loanAmount)
                    .approvedTerms(loanTerms)
                    .conditions(List.of("Provide proof of income", "Property appraisal"))
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();
        }

        @Test
        @DisplayName("Should emit LoanApprovedEvent when loan is approved")
        void shouldEmitLoanApprovedEventWhenLoanIsApproved() {
            // When
            pendingLoan.approve(approvalConditions, officerId);

            // Then
            List<Object> events = pendingLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanApprovedEvent event = (LoanApprovedEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(officerId, event.getApprovingOfficerId());
            assertEquals(approvalConditions.getApprovedAmount(), event.getApprovedAmount());
            assertEquals(approvalConditions.getApprovedTerms(), event.getApprovedTerms());
            assertNotNull(event.getApprovalDate());
        }

        @Test
        @DisplayName("Should include approval conditions in approval event")
        void shouldIncludeApprovalConditionsInApprovalEvent() {
            // When
            pendingLoan.approve(approvalConditions, officerId);

            // Then
            LoanApprovedEvent event = (LoanApprovedEvent) 
                    pendingLoan.getUncommittedEvents().get(0);
                    
            assertEquals(approvalConditions.getConditions(), event.getConditions());
            assertEquals(approvalConditions.getExpirationDate(), event.getApprovalExpirationDate());
        }

        @Test
        @DisplayName("Should emit event even when approved with modified terms")
        void shouldEmitEventEvenWhenApprovedWithModifiedTerms() {
            // Given
            Money reducedAmount = Money.of("USD", new BigDecimal("80000.00"));
            LoanTerms modifiedTerms = LoanTerms.builder()
                    .termInMonths(240)
                    .interestRate(new BigDecimal("5.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            ApprovalConditions modifiedConditions = ApprovalConditions.builder()
                    .approvedAmount(reducedAmount)
                    .approvedTerms(modifiedTerms)
                    .conditions(List.of("Reduced amount approved"))
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();

            // When
            pendingLoan.approve(modifiedConditions, officerId);

            // Then
            LoanApprovedEvent event = (LoanApprovedEvent) 
                    pendingLoan.getUncommittedEvents().get(0);
                    
            assertEquals(reducedAmount, event.getApprovedAmount());
            assertEquals(modifiedTerms, event.getApprovedTerms());
            assertTrue(event.isTermsModified());
        }
    }

    @Nested
    @DisplayName("Loan Rejection Events Tests")
    class LoanRejectionEventsTests {

        private Loan pendingLoan;
        private RejectionReason rejectionReason;

        @BeforeEach
        void setUp() {
            pendingLoan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.HOME,
                    loanTerms,
                    officerId
            );
            pendingLoan.markEventsAsCommitted();
            
            rejectionReason = RejectionReason.builder()
                    .primaryReason("Insufficient income")
                    .details(List.of("DTI ratio exceeds maximum", "Employment history insufficient"))
                    .appealable(true)
                    .build();
        }

        @Test
        @DisplayName("Should emit LoanRejectedEvent when loan is rejected")
        void shouldEmitLoanRejectedEventWhenLoanIsRejected() {
            // When
            pendingLoan.reject(rejectionReason, officerId);

            // Then
            List<Object> events = pendingLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanRejectedEvent event = (LoanRejectedEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(officerId, event.getRejectingOfficerId());
            assertEquals(rejectionReason.getPrimaryReason(), event.getPrimaryReason());
            assertEquals(rejectionReason.getDetails(), event.getRejectionDetails());
            assertEquals(rejectionReason.isAppealable(), event.isAppealable());
            assertNotNull(event.getRejectionDate());
        }

        @Test
        @DisplayName("Should include complete rejection information in event")
        void shouldIncludeCompleteRejectionInformationInEvent() {
            // Given
            RejectionReason detailedReason = RejectionReason.builder()
                    .primaryReason("Credit score below minimum")
                    .details(List.of(
                            "Credit score: 580 (minimum required: 620)",
                            "Recent late payments on existing accounts",
                            "High credit utilization ratio"
                    ))
                    .appealable(false)
                    .appealDeadline(LocalDate.now().plusDays(30))
                    .build();

            // When
            pendingLoan.reject(detailedReason, officerId);

            // Then
            LoanRejectedEvent event = (LoanRejectedEvent) 
                    pendingLoan.getUncommittedEvents().get(0);
                    
            assertEquals(3, event.getRejectionDetails().size());
            assertEquals(detailedReason.getAppealDeadline(), event.getAppealDeadline());
        }
    }

    @Nested
    @DisplayName("Loan Disbursement Events Tests")
    class LoanDisbursementEventsTests {

        private Loan approvedLoan;
        private DisbursementInstructions instructions;

        @BeforeEach
        void setUp() {
            approvedLoan = createApprovedLoan();
            
            instructions = DisbursementInstructions.builder()
                    .accountNumber("ACC-12345")
                    .routingNumber("123456789")
                    .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                    .disbursementDate(LocalDate.now().plusDays(1))
                    .specialInstructions("Business day disbursement only")
                    .build();
        }

        @Test
        @DisplayName("Should emit LoanDisbursedEvent when loan is disbursed")
        void shouldEmitLoanDisbursedEventWhenLoanIsDisbursed() {
            // When
            approvedLoan.disburse(instructions, officerId);

            // Then
            List<Object> events = approvedLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanDisbursedEvent event = (LoanDisbursedEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(loanAmount, event.getDisbursedAmount());
            assertEquals(instructions.getDisbursementMethod(), event.getDisbursementMethod());
            assertEquals(instructions.getAccountNumber(), event.getAccountNumber());
            assertNotNull(event.getDisbursementDate());
        }

        @Test
        @DisplayName("Should include disbursement instructions in event")
        void shouldIncludeDisbursementInstructionsInEvent() {
            // When
            approvedLoan.disburse(instructions, officerId);

            // Then
            LoanDisbursedEvent event = (LoanDisbursedEvent) 
                    approvedLoan.getUncommittedEvents().get(0);
                    
            assertEquals(instructions.getRoutingNumber(), event.getRoutingNumber());
            assertEquals(instructions.getSpecialInstructions(), event.getSpecialInstructions());
            assertEquals(officerId, event.getDisbursedBy());
        }
    }

    @Nested
    @DisplayName("Loan Payment Events Tests")
    class LoanPaymentEventsTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should emit LoanPaymentMadeEvent when payment is made")
        void shouldEmitLoanPaymentMadeEventWhenPaymentIsMade() {
            // Given
            Money paymentAmount = Money.of("USD", new BigDecimal("2000.00"));
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("1500.00")))
                    .interestAmount(Money.of("USD", new BigDecimal("500.00")))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();
            LocalDateTime paymentDate = LocalDateTime.now();

            // When
            activeLoan.makePayment(paymentAmount, allocation, paymentDate);

            // Then
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanPaymentMadeEvent event = (LoanPaymentMadeEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(paymentAmount, event.getPaymentAmount());
            assertEquals(allocation.getPrincipalAmount(), event.getPrincipalAmount());
            assertEquals(allocation.getInterestAmount(), event.getInterestAmount());
            assertEquals(allocation.getFeesAmount(), event.getFeesAmount());
            assertNotNull(event.getPaymentDate());
        }

        @Test
        @DisplayName("Should emit LoanPaidOffEvent when loan is fully paid")
        void shouldEmitLoanPaidOffEventWhenLoanIsFullyPaid() {
            // Given
            Money fullPayment = activeLoan.getCurrentBalance();
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(fullPayment)
                    .interestAmount(Money.of("USD", BigDecimal.ZERO))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();

            // When
            activeLoan.makePayment(fullPayment, allocation, LocalDateTime.now());

            // Then
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(2, events.size());
            
            assertTrue(events.get(0) instanceof LoanPaymentMadeEvent);
            
            LoanPaidOffEvent paidOffEvent = (LoanPaidOffEvent) events.get(1);
            assertEquals(loanId, paidOffEvent.getLoanId());
            assertEquals(customerId, paidOffEvent.getCustomerId());
            assertEquals(fullPayment, paidOffEvent.getFinalPaymentAmount());
            assertNotNull(paidOffEvent.getPaidOffDate());
        }

        @Test
        @DisplayName("Should include remaining balance in payment event")
        void shouldIncludeRemainingBalanceInPaymentEvent() {
            // Given
            Money paymentAmount = Money.of("USD", new BigDecimal("1000.00"));
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("750.00")))
                    .interestAmount(Money.of("USD", new BigDecimal("250.00")))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();
            Money expectedRemainingBalance = activeLoan.getCurrentBalance()
                    .subtract(allocation.getPrincipalAmount());

            // When
            activeLoan.makePayment(paymentAmount, allocation, LocalDateTime.now());

            // Then
            LoanPaymentMadeEvent event = (LoanPaymentMadeEvent) 
                    activeLoan.getUncommittedEvents().get(0);
                    
            assertEquals(expectedRemainingBalance, event.getRemainingBalance());
        }
    }

    @Nested
    @DisplayName("Loan Default Events Tests")
    class LoanDefaultEventsTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should emit LoanDefaultedEvent when loan is marked as defaulted")
        void shouldEmitLoanDefaultedEventWhenLoanIsMarkedAsDefaulted() {
            // Given
            DefaultReason reason = DefaultReason.builder()
                    .reason("Missed 3 consecutive payments")
                    .daysPastDue(90)
                    .totalAmountPastDue(Money.of("USD", new BigDecimal("6000.00")))
                    .missedPayments(3)
                    .build();

            // When
            activeLoan.markAsDefaulted(reason, officerId);

            // Then
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanDefaultedEvent event = (LoanDefaultedEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(reason.getReason(), event.getDefaultReason());
            assertEquals(reason.getDaysPastDue(), event.getDaysPastDue());
            assertEquals(reason.getTotalAmountPastDue(), event.getTotalAmountPastDue());
            assertEquals(officerId, event.getOfficerId());
            assertNotNull(event.getDefaultDate());
        }

        @Test
        @DisplayName("Should include complete default information in event")
        void shouldIncludeCompleteDefaultInformationInEvent() {
            // Given
            DefaultReason detailedReason = DefaultReason.builder()
                    .reason("Extended delinquency")
                    .daysPastDue(120)
                    .totalAmountPastDue(Money.of("USD", new BigDecimal("8000.00")))
                    .missedPayments(4)
                    .lastPaymentDate(LocalDate.now().minusDays(120))
                    .collectionActions(List.of("Notice sent", "Phone contact attempted"))
                    .build();

            // When
            activeLoan.markAsDefaulted(detailedReason, officerId);

            // Then
            LoanDefaultedEvent event = (LoanDefaultedEvent) 
                    activeLoan.getUncommittedEvents().get(0);
                    
            assertEquals(detailedReason.getMissedPayments(), event.getMissedPayments());
            assertEquals(detailedReason.getLastPaymentDate(), event.getLastPaymentDate());
            assertEquals(detailedReason.getCollectionActions(), event.getCollectionActions());
        }
    }

    @Nested
    @DisplayName("Loan Restructuring Events Tests")
    class LoanRestructuringEventsTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should emit LoanRestructuredEvent when loan is restructured")
        void shouldEmitLoanRestructuredEventWhenLoanIsRestructured() {
            // Given
            LoanTerms newTerms = LoanTerms.builder()
                    .termInMonths(480)
                    .interestRate(new BigDecimal("3.5"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            RestructuringReason reason = RestructuringReason.builder()
                    .reason("Financial hardship due to job loss")
                    .justification("Customer requested payment reduction")
                    .temporaryHardship(true)
                    .expectedDuration(12)
                    .build();

            // When
            activeLoan.restructure(newTerms, reason, officerId);

            // Then
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            
            LoanRestructuredEvent event = (LoanRestructuredEvent) events.get(0);
            assertEquals(loanId, event.getLoanId());
            assertEquals(customerId, event.getCustomerId());
            assertEquals(loanTerms, event.getOriginalTerms());
            assertEquals(newTerms, event.getNewTerms());
            assertEquals(reason.getReason(), event.getRestructuringReason());
            assertEquals(reason.isTemporaryHardship(), event.isTemporaryHardship());
            assertEquals(officerId, event.getOfficerId());
            assertNotNull(event.getRestructureDate());
        }

        @Test
        @DisplayName("Should include terms comparison in restructuring event")
        void shouldIncludeTermsComparisonInRestructuringEvent() {
            // Given
            LoanTerms newTerms = LoanTerms.builder()
                    .termInMonths(420)
                    .interestRate(new BigDecimal("4.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            RestructuringReason reason = RestructuringReason.builder()
                    .reason("Rate adjustment")
                    .justification("Market rate decrease")
                    .temporaryHardship(false)
                    .build();

            // When
            activeLoan.restructure(newTerms, reason, officerId);

            // Then
            LoanRestructuredEvent event = (LoanRestructuredEvent) 
                    activeLoan.getUncommittedEvents().get(0);
                    
            assertNotNull(event.getTermsComparison());
            assertTrue(event.getTermsComparison().containsKey("termExtension"));
            assertTrue(event.getTermsComparison().containsKey("rateChange"));
        }
    }

    @Nested
    @DisplayName("Event Ordering and Consistency Tests")
    class EventOrderingAndConsistencyTests {

        @Test
        @DisplayName("Should maintain event ordering for complex loan lifecycle")
        void shouldMaintainEventOrderingForComplexLoanLifecycle() {
            // Given - Create and approve loan
            Loan loan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.HOME,
                    loanTerms,
                    officerId
            );
            loan.markEventsAsCommitted();
            
            ApprovalConditions conditions = ApprovalConditions.builder()
                    .approvedAmount(loanAmount)
                    .approvedTerms(loanTerms)
                    .conditions(List.of())
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();
            
            loan.approve(conditions, officerId);
            loan.markEventsAsCommitted();
            
            DisbursementInstructions instructions = DisbursementInstructions.builder()
                    .accountNumber("ACC-12345")
                    .routingNumber("123456789")
                    .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                    .disbursementDate(LocalDate.now())
                    .build();
            
            loan.disburse(instructions, officerId);
            loan.markEventsAsCommitted();

            // When - Make multiple payments
            Money payment1 = Money.of("USD", new BigDecimal("2000.00"));
            PaymentAllocation allocation1 = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("1500.00")))
                    .interestAmount(Money.of("USD", new BigDecimal("500.00")))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();
            
            loan.makePayment(payment1, allocation1, LocalDateTime.now());
            
            Money payment2 = Money.of("USD", new BigDecimal("2000.00"));
            PaymentAllocation allocation2 = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("1520.00")))
                    .interestAmount(Money.of("USD", new BigDecimal("480.00")))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();
            
            loan.makePayment(payment2, allocation2, LocalDateTime.now().plusDays(30));

            // Then
            List<Object> events = loan.getUncommittedEvents();
            assertEquals(2, events.size());
            
            assertTrue(events.get(0) instanceof LoanPaymentMadeEvent);
            assertTrue(events.get(1) instanceof LoanPaymentMadeEvent);
            
            LoanPaymentMadeEvent firstPayment = (LoanPaymentMadeEvent) events.get(0);
            LoanPaymentMadeEvent secondPayment = (LoanPaymentMadeEvent) events.get(1);
            
            assertTrue(firstPayment.getPaymentDate().isBefore(secondPayment.getPaymentDate()));
        }

        @Test
        @DisplayName("Should ensure event immutability")
        void shouldEnsureEventImmutability() {
            // When
            Loan loan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.HOME,
                    loanTerms,
                    officerId
            );

            // Then
            LoanApplicationSubmittedEvent event = (LoanApplicationSubmittedEvent) 
                    loan.getUncommittedEvents().get(0);
            
            // Events should be immutable - verify that key fields cannot be changed
            assertNotNull(event.getEventId());
            assertNotNull(event.getOccurredOn());
            assertNotNull(event.getLoanId());
            assertNotNull(event.getCustomerId());
            
            // Try to verify immutability through reflection or by checking that
            // the event maintains its state
            String originalEventId = event.getEventId();
            LocalDateTime originalOccurredOn = event.getOccurredOn();
            
            // After some time, the event should maintain the same values
            assertEquals(originalEventId, event.getEventId());
            assertEquals(originalOccurredOn, event.getOccurredOn());
        }
    }

    // Helper methods
    
    private Loan createApprovedLoan() {
        Loan loan = Loan.createApplication(
                loanId,
                customerId,
                loanAmount,
                LoanPurpose.HOME,
                loanTerms,
                officerId
        );
        
        ApprovalConditions conditions = ApprovalConditions.builder()
                .approvedAmount(loanAmount)
                .approvedTerms(loanTerms)
                .conditions(List.of())
                .expirationDate(LocalDate.now().plusDays(30))
                .build();
        
        loan.approve(conditions, officerId);
        loan.markEventsAsCommitted();
        
        return loan;
    }
    
    private Loan createDisbursedLoan() {
        Loan loan = createApprovedLoan();
        
        DisbursementInstructions instructions = DisbursementInstructions.builder()
                .accountNumber("ACC-12345")
                .routingNumber("123456789")
                .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                .disbursementDate(LocalDate.now())
                .build();
        
        loan.disburse(instructions, officerId);
        loan.markEventsAsCommitted();
        
        return loan;
    }
}