package com.loanmanagement.loan.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.event.*;
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
 * Comprehensive tests for Loan Aggregate
 * Tests business rules, state transitions, and domain events
 */
@DisplayName("Loan Aggregate Tests")
class LoanAggregateTest {

    private LoanId loanId;
    private CustomerId customerId;
    private Money loanAmount;
    private LoanTerms loanTerms;
    private LoanOfficerId officerId;

    @BeforeEach
    void setUp() {
        loanId = LoanId.of("LOAN-001");
        customerId = CustomerId.of("CUST-001");
        loanAmount = Money.of("USD", new BigDecimal("50000.00"));
        loanTerms = LoanTerms.builder()
                .termInMonths(60)
                .interestRate(new BigDecimal("5.5"))
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .build();
        officerId = LoanOfficerId.of("OFFICER-001");
    }

    @Nested
    @DisplayName("Loan Creation Tests")
    class LoanCreationTests {

        @Test
        @DisplayName("Should create new loan application successfully")
        void shouldCreateNewLoanApplication() {
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
            assertNotNull(loan);
            assertEquals(loanId, loan.getId());
            assertEquals(customerId, loan.getCustomerId());
            assertEquals(loanAmount, loan.getPrincipalAmount());
            assertEquals(LoanStatus.PENDING, loan.getStatus());
            assertEquals(LoanPurpose.AUTO, loan.getPurpose());
            
            // Verify domain event
            List<Object> events = loan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanApplicationSubmittedEvent);
        }

        @Test
        @DisplayName("Should fail to create loan with invalid amount")
        void shouldFailToCreateLoanWithInvalidAmount() {
            // Given
            Money invalidAmount = Money.of("USD", BigDecimal.ZERO);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Loan.createApplication(
                        loanId,
                        customerId,
                        invalidAmount,
                        LoanPurpose.AUTO,
                        loanTerms,
                        officerId
                );
            });
        }

        @Test
        @DisplayName("Should fail to create loan with null customer ID")
        void shouldFailToCreateLoanWithNullCustomerId() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Loan.createApplication(
                        loanId,
                        null,
                        loanAmount,
                        LoanPurpose.AUTO,
                        loanTerms,
                        officerId
                );
            });
        }

        @Test
        @DisplayName("Should fail to create loan with invalid loan terms")
        void shouldFailToCreateLoanWithInvalidTerms() {
            // Given
            LoanTerms invalidTerms = LoanTerms.builder()
                    .termInMonths(0)
                    .interestRate(new BigDecimal("-1.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Loan.createApplication(
                        loanId,
                        customerId,
                        loanAmount,
                        LoanPurpose.AUTO,
                        invalidTerms,
                        officerId
                );
            });
        }
    }

    @Nested
    @DisplayName("Loan Approval Tests")
    class LoanApprovalTests {

        private Loan pendingLoan;
        private ApprovalConditions approvalConditions;

        @BeforeEach
        void setUp() {
            pendingLoan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );
            pendingLoan.markEventsAsCommitted();
            
            approvalConditions = ApprovalConditions.builder()
                    .approvedAmount(loanAmount)
                    .approvedTerms(loanTerms)
                    .conditions(List.of("Provide proof of income", "Verify employment"))
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();
        }

        @Test
        @DisplayName("Should approve pending loan successfully")
        void shouldApprovePendingLoan() {
            // When
            pendingLoan.approve(approvalConditions, officerId);

            // Then
            assertEquals(LoanStatus.APPROVED, pendingLoan.getStatus());
            assertEquals(approvalConditions, pendingLoan.getApprovalConditions());
            assertNotNull(pendingLoan.getApprovalDate());
            assertEquals(officerId, pendingLoan.getApprovingOfficerId());
            
            // Verify domain event
            List<Object> events = pendingLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanApprovedEvent);
        }

        @Test
        @DisplayName("Should fail to approve already approved loan")
        void shouldFailToApproveAlreadyApprovedLoan() {
            // Given
            pendingLoan.approve(approvalConditions, officerId);
            pendingLoan.markEventsAsCommitted();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                pendingLoan.approve(approvalConditions, officerId);
            });
        }

        @Test
        @DisplayName("Should fail to approve rejected loan")
        void shouldFailToApproveRejectedLoan() {
            // Given
            RejectionReason reason = RejectionReason.builder()
                    .primaryReason("Insufficient income")
                    .details(List.of("DTI ratio too high", "Unstable employment"))
                    .build();
            pendingLoan.reject(reason, officerId);
            pendingLoan.markEventsAsCommitted();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                pendingLoan.approve(approvalConditions, officerId);
            });
        }

        @Test
        @DisplayName("Should approve with modified terms")
        void shouldApproveWithModifiedTerms() {
            // Given
            Money reducedAmount = Money.of("USD", new BigDecimal("40000.00"));
            LoanTerms modifiedTerms = LoanTerms.builder()
                    .termInMonths(48)
                    .interestRate(new BigDecimal("6.0"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            ApprovalConditions modifiedConditions = ApprovalConditions.builder()
                    .approvedAmount(reducedAmount)
                    .approvedTerms(modifiedTerms)
                    .conditions(List.of("Provide additional collateral"))
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();

            // When
            pendingLoan.approve(modifiedConditions, officerId);

            // Then
            assertEquals(LoanStatus.APPROVED, pendingLoan.getStatus());
            assertEquals(reducedAmount, pendingLoan.getApprovalConditions().getApprovedAmount());
            assertEquals(modifiedTerms, pendingLoan.getApprovalConditions().getApprovedTerms());
        }
    }

    @Nested
    @DisplayName("Loan Rejection Tests")
    class LoanRejectionTests {

        private Loan pendingLoan;
        private RejectionReason rejectionReason;

        @BeforeEach
        void setUp() {
            pendingLoan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );
            pendingLoan.markEventsAsCommitted();
            
            rejectionReason = RejectionReason.builder()
                    .primaryReason("Credit score too low")
                    .details(List.of("Score below minimum threshold", "Recent bankruptcy"))
                    .appealable(true)
                    .build();
        }

        @Test
        @DisplayName("Should reject pending loan successfully")
        void shouldRejectPendingLoan() {
            // When
            pendingLoan.reject(rejectionReason, officerId);

            // Then
            assertEquals(LoanStatus.REJECTED, pendingLoan.getStatus());
            assertEquals(rejectionReason, pendingLoan.getRejectionReason());
            assertNotNull(pendingLoan.getRejectionDate());
            assertEquals(officerId, pendingLoan.getRejectingOfficerId());
            
            // Verify domain event
            List<Object> events = pendingLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanRejectedEvent);
        }

        @Test
        @DisplayName("Should fail to reject already approved loan")
        void shouldFailToRejectAlreadyApprovedLoan() {
            // Given
            ApprovalConditions conditions = ApprovalConditions.builder()
                    .approvedAmount(loanAmount)
                    .approvedTerms(loanTerms)
                    .conditions(List.of())
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();
            pendingLoan.approve(conditions, officerId);
            pendingLoan.markEventsAsCommitted();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                pendingLoan.reject(rejectionReason, officerId);
            });
        }
    }

    @Nested
    @DisplayName("Loan Disbursement Tests")
    class LoanDisbursementTests {

        private Loan approvedLoan;
        private DisbursementInstructions instructions;

        @BeforeEach
        void setUp() {
            approvedLoan = Loan.createApplication(
                    loanId,
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );
            
            ApprovalConditions conditions = ApprovalConditions.builder()
                    .approvedAmount(loanAmount)
                    .approvedTerms(loanTerms)
                    .conditions(List.of())
                    .expirationDate(LocalDate.now().plusDays(30))
                    .build();
            
            approvedLoan.approve(conditions, officerId);
            approvedLoan.markEventsAsCommitted();
            
            instructions = DisbursementInstructions.builder()
                    .accountNumber("ACC-12345")
                    .routingNumber("123456789")
                    .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                    .disbursementDate(LocalDate.now().plusDays(1))
                    .build();
        }

        @Test
        @DisplayName("Should disburse approved loan successfully")
        void shouldDisburseApprovedLoan() {
            // When
            approvedLoan.disburse(instructions, officerId);

            // Then
            assertEquals(LoanStatus.ACTIVE, approvedLoan.getStatus());
            assertEquals(instructions, approvedLoan.getDisbursementInstructions());
            assertNotNull(approvedLoan.getDisbursementDate());
            assertEquals(loanAmount, approvedLoan.getCurrentBalance());
            
            // Verify domain event
            List<Object> events = approvedLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanDisbursedEvent);
        }

        @Test
        @DisplayName("Should fail to disburse pending loan")
        void shouldFailToDisbursePendingLoan() {
            // Given
            Loan pendingLoan = Loan.createApplication(
                    LoanId.of("LOAN-002"),
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                pendingLoan.disburse(instructions, officerId);
            });
        }

        @Test
        @DisplayName("Should fail to disburse with invalid instructions")
        void shouldFailToDisburseWithInvalidInstructions() {
            // Given
            DisbursementInstructions invalidInstructions = DisbursementInstructions.builder()
                    .accountNumber("")
                    .routingNumber("invalid")
                    .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                    .disbursementDate(LocalDate.now().minusDays(1))
                    .build();

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                approvedLoan.disburse(invalidInstructions, officerId);
            });
        }
    }

    @Nested
    @DisplayName("Loan Payment Tests")
    class LoanPaymentTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            // Given
            Money paymentAmount = Money.of("USD", new BigDecimal("1000.00"));
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("770.83")))
                    .interestAmount(Money.of("USD", new BigDecimal("229.17")))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();

            // When
            activeLoan.makePayment(paymentAmount, allocation, LocalDateTime.now());

            // Then
            Money expectedBalance = loanAmount.subtract(allocation.getPrincipalAmount());
            assertEquals(expectedBalance, activeLoan.getCurrentBalance());
            assertFalse(activeLoan.getPaymentHistory().isEmpty());
            
            // Verify domain event
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanPaymentMadeEvent);
        }

        @Test
        @DisplayName("Should mark loan as paid off when balance reaches zero")
        void shouldMarkLoanAsPaidOffWhenBalanceReachesZero() {
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
            assertEquals(LoanStatus.PAID_OFF, activeLoan.getStatus());
            assertEquals(Money.of("USD", BigDecimal.ZERO), activeLoan.getCurrentBalance());
            assertNotNull(activeLoan.getPaidOffDate());
            
            // Verify domain event
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(2, events.size());
            assertTrue(events.get(0) instanceof LoanPaymentMadeEvent);
            assertTrue(events.get(1) instanceof LoanPaidOffEvent);
        }

        @Test
        @DisplayName("Should fail to make payment on non-active loan")
        void shouldFailToMakePaymentOnNonActiveLoan() {
            // Given
            Loan pendingLoan = Loan.createApplication(
                    LoanId.of("LOAN-003"),
                    customerId,
                    loanAmount,
                    LoanPurpose.AUTO,
                    loanTerms,
                    officerId
            );
            
            Money paymentAmount = Money.of("USD", new BigDecimal("1000.00"));
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(Money.of("USD", new BigDecimal("1000.00")))
                    .interestAmount(Money.of("USD", BigDecimal.ZERO))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                pendingLoan.makePayment(paymentAmount, allocation, LocalDateTime.now());
            });
        }
    }

    @Nested
    @DisplayName("Loan Default Tests")
    class LoanDefaultTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should mark loan as defaulted")
        void shouldMarkLoanAsDefaulted() {
            // Given
            DefaultReason reason = DefaultReason.builder()
                    .reason("Missed 3 consecutive payments")
                    .daysPastDue(90)
                    .totalAmountPastDue(Money.of("USD", new BigDecimal("3000.00")))
                    .build();

            // When
            activeLoan.markAsDefaulted(reason, officerId);

            // Then
            assertEquals(LoanStatus.DEFAULTED, activeLoan.getStatus());
            assertEquals(reason, activeLoan.getDefaultReason());
            assertNotNull(activeLoan.getDefaultDate());
            
            // Verify domain event
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanDefaultedEvent);
        }

        @Test
        @DisplayName("Should fail to mark paid-off loan as defaulted")
        void shouldFailToMarkPaidOffLoanAsDefaulted() {
            // Given
            Money fullPayment = activeLoan.getCurrentBalance();
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .principalAmount(fullPayment)
                    .interestAmount(Money.of("USD", BigDecimal.ZERO))
                    .feesAmount(Money.of("USD", BigDecimal.ZERO))
                    .build();
            activeLoan.makePayment(fullPayment, allocation, LocalDateTime.now());
            activeLoan.markEventsAsCommitted();
            
            DefaultReason reason = DefaultReason.builder()
                    .reason("Test reason")
                    .daysPastDue(90)
                    .totalAmountPastDue(Money.of("USD", BigDecimal.ZERO))
                    .build();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                activeLoan.markAsDefaulted(reason, officerId);
            });
        }
    }

    @Nested
    @DisplayName("Loan Restructuring Tests")
    class LoanRestructuringTests {

        private Loan activeLoan;

        @BeforeEach
        void setUp() {
            activeLoan = createDisbursedLoan();
        }

        @Test
        @DisplayName("Should restructure loan successfully")
        void shouldRestructureLoanSuccessfully() {
            // Given
            LoanTerms newTerms = LoanTerms.builder()
                    .termInMonths(72)
                    .interestRate(new BigDecimal("4.5"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            RestructuringReason reason = RestructuringReason.builder()
                    .reason("Financial hardship")
                    .justification("Customer requested payment reduction due to job loss")
                    .temporaryHardship(true)
                    .build();

            // When
            activeLoan.restructure(newTerms, reason, officerId);

            // Then
            assertEquals(LoanStatus.RESTRUCTURED, activeLoan.getStatus());
            assertEquals(newTerms, activeLoan.getCurrentTerms());
            assertEquals(reason, activeLoan.getRestructuringReason());
            assertNotNull(activeLoan.getRestructureDate());
            
            // Verify domain event
            List<Object> events = activeLoan.getUncommittedEvents();
            assertEquals(1, events.size());
            assertTrue(events.get(0) instanceof LoanRestructuredEvent);
        }

        @Test
        @DisplayName("Should fail to restructure defaulted loan")
        void shouldFailToRestructureDefaultedLoan() {
            // Given
            DefaultReason defaultReason = DefaultReason.builder()
                    .reason("Missed payments")
                    .daysPastDue(90)
                    .totalAmountPastDue(Money.of("USD", new BigDecimal("3000.00")))
                    .build();
            activeLoan.markAsDefaulted(defaultReason, officerId);
            activeLoan.markEventsAsCommitted();
            
            LoanTerms newTerms = LoanTerms.builder()
                    .termInMonths(72)
                    .interestRate(new BigDecimal("4.5"))
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .build();
            
            RestructuringReason reason = RestructuringReason.builder()
                    .reason("Test reason")
                    .justification("Test justification")
                    .temporaryHardship(false)
                    .build();

            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                activeLoan.restructure(newTerms, reason, officerId);
            });
        }
    }

    // Helper method to create a disbursed loan for testing
    private Loan createDisbursedLoan() {
        Loan loan = Loan.createApplication(
                loanId,
                customerId,
                loanAmount,
                LoanPurpose.AUTO,
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