package com.loanmanagement.performance;

import com.loanmanagement.banking.service.BankingFeaturesService;
import com.loanmanagement.loan.application.service.LoanProcessingService;
import com.loanmanagement.payment.application.service.PaymentProcessingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Java 21 Performance Benchmark Test Suite
 * 
 * Validates performance improvements from Java 21 features:
 * - Virtual Threads scalability vs Platform Threads
 * - Pattern Matching performance vs if-else chains
 * - Sequenced Collections performance vs traditional collections
 * - Record Patterns performance vs traditional object extraction
 * - Overall system throughput improvements
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test,java21-performance",
    "logging.level.com.loanmanagement=INFO"
})
@DisplayName("Java 21 Performance Benchmarks")
class Java21PerformanceBenchmarkTest {
    
    private BankingFeaturesService bankingFeaturesService;
    private LoanProcessingService loanProcessingService;
    private PaymentProcessingService paymentProcessingService;
    
    @BeforeEach
    void setUp() {
        bankingFeaturesService = new BankingFeaturesService();
        // Note: In real implementation, these would be injected with proper dependencies
        // For benchmark purposes, we'll create simplified instances
    }
    
    @Nested
    @DisplayName("Virtual Threads Performance")
    class VirtualThreadsPerformance {
        
        @Test
        @DisplayName("Should demonstrate Virtual Threads scalability for high-frequency trading")
        void shouldDemonstrateVirtualThreadsScalabilityForHighFrequencyTrading() {
            // Performance test scenarios
            var smallBatch = 1000;
            var mediumBatch = 5000;
            var largeBatch = 10000;
            
            // Test Virtual Threads performance
            var virtualThreadsResults = List.of(
                benchmarkMarketDataProcessing(smallBatch, "Virtual Threads - Small"),
                benchmarkMarketDataProcessing(mediumBatch, "Virtual Threads - Medium"),
                benchmarkMarketDataProcessing(largeBatch, "Virtual Threads - Large")
            );
            
            // Assertions for performance characteristics
            virtualThreadsResults.forEach(result -> {
                assertThat(result.successRate()).isGreaterThan(0.85); // > 85% success rate
                assertThat(result.throughputPerSecond()).isGreaterThan(500); // > 500 updates/second
                
                // Virtual Threads should handle large batches efficiently
                if (result.batchSize() >= 5000) {
                    assertThat(result.processingTimeMs()).isLessThan(15000); // < 15 seconds for 5K+ items
                }
            });
            
            // Memory usage should remain low with Virtual Threads
            var largeResult = virtualThreadsResults.get(2);
            assertThat(largeResult.memoryUsageMB()).isLessThan(500); // < 500MB for 10K virtual threads
            
            logPerformanceResults("Virtual Threads Market Data Processing", virtualThreadsResults);
        }
        
        @Test
        @DisplayName("Should compare Virtual Threads vs Platform Threads for payment processing")
        void shouldCompareVirtualThreadsVsPlatformThreadsForPaymentProcessing() {
            var paymentCount = 1000;
            var payments = generateMockPaymentRequests(paymentCount);
            
            // Benchmark Virtual Threads
            var virtualThreadsResult = benchmarkPaymentProcessing(payments, true);
            
            // Benchmark Platform Threads (simulated)
            var platformThreadsResult = benchmarkPaymentProcessing(payments, false);
            
            // Virtual Threads should show better resource utilization
            assertThat(virtualThreadsResult.throughputPerSecond())
                .isGreaterThanOrEqualTo(platformThreadsResult.throughputPerSecond());
                
            assertThat(virtualThreadsResult.memoryUsageMB())
                .isLessThan(platformThreadsResult.memoryUsageMB());
                
            // Virtual Threads should scale better under load
            assertThat(virtualThreadsResult.successRate())
                .isGreaterThanOrEqualTo(platformThreadsResult.successRate());
                
            logComparisonResults("Payment Processing: Virtual vs Platform Threads", 
                               virtualThreadsResult, platformThreadsResult);
        }
        
        @Test
        @DisplayName("Should validate Virtual Threads memory efficiency")
        void shouldValidateVirtualThreadsMemoryEfficiency() {
            var concurrentOperations = 5000;
            
            var beforeMemory = getCurrentMemoryUsage();
            
            // Create many Virtual Threads concurrently
            var futures = IntStream.range(0, concurrentOperations)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    // Simulate banking operation
                    try {
                        Thread.sleep(100); // 100ms operation
                        return "Operation " + i + " completed";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Operation " + i + " interrupted";
                    }
                }, Executors.newVirtualThreadPerTaskExecutor()))
                .toList();
            
            // Wait for completion
            var results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            var afterMemory = getCurrentMemoryUsage();
            var memoryIncrease = afterMemory - beforeMemory;
            
            // Assertions
            assertThat(results).hasSize(concurrentOperations);
            assertThat(memoryIncrease).isLessThan(100); // < 100MB increase for 5K virtual threads
            
            System.out.printf("Virtual Threads Memory Test: %d operations, %d MB memory increase%n", 
                            concurrentOperations, memoryIncrease);
        }
    }
    
    @Nested
    @DisplayName("Pattern Matching Performance")
    class PatternMatchingPerformance {
        
        @Test
        @DisplayName("Should demonstrate Pattern Matching performance for customer risk assessment")
        void shouldDemonstratePatternMatchingPerformanceForCustomerRiskAssessment() {
            var customerCount = 10000;
            var customers = generateMockCustomers(customerCount);
            
            // Benchmark pattern matching approach
            var patternMatchingResult = benchmarkCustomerRiskAssessment(customers, true);
            
            // Benchmark traditional if-else approach (simulated)
            var traditionalResult = benchmarkCustomerRiskAssessment(customers, false);
            
            // Pattern matching should be faster and more maintainable
            assertThat(patternMatchingResult.processingTimeMs())
                .isLessThanOrEqualTo(traditionalResult.processingTimeMs() * 1.1); // Within 10%
                
            assertThat(patternMatchingResult.successRate()).isEqualTo(1.0); // 100% success rate
            assertThat(patternMatchingResult.throughputPerSecond()).isGreaterThan(1000); // > 1K customers/second
            
            logComparisonResults("Customer Risk Assessment: Pattern Matching vs Traditional", 
                               patternMatchingResult, traditionalResult);
        }
        
        @Test
        @DisplayName("Should validate pattern matching performance for loan state transitions")
        void shouldValidatePatternMatchingPerformanceForLoanStateTransitions() {
            var transitionCount = 5000;
            var stateTransitions = generateMockLoanStateTransitions(transitionCount);
            
            var startTime = System.currentTimeMillis();
            
            var results = stateTransitions.stream()
                .map(this::processLoanStateTransition)
                .toList();
            
            var endTime = System.currentTimeMillis();
            var processingTime = endTime - startTime;
            
            // Performance assertions
            assertThat(results).hasSize(transitionCount);
            assertThat(processingTime).isLessThan(5000); // < 5 seconds for 5K transitions
            
            var throughput = (transitionCount * 1000.0) / processingTime;
            assertThat(throughput).isGreaterThan(1000); // > 1K transitions/second
            
            System.out.printf("Loan State Transitions: %d transitions in %d ms (%.1f/sec)%n", 
                            transitionCount, processingTime, throughput);
        }
    }
    
    @Nested
    @DisplayName("Sequenced Collections Performance")
    class SequencedCollectionsPerformance {
        
        @Test
        @DisplayName("Should validate Sequenced Collections performance for transaction processing")
        void shouldValidateSequencedCollectionsPerformanceForTransactionProcessing() {
            var transactionCount = 50000;
            var transactions = generateMockTransactions(transactionCount);
            
            var startTime = System.currentTimeMillis();
            
            // Use Sequenced Collections for ordered processing
            var analysis = bankingFeaturesService.analyzeTransactionSequence(transactions);
            
            var endTime = System.currentTimeMillis();
            var processingTime = endTime - startTime;
            
            // Performance assertions
            assertThat(analysis.transactionCount()).isEqualTo(transactionCount);
            assertThat(processingTime).isLessThan(10000); // < 10 seconds for 50K transactions
            
            var throughput = (transactionCount * 1000.0) / processingTime;
            assertThat(throughput).isGreaterThan(5000); // > 5K transactions/second
            
            // Verify first/last access is O(1)
            var sequenceAccessTime = measureSequenceAccess(transactions);
            assertThat(sequenceAccessTime).isLessThan(1); // < 1ms for first/last access
            
            System.out.printf("Transaction Analysis: %d transactions in %d ms (%.1f/sec)%n", 
                            transactionCount, processingTime, throughput);
        }
        
        @Test
        @DisplayName("Should compare Sequenced Collections vs traditional collections")
        void shouldCompareSequencedCollectionsVsTraditionalCollections() {
            var elementCount = 100000;
            var elements = generateMockTransactions(elementCount);
            
            // Benchmark Sequenced Collections
            var sequencedResult = benchmarkSequencedCollectionsOperations(elements);
            
            // Benchmark traditional collections
            var traditionalResult = benchmarkTraditionalCollectionsOperations(elements);
            
            // Sequenced Collections should provide better API while maintaining performance
            assertThat(sequencedResult.insertionTimeMs())
                .isLessThanOrEqualTo(traditionalResult.insertionTimeMs() * 1.2); // Within 20%
                
            assertThat(sequencedResult.firstLastAccessTimeMs()).isLessThan(5); // < 5ms
            assertThat(sequencedResult.iterationTimeMs())
                .isLessThanOrEqualTo(traditionalResult.iterationTimeMs() * 1.1); // Within 10%
                
            logSequencedCollectionsComparison(sequencedResult, traditionalResult);
        }
    }
    
    @Nested
    @DisplayName("Record Patterns Performance")
    class RecordPatternsPerformance {
        
        @Test
        @DisplayName("Should validate Record Patterns performance for payment validation")
        void shouldValidateRecordPatternsPerformanceForPaymentValidation() {
            var paymentCount = 25000;
            var payments = generateMockPaymentRequests(paymentCount);
            
            var startTime = System.currentTimeMillis();
            
            var validationResults = payments.stream()
                .map(payment -> bankingFeaturesService.validatePaymentRequest(
                    new BankingFeaturesService.PaymentRequest(
                        payment.paymentId(),
                        payment.amount(),
                        payment.fromAccount(),
                        payment.toAccount(),
                        payment.type()
                    )
                ))
                .toList();
            
            var endTime = System.currentTimeMillis();
            var processingTime = endTime - startTime;
            
            // Performance assertions
            assertThat(validationResults).hasSize(paymentCount);
            assertThat(processingTime).isLessThan(15000); // < 15 seconds for 25K validations
            
            var throughput = (paymentCount * 1000.0) / processingTime;
            assertThat(throughput).isGreaterThan(1500); // > 1.5K validations/second
            
            var successfulValidations = validationResults.stream()
                .mapToInt(result -> result.isValid() ? 1 : 0)
                .sum();
            var successRate = (double) successfulValidations / paymentCount;
            assertThat(successRate).isGreaterThan(0.8); // > 80% valid payments
            
            System.out.printf("Payment Validation: %d payments in %d ms (%.1f/sec, %.1f%% valid)%n", 
                            paymentCount, processingTime, throughput, successRate * 100);
        }
    }
    
    @Nested
    @DisplayName("Overall System Performance")
    class OverallSystemPerformance {
        
        @Test
        @DisplayName("Should validate end-to-end loan processing performance")
        void shouldValidateEndToEndLoanProcessingPerformance() {
            var loanApplications = 1000;
            var applications = generateMockLoanApplications(loanApplications);
            
            var startTime = System.currentTimeMillis();
            
            // Simulate end-to-end loan processing
            var processedLoans = applications.parallelStream()
                .map(this::processLoanApplication)
                .toList();
            
            var endTime = System.currentTimeMillis();
            var processingTime = endTime - startTime;
            
            // Performance assertions
            assertThat(processedLoans).hasSize(loanApplications);
            assertThat(processingTime).isLessThan(30000); // < 30 seconds for 1K loans
            
            var throughput = (loanApplications * 1000.0) / processingTime;
            assertThat(throughput).isGreaterThan(30); // > 30 loans/second
            
            var approvedLoans = processedLoans.stream()
                .mapToInt(result -> "APPROVED".equals(result.status()) ? 1 : 0)
                .sum();
            var approvalRate = (double) approvedLoans / loanApplications;
            
            System.out.printf("End-to-End Loan Processing: %d applications in %d ms (%.1f/sec, %.1f%% approved)%n", 
                            loanApplications, processingTime, throughput, approvalRate * 100);
        }
        
        @Test
        @DisplayName("Should validate system scalability under load")
        void shouldValidateSystemScalabilityUnderLoad() {
            var loadLevels = List.of(100, 500, 1000, 2000);
            var scalabilityResults = new ArrayList<ScalabilityResult>();
            
            for (var loadLevel : loadLevels) {
                var result = measureSystemPerformanceUnderLoad(loadLevel);
                scalabilityResults.add(result);
                
                // Each load level should maintain reasonable performance
                assertThat(result.throughputPerSecond()).isGreaterThan(loadLevel * 0.3); // > 30% of load
                assertThat(result.errorRate()).isLessThan(0.05); // < 5% error rate
            }
            
            // Verify near-linear scalability
            var baseResult = scalabilityResults.get(0);
            var highLoadResult = scalabilityResults.get(scalabilityResults.size() - 1);
            
            var scalabilityFactor = (double) highLoadResult.throughputPerSecond() / baseResult.throughputPerSecond();
            var loadFactor = (double) highLoadResult.loadLevel() / baseResult.loadLevel();
            
            assertThat(scalabilityFactor).isGreaterThan(loadFactor * 0.5); // > 50% of linear scaling
            
            logScalabilityResults(scalabilityResults);
        }
    }
    
    // Helper methods and data generators
    private PerformanceResult benchmarkMarketDataProcessing(int batchSize, String testName) {
        var updates = generateMarketDataUpdates(batchSize);
        
        var startTime = System.currentTimeMillis();
        var startMemory = getCurrentMemoryUsage();
        
        var result = bankingFeaturesService.processHighFrequencyTrading(updates);
        
        var endTime = System.currentTimeMillis();
        var endMemory = getCurrentMemoryUsage();
        
        var processingTime = endTime - startTime;
        var memoryUsage = endMemory - startMemory;
        var throughput = (batchSize * 1000.0) / processingTime;
        var successRate = (double) result.successfulUpdates() / result.totalUpdates();
        
        return new PerformanceResult(
            testName,
            batchSize,
            processingTime,
            throughput,
            successRate,
            memoryUsage
        );
    }
    
    private List<BankingFeaturesService.MarketDataUpdate> generateMarketDataUpdates(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new BankingFeaturesService.MarketDataUpdate(
                "SYMBOL" + (i % 100),
                BigDecimal.valueOf(100 + (i * 0.1)),
                1000L + i,
                LocalDateTime.now().minusSeconds(count - i)
            ))
            .toList();
    }
    
    private long getCurrentMemoryUsage() {
        var runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // MB
    }
    
    private void logPerformanceResults(String testName, List<PerformanceResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(testName);
        System.out.println("=".repeat(60));
        results.forEach(result -> {
            System.out.printf("%-25s | %6d items | %6d ms | %8.1f/sec | %6.1f%% | %4d MB%n",
                result.testName(), result.batchSize(), result.processingTimeMs(),
                result.throughputPerSecond(), result.successRate() * 100, result.memoryUsageMB());
        });
        System.out.println();
    }
    
    private void logComparisonResults(String testName, PerformanceResult result1, PerformanceResult result2) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(testName);
        System.out.println("=".repeat(60));
        System.out.printf("%-20s | %8.1f/sec | %6d ms | %4d MB%n",
            result1.testName(), result1.throughputPerSecond(), result1.processingTimeMs(), result1.memoryUsageMB());
        System.out.printf("%-20s | %8.1f/sec | %6d ms | %4d MB%n",
            result2.testName(), result2.throughputPerSecond(), result2.processingTimeMs(), result2.memoryUsageMB());
        System.out.println();
    }
    
    // Mock data generation and processing methods
    private List<MockPaymentRequest> generateMockPaymentRequests(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new MockPaymentRequest(
                "PAY" + String.format("%06d", i),
                BigDecimal.valueOf(1000 + (i % 5000)),
                "ACC" + String.format("%010d", i),
                "ACC" + String.format("%010d", i + 1000000),
                i % 2 == 0 ? BankingFeaturesService.PaymentType.DOMESTIC :
                            BankingFeaturesService.PaymentType.INTERNATIONAL
            ))
            .toList();
    }
    
    // Additional helper methods would be implemented here...
    // (Truncated for brevity - would include all necessary mock data generators,
    //  benchmark methods, and performance measurement utilities)
    
    // Performance result records
    record PerformanceResult(
        String testName,
        int batchSize,
        long processingTimeMs,
        double throughputPerSecond,
        double successRate,
        long memoryUsageMB
    ) {}
    
    record MockPaymentRequest(
        String paymentId,
        BigDecimal amount,
        String fromAccount,
        String toAccount,
        BankingFeaturesService.PaymentType type
    ) {}
    
    // Additional record types for benchmarking...
}