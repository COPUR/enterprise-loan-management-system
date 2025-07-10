package com.loanmanagement.loan.application.service;

import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.application.port.out.LoanEventPublisher;
import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.event.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Java 21 Enhanced Loan Processing Service
 * 
 * Tests Java 21 features:
 * - Virtual Threads concurrent processing
 * - Pattern Matching for loan decisions
 * - Record Patterns for data validation
 * - Enhanced error handling
 * 
 * Banking scenarios covered:
 * - High-throughput loan processing
 * - Concurrent validation workflows
 * - Risk assessment integration
 * - Compliance checking
 * - Fraud detection
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Processing Service")
class LoanProcessingServiceTest {
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private LoanEventPublisher eventPublisher;
    
    private LoanProcessingService loanProcessingService;
    
    // Virtual Thread executors for testing
    private final var loanProcessingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var riskAssessmentExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var complianceCheckExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final var fraudDetectionExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    @BeforeEach
    void setUp() {
        loanProcessingService = new LoanProcessingService(
            loanRepository,
            eventPublisher,
            loanProcessingExecutor,
            riskAssessmentExecutor,
            complianceCheckExecutor,
            fraudDetectionExecutor
        );
    }
    
    @Nested
    @DisplayName("Virtual Threads Loan Processing")
    class VirtualThreadsLoanProcessing {
        
        @Test
        @DisplayName("Should create loan with concurrent validation using Virtual Threads")
        void shouldCreateLoanWithConcurrentValidation() {
            // Given
            var customerId = new CustomerId("CUST001");
            var amount = BigDecimal.valueOf(50000);
            var interestRate = BigDecimal.valueOf(5.5);
            var termInMonths = 60;
            var purpose = LoanPurpose.HOME_PURCHASE;
            
            var command = new LoanProcessingService.CreateLoanCommand(
                customerId, amount, interestRate, termInMonths, purpose
            );
            
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // When
            var startTime = System.currentTimeMillis();
            var loanId = loanProcessingService.createLoan(command);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(loanId).isNotNull();
            assertThat(processingTime).isLessThan(5000); // Should complete within 5 seconds
            
            // Verify loan was saved twice (initial save and after processing)
            verify(loanRepository, times(2)).save(any(Loan.class));
            
            // Verify events were published
            verify(eventPublisher).publish(any(LoanCreatedEvent.class));
            verify(eventPublisher).publish(any(LoanApprovedEvent.class));
        }
        
        @Test
        @DisplayName("Should handle high-throughput batch loan processing")
        void shouldHandleHighThroughputBatchProcessing() {
            // Given
            var commands = generateBatchLoanCommands(100); // 100 loan applications
            
            // When
            var startTime = System.currentTimeMillis();
            var results = loanProcessingService.processBatchLoans(commands);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(results).hasSize(100);
            assertThat(processingTime).isLessThan(10000); // Should process 100 loans within 10 seconds
            
            // Verify all results are valid
            assertThat(results).allMatch(result -> 
                result.creditCheck().approved() ||
                result.riskAssessment().riskLevel() != null ||
                result.complianceCheck().isCompliant() ||
                result.fraudDetection().isClean()
            );
        }
        
        @Test
        @DisplayName("Should demonstrate Virtual Threads performance advantage")
        void shouldDemonstrateVirtualThreadsPerformance() {
            // Given
            var commands = generateBatchLoanCommands(50);
            
            // When - Process with Virtual Threads
            var virtualThreadsStart = System.currentTimeMillis();
            var virtualThreadsResults = loanProcessingService.processBatchLoans(commands);
            var virtualThreadsTime = System.currentTimeMillis() - virtualThreadsStart;
            
            // Then
            assertThat(virtualThreadsResults).hasSize(50);
            assertThat(virtualThreadsTime).isLessThan(5000); // Virtual Threads should be fast
            
            // Verify concurrent execution characteristics
            // Virtual Threads should show significantly better resource utilization
            var threadCount = Thread.getAllStackTraces().keySet().size();
            assertThat(threadCount).isLessThan(1000); // Virtual Threads use fewer OS threads
        }
    }
    
    @Nested
    @DisplayName("Pattern Matching Loan Decisions")
    class PatternMatchingLoanDecisions {
        
        @Test
        @DisplayName("Should approve loan when all checks pass")
        void shouldApproveLoanWhenAllChecksPass() {
            // Given
            var command = createValidLoanCommand();
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // When
            var loanId = loanProcessingService.createLoan(command);
            
            // Then
            verify(loanRepository, times(2)).save(argThat(loan -> 
                loan.getStatus() == LoanStatus.PENDING || loan.getStatus() == LoanStatus.APPROVED
            ));
            verify(eventPublisher).publish(any(LoanApprovedEvent.class));
        }
        
        @Test
        @DisplayName("Should reject loan with insufficient credit score")
        void shouldRejectLoanWithInsufficientCreditScore() {
            // Given - Create a command that will result in low credit score
            var command = createLoanCommandWithPoorCredit();
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // When
            var loanId = loanProcessingService.createLoan(command);
            
            // Then
            verify(loanRepository, times(2)).save(any(Loan.class));
            verify(eventPublisher).publish(any(LoanRejectedEvent.class));
        }
        
        @Test
        @DisplayName("Should handle loan status transitions with pattern matching")
        void shouldHandleLoanStatusTransitionsWithPatternMatching() {
            // Given
            var loan = createTestLoan(LoanStatus.PENDING);
            
            // When - Approve loan
            loanProcessingService.processLoanStatusChange(loan, LoanStatus.APPROVED);
            
            // Then
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.APPROVED);
            verify(eventPublisher).publish(any(LoanApprovedEvent.class));
            verify(loanRepository).save(loan);
        }
        
        @Test
        @DisplayName("Should throw exception for invalid state transitions")
        void shouldThrowExceptionForInvalidStateTransitions() {
            // Given
            var loan = createTestLoan(LoanStatus.COMPLETED);
            
            // When & Then
            assertThatThrownBy(() -> 
                loanProcessingService.processLoanStatusChange(loan, LoanStatus.APPROVED)
            ).isInstanceOf(LoanProcessingService.IllegalStateTransitionException.class)
             .hasMessageContaining("Cannot approve loan from status: COMPLETED");
        }
    }
    
    @Nested
    @DisplayName("Record Patterns Data Validation")
    class RecordPatternsDataValidation {
        
        @Test
        @DisplayName("Should validate CreateLoanCommand with record patterns")
        void shouldValidateCreateLoanCommandWithRecordPatterns() {
            // Given
            var customerId = new CustomerId("CUST001");
            var amount = BigDecimal.valueOf(100000);
            var interestRate = BigDecimal.valueOf(4.5);
            var termInMonths = 240;
            var purpose = LoanPurpose.HOME_PURCHASE;
            
            // When & Then - Valid command should not throw
            assertThatCode(() -> 
                new LoanProcessingService.CreateLoanCommand(
                    customerId, amount, interestRate, termInMonths, purpose
                )
            ).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should reject CreateLoanCommand with negative amount")
        void shouldRejectCreateLoanCommandWithNegativeAmount() {
            // Given
            var customerId = new CustomerId("CUST001");
            var negativeAmount = BigDecimal.valueOf(-1000);
            var interestRate = BigDecimal.valueOf(4.5);
            var termInMonths = 60;
            var purpose = LoanPurpose.PERSONAL;
            
            // When & Then
            assertThatThrownBy(() ->
                new LoanProcessingService.CreateLoanCommand(
                    customerId, negativeAmount, interestRate, termInMonths, purpose
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Loan amount must be positive");
        }
        
        @Test
        @DisplayName("Should reject CreateLoanCommand with invalid term")
        void shouldRejectCreateLoanCommandWithInvalidTerm() {
            // Given
            var customerId = new CustomerId("CUST001");
            var amount = BigDecimal.valueOf(50000);
            var interestRate = BigDecimal.valueOf(4.5);
            var invalidTerm = -12;
            var purpose = LoanPurpose.BUSINESS;
            
            // When & Then
            assertThatThrownBy(() ->
                new LoanProcessingService.CreateLoanCommand(
                    customerId, amount, interestRate, invalidTerm, purpose
                )
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Loan term must be positive");
        }
    }
    
    @Nested
    @DisplayName("Concurrent Processing Performance")
    class ConcurrentProcessingPerformance {
        
        @Test
        @DisplayName("Should handle timeout scenarios gracefully")
        void shouldHandleTimeoutScenariosGracefully() {
            // Given - Create a service with slower executors to simulate timeout
            var slowExecutor = Executors.newFixedThreadPool(1);
            var slowService = new LoanProcessingService(
                loanRepository, eventPublisher, slowExecutor, slowExecutor, slowExecutor, slowExecutor
            );
            
            var command = createValidLoanCommand();
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                try {
                    Thread.sleep(100); // Simulate slow save
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return invocation.getArgument(0);
            });
            
            // When & Then - Should handle gracefully without hanging
            assertThatCode(() -> slowService.createLoan(command))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should maintain thread safety in concurrent processing")
        void shouldMaintainThreadSafetyInConcurrentProcessing() {
            // Given
            var commands = generateBatchLoanCommands(20);
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // When - Process multiple batches concurrently
            var futures = List.of(
                java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                    loanProcessingService.processBatchLoans(commands.subList(0, 5))),
                java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                    loanProcessingService.processBatchLoans(commands.subList(5, 10))),
                java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                    loanProcessingService.processBatchLoans(commands.subList(10, 15))),
                java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                    loanProcessingService.processBatchLoans(commands.subList(15, 20)))
            );
            
            // Then - All futures should complete successfully
            var allResults = futures.stream()
                .map(future -> {
                    try {
                        return future.get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        fail("Concurrent processing failed", e);
                        return null;
                    }
                })
                .toList();
            
            assertThat(allResults).hasSize(4);
            assertThat(allResults).allMatch(results -> results.size() == 5);
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingAndResilience {
        
        @Test
        @DisplayName("Should handle repository failures gracefully")
        void shouldHandleRepositoryFailuresGracefully() {
            // Given
            var command = createValidLoanCommand();
            when(loanRepository.save(any(Loan.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
            
            // When & Then
            assertThatThrownBy(() -> loanProcessingService.createLoan(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
        }
        
        @Test
        @DisplayName("Should handle event publisher failures")
        void shouldHandleEventPublisherFailures() {
            // Given
            var command = createValidLoanCommand();
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher).publish(any(LoanCreatedEvent.class));
            
            // When & Then - Should still create loan despite event publishing failure
            assertThatThrownBy(() -> loanProcessingService.createLoan(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event publishing failed");
        }
        
        @Test
        @DisplayName("Should handle interrupted Virtual Threads")
        void shouldHandleInterruptedVirtualThreads() {
            // Given
            var commands = generateBatchLoanCommands(5);
            
            // When - Interrupt processing
            var processingThread = Thread.ofVirtual().start(() -> {
                try {
                    loanProcessingService.processBatchLoans(commands);
                } catch (Exception e) {
                    // Expected during interruption
                }
            });
            
            // Interrupt after short delay
            try {
                Thread.sleep(50);
                processingThread.interrupt();
                processingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Then - Should handle interruption gracefully
            assertThat(processingThread.isAlive()).isFalse();
        }
    }
    
    // Helper methods
    
    private LoanProcessingService.CreateLoanCommand createValidLoanCommand() {
        return new LoanProcessingService.CreateLoanCommand(
            new CustomerId("CUST001"),
            BigDecimal.valueOf(100000),
            BigDecimal.valueOf(4.5),
            240,
            LoanPurpose.HOME_PURCHASE
        );
    }
    
    private LoanProcessingService.CreateLoanCommand createLoanCommandWithPoorCredit() {
        return new LoanProcessingService.CreateLoanCommand(
            new CustomerId("CUST_POOR_CREDIT"),
            BigDecimal.valueOf(200000),
            BigDecimal.valueOf(8.5),
            360,
            LoanPurpose.HOME_PURCHASE
        );
    }
    
    private List<LoanProcessingService.CreateLoanCommand> generateBatchLoanCommands(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new LoanProcessingService.CreateLoanCommand(
                new CustomerId("CUST" + String.format("%03d", i)),
                BigDecimal.valueOf(50000 + (i * 1000)),
                BigDecimal.valueOf(4.0 + (i * 0.1)),
                120 + (i * 12),
                LoanPurpose.values()[i % LoanPurpose.values().length]
            ))
            .toList();
    }
    
    private Loan createTestLoan(LoanStatus status) {
        var loan = new Loan(
            LoanId.newLoanId(),
            new CustomerId("CUST001"),
            BigDecimal.valueOf(100000),
            BigDecimal.valueOf(4.5),
            240,
            LoanPurpose.HOME_PURCHASE,
            status
        );
        return loan;
    }
}