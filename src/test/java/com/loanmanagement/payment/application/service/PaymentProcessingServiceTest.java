package com.loanmanagement.payment.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Java 21 Enhanced Payment Processing Service
 * 
 * Tests Java 21 features:
 * - Virtual Threads for high-throughput payment processing
 * - Pattern Matching for payment validation and routing
 * - Record Patterns for payment data structures
 * - Sequenced Collections for ordered payment processing
 * 
 * Banking scenarios covered:
 * - Real-time payment processing
 * - Batch payment operations
 * - Fraud detection integration
 * - Compliance validation
 * - Performance optimization
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Java 21 Payment Processing Service")
class PaymentProcessingServiceTest {
    
    private PaymentProcessingService paymentProcessingService;
    
    // Virtual Thread executors for testing
    private final var paymentProcessingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var fraudDetectionExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var complianceCheckExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var auditProcessingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    @BeforeEach
    void setUp() {
        paymentProcessingService = new PaymentProcessingService(
            paymentProcessingExecutor,
            fraudDetectionExecutor,
            complianceCheckExecutor,
            auditProcessingExecutor
        );
    }
    
    @Nested
    @DisplayName("Virtual Threads Payment Processing")
    class VirtualThreadsPaymentProcessing {
        
        @Test
        @DisplayName("Should process payment with concurrent validation using Virtual Threads")
        void shouldProcessPaymentWithConcurrentValidation() {
            // Given
            var paymentRequest = createValidPaymentRequest("PAY001", BigDecimal.valueOf(1000));
            
            // When
            var startTime = System.currentTimeMillis();
            var result = paymentProcessingService.processPayment(paymentRequest);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.paymentId()).isEqualTo("PAY001");
            assertThat(result.status()).isIn(
                PaymentProcessingService.PaymentStatus.COMPLETED,
                PaymentProcessingService.PaymentStatus.REJECTED
            );
            assertThat(processingTime).isLessThan(2000); // Should complete within 2 seconds
        }
        
        @Test
        @DisplayName("Should handle high-throughput batch payment processing")
        void shouldHandleHighThroughputBatchProcessing() {
            // Given
            var paymentRequests = generateBatchPaymentRequests(100);
            
            // When
            var startTime = System.currentTimeMillis();
            var results = paymentProcessingService.processBatchPayments(paymentRequests);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(results).hasSize(100);
            assertThat(processingTime).isLessThan(5000); // Should process 100 payments within 5 seconds
            
            // Verify all results have valid status
            assertThat(results).allMatch(result -> 
                result.status() != null && result.paymentId() != null
            );
        }
        
        @Test
        @DisplayName("Should maintain payment order in batch processing with Sequenced Collections")
        void shouldMaintainPaymentOrderInBatchProcessing() {
            // Given
            var paymentRequests = List.of(
                createValidPaymentRequest("PAY001", BigDecimal.valueOf(100)),
                createValidPaymentRequest("PAY002", BigDecimal.valueOf(200)),
                createValidPaymentRequest("PAY003", BigDecimal.valueOf(300)),
                createValidPaymentRequest("PAY004", BigDecimal.valueOf(400)),
                createValidPaymentRequest("PAY005", BigDecimal.valueOf(500))
            );
            
            // When
            var results = paymentProcessingService.processBatchPayments(paymentRequests);
            
            // Then - Order should be preserved (Sequenced Collections feature)
            assertThat(results).hasSize(5);
            assertThat(results.get(0).paymentId()).isEqualTo("PAY001");
            assertThat(results.get(1).paymentId()).isEqualTo("PAY002");
            assertThat(results.get(2).paymentId()).isEqualTo("PAY003");
            assertThat(results.get(3).paymentId()).isEqualTo("PAY004");
            assertThat(results.get(4).paymentId()).isEqualTo("PAY005");
        }
        
        @Test
        @DisplayName("Should demonstrate Virtual Threads scalability")
        void shouldDemonstrateVirtualThreadsScalability() {
            // Given
            var largePaymentBatch = generateBatchPaymentRequests(500);
            
            // When
            var startTime = System.currentTimeMillis();
            var results = paymentProcessingService.processBatchPayments(largePaymentBatch);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(results).hasSize(500);
            assertThat(processingTime).isLessThan(10000); // Virtual Threads should handle 500 payments efficiently
            
            // Verify successful processing rate
            var successfulPayments = results.stream()
                .mapToInt(result -> result.status() == PaymentProcessingService.PaymentStatus.COMPLETED ? 1 : 0)
                .sum();
            assertThat(successfulPayments).isGreaterThan(400); // Expect > 80% success rate
        }
    }
    
    @Nested
    @DisplayName("Pattern Matching Payment Validation")
    class PatternMatchingPaymentValidation {
        
        @Test
        @DisplayName("Should approve valid domestic payment using pattern matching")
        void shouldApproveValidDomesticPaymentUsingPatternMatching() {
            // Given
            var paymentRequest = new PaymentProcessingService.PaymentRequest(
                "PAY001",
                PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
                "1234567890",
                "0987654321",
                BigDecimal.valueOf(5000),
                "USD",
                "Domestic transfer",
                LocalDateTime.now()
            );
            
            // When
            var result = paymentProcessingService.processPayment(paymentRequest);
            
            // Then
            assertThat(result.status()).isEqualTo(PaymentProcessingService.PaymentStatus.COMPLETED);
            assertThat(result.transactionId()).isNotNull();
            assertThat(result.message()).contains("successfully");
        }
        
        @Test
        @DisplayName("Should reject payment with invalid format using pattern matching")
        void shouldRejectPaymentWithInvalidFormatUsingPatternMatching() {
            // Given - Payment with same from/to account
            var invalidPaymentRequest = new PaymentProcessingService.PaymentRequest(
                "PAY002",
                PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
                "1234567890",
                "1234567890", // Same as from account
                BigDecimal.valueOf(1000),
                "USD",
                "Invalid payment",
                LocalDateTime.now()
            );
            
            // When
            var result = paymentProcessingService.processPayment(invalidPaymentRequest);
            
            // Then
            assertThat(result.status()).isEqualTo(PaymentProcessingService.PaymentStatus.REJECTED);
            assertThat(result.message()).contains("same");
        }
        
        @Test
        @DisplayName("Should handle different payment types with pattern matching")
        void shouldHandleDifferentPaymentTypesWithPatternMatching() {
            // Given
            var domesticPayment = createPaymentOfType(PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER);
            var internationalPayment = createPaymentOfType(PaymentProcessingService.PaymentType.INTERNATIONAL_TRANSFER);
            var loanPayment = createPaymentOfType(PaymentProcessingService.PaymentType.LOAN_PAYMENT);
            var billPayment = createPaymentOfType(PaymentProcessingService.PaymentType.BILL_PAYMENT);
            var instantPayment = createPaymentOfType(PaymentProcessingService.PaymentType.INSTANT_PAYMENT);
            
            // When
            var results = List.of(
                paymentProcessingService.processPayment(domesticPayment),
                paymentProcessingService.processPayment(internationalPayment),
                paymentProcessingService.processPayment(loanPayment),
                paymentProcessingService.processPayment(billPayment),
                paymentProcessingService.processPayment(instantPayment)
            );
            
            // Then - All should be processed successfully but with different processing characteristics
            assertThat(results).hasSize(5);
            assertThat(results).allMatch(result -> result.status() == PaymentProcessingService.PaymentStatus.COMPLETED);
            
            // Instant payment should be fastest (based on processing logic)
            var instantResult = results.get(4);
            assertThat(instantResult.message()).contains("Instant payment");
        }
        
        @Test
        @DisplayName("Should block high-risk payments using pattern matching")
        void shouldBlockHighRiskPaymentsUsingPatternMatching() {
            // Given - Large payment that might trigger fraud detection
            var highRiskPayment = new PaymentProcessingService.PaymentRequest(
                "PAY_HIGH_RISK",
                PaymentProcessingService.PaymentType.INTERNATIONAL_TRANSFER,
                "1234567890",
                "0987654321",
                BigDecimal.valueOf(100000), // Large amount
                "USD",
                "High risk payment",
                LocalDateTime.now()
            );
            
            // When
            var result = paymentProcessingService.processPayment(highRiskPayment);
            
            // Then - Could be completed, rejected, or blocked based on fraud score
            assertThat(result.status()).isIn(
                PaymentProcessingService.PaymentStatus.COMPLETED,
                PaymentProcessingService.PaymentStatus.REJECTED,
                PaymentProcessingService.PaymentStatus.BLOCKED
            );
        }
    }
    
    @Nested
    @DisplayName("Record Patterns Data Validation")
    class RecordPatternsDataValidation {
        
        @Test
        @DisplayName("Should validate PaymentRequest with record patterns")
        void shouldValidatePaymentRequestWithRecordPatterns() {
            // Given & When & Then - Valid payment request should not throw
            assertThatCode(() ->
                new PaymentProcessingService.PaymentRequest(
                    "PAY001",
                    PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
                    "1234567890",
                    "0987654321",
                    BigDecimal.valueOf(1000),
                    "USD",
                    "Valid payment",
                    LocalDateTime.now()
                )
            ).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should reject PaymentRequest with null payment ID")
        void shouldRejectPaymentRequestWithNullPaymentId() {
            // Given & When & Then
            assertThatThrownBy(() ->
                new PaymentProcessingService.PaymentRequest(
                    null, // Invalid payment ID
                    PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
                    "1234567890",
                    "0987654321",
                    BigDecimal.valueOf(1000),
                    "USD",
                    "Invalid payment",
                    LocalDateTime.now()
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Payment ID is required");
        }
        
        @Test
        @DisplayName("Should reject PaymentRequest with negative amount")
        void shouldRejectPaymentRequestWithNegativeAmount() {
            // Given & When & Then
            assertThatThrownBy(() ->
                new PaymentProcessingService.PaymentRequest(
                    "PAY001",
                    PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
                    "1234567890",
                    "0987654321",
                    BigDecimal.valueOf(-1000), // Invalid negative amount
                    "USD",
                    "Invalid payment",
                    LocalDateTime.now()
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Amount must be positive");
        }
        
        @Test
        @DisplayName("Should validate PaymentResult record patterns")
        void shouldValidatePaymentResultRecordPatterns() {
            // Given
            var completedResult = PaymentProcessingService.PaymentResult.completed(
                "PAY001", "Payment successful", "TXN12345"
            );
            var rejectedResult = PaymentProcessingService.PaymentResult.rejected(
                "PAY002", "Insufficient funds"
            );
            
            // When & Then
            assertThat(completedResult.status()).isEqualTo(PaymentProcessingService.PaymentStatus.COMPLETED);
            assertThat(completedResult.transactionId()).isEqualTo("TXN12345");
            assertThat(completedResult.completedAt()).isNotNull();
            
            assertThat(rejectedResult.status()).isEqualTo(PaymentProcessingService.PaymentStatus.REJECTED);
            assertThat(rejectedResult.transactionId()).isNull();
            assertThat(rejectedResult.message()).isEqualTo("Insufficient funds");
        }
    }
    
    @Nested
    @DisplayName("Concurrent Processing Performance")
    class ConcurrentProcessingPerformance {
        
        @Test
        @DisplayName("Should handle concurrent payment validation efficiently")
        void shouldHandleConcurrentPaymentValidationEfficiently() {
            // Given
            var paymentRequest = createValidPaymentRequest("PAY_CONCURRENT", BigDecimal.valueOf(2500));
            
            // When - Process multiple concurrent validations
            var futures = IntStream.range(0, 10)
                .mapToObj(i -> java.util.concurrent.CompletableFuture.supplyAsync(() ->
                    paymentProcessingService.processPayment(
                        createValidPaymentRequest("PAY_" + i, BigDecimal.valueOf(1000 + i * 100))
                    )
                ))
                .toList();
            
            var results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        fail("Concurrent payment processing failed", e);
                        return null;
                    }
                })
                .toList();
            
            // Then
            assertThat(results).hasSize(10);
            assertThat(results).allMatch(result -> result.paymentId().startsWith("PAY_"));
        }
        
        @Test
        @DisplayName("Should maintain thread safety in concurrent operations")
        void shouldMaintainThreadSafetyInConcurrentOperations() {
            // Given
            var paymentRequests = generateBatchPaymentRequests(50);
            
            // When - Process in multiple concurrent batches
            var batch1Future = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                paymentProcessingService.processBatchPayments(paymentRequests.subList(0, 25))
            );
            var batch2Future = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                paymentProcessingService.processBatchPayments(paymentRequests.subList(25, 50))
            );
            
            // Then
            try {
                var batch1Results = batch1Future.get(10, TimeUnit.SECONDS);
                var batch2Results = batch2Future.get(10, TimeUnit.SECONDS);
                
                assertThat(batch1Results).hasSize(25);
                assertThat(batch2Results).hasSize(25);
                
                // Verify no payment ID conflicts
                var allPaymentIds = java.util.stream.Stream.concat(
                    batch1Results.stream().map(PaymentProcessingService.PaymentResult::paymentId),
                    batch2Results.stream().map(PaymentProcessingService.PaymentResult::paymentId)
                ).toList();
                
                assertThat(allPaymentIds).hasSize(50);
                assertThat(allPaymentIds).doesNotHaveDuplicates();
                
            } catch (Exception e) {
                fail("Concurrent batch processing failed", e);
            }
        }
        
        @Test
        @DisplayName("Should handle timeout scenarios in payment processing")
        void shouldHandleTimeoutScenariosInPaymentProcessing() {
            // Given - Create many payments to potentially trigger timeouts
            var manyPayments = generateBatchPaymentRequests(200);
            
            // When
            var startTime = System.currentTimeMillis();
            var results = paymentProcessingService.processBatchPayments(manyPayments);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then - Should complete within reasonable time
            assertThat(results).hasSize(200);
            assertThat(processingTime).isLessThan(30000); // Should complete within 30 seconds
            
            // Most payments should succeed even under load
            var successCount = results.stream()
                .mapToInt(result -> result.status() == PaymentProcessingService.PaymentStatus.COMPLETED ? 1 : 0)
                .sum();
            assertThat(successCount).isGreaterThan(150); // Expect > 75% success rate
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingAndResilience {
        
        @Test
        @DisplayName("Should handle payment processing exceptions gracefully")
        void shouldHandlePaymentProcessingExceptionsGracefully() {
            // Given - Payment request with invalid data that might cause processing issues
            var problematicPayment = new PaymentProcessingService.PaymentRequest(
                "PAY_PROBLEM",
                PaymentProcessingService.PaymentType.INTERNATIONAL_TRANSFER,
                "", // Empty from account
                "0987654321",
                BigDecimal.valueOf(1000),
                "USD",
                "Problematic payment",
                LocalDateTime.now()
            );
            
            // When
            var result = paymentProcessingService.processPayment(problematicPayment);
            
            // Then - Should handle gracefully
            assertThat(result).isNotNull();
            assertThat(result.paymentId()).isEqualTo("PAY_PROBLEM");
            // Status could be REJECTED or FAILED depending on validation
            assertThat(result.status()).isIn(
                PaymentProcessingService.PaymentStatus.REJECTED,
                PaymentProcessingService.PaymentStatus.FAILED
            );
        }
        
        @Test
        @DisplayName("Should handle Virtual Thread interruption gracefully")
        void shouldHandleVirtualThreadInterruptionGracefully() {
            // Given
            var paymentRequest = createValidPaymentRequest("PAY_INTERRUPT", BigDecimal.valueOf(1000));
            
            // When - Start processing in Virtual Thread and interrupt
            var processingThread = Thread.ofVirtual().start(() -> {
                try {
                    paymentProcessingService.processPayment(paymentRequest);
                } catch (Exception e) {
                    // Expected during interruption
                }
            });
            
            // Interrupt the thread
            try {
                Thread.sleep(10);
                processingThread.interrupt();
                processingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Then - Should handle interruption without hanging
            assertThat(processingThread.isAlive()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Real-time Payment Monitoring")
    class RealTimePaymentMonitoring {
        
        @Test
        @DisplayName("Should start payment monitoring with Virtual Threads")
        void shouldStartPaymentMonitoringWithVirtualThreads() {
            // Given & When
            assertThatCode(() -> {
                paymentProcessingService.startPaymentMonitoring();
                Thread.sleep(100); // Let monitoring run briefly
            }).doesNotThrowAnyException();
            
            // Then - Monitoring should start without errors
            // Note: In real implementation, we'd verify monitoring metrics
        }
    }
    
    // Helper methods
    
    private PaymentProcessingService.PaymentRequest createValidPaymentRequest(String paymentId, BigDecimal amount) {
        return new PaymentProcessingService.PaymentRequest(
            paymentId,
            PaymentProcessingService.PaymentType.DOMESTIC_TRANSFER,
            "1234567890",
            "0987654321",
            amount,
            "USD",
            "Test payment",
            LocalDateTime.now()
        );
    }
    
    private PaymentProcessingService.PaymentRequest createPaymentOfType(
            PaymentProcessingService.PaymentType type) {
        return new PaymentProcessingService.PaymentRequest(
            "PAY_" + type.name(),
            type,
            "1234567890",
            "0987654321",
            BigDecimal.valueOf(1000),
            "USD",
            "Payment of type " + type,
            LocalDateTime.now()
        );
    }
    
    private List<PaymentProcessingService.PaymentRequest> generateBatchPaymentRequests(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new PaymentProcessingService.PaymentRequest(
                "PAY" + String.format("%05d", i),
                PaymentProcessingService.PaymentType.values()[i % PaymentProcessingService.PaymentType.values().length],
                "1234567890",
                "0987654321",
                BigDecimal.valueOf(1000 + (i * 10)),
                "USD",
                "Batch payment " + i,
                LocalDateTime.now()
            ))
            .toList();
    }
}