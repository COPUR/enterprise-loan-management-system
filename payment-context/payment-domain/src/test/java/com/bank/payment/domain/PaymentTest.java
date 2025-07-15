package com.bank.payment.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive TDD tests for Payment domain entity
 * 
 * Tests focus on:
 * - Payment state machine verification
 * - State transition validation
 * - Financial integrity during processing
 * - GRASP principles: Information Expert, Low Coupling
 * - Payment lifecycle management
 */
@DisplayName("Payment Domain Entity Tests")
class PaymentTest {

    private PaymentId paymentId;
    private CustomerId customerId;
    private AccountId fromAccountId;
    private AccountId toAccountId;
    private Money amount;
    private PaymentType paymentType;
    private String description;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = PaymentId.generate();
        customerId = CustomerId.generate();
        fromAccountId = AccountId.generate();
        toAccountId = AccountId.generate();
        amount = Money.aed(new BigDecimal("1000"));
        paymentType = PaymentType.TRANSFER;
        description = "Test payment";
        payment = Payment.create(paymentId, customerId, fromAccountId, toAccountId, amount, paymentType, description);
    }

    @Nested
    @DisplayName("Payment Creation Tests - Information Expert Pattern")
    class PaymentCreationTests {

        @Test
        @DisplayName("Should create payment with valid parameters")
        void shouldCreatePaymentWithValidParameters() {
            assertAll(
                () -> assertEquals(paymentId, payment.getId()),
                () -> assertEquals(customerId, payment.getCustomerId()),
                () -> assertEquals(fromAccountId, payment.getFromAccountId()),
                () -> assertEquals(toAccountId, payment.getToAccountId()),
                () -> assertEquals(amount, payment.getAmount()),
                () -> assertEquals(paymentType, payment.getPaymentType()),
                () -> assertEquals(description, payment.getDescription()),
                () -> assertEquals(PaymentStatus.PENDING, payment.getStatus()),
                () -> assertNotNull(payment.getFee()),
                () -> assertNotNull(payment.getCreatedAt()),
                () -> assertNotNull(payment.getUpdatedAt()),
                () -> assertTrue(payment.isPending())
            );
        }

        @Test
        @DisplayName("Should calculate total amount including fees")
        void shouldCalculateTotalAmountIncludingFees() {
            Money expectedTotal = amount.add(payment.getFee());
            assertEquals(expectedTotal, payment.getTotalAmount());
        }

        @Test
        @DisplayName("Should reject null payment ID")
        void shouldRejectNullPaymentId() {
            assertThrows(NullPointerException.class, () ->
                Payment.create(null, customerId, fromAccountId, toAccountId, amount, paymentType, description)
            );
        }

        @Test
        @DisplayName("Should reject null customer ID")
        void shouldRejectNullCustomerId() {
            assertThrows(NullPointerException.class, () ->
                Payment.create(paymentId, null, fromAccountId, toAccountId, amount, paymentType, description)
            );
        }

        @Test
        @DisplayName("Should reject null account IDs")
        void shouldRejectNullAccountIds() {
            assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                    Payment.create(paymentId, customerId, null, toAccountId, amount, paymentType, description)),
                () -> assertThrows(NullPointerException.class, () ->
                    Payment.create(paymentId, customerId, fromAccountId, null, amount, paymentType, description))
            );
        }

        @Test
        @DisplayName("Should reject negative or zero payment amount")
        void shouldRejectInvalidPaymentAmount() {
            Money negativeAmount = Money.aed(new BigDecimal("-500"));
            Money zeroAmount = Money.aed(BigDecimal.ZERO);
            
            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Payment.create(paymentId, customerId, fromAccountId, toAccountId, negativeAmount, paymentType, description)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                    Payment.create(paymentId, customerId, fromAccountId, toAccountId, zeroAmount, paymentType, description))
            );
        }

        @Test
        @DisplayName("Should reject same from and to accounts")
        void shouldRejectSameFromAndToAccounts() {
            assertThrows(IllegalArgumentException.class, () ->
                Payment.create(paymentId, customerId, fromAccountId, fromAccountId, amount, paymentType, description)
            );
        }

        @Test
        @DisplayName("Should handle null description gracefully")
        void shouldHandleNullDescriptionGracefully() {
            Payment paymentWithoutDescription = Payment.create(
                paymentId, customerId, fromAccountId, toAccountId, amount, paymentType, null
            );
            
            assertNull(paymentWithoutDescription.getDescription());
        }
    }

    @Nested
    @DisplayName("Payment State Machine Tests - Low Coupling Design")
    class PaymentStateMachineTests {

        @Test
        @DisplayName("Should transition from PENDING to PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            
            payment.markAsProcessing();
            
            assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
        }

        @Test
        @DisplayName("Should reject processing transition from non-pending status")
        void shouldRejectProcessingTransitionFromNonPendingStatus() {
            payment.markAsProcessing();
            
            assertThrows(IllegalStateException.class, () ->
                payment.markAsProcessing()
            );
        }

        @Test
        @DisplayName("Should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            payment.markAsProcessing();
            
            payment.confirm();
            
            assertAll(
                () -> assertEquals(PaymentStatus.COMPLETED, payment.getStatus()),
                () -> assertNotNull(payment.getCompletedAt()),
                () -> assertTrue(payment.isCompleted())
            );
        }

        @Test
        @DisplayName("Should transition from PENDING to COMPLETED directly")
        void shouldTransitionFromPendingToCompletedDirectly() {
            payment.confirm();
            
            assertAll(
                () -> assertEquals(PaymentStatus.COMPLETED, payment.getStatus()),
                () -> assertNotNull(payment.getCompletedAt()),
                () -> assertTrue(payment.isCompleted())
            );
        }

        @Test
        @DisplayName("Should transition from PENDING to FAILED")
        void shouldTransitionFromPendingToFailed() {
            String failureReason = "Insufficient funds";
            
            payment.fail(failureReason);
            
            assertAll(
                () -> assertEquals(PaymentStatus.FAILED, payment.getStatus()),
                () -> assertEquals(failureReason, payment.getFailureReason()),
                () -> assertTrue(payment.isFailed())
            );
        }

        @Test
        @DisplayName("Should transition from PROCESSING to FAILED")
        void shouldTransitionFromProcessingToFailed() {
            String failureReason = "Network timeout";
            payment.markAsProcessing();
            
            payment.fail(failureReason);
            
            assertAll(
                () -> assertEquals(PaymentStatus.FAILED, payment.getStatus()),
                () -> assertEquals(failureReason, payment.getFailureReason()),
                () -> assertTrue(payment.isFailed())
            );
        }

        @Test
        @DisplayName("Should transition from PENDING to CANCELLED")
        void shouldTransitionFromPendingToCancelled() {
            String cancellationReason = "Customer request";
            
            payment.cancel(cancellationReason);
            
            assertAll(
                () -> assertEquals(PaymentStatus.CANCELLED, payment.getStatus()),
                () -> assertEquals(cancellationReason, payment.getFailureReason())
            );
        }

        @Test
        @DisplayName("Should transition from COMPLETED to REFUNDED")
        void shouldTransitionFromCompletedToRefunded() {
            payment.confirm();
            
            payment.refund();
            
            assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"FAILED", "CANCELLED", "REFUNDED"})
        @DisplayName("Should reject confirmation from terminal states")
        void shouldRejectConfirmationFromTerminalStates(PaymentStatus terminalStatus) {
            // Force payment to terminal status
            forcePaymentToStatus(payment, terminalStatus);
            
            assertThrows(IllegalStateException.class, () ->
                payment.confirm()
            );
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"COMPLETED", "FAILED", "CANCELLED", "REFUNDED"})
        @DisplayName("Should reject processing from non-pending states")
        void shouldRejectProcessingFromNonPendingStates(PaymentStatus nonPendingStatus) {
            if (nonPendingStatus != PaymentStatus.PENDING) {
                forcePaymentToStatus(payment, nonPendingStatus);
                
                assertThrows(IllegalStateException.class, () ->
                    payment.markAsProcessing()
                );
            }
        }
    }

    @Nested
    @DisplayName("Payment Business Rules Tests - High Cohesion")
    class PaymentBusinessRulesTests {

        @Test
        @DisplayName("Should maintain payment integrity during state changes")
        void shouldMaintainPaymentIntegrityDuringStateChanges() {
            Money originalAmount = payment.getAmount();
            Money originalFee = payment.getFee();
            Money originalTotal = payment.getTotalAmount();
            PaymentId originalId = payment.getId();
            
            // Go through various state changes
            payment.markAsProcessing();
            payment.confirm();
            
            // Core payment data should remain unchanged
            assertAll(
                () -> assertEquals(originalAmount, payment.getAmount()),
                () -> assertEquals(originalFee, payment.getFee()),
                () -> assertEquals(originalTotal, payment.getTotalAmount()),
                () -> assertEquals(originalId, payment.getId())
            );
        }

        @Test
        @DisplayName("Should update timestamps appropriately during transitions")
        void shouldUpdateTimestampsAppropriatelyDuringTransitions() {
            LocalDateTime originalUpdatedAt = payment.getUpdatedAt();
            
            // Small delay to ensure timestamp difference
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            payment.markAsProcessing();
            assertTrue(payment.getUpdatedAt().isAfter(originalUpdatedAt));
            
            LocalDateTime processingUpdatedAt = payment.getUpdatedAt();
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            payment.confirm();
            assertTrue(payment.getUpdatedAt().isAfter(processingUpdatedAt));
            assertNotNull(payment.getCompletedAt());
        }

        @Test
        @DisplayName("Should enforce valid state transitions only")
        void shouldEnforceValidStateTransitionsOnly() {
            // Test all valid transitions from PENDING
            Payment pendingPayment1 = createTestPayment();
            Payment pendingPayment2 = createTestPayment();
            Payment pendingPayment3 = createTestPayment();
            Payment pendingPayment4 = createTestPayment();
            
            assertDoesNotThrow(() -> pendingPayment1.markAsProcessing());
            assertDoesNotThrow(() -> pendingPayment2.confirm());
            assertDoesNotThrow(() -> pendingPayment3.fail("Test failure"));
            assertDoesNotThrow(() -> pendingPayment4.cancel("Test cancellation"));
            
            // Test valid transitions from PROCESSING
            Payment processingPayment1 = createTestPayment();
            Payment processingPayment2 = createTestPayment();
            processingPayment1.markAsProcessing();
            processingPayment2.markAsProcessing();
            
            assertDoesNotThrow(() -> processingPayment1.confirm());
            assertDoesNotThrow(() -> processingPayment2.fail("Processing failure"));
            
            // Test valid transition from COMPLETED
            Payment completedPayment = createTestPayment();
            completedPayment.confirm();
            
            assertDoesNotThrow(() -> completedPayment.refund());
        }

        @Test
        @DisplayName("Should handle concurrent-like state transition attempts gracefully")
        void shouldHandleConcurrentLikeStateTransitionAttemptsGracefully() {
            // Simulate scenario where multiple operations might be attempted
            Payment testPayment = createTestPayment();
            
            // First operation succeeds
            testPayment.markAsProcessing();
            
            // Subsequent conflicting operations should fail
            assertAll(
                () -> assertThrows(IllegalStateException.class, () -> testPayment.markAsProcessing()),
                () -> assertThrows(IllegalStateException.class, () -> testPayment.cancel("Late cancellation"))
            );
            
            // But valid operations should still work
            assertDoesNotThrow(() -> testPayment.confirm());
        }
    }

    @Nested
    @DisplayName("Payment Fee Calculation Tests - Single Responsibility")
    class PaymentFeeCalculationTests {

        @Test
        @DisplayName("Should calculate fees based on payment type")
        void shouldCalculateFeesBasedOnPaymentType() {
            Payment domesticPayment = Payment.create(
                PaymentId.generate(), customerId, fromAccountId, toAccountId, 
                amount, PaymentType.TRANSFER, description
            );
            
            Payment internationalPayment = Payment.create(
                PaymentId.generate(), customerId, fromAccountId, toAccountId, 
                amount, PaymentType.WIRE_TRANSFER, description
            );
            
            // International transfers should typically have higher fees
            assertTrue(internationalPayment.getFee().compareTo(domesticPayment.getFee()) >= 0);
        }

        @Test
        @DisplayName("Should maintain fee consistency throughout payment lifecycle")
        void shouldMaintainFeeConsistencyThroughoutPaymentLifecycle() {
            Money originalFee = payment.getFee();
            
            payment.markAsProcessing();
            assertEquals(originalFee, payment.getFee());
            
            payment.confirm();
            assertEquals(originalFee, payment.getFee());
        }

        @Test
        @DisplayName("Should calculate total amount correctly with fees")
        void shouldCalculateTotalAmountCorrectlyWithFees() {
            Money calculatedTotal = payment.getAmount().add(payment.getFee());
            assertEquals(calculatedTotal, payment.getTotalAmount());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Property-Based Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle minimum payment amounts")
        void shouldHandleMinimumPaymentAmounts() {
            Money minimumAmount = Money.aed(new BigDecimal("0.01"));
            Payment minPayment = Payment.create(
                PaymentId.generate(), customerId, fromAccountId, toAccountId, 
                minimumAmount, paymentType, description
            );
            
            assertEquals(minimumAmount, minPayment.getAmount());
            assertTrue(minPayment.getFee().isPositive() || minPayment.getFee().isZero());
            assertTrue(minPayment.getTotalAmount().compareTo(minimumAmount) >= 0);
        }

        @Test
        @DisplayName("Should handle large payment amounts")
        void shouldHandleLargePaymentAmounts() {
            Money largeAmount = Money.aed(new BigDecimal("10000000")); // 10M AED
            Payment largePayment = Payment.create(
                PaymentId.generate(), customerId, fromAccountId, toAccountId, 
                largeAmount, paymentType, description
            );
            
            assertEquals(largeAmount, largePayment.getAmount());
            assertTrue(largePayment.getFee().isPositive() || largePayment.getFee().isZero());
            assertEquals(largeAmount.add(largePayment.getFee()), largePayment.getTotalAmount());
        }

        @Test
        @DisplayName("Should maintain state machine integrity under error conditions")
        void shouldMaintainStateMachineIntegrityUnderErrorConditions() {
            // Test that failed state transitions don't corrupt the payment
            Payment testPayment = createTestPayment();
            PaymentStatus originalStatus = testPayment.getStatus();
            
            testPayment.markAsProcessing();
            
            // Try invalid transition
            try {
                testPayment.markAsProcessing(); // Should fail
            } catch (IllegalStateException e) {
                // Expected
            }
            
            // Payment should still be in valid state
            assertEquals(PaymentStatus.PROCESSING, testPayment.getStatus());
            assertDoesNotThrow(() -> testPayment.confirm());
        }

        @Test
        @DisplayName("Should handle payment lifecycle edge cases")
        void shouldHandlePaymentLifecycleEdgeCases() {
            // Test rapid succession of valid operations
            Payment rapidPayment = createTestPayment();
            
            // Rapid processing and completion
            rapidPayment.markAsProcessing();
            rapidPayment.confirm();
            rapidPayment.refund();
            
            assertEquals(PaymentStatus.REFUNDED, rapidPayment.getStatus());
            
            // Test that no further operations are allowed
            assertAll(
                () -> assertThrows(IllegalStateException.class, () -> rapidPayment.confirm()),
                () -> assertThrows(IllegalStateException.class, () -> rapidPayment.fail("Should fail")),
                () -> assertThrows(IllegalStateException.class, () -> rapidPayment.cancel("Should fail")),
                () -> assertThrows(IllegalStateException.class, () -> rapidPayment.refund())
            );
        }
    }

    @Nested
    @DisplayName("Payment Status Query Tests - Information Expert")
    class PaymentStatusQueryTests {

        @Test
        @DisplayName("Should provide accurate status queries")
        void shouldProvideAccurateStatusQueries() {
            // Initial state
            assertTrue(payment.isPending());
            assertFalse(payment.isCompleted());
            assertFalse(payment.isFailed());
            
            // After processing
            payment.markAsProcessing();
            assertFalse(payment.isPending());
            assertFalse(payment.isCompleted());
            assertFalse(payment.isFailed());
            
            // After completion
            payment.confirm();
            assertFalse(payment.isPending());
            assertTrue(payment.isCompleted());
            assertFalse(payment.isFailed());
        }

        @Test
        @DisplayName("Should provide accurate status queries for failed payments")
        void shouldProvideAccurateStatusQueriesForFailedPayments() {
            payment.fail("Test failure");
            
            assertFalse(payment.isPending());
            assertFalse(payment.isCompleted());
            assertTrue(payment.isFailed());
        }

        @Test
        @DisplayName("Should maintain status query consistency")
        void shouldMaintainStatusQueryConsistency() {
            // Only one status should be true at any time
            assertStatusConsistency(payment);
            
            payment.markAsProcessing();
            assertStatusConsistency(payment);
            
            payment.confirm();
            assertStatusConsistency(payment);
        }
    }

    // Helper methods
    
    private Payment createTestPayment() {
        return Payment.create(
            PaymentId.generate(), 
            CustomerId.generate(), 
            AccountId.generate(), 
            AccountId.generate(), 
            Money.aed(new BigDecimal("1000")), 
            PaymentType.TRANSFER, 
            "Test payment"
        );
    }
    
    private void forcePaymentToStatus(Payment payment, PaymentStatus targetStatus) {
        // Helper method to force payment to specific status for testing
        switch (targetStatus) {
            case PENDING:
                // Already pending by default
                break;
            case PROCESSING:
                payment.markAsProcessing();
                break;
            case COMPLETED:
                payment.confirm();
                break;
            case FAILED:
                payment.fail("Forced failure for testing");
                break;
            case CANCELLED:
                payment.cancel("Forced cancellation for testing");
                break;
            case REFUNDED:
                payment.confirm();
                payment.refund();
                break;
        }
    }
    
    private void assertStatusConsistency(Payment payment) {
        // Ensure only one status query returns true
        int trueCount = 0;
        if (payment.isPending()) trueCount++;
        if (payment.isCompleted()) trueCount++;
        if (payment.isFailed()) trueCount++;
        
        assertTrue(trueCount <= 1, "Multiple status queries returned true simultaneously");
    }
}