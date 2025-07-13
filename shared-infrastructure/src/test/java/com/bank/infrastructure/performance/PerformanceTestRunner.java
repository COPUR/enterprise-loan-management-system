package com.bank.infrastructure.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance Test Runner for Enterprise Banking Platform
 * 
 * Executes comprehensive performance and load tests
 * to validate system scalability and response times.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("performance-test")
@TestPropertySource(locations = "classpath:application-performance-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("performance")
public class PerformanceTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestRunner.class);

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    private PerformanceMetricsCollector metricsCollector;
    private LoadTestExecutor loadTestExecutor;
    private PerformanceReporter performanceReporter;

    // Performance Test Configuration
    private static final int WARM_UP_REQUESTS = 100;
    private static final int LOAD_TEST_DURATION_SECONDS = 30;
    private static final int MAX_CONCURRENT_USERS = 100;
    private static final int RAMP_UP_TIME_SECONDS = 10;
    private static final double ACCEPTABLE_ERROR_RATE = 0.01; // 1%
    private static final long ACCEPTABLE_P95_RESPONSE_TIME_MS = 1000;
    private static final long ACCEPTABLE_P99_RESPONSE_TIME_MS = 2000;

    @BeforeEach
    void setUp() {
        metricsCollector = new PerformanceMetricsCollector();
        loadTestExecutor = new LoadTestExecutor(webTestClient, metricsCollector);
        performanceReporter = new PerformanceReporter();
        
        logger.info("Starting performance test suite on port: {}", port);
        
        // Warm up the application
        warmUpApplication();
    }

    @AfterEach
    void tearDown() {
        // Generate performance report
        PerformanceTestResults results = metricsCollector.getResults();
        performanceReporter.generateReport(results);
        
        logger.info("Performance test suite completed");
    }

    /**
     * Load test for payment processing endpoints
     */
    @Test
    void testPaymentProcessingLoad() {
        logger.info("Starting payment processing load test");
        
        LoadTestScenario scenario = LoadTestScenario.builder()
            .name("Payment Processing Load Test")
            .endpoint("/api/v1/payments")
            .httpMethod("POST")
            .requestBody(createSamplePaymentRequest())
            .maxConcurrentUsers(MAX_CONCURRENT_USERS)
            .testDuration(Duration.ofSeconds(LOAD_TEST_DURATION_SECONDS))
            .rampUpTime(Duration.ofSeconds(RAMP_UP_TIME_SECONDS))
            .build();

        LoadTestResults results = loadTestExecutor.executeLoadTest(scenario);
        
        // Validate performance criteria
        assertPerformanceCriteria(results);
    }

    /**
     * Load test for customer management endpoints
     */
    @Test
    void testCustomerManagementLoad() {
        logger.info("Starting customer management load test");
        
        LoadTestScenario scenario = LoadTestScenario.builder()
            .name("Customer Management Load Test")
            .endpoint("/api/v1/customers")
            .httpMethod("GET")
            .maxConcurrentUsers(MAX_CONCURRENT_USERS / 2)
            .testDuration(Duration.ofSeconds(LOAD_TEST_DURATION_SECONDS))
            .rampUpTime(Duration.ofSeconds(RAMP_UP_TIME_SECONDS))
            .build();

        LoadTestResults results = loadTestExecutor.executeLoadTest(scenario);
        
        // Validate performance criteria
        assertPerformanceCriteria(results);
    }

    /**
     * Load test for loan processing endpoints
     */
    @Test
    void testLoanProcessingLoad() {
        logger.info("Starting loan processing load test");
        
        LoadTestScenario scenario = LoadTestScenario.builder()
            .name("Loan Processing Load Test")
            .endpoint("/api/v1/loans")
            .httpMethod("POST")
            .requestBody(createSampleLoanRequest())
            .maxConcurrentUsers(MAX_CONCURRENT_USERS / 3)
            .testDuration(Duration.ofSeconds(LOAD_TEST_DURATION_SECONDS))
            .rampUpTime(Duration.ofSeconds(RAMP_UP_TIME_SECONDS))
            .build();

        LoadTestResults results = loadTestExecutor.executeLoadTest(scenario);
        
        // Validate performance criteria
        assertPerformanceCriteria(results);
    }

    /**
     * Stress test to find breaking point
     */
    @Test
    void testSystemBreakingPoint() {
        logger.info("Starting system stress test to find breaking point");
        
        StressTestResults results = loadTestExecutor.executeStressTest(
            StressTestConfiguration.builder()
                .startConcurrentUsers(10)
                .maxConcurrentUsers(500)
                .incrementStep(10)
                .stepDuration(Duration.ofSeconds(10))
                .endpoint("/api/v1/health")
                .acceptableErrorRate(0.05) // 5% for stress test
                .build()
        );
        
        logger.info("System breaking point: {} concurrent users", results.getBreakingPoint());
        
        // Assert that breaking point is reasonable
        assertTrue(results.getBreakingPoint() >= 50, 
            "System should handle at least 50 concurrent users");
    }

    /**
     * Endurance test for extended load
     */
    @Test
    void testSystemEndurance() {
        logger.info("Starting system endurance test");
        
        LoadTestScenario scenario = LoadTestScenario.builder()
            .name("System Endurance Test")
            .endpoint("/api/v1/health")
            .httpMethod("GET")
            .maxConcurrentUsers(20) // Sustained load
            .testDuration(Duration.ofMinutes(5)) // Extended duration
            .rampUpTime(Duration.ofSeconds(30))
            .build();

        LoadTestResults results = loadTestExecutor.executeLoadTest(scenario);
        
        // Check for memory leaks or performance degradation
        assertEndurancePerformance(results);
    }

    /**
     * Database performance test
     */
    @Test
    void testDatabasePerformance() {
        logger.info("Starting database performance test");
        
        DatabasePerformanceTest dbTest = new DatabasePerformanceTest();
        DatabasePerformanceResults results = dbTest.runPerformanceTests();
        
        // Validate database performance
        assertTrue(results.getAverageQueryTime() < 100, 
            "Average query time should be under 100ms");
        assertTrue(results.getConnectionPoolUtilization() < 0.8, 
            "Connection pool utilization should be under 80%");
    }

    /**
     * Memory and CPU performance test
     */
    @Test
    void testResourceUtilization() {
        logger.info("Starting resource utilization test");
        
        ResourceMonitor resourceMonitor = new ResourceMonitor();
        resourceMonitor.startMonitoring();
        
        // Execute load test while monitoring resources
        LoadTestScenario scenario = LoadTestScenario.builder()
            .name("Resource Utilization Test")
            .endpoint("/api/v1/payments")
            .httpMethod("POST")
            .requestBody(createSamplePaymentRequest())
            .maxConcurrentUsers(50)
            .testDuration(Duration.ofSeconds(60))
            .rampUpTime(Duration.ofSeconds(10))
            .build();

        loadTestExecutor.executeLoadTest(scenario);
        
        ResourceUtilizationResults resourceResults = resourceMonitor.stopMonitoring();
        
        // Validate resource usage
        assertTrue(resourceResults.getMaxCpuUsage() < 80.0, 
            "CPU usage should stay under 80%");
        assertTrue(resourceResults.getMaxMemoryUsage() < 80.0, 
            "Memory usage should stay under 80%");
    }

    private void warmUpApplication() {
        logger.info("Warming up application with {} requests", WARM_UP_REQUESTS);
        
        IntStream.range(0, WARM_UP_REQUESTS)
            .parallel()
            .forEach(i -> {
                try {
                    webTestClient.get()
                        .uri("/actuator/health")
                        .exchange()
                        .expectStatus().isOk();
                } catch (Exception e) {
                    logger.warn("Warm-up request failed: {}", e.getMessage());
                }
            });
        
        logger.info("Application warm-up completed");
    }

    private void assertPerformanceCriteria(LoadTestResults results) {
        logger.info("Validating performance criteria for: {}", results.getScenarioName());
        
        // Error rate validation
        double errorRate = results.getErrorRate();
        assertTrue(errorRate <= ACCEPTABLE_ERROR_RATE, 
            String.format("Error rate %.2f%% exceeds threshold %.2f%%", 
                errorRate * 100, ACCEPTABLE_ERROR_RATE * 100));
        
        // Response time validation
        long p95ResponseTime = results.getP95ResponseTime();
        assertTrue(p95ResponseTime <= ACCEPTABLE_P95_RESPONSE_TIME_MS,
            String.format("P95 response time %dms exceeds threshold %dms", 
                p95ResponseTime, ACCEPTABLE_P95_RESPONSE_TIME_MS));
        
        long p99ResponseTime = results.getP99ResponseTime();
        assertTrue(p99ResponseTime <= ACCEPTABLE_P99_RESPONSE_TIME_MS,
            String.format("P99 response time %dms exceeds threshold %dms", 
                p99ResponseTime, ACCEPTABLE_P99_RESPONSE_TIME_MS));
        
        // Throughput validation
        double throughput = results.getThroughputRps();
        assertTrue(throughput > 10, 
            String.format("Throughput %.2f RPS is too low", throughput));
        
        logger.info("Performance criteria validation passed");
    }

    private void assertEndurancePerformance(LoadTestResults results) {
        logger.info("Validating endurance performance");
        
        // Check that performance doesn't degrade over time
        List<Long> responseTimes = results.getResponseTimesOverTime();
        if (responseTimes.size() >= 10) {
            double firstQuartileAvg = responseTimes.subList(0, responseTimes.size() / 4)
                .stream().mapToLong(Long::longValue).average().orElse(0.0);
            double lastQuartileAvg = responseTimes.subList(3 * responseTimes.size() / 4, responseTimes.size())
                .stream().mapToLong(Long::longValue).average().orElse(0.0);
            
            double degradationRatio = lastQuartileAvg / firstQuartileAvg;
            assertTrue(degradationRatio < 1.5, 
                String.format("Performance degraded by %.1fx during endurance test", degradationRatio));
        }
        
        logger.info("Endurance performance validation passed");
    }

    private String createSamplePaymentRequest() {
        return """
            {
                "customerId": "CUST123",
                "amount": 100.00,
                "currency": "USD",
                "description": "Performance test payment",
                "paymentMethod": {
                    "type": "CREDIT_CARD",
                    "cardNumber": "4111111111111111",
                    "expiryMonth": 12,
                    "expiryYear": 2025
                }
            }
            """;
    }

    private String createSampleLoanRequest() {
        return """
            {
                "customerId": "CUST123",
                "loanType": "PERSONAL",
                "requestedAmount": 10000.00,
                "currency": "USD",
                "termInMonths": 24,
                "purpose": "Debt consolidation"
            }
            """;
    }
}