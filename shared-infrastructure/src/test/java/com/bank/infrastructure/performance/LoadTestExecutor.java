package com.bank.infrastructure.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Load Test Executor for Enterprise Banking Platform
 * 
 * Executes various types of load tests and collects performance metrics.
 */
public class LoadTestExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LoadTestExecutor.class);

    private final WebTestClient webTestClient;
    private final PerformanceMetricsCollector metricsCollector;
    private final ExecutorService executorService;

    public LoadTestExecutor(WebTestClient webTestClient, PerformanceMetricsCollector metricsCollector) {
        this.webTestClient = webTestClient;
        this.metricsCollector = metricsCollector;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Execute a standard load test scenario
     */
    public LoadTestResults executeLoadTest(LoadTestScenario scenario) {
        logger.info("Executing load test: {}", scenario.getName());
        
        LoadTestResults results = new LoadTestResults(scenario.getName());
        AtomicInteger activeUsers = new AtomicInteger(0);
        AtomicLong requestCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(scenario.getTestDuration());
        
        // Create user simulation tasks
        List<CompletableFuture<Void>> userTasks = new ArrayList<>();
        
        // Ramp up users gradually
        int totalUsers = scenario.getMaxConcurrentUsers();
        Duration rampUpTime = scenario.getRampUpTime();
        long rampUpIntervalMs = rampUpTime.toMillis() / totalUsers;
        
        for (int i = 0; i < totalUsers; i++) {
            final int userId = i;
            CompletableFuture<Void> userTask = CompletableFuture.runAsync(() -> {
                try {
                    // Wait for ramp-up delay
                    Thread.sleep(userId * rampUpIntervalMs);
                    
                    activeUsers.incrementAndGet();
                    simulateUser(scenario, endTime, requestCount, errorCount, results);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("User simulation interrupted for user {}", userId);
                } finally {
                    activeUsers.decrementAndGet();
                }
            }, executorService);
            
            userTasks.add(userTask);
        }
        
        // Wait for all users to complete
        CompletableFuture.allOf(userTasks.toArray(new CompletableFuture[0])).join();
        
        // Calculate final results
        Duration totalDuration = Duration.between(startTime, Instant.now());
        results.setTotalRequests(requestCount.get());
        results.setTotalErrors(errorCount.get());
        results.setTestDuration(totalDuration);
        results.calculateMetrics();
        
        logger.info("Load test completed: {} requests, {} errors, {:.2f} RPS", 
            requestCount.get(), errorCount.get(), results.getThroughputRps());
        
        return results;
    }

    /**
     * Execute a stress test to find the breaking point
     */
    public StressTestResults executeStressTest(StressTestConfiguration config) {
        logger.info("Executing stress test");
        
        StressTestResults results = new StressTestResults();
        int currentUsers = config.getStartConcurrentUsers();
        boolean breakingPointFound = false;
        
        while (currentUsers <= config.getMaxConcurrentUsers() && !breakingPointFound) {
            logger.info("Testing with {} concurrent users", currentUsers);
            
            LoadTestScenario scenario = LoadTestScenario.builder()
                .name("Stress Test - " + currentUsers + " users")
                .endpoint(config.getEndpoint())
                .httpMethod("GET")
                .maxConcurrentUsers(currentUsers)
                .testDuration(config.getStepDuration())
                .rampUpTime(Duration.ofSeconds(5))
                .build();
            
            LoadTestResults stepResults = executeLoadTest(scenario);
            
            // Check if this step exceeded acceptable error rate
            if (stepResults.getErrorRate() > config.getAcceptableErrorRate()) {
                results.setBreakingPoint(currentUsers);
                breakingPointFound = true;
                logger.info("Breaking point found at {} concurrent users (error rate: {:.2f}%)", 
                    currentUsers, stepResults.getErrorRate() * 100);
            } else {
                results.addStepResult(currentUsers, stepResults);
                currentUsers += config.getIncrementStep();
            }
        }
        
        if (!breakingPointFound) {
            results.setBreakingPoint(config.getMaxConcurrentUsers());
            logger.info("Breaking point not found up to {} users", config.getMaxConcurrentUsers());
        }
        
        return results;
    }

    /**
     * Simulate a single user's behavior during the test
     */
    private void simulateUser(LoadTestScenario scenario, Instant endTime, 
                             AtomicLong requestCount, AtomicLong errorCount, 
                             LoadTestResults results) {
        
        Random random = new Random();
        
        while (Instant.now().isBefore(endTime)) {
            try {
                Instant requestStart = Instant.now();
                
                // Execute request based on scenario
                boolean success = executeRequest(scenario);
                
                Instant requestEnd = Instant.now();
                long responseTime = Duration.between(requestStart, requestEnd).toMillis();
                
                requestCount.incrementAndGet();
                results.addResponseTime(responseTime);
                
                if (!success) {
                    errorCount.incrementAndGet();
                }
                
                // Add realistic think time between requests
                int thinkTime = 500 + random.nextInt(1000); // 500-1500ms
                Thread.sleep(thinkTime);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.debug("Request failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Execute a single HTTP request based on the scenario
     */
    private boolean executeRequest(LoadTestScenario scenario) {
        try {
            WebTestClient.RequestHeadersSpec<?> request;
            
            switch (scenario.getHttpMethod().toUpperCase()) {
                case "GET":
                    request = webTestClient.get().uri(scenario.getEndpoint());
                    break;
                case "POST":
                    request = webTestClient.post()
                        .uri(scenario.getEndpoint())
                        .header("Content-Type", "application/json")
                        .bodyValue(scenario.getRequestBody() != null ? scenario.getRequestBody() : "{}");
                    break;
                case "PUT":
                    request = webTestClient.put()
                        .uri(scenario.getEndpoint())
                        .header("Content-Type", "application/json")
                        .bodyValue(scenario.getRequestBody() != null ? scenario.getRequestBody() : "{}");
                    break;
                case "DELETE":
                    request = webTestClient.delete().uri(scenario.getEndpoint());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + scenario.getHttpMethod());
            }
            
            // Execute request and check response
            request.exchange()
                .expectStatus().is2xxSuccessful();
            
            return true;
            
        } catch (Exception e) {
            logger.debug("Request failed for endpoint {}: {}", scenario.getEndpoint(), e.getMessage());
            return false;
        }
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}