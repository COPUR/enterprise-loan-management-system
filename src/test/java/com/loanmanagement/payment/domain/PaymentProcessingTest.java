package com.loanmanagement.payment.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.payment.domain.service.PaymentProcessingService;
import com.loanmanagement.payment.domain.service.PaymentValidationService;
import com.loanmanagement.payment.domain.service.PaymentAllocationService;
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
 * Tests for Payment Processing Domain
 * Comprehensive testing of payment processing logic
 */
@DisplayName("Payment Processing Tests")
class PaymentProcessingTest {

    private PaymentProcessingService paymentProcessingService;
    private PaymentValidationService paymentValidationService;
    private PaymentAllocationService paymentAllocationService;
    private LoanId testLoanId;
    private CustomerId testCustomerId;
    private Money standardPaymentAmount;
    private PaymentSource testPaymentSource;

    @BeforeEach
    void setUp() {
        paymentProcessingService = new PaymentProcessingService();
        paymentValidationService = new PaymentValidationService();
        paymentAllocationService = new PaymentAllocationService();
        testLoanId = LoanId.generate();
        testCustomerId = CustomerId.generate();
        standardPaymentAmount = Money.of("USD", new BigDecimal("1200.00"));
        testPaymentSource = PaymentSource.builder()
                .sourceType(PaymentSourceType.BANK_ACCOUNT)
                .accountNumber("****1234")
                .routingNumber("123456789")
                .bankName("Test Bank")
                .build();
    }

    @Nested
    @DisplayName("Basic Payment Processing Tests")
    class BasicPaymentProcessingTests {

        @Test
        @DisplayName("Should process standard loan payment successfully")
        void shouldProcessStandardLoanPaymentSuccessfully() {
            // Given
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .build();

            // When
            PaymentResult result = paymentProcessingService.processPayment(paymentRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.PROCESSED, result.getPaymentStatus());
            assertNotNull(result.getPaymentId());
            assertEquals(standardPaymentAmount, result.getProcessedAmount());
            assertTrue(result.isSuccessful());
            assertNotNull(result.getTransactionId());
        }

        @Test
        @DisplayName("Should handle partial payment processing")
        void shouldHandlePartialPaymentProcessing() {
            // Given
            Money partialAmount = Money.of("USD", new BigDecimal("600.00"));
            PaymentRequest partialPaymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(partialAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.PARTIAL_PAYMENT)
                    .allowPartialPayment(true)
                    .build();

            // When
            PaymentResult result = paymentProcessingService.processPayment(partialPaymentRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.PROCESSED, result.getPaymentStatus());
            assertEquals(partialAmount, result.getProcessedAmount());
            assertTrue(result.isPartialPayment());
            assertNotNull(result.getRemainingBalance());
        }

        @Test
        @DisplayName("Should reject payment with insufficient funds")
        void shouldRejectPaymentWithInsufficientFunds() {
            // Given
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .build();

            // Mock insufficient funds scenario
            PaymentValidationResult validationResult = PaymentValidationResult.failed(
                    PaymentFailureReason.INSUFFICIENT_FUNDS, "Insufficient account balance");

            // When
            PaymentResult result = paymentProcessingService.processPaymentWithValidation(
                    paymentRequest, validationResult);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.FAILED, result.getPaymentStatus());
            assertFalse(result.isSuccessful());
            assertEquals(PaymentFailureReason.INSUFFICIENT_FUNDS, result.getFailureReason());
            assertNotNull(result.getFailureMessage());
        }

        @Test
        @DisplayName("Should handle early payment with interest savings")
        void shouldHandleEarlyPaymentWithInterestSavings() {
            // Given
            Money earlyPaymentAmount = Money.of("USD", new BigDecimal("50000.00"));
            PaymentRequest earlyPaymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(earlyPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.EARLY_PAYOFF)
                    .build();

            // When
            PaymentResult result = paymentProcessingService.processPayment(earlyPaymentRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.PROCESSED, result.getPaymentStatus());
            assertTrue(result.isEarlyPayoff());
            assertNotNull(result.getInterestSavings());
            assertTrue(result.getInterestSavings().getAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    @DisplayName("Payment Allocation Tests")
    class PaymentAllocationTests {

        @Test
        @DisplayName("Should allocate payment to principal and interest correctly")
        void shouldAllocatePaymentToPrincipalAndInterestCorrectly() {
            // Given
            LoanBalance currentBalance = LoanBalance.builder()
                    .principalBalance(Money.of("USD", new BigDecimal("95000.00")))
                    .interestBalance(Money.of("USD", new BigDecimal("450.00")))
                    .feesBalance(Money.of("USD", new BigDecimal("25.00")))
                    .build();

            // When
            PaymentAllocation allocation = paymentAllocationService.allocatePayment(
                    standardPaymentAmount, currentBalance, PaymentAllocationStrategy.STANDARD);

            // Then
            assertNotNull(allocation);
            assertEquals(standardPaymentAmount, allocation.getTotalAmount());
            
            // Fees should be paid first
            assertEquals(Money.of("USD", new BigDecimal("25.00")), allocation.getFeesAmount());
            
            // Interest should be paid next
            assertEquals(Money.of("USD", new BigDecimal("450.00")), allocation.getInterestAmount());
            
            // Remaining goes to principal
            assertEquals(Money.of("USD", new BigDecimal("725.00")), allocation.getPrincipalAmount());
            
            // Verify total allocation
            Money totalAllocated = allocation.getFeesAmount()
                    .add(allocation.getInterestAmount())
                    .add(allocation.getPrincipalAmount());
            assertEquals(standardPaymentAmount, totalAllocated);
        }

        @Test
        @DisplayName("Should handle payment allocation with escrow")
        void shouldHandlePaymentAllocationWithEscrow() {
            // Given
            Money escrowPayment = Money.of("USD", new BigDecimal("300.00"));
            LoanBalance currentBalance = LoanBalance.builder()
                    .principalBalance(Money.of("USD", new BigDecimal("95000.00")))
                    .interestBalance(Money.of("USD", new BigDecimal("450.00")))
                    .escrowBalance(Money.of("USD", new BigDecimal("150.00")))
                    .build();

            Money totalPayment = standardPaymentAmount.add(escrowPayment);

            // When
            PaymentAllocation allocation = paymentAllocationService.allocatePayment(
                    totalPayment, currentBalance, PaymentAllocationStrategy.WITH_ESCROW);

            // Then
            assertNotNull(allocation);
            assertEquals(escrowPayment, allocation.getEscrowAmount());
            assertNotNull(allocation.getEscrowDetails());
            assertTrue(allocation.includesEscrow());
        }

        @Test
        @DisplayName("Should prioritize late fees in payment allocation")
        void shouldPrioritizeLateFeesinPaymentAllocation() {
            // Given
            LoanBalance balanceWithLateFees = LoanBalance.builder()
                    .principalBalance(Money.of("USD", new BigDecimal("95000.00")))
                    .interestBalance(Money.of("USD", new BigDecimal("450.00")))
                    .lateFeesBalance(Money.of("USD", new BigDecimal("75.00")))
                    .otherFeesBalance(Money.of("USD", new BigDecimal("25.00")))
                    .build();

            // When
            PaymentAllocation allocation = paymentAllocationService.allocatePayment(
                    standardPaymentAmount, balanceWithLateFees, PaymentAllocationStrategy.FEES_FIRST);

            // Then
            assertNotNull(allocation);
            
            // Late fees should be paid first
            assertEquals(Money.of("USD", new BigDecimal("75.00")), allocation.getLateFeesAmount());
            
            // Other fees should be paid next
            assertEquals(Money.of("USD", new BigDecimal("25.00")), allocation.getOtherFeesAmount());
            
            // Verify allocation order
            assertTrue(allocation.getAllocationOrder().get(0).equals("LATE_FEES"));
            assertTrue(allocation.getAllocationOrder().get(1).equals("OTHER_FEES"));
        }
    }

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {

        @Test
        @DisplayName("Should validate payment amount limits")
        void shouldValidatePaymentAmountLimits() {
            // Given
            Money excessiveAmount = Money.of("USD", new BigDecimal("1000000.00"));
            PaymentRequest excessivePaymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(excessiveAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .build();

            // When
            PaymentValidationResult validationResult = paymentValidationService
                    .validatePayment(excessivePaymentRequest);

            // Then
            assertNotNull(validationResult);
            assertFalse(validationResult.isValid());
            assertTrue(validationResult.getViolations().stream()
                    .anyMatch(v -> v.getViolationType() == PaymentViolationType.AMOUNT_EXCEEDS_LIMIT));
        }

        @Test
        @DisplayName("Should validate payment source information")
        void shouldValidatePaymentSourceInformation() {
            // Given
            PaymentSource invalidSource = PaymentSource.builder()
                    .sourceType(PaymentSourceType.BANK_ACCOUNT)
                    .accountNumber("") // Invalid empty account
                    .routingNumber("invalid") // Invalid routing number
                    .build();

            PaymentRequest invalidSourceRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(invalidSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .build();

            // When
            PaymentValidationResult validationResult = paymentValidationService
                    .validatePayment(invalidSourceRequest);

            // Then
            assertNotNull(validationResult);
            assertFalse(validationResult.isValid());
            assertTrue(validationResult.getViolations().stream()
                    .anyMatch(v -> v.getViolationType() == PaymentViolationType.INVALID_PAYMENT_SOURCE));
        }

        @Test
        @DisplayName("Should validate payment timing restrictions")
        void shouldValidatePaymentTimingRestrictions() {
            // Given - Payment on weekend
            LocalDateTime weekendPayment = LocalDateTime.of(2024, 1, 6, 10, 0); // Saturday
            PaymentRequest weekendPaymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(weekendPayment)
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .build();

            // When
            PaymentValidationResult validationResult = paymentValidationService
                    .validatePaymentTiming(weekendPaymentRequest);

            // Then
            assertNotNull(validationResult);
            assertTrue(validationResult.hasWarnings());
            assertTrue(validationResult.getWarnings().stream()
                    .anyMatch(w -> w.contains("weekend")));
        }

        @Test
        @DisplayName("Should validate duplicate payment prevention")
        void shouldValidateDuplicatePaymentPrevention() {
            // Given
            PaymentRequest duplicateRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .idempotencyKey("duplicate-key-123")
                    .build();

            // When
            PaymentValidationResult firstValidation = paymentValidationService
                    .validatePayment(duplicateRequest);
            PaymentValidationResult duplicateValidation = paymentValidationService
                    .validatePayment(duplicateRequest);

            // Then
            assertTrue(firstValidation.isValid());
            assertFalse(duplicateValidation.isValid());
            assertTrue(duplicateValidation.getViolations().stream()
                    .anyMatch(v -> v.getViolationType() == PaymentViolationType.DUPLICATE_PAYMENT));
        }
    }

    @Nested
    @DisplayName("Advanced Payment Processing Tests")
    class AdvancedPaymentProcessingTests {

        @Test
        @DisplayName("Should process recurring payment setup")
        void shouldProcessRecurringPaymentSetup() {
            // Given
            RecurringPaymentSchedule recurringSchedule = RecurringPaymentSchedule.builder()
                    .frequency(PaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusYears(1))
                    .paymentAmount(standardPaymentAmount)
                    .build();

            PaymentRequest recurringSetupRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(standardPaymentAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.RECURRING_SETUP)
                    .recurringSchedule(recurringSchedule)
                    .build();

            // When
            PaymentResult result = paymentProcessingService.processPayment(recurringSetupRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.PROCESSED, result.getPaymentStatus());
            assertTrue(result.isRecurringPaymentSetup());
            assertNotNull(result.getRecurringPaymentId());
            assertEquals(12, result.getScheduledPaymentsCount()); // Monthly for 1 year
        }

        @Test
        @DisplayName("Should handle payment reversal processing")
        void shouldHandlePaymentReversalProcessing() {
            // Given
            PaymentId originalPaymentId = PaymentId.generate();
            PaymentReversalRequest reversalRequest = PaymentReversalRequest.builder()
                    .originalPaymentId(originalPaymentId)
                    .reversalReason(PaymentReversalReason.CUSTOMER_REQUEST)
                    .reversalAmount(standardPaymentAmount)
                    .reversalDate(LocalDateTime.now())
                    .requestedBy("customer-service-agent")
                    .build();

            // When
            PaymentReversalResult reversalResult = paymentProcessingService
                    .processPaymentReversal(reversalRequest);

            // Then
            assertNotNull(reversalResult);
            assertTrue(reversalResult.isSuccessful());
            assertEquals(originalPaymentId, reversalResult.getOriginalPaymentId());
            assertEquals(standardPaymentAmount, reversalResult.getReversedAmount());
            assertNotNull(reversalResult.getReversalTransactionId());
        }

        @Test
        @DisplayName("Should process payment with currency conversion")
        void shouldProcessPaymentWithCurrencyConversion() {
            // Given
            Money foreignCurrencyAmount = Money.of("EUR", new BigDecimal("1000.00"));
            CurrencyConversionRate conversionRate = CurrencyConversionRate.builder()
                    .fromCurrency("EUR")
                    .toCurrency("USD")
                    .rate(new BigDecimal("1.10"))
                    .effectiveDate(LocalDateTime.now())
                    .build();

            PaymentRequest foreignPaymentRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(foreignCurrencyAmount)
                    .paymentDate(LocalDateTime.now())
                    .paymentSource(testPaymentSource)
                    .paymentType(PaymentType.REGULAR_PAYMENT)
                    .currencyConversion(conversionRate)
                    .build();

            // When
            PaymentResult result = paymentProcessingService.processPayment(foreignPaymentRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentStatus.PROCESSED, result.getPaymentStatus());
            assertEquals(Money.of("USD", new BigDecimal("1100.00")), result.getProcessedAmount());
            assertTrue(result.involvesCurrencyConversion());
            assertNotNull(result.getConversionDetails());
        }

        @Test
        @DisplayName("Should handle payment batch processing")
        void shouldHandlePaymentBatchProcessing() {
            // Given
            List<PaymentRequest> batchPayments = List.of(
                    createPaymentRequest(Money.of("USD", new BigDecimal("1200.00"))),
                    createPaymentRequest(Money.of("USD", new BigDecimal("800.00"))),
                    createPaymentRequest(Money.of("USD", new BigDecimal("1500.00")))
            );

            PaymentBatchRequest batchRequest = PaymentBatchRequest.builder()
                    .batchId("BATCH-001")
                    .payments(batchPayments)
                    .processingDate(LocalDateTime.now())
                    .batchType(PaymentBatchType.SCHEDULED_PAYMENTS)
                    .build();

            // When
            PaymentBatchResult batchResult = paymentProcessingService
                    .processBatchPayments(batchRequest);

            // Then
            assertNotNull(batchResult);
            assertEquals(3, batchResult.getTotalPayments());
            assertEquals(3, batchResult.getSuccessfulPayments());
            assertEquals(0, batchResult.getFailedPayments());
            assertTrue(batchResult.isAllSuccessful());
            assertEquals(Money.of("USD", new BigDecimal("3500.00")), 
                    batchResult.getTotalProcessedAmount());
        }
    }

    @Nested
    @DisplayName("Payment Analytics Tests")
    class PaymentAnalyticsTests {

        @Test
        @DisplayName("Should calculate payment history analytics")
        void shouldCalculatePaymentHistoryAnalytics() {
            // Given
            List<Payment> paymentHistory = createSamplePaymentHistory();

            // When
            PaymentAnalytics analytics = paymentProcessingService
                    .calculatePaymentAnalytics(testLoanId, paymentHistory);

            // Then
            assertNotNull(analytics);
            assertTrue(analytics.getAveragePaymentAmount().getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(analytics.getOnTimePaymentPercentage().compareTo(BigDecimal.ZERO) > 0);
            assertNotNull(analytics.getPaymentTrends());
            assertNotNull(analytics.getLargestPayment());
            assertNotNull(analytics.getSmallestPayment());
        }

        @Test
        @DisplayName("Should predict next payment date")
        void shouldPredictNextPaymentDate() {
            // Given
            List<Payment> paymentHistory = createSamplePaymentHistory();

            // When
            PaymentPrediction prediction = paymentProcessingService
                    .predictNextPayment(testLoanId, paymentHistory);

            // Then
            assertNotNull(prediction);
            assertNotNull(prediction.getPredictedPaymentDate());
            assertNotNull(prediction.getPredictedPaymentAmount());
            assertTrue(prediction.getConfidenceScore().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(prediction.getConfidenceScore().compareTo(new BigDecimal("100")) <= 0);
        }

        @Test
        @DisplayName("Should detect payment anomalies")
        void shouldDetectPaymentAnomalies() {
            // Given
            List<Payment> paymentHistory = createSamplePaymentHistory();
            Payment anomalousPayment = Payment.builder()
                    .paymentId(PaymentId.generate())
                    .loanId(testLoanId)
                    .paymentAmount(Money.of("USD", new BigDecimal("10000.00"))) // Unusually large
                    .paymentDate(LocalDateTime.now())
                    .paymentStatus(PaymentStatus.PROCESSED)
                    .build();

            // When
            PaymentAnomalyAnalysis analysis = paymentProcessingService
                    .detectPaymentAnomalies(testLoanId, paymentHistory, anomalousPayment);

            // Then
            assertNotNull(analysis);
            assertTrue(analysis.hasAnomalies());
            assertTrue(analysis.getAnomalies().stream()
                    .anyMatch(a -> a.getAnomalyType() == PaymentAnomalyType.UNUSUAL_AMOUNT));
        }
    }

    // Helper methods
    private PaymentRequest createPaymentRequest(Money amount) {
        return PaymentRequest.builder()
                .loanId(testLoanId)
                .customerId(testCustomerId)
                .paymentAmount(amount)
                .paymentDate(LocalDateTime.now())
                .paymentSource(testPaymentSource)
                .paymentType(PaymentType.REGULAR_PAYMENT)
                .build();
    }

    private List<Payment> createSamplePaymentHistory() {
        return List.of(
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1200.00")))
                        .paymentDate(LocalDateTime.now().minusMonths(3))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build(),
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1200.00")))
                        .paymentDate(LocalDateTime.now().minusMonths(2))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build(),
                Payment.builder()
                        .paymentId(PaymentId.generate())
                        .loanId(testLoanId)
                        .paymentAmount(Money.of("USD", new BigDecimal("1150.00")))
                        .paymentDate(LocalDateTime.now().minusMonths(1))
                        .paymentStatus(PaymentStatus.PROCESSED)
                        .build()
        );
    }
}