package com.loanmanagement.shared.infrastructure.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Gateway Performance Components
 * These tests verify load balancing, caching, compression, and performance optimization
 */
@DisplayName("Gateway Performance Components - TDD Tests")
class GatewayPerformanceTest {

    private GatewayPerformanceService performanceService;
    private ResponseCachingService cachingService;
    private CompressionService compressionService;
    private ConnectionPoolService connectionPoolService;
    private LoadTestService loadTestService;

    @BeforeEach
    void setUp() {
        // These services will be implemented following TDD approach
        performanceService = new GatewayPerformanceService();
        cachingService = new ResponseCachingService();
        compressionService = new CompressionService();
        connectionPoolService = new ConnectionPoolService();
        loadTestService = new LoadTestService();
    }

    @Test
    @DisplayName("Should implement response caching with TTL")
    void shouldImplementResponseCachingWithTTL() {
        // Given: Cacheable API response
        ApiRequest cacheableRequest = ApiRequest.builder()
            .path("/api/v1/reference/currencies")
            .method("GET")
            .headers(Map.of("Cache-Control", "max-age=3600"))
            .build();

        ApiResponse originalResponse = ApiResponse.builder()
            .status(200)
            .body("{\"currencies\": [\"USD\", \"EUR\", \"GBP\"]}")
            .headers(Map.of("Content-Type", "application/json"))
            .build();

        // When: Cache the response
        cachingService.cacheResponse(cacheableRequest, originalResponse, Duration.ofHours(1));

        // Then: Should retrieve from cache
        CacheResult cacheResult = cachingService.getCachedResponse(cacheableRequest);
        
        assertThat(cacheResult.isCacheHit()).isTrue();
        assertThat(cacheResult.getResponse().getBody()).isEqualTo(originalResponse.getBody());
        assertThat(cacheResult.getTtlRemaining()).isLessThanOrEqualTo(Duration.ofHours(1));
        assertThat(cacheResult.getCacheAge()).isGreaterThan(Duration.ZERO);

        // And: Should add cache headers
        assertThat(cacheResult.getResponse().getHeaders()).containsKey("X-Cache");
        assertThat(cacheResult.getResponse().getHeaders().get("X-Cache")).isEqualTo("HIT");
    }

    @Test
    @DisplayName("Should implement HTTP compression for large responses")
    void shouldImplementHTTPCompressionForLargeResponses() {
        // Given: Large response payload
        String largePayload = generateLargeJsonPayload(50000); // 50KB payload
        ApiResponse largeResponse = ApiResponse.builder()
            .status(200)
            .body(largePayload)
            .headers(Map.of("Content-Type", "application/json"))
            .build();

        ApiRequest compressionRequest = ApiRequest.builder()
            .path("/api/v1/reports/transactions")
            .method("GET")
            .headers(Map.of("Accept-Encoding", "gzip, deflate, br"))
            .build();

        // When: Apply compression
        CompressionResult result = compressionService.compressResponse(largeResponse, compressionRequest);

        // Then: Should compress large responses
        assertThat(result.isCompressed()).isTrue();
        assertThat(result.getCompressionType()).isIn(CompressionType.GZIP, CompressionType.BROTLI);
        assertThat(result.getCompressedSize()).isLessThan(result.getOriginalSize());
        assertThat(result.getCompressionRatio()).isBetween(0.1, 0.9); // 10-90% compression

        // And: Should add compression headers
        assertThat(result.getHeaders()).containsKey("Content-Encoding");
        assertThat(result.getHeaders()).containsKey("Content-Length");
        assertThat(result.getHeaders().get("Content-Length")).isEqualTo(String.valueOf(result.getCompressedSize()));
    }

    @Test
    @DisplayName("Should implement connection pooling for downstream services")
    void shouldImplementConnectionPoolingForDownstreamServices() {
        // Given: Connection pool configuration
        ConnectionPoolConfig config = ConnectionPoolConfig.builder()
            .serviceName("loan-service")
            .maxConnections(50)
            .maxPerRoute(10)
            .connectionTimeout(Duration.ofSeconds(5))
            .socketTimeout(Duration.ofSeconds(30))
            .timeToLive(Duration.ofMinutes(5))
            .build();

        connectionPoolService.configurePool("loan-service", config);

        // When: Get connection pool statistics
        ConnectionPoolStats stats = connectionPoolService.getPoolStats("loan-service");

        // Then: Should manage connection pool
        assertThat(stats.getMaxConnections()).isEqualTo(50);
        assertThat(stats.getActiveConnections()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getIdleConnections()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getPendingRequests()).isGreaterThanOrEqualTo(0);

        // When: Make multiple concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < 100; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    connectionPoolService.borrowConnection("loan-service");
                    Thread.sleep(100); // Simulate request processing
                    connectionPoolService.returnConnection("loan-service");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor));
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Then: Should handle concurrent requests efficiently
        ConnectionPoolStats finalStats = connectionPoolService.getPoolStats("loan-service");
        assertThat(finalStats.getActiveConnections()).isLessThanOrEqualTo(config.getMaxConnections());
        assertThat(finalStats.getConnectionLeaks()).isEqualTo(0);
        
        executor.shutdown();
    }

    @Test
    @DisplayName("Should implement request batching for efficiency")
    void shouldImplementRequestBatchingForEfficiency() {
        // Given: Multiple similar requests
        List<ApiRequest> batchableRequests = List.of(
            createCustomerRequest("CUST-001"),
            createCustomerRequest("CUST-002"),
            createCustomerRequest("CUST-003"),
            createCustomerRequest("CUST-004"),
            createCustomerRequest("CUST-005")
        );

        // When: Analyze batch potential
        BatchAnalysisResult batchAnalysis = performanceService.analyzeBatchPotential(batchableRequests);

        // Then: Should identify batchable requests
        assertThat(batchAnalysis.isBatchable()).isTrue();
        assertThat(batchAnalysis.getBatchSize()).isEqualTo(5);
        assertThat(batchAnalysis.getEstimatedSavings()).isGreaterThan(0.5); // >50% time savings

        // When: Execute batch request
        BatchExecutionResult batchResult = performanceService.executeBatch(batchableRequests);

        // Then: Should process more efficiently
        assertThat(batchResult.getTotalResponseTime()).isLessThan(
            Duration.ofMillis(batchableRequests.size() * 100) // Individual request time
        );
        assertThat(batchResult.getSuccessfulRequests()).isEqualTo(5);
        assertThat(batchResult.getFailedRequests()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should implement adaptive timeout management")
    void shouldImplementAdaptiveTimeoutManagement() {
        // Given: Service with varying response times
        String serviceName = "payment-service";
        
        // Simulate historical response times
        for (int i = 0; i < 100; i++) {
            long responseTime = 50 + (i % 10) * 20; // 50-240ms range
            performanceService.recordResponseTime(serviceName, Duration.ofMillis(responseTime));
        }

        // When: Calculate adaptive timeout
        AdaptiveTimeoutResult timeoutResult = performanceService.calculateAdaptiveTimeout(serviceName);

        // Then: Should set reasonable timeout based on history
        assertThat(timeoutResult.getRecommendedTimeout()).isBetween(
            Duration.ofMillis(100), // Minimum reasonable timeout
            Duration.ofSeconds(5)   // Maximum reasonable timeout
        );
        assertThat(timeoutResult.getConfidenceLevel()).isGreaterThan(0.8); // 80% confidence
        assertThat(timeoutResult.getBasedOnSamples()).isEqualTo(100);

        // And: Should handle circuit breaker integration
        assertThat(timeoutResult.getCircuitBreakerTimeout()).isGreaterThan(timeoutResult.getRecommendedTimeout());
    }

    @Test
    @DisplayName("Should implement load shedding during high traffic")
    void shouldImplementLoadSheddingDuringHighTraffic() {
        // Given: High traffic scenario
        performanceService.setTrafficLevel(TrafficLevel.HIGH);
        
        LoadSheddingConfig config = LoadSheddingConfig.builder()
            .cpuThreshold(80.0)
            .memoryThreshold(85.0)
            .requestQueueThreshold(1000)
            .responseTimeThreshold(Duration.ofSeconds(2))
            .build();

        performanceService.configureLoadShedding(config);

        // When: System is under load
        performanceService.simulateHighLoad(
            90.0, // 90% CPU usage
            88.0, // 88% memory usage
            1200,  // 1200 queued requests
            Duration.ofSeconds(3) // 3s response time
        );

        // Then: Should activate load shedding
        LoadSheddingStatus status = performanceService.getLoadSheddingStatus();
        
        assertThat(status.isActive()).isTrue();
        assertThat(status.getSheddingReason()).contains("CPU_THRESHOLD_EXCEEDED");
        assertThat(status.getSheddingPercentage()).isBetween(10.0, 50.0); // 10-50% requests shed

        // When: Make request during load shedding
        ApiRequest request = ApiRequest.builder()
            .path("/api/v1/loans")
            .method("GET")
            .priority(RequestPriority.LOW)
            .build();

        LoadSheddingDecision decision = performanceService.shouldShedRequest(request);

        // Then: Should shed low priority requests
        assertThat(decision.shouldShed()).isTrue();
        assertThat(decision.getReason()).isEqualTo("LOAD_SHEDDING_ACTIVE");
        assertThat(decision.getAlternativeAction()).isIn(
            AlternativeAction.RETRY_LATER, 
            AlternativeAction.CACHED_RESPONSE
        );
    }

    @Test
    @DisplayName("Should implement request prioritization")
    void shouldImplementRequestPrioritization() {
        // Given: Requests with different priorities
        ApiRequest criticalRequest = ApiRequest.builder()
            .path("/api/v1/payments/urgent")
            .method("POST")
            .priority(RequestPriority.CRITICAL)
            .build();

        ApiRequest normalRequest = ApiRequest.builder()
            .path("/api/v1/customers")
            .method("GET")
            .priority(RequestPriority.NORMAL)
            .build();

        ApiRequest lowRequest = ApiRequest.builder()
            .path("/api/v1/reports")
            .method("GET")
            .priority(RequestPriority.LOW)
            .build();

        // When: Queue requests during high load
        performanceService.queueRequest(criticalRequest);
        performanceService.queueRequest(lowRequest);
        performanceService.queueRequest(normalRequest);

        // Then: Should process in priority order
        List<ApiRequest> processingOrder = performanceService.getProcessingQueue();
        
        assertThat(processingOrder.get(0).getPriority()).isEqualTo(RequestPriority.CRITICAL);
        assertThat(processingOrder.get(1).getPriority()).isEqualTo(RequestPriority.NORMAL);
        assertThat(processingOrder.get(2).getPriority()).isEqualTo(RequestPriority.LOW);

        // When: Get queue statistics
        QueueStatistics queueStats = performanceService.getQueueStatistics();

        // Then: Should provide queue insights
        assertThat(queueStats.getTotalQueued()).isEqualTo(3);
        assertThat(queueStats.getCriticalQueued()).isEqualTo(1);
        assertThat(queueStats.getNormalQueued()).isEqualTo(1);
        assertThat(queueStats.getLowQueued()).isEqualTo(1);
        assertThat(queueStats.getAverageWaitTime()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("Should implement performance monitoring and alerting")
    void shouldImplementPerformanceMonitoringAndAlerting() {
        // Given: Performance monitoring configuration
        PerformanceThresholds thresholds = PerformanceThresholds.builder()
            .maxResponseTime(Duration.ofSeconds(2))
            .maxCpuUsage(80.0)
            .maxMemoryUsage(85.0)
            .minThroughput(1000) // requests per minute
            .maxErrorRate(0.05)  // 5% error rate
            .build();

        performanceService.configureMonitoring(thresholds);

        // When: Simulate performance degradation
        performanceService.recordMetrics(PerformanceMetrics.builder()
            .responseTime(Duration.ofSeconds(3))
            .cpuUsage(85.0)
            .memoryUsage(90.0)
            .throughput(800)
            .errorRate(0.08)
            .build());

        // Then: Should trigger alerts
        List<PerformanceAlert> alerts = performanceService.getActiveAlerts();
        
        assertThat(alerts).hasSize(5); // All thresholds exceeded
        assertThat(alerts.stream().map(PerformanceAlert::getType))
            .contains(
                AlertType.RESPONSE_TIME_HIGH,
                AlertType.CPU_USAGE_HIGH,
                AlertType.MEMORY_USAGE_HIGH,
                AlertType.THROUGHPUT_LOW,
                AlertType.ERROR_RATE_HIGH
            );

        // And: Should provide recommendations
        for (PerformanceAlert alert : alerts) {
            assertThat(alert.getRecommendation()).isNotBlank();
            assertThat(alert.getSeverity()).isIn(AlertSeverity.WARNING, AlertSeverity.CRITICAL);
        }
    }

    @Test
    @DisplayName("Should implement async request processing")
    void shouldImplementAsyncRequestProcessing() {
        // Given: Long-running request
        ApiRequest longRunningRequest = ApiRequest.builder()
            .path("/api/v1/reports/annual")
            .method("GET")
            .timeout(Duration.ofMinutes(5))
            .build();

        // When: Submit for async processing
        AsyncProcessingResult asyncResult = performanceService.submitAsync(longRunningRequest);

        // Then: Should return immediately with tracking info
        assertThat(asyncResult.getJobId()).isNotNull();
        assertThat(asyncResult.getEstimatedCompletion()).isAfter(
            java.time.LocalDateTime.now()
        );
        assertThat(asyncResult.getStatus()).isEqualTo(AsyncStatus.QUEUED);
        assertThat(asyncResult.getCallbackUrl()).isNotNull();

        // When: Check job status
        AsyncJobStatus status = performanceService.getAsyncStatus(asyncResult.getJobId());

        // Then: Should provide job progress
        assertThat(status.getJobId()).isEqualTo(asyncResult.getJobId());
        assertThat(status.getStatus()).isIn(AsyncStatus.QUEUED, AsyncStatus.PROCESSING, AsyncStatus.COMPLETED);
        assertThat(status.getProgress()).isBetween(0.0, 1.0);
        
        if (status.getStatus() == AsyncStatus.COMPLETED) {
            assertThat(status.getResult()).isNotNull();
            assertThat(status.getCompletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should provide comprehensive performance analytics")
    void shouldProvideComprehensivePerformanceAnalytics() {
        // Given: Historical performance data
        simulatePerformanceData();

        // When: Generate performance report
        PerformanceReport report = performanceService.generatePerformanceReport(
            Duration.ofDays(7) // Last 7 days
        );

        // Then: Should provide comprehensive metrics
        assertThat(report.getTimeRange()).isEqualTo(Duration.ofDays(7));
        assertThat(report.getTotalRequests()).isGreaterThan(0);
        assertThat(report.getAverageResponseTime()).isGreaterThan(Duration.ZERO);
        assertThat(report.getP95ResponseTime()).isGreaterThan(report.getAverageResponseTime());
        assertThat(report.getP99ResponseTime()).isGreaterThan(report.getP95ResponseTime());

        // And: Should provide service-level metrics
        Map<String, ServicePerformanceMetrics> serviceMetrics = report.getServiceMetrics();
        assertThat(serviceMetrics).isNotEmpty();
        
        for (ServicePerformanceMetrics metrics : serviceMetrics.values()) {
            assertThat(metrics.getRequestCount()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.getErrorRate()).isBetween(0.0, 1.0);
            assertThat(metrics.getAverageResponseTime()).isGreaterThanOrEqualTo(Duration.ZERO);
        }

        // And: Should identify performance trends
        assertThat(report.getTrends()).isNotEmpty();
        assertThat(report.getBottlenecks()).isNotNull();
        assertThat(report.getRecommendations()).isNotEmpty();
    }

    // Helper methods
    private ApiRequest createCustomerRequest(String customerId) {
        return ApiRequest.builder()
            .path("/api/v1/customers/" + customerId)
            .method("GET")
            .build();
    }

    private String generateLargeJsonPayload(int sizeBytes) {
        StringBuilder payload = new StringBuilder("{\"data\":[");
        int currentSize = payload.length();
        
        while (currentSize < sizeBytes - 10) {
            payload.append("{\"id\":").append(currentSize).append(",\"value\":\"test\"},");
            currentSize = payload.length();
        }
        
        payload.setLength(payload.length() - 1); // Remove last comma
        payload.append("]}");
        
        return payload.toString();
    }

    private void simulatePerformanceData() {
        // Simulate various performance scenarios for analytics
        for (int i = 0; i < 1000; i++) {
            performanceService.recordMetrics(PerformanceMetrics.builder()
                .responseTime(Duration.ofMillis(50 + (int)(Math.random() * 200)))
                .cpuUsage(30 + Math.random() * 40)
                .memoryUsage(40 + Math.random() * 30)
                .throughput(800 + (int)(Math.random() * 400))
                .errorRate(Math.random() * 0.02) // 0-2% error rate
                .build());
        }
    }

    // Enums for testing
    enum CompressionType {
        GZIP, DEFLATE, BROTLI
    }

    enum TrafficLevel {
        LOW, NORMAL, HIGH, CRITICAL
    }

    enum RequestPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    enum AlternativeAction {
        RETRY_LATER, CACHED_RESPONSE, DEGRADED_SERVICE
    }

    enum AsyncStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }

    enum AlertType {
        RESPONSE_TIME_HIGH, CPU_USAGE_HIGH, MEMORY_USAGE_HIGH, 
        THROUGHPUT_LOW, ERROR_RATE_HIGH, CIRCUIT_BREAKER_OPEN
    }

    enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
}