package com.bank.infrastructure.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Database Performance Test for Enterprise Banking Platform
 * 
 * Tests database performance under various load conditions
 * including connection pooling, query optimization, and concurrent access.
 */
@Component
public class DatabasePerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePerformanceTest.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final ExecutorService executorService = Executors.newFixedThreadPool(50);

    // Test configuration
    private static final int CONCURRENT_CONNECTIONS = 20;
    private static final int QUERIES_PER_CONNECTION = 50;
    private static final Duration TEST_DURATION = Duration.ofMinutes(2);

    /**
     * Run comprehensive database performance tests
     */
    public DatabasePerformanceResults runPerformanceTests() {
        logger.info("Starting database performance tests");
        
        DatabasePerformanceResults results = new DatabasePerformanceResults();
        
        try {
            // Test basic query performance
            results.setBasicQueryResults(testBasicQueryPerformance());
            
            // Test connection pool performance
            results.setConnectionPoolResults(testConnectionPoolPerformance());
            
            // Test concurrent access performance
            results.setConcurrentAccessResults(testConcurrentAccessPerformance());
            
            // Test complex query performance
            results.setComplexQueryResults(testComplexQueryPerformance());
            
            // Test transaction performance
            results.setTransactionResults(testTransactionPerformance());
            
            // Test database resource utilization
            results.setResourceUtilization(testDatabaseResourceUtilization());
            
            logger.info("Database performance tests completed");
            
        } catch (Exception e) {
            logger.error("Database performance tests failed", e);
            throw new RuntimeException("Database performance tests failed", e);
        }
        
        return results;
    }

    /**
     * Test basic CRUD operation performance
     */
    private QueryPerformanceResults testBasicQueryPerformance() {
        logger.info("Testing basic query performance");
        
        QueryPerformanceResults results = new QueryPerformanceResults("Basic Queries");
        List<Long> queryTimes = new ArrayList<>();
        
        // Test SELECT queries
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();
            try {
                jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM customer_context.customers", 
                    Integer.class);
                
                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();
                queryTimes.add(duration);
                
            } catch (Exception e) {
                logger.warn("Query failed: {}", e.getMessage());
                results.incrementErrorCount();
            }
        }
        
        results.setQueryTimes(queryTimes);
        results.calculateMetrics();
        
        logger.info("Basic query performance - Avg: {}ms, P95: {}ms", 
            results.getAverageQueryTime(), results.getP95QueryTime());
        
        return results;
    }

    /**
     * Test connection pool performance under load
     */
    private ConnectionPoolResults testConnectionPoolPerformance() {
        logger.info("Testing connection pool performance");
        
        ConnectionPoolResults results = new ConnectionPoolResults();
        AtomicLong totalConnections = new AtomicLong(0);
        AtomicLong connectionErrors = new AtomicLong(0);
        List<Long> connectionTimes = Collections.synchronizedList(new ArrayList<>());
        
        List<CompletableFuture<Void>> tasks = IntStream.range(0, CONCURRENT_CONNECTIONS)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                for (int j = 0; j < QUERIES_PER_CONNECTION; j++) {
                    Instant start = Instant.now();
                    try {
                        jdbcTemplate.queryForObject(
                            "SELECT 1", Integer.class);
                        
                        Instant end = Instant.now();
                        long duration = Duration.between(start, end).toMillis();
                        connectionTimes.add(duration);
                        totalConnections.incrementAndGet();
                        
                    } catch (Exception e) {
                        connectionErrors.incrementAndGet();
                        logger.debug("Connection error: {}", e.getMessage());
                    }
                    
                    try {
                        Thread.sleep(10); // Small delay between queries
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, executorService))
            .toList();
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        
        results.setTotalConnections(totalConnections.get());
        results.setConnectionErrors(connectionErrors.get());
        results.setConnectionTimes(connectionTimes);
        results.calculateConnectionPoolUtilization(dataSource);
        
        logger.info("Connection pool performance - Total: {}, Errors: {}, Utilization: {}%", 
            totalConnections.get(), connectionErrors.get(), results.getPoolUtilization());
        
        return results;
    }

    /**
     * Test concurrent database access performance
     */
    private ConcurrentAccessResults testConcurrentAccessPerformance() {
        logger.info("Testing concurrent database access performance");
        
        ConcurrentAccessResults results = new ConcurrentAccessResults();
        AtomicLong readOperations = new AtomicLong(0);
        AtomicLong writeOperations = new AtomicLong(0);
        AtomicLong concurrencyErrors = new AtomicLong(0);
        
        Instant testStart = Instant.now();
        Instant testEnd = testStart.plus(TEST_DURATION);
        
        List<CompletableFuture<Void>> readerTasks = IntStream.range(0, 10)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                while (Instant.now().isBefore(testEnd)) {
                    try {
                        jdbcTemplate.queryForList(
                            "SELECT customer_id, first_name, last_name FROM customer_context.customers LIMIT 10");
                        readOperations.incrementAndGet();
                        
                        Thread.sleep(50); // Simulate read processing time
                    } catch (Exception e) {
                        concurrencyErrors.incrementAndGet();
                        logger.debug("Read operation error: {}", e.getMessage());
                    }
                }
            }, executorService))
            .toList();
        
        List<CompletableFuture<Void>> writerTasks = IntStream.range(0, 5)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                while (Instant.now().isBefore(testEnd)) {
                    try {
                        // Simulate write operations (updates)
                        jdbcTemplate.update(
                            "UPDATE customer_context.customers SET last_login = CURRENT_TIMESTAMP WHERE customer_id = ?",
                            "CUST" + (1000 + (i * 100) + System.currentTimeMillis() % 100));
                        writeOperations.incrementAndGet();
                        
                        Thread.sleep(100); // Simulate write processing time
                    } catch (Exception e) {
                        concurrencyErrors.incrementAndGet();
                        logger.debug("Write operation error: {}", e.getMessage());
                    }
                }
            }, executorService))
            .toList();
        
        // Combine all tasks
        List<CompletableFuture<Void>> allTasks = new ArrayList<>();
        allTasks.addAll(readerTasks);
        allTasks.addAll(writerTasks);
        
        CompletableFuture.allOf(allTasks.toArray(new CompletableFuture[0])).join();
        
        Duration actualDuration = Duration.between(testStart, Instant.now());
        
        results.setReadOperations(readOperations.get());
        results.setWriteOperations(writeOperations.get());
        results.setConcurrencyErrors(concurrencyErrors.get());
        results.setTestDuration(actualDuration);
        results.calculateThroughput();
        
        logger.info("Concurrent access performance - Reads: {}, Writes: {}, Errors: {}, Throughput: {:.2f} ops/sec", 
            readOperations.get(), writeOperations.get(), concurrencyErrors.get(), results.getTotalThroughput());
        
        return results;
    }

    /**
     * Test complex query performance
     */
    private QueryPerformanceResults testComplexQueryPerformance() {
        logger.info("Testing complex query performance");
        
        QueryPerformanceResults results = new QueryPerformanceResults("Complex Queries");
        List<Long> queryTimes = new ArrayList<>();
        
        // Test complex join queries
        String complexQuery = """
            SELECT 
                c.customer_id,
                c.first_name,
                c.last_name,
                COUNT(l.loan_id) as loan_count,
                SUM(l.principal_amount) as total_loan_amount,
                COUNT(p.payment_id) as payment_count,
                SUM(p.amount) as total_payment_amount
            FROM customer_context.customers c
            LEFT JOIN loan_context.loans l ON c.customer_id = l.customer_id
            LEFT JOIN payment_context.payments p ON c.customer_id = p.customer_id
            WHERE c.created_at >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY c.customer_id, c.first_name, c.last_name
            HAVING COUNT(l.loan_id) > 0 OR COUNT(p.payment_id) > 0
            ORDER BY total_loan_amount DESC
            LIMIT 50
            """;
        
        for (int i = 0; i < 20; i++) {
            Instant start = Instant.now();
            try {
                jdbcTemplate.queryForList(complexQuery);
                
                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();
                queryTimes.add(duration);
                
            } catch (Exception e) {
                logger.warn("Complex query failed: {}", e.getMessage());
                results.incrementErrorCount();
            }
        }
        
        results.setQueryTimes(queryTimes);
        results.calculateMetrics();
        
        logger.info("Complex query performance - Avg: {}ms, P95: {}ms", 
            results.getAverageQueryTime(), results.getP95QueryTime());
        
        return results;
    }

    /**
     * Test transaction performance
     */
    private TransactionPerformanceResults testTransactionPerformance() {
        logger.info("Testing transaction performance");
        
        TransactionPerformanceResults results = new TransactionPerformanceResults();
        List<Long> transactionTimes = new ArrayList<>();
        AtomicLong successfulTransactions = new AtomicLong(0);
        AtomicLong failedTransactions = new AtomicLong(0);
        
        for (int i = 0; i < 50; i++) {
            Instant start = Instant.now();
            try {
                jdbcTemplate.execute("BEGIN");
                
                // Simulate a multi-step transaction
                jdbcTemplate.update(
                    "INSERT INTO payment_context.payments (payment_id, customer_id, amount, currency, status) VALUES (?, ?, ?, ?, ?)",
                    "PAY_PERF_" + i, "CUST_PERF_123", 100.0, "USD", "COMPLETED");
                
                jdbcTemplate.update(
                    "UPDATE customer_context.customers SET last_transaction_date = CURRENT_TIMESTAMP WHERE customer_id = ?",
                    "CUST_PERF_123");
                
                jdbcTemplate.execute("COMMIT");
                
                Instant end = Instant.now();
                long duration = Duration.between(start, end).toMillis();
                transactionTimes.add(duration);
                successfulTransactions.incrementAndGet();
                
            } catch (Exception e) {
                try {
                    jdbcTemplate.execute("ROLLBACK");
                } catch (Exception rollbackError) {
                    logger.warn("Rollback failed: {}", rollbackError.getMessage());
                }
                failedTransactions.incrementAndGet();
                logger.debug("Transaction failed: {}", e.getMessage());
            }
        }
        
        results.setTransactionTimes(transactionTimes);
        results.setSuccessfulTransactions(successfulTransactions.get());
        results.setFailedTransactions(failedTransactions.get());
        results.calculateMetrics();
        
        logger.info("Transaction performance - Successful: {}, Failed: {}, Avg time: {}ms", 
            successfulTransactions.get(), failedTransactions.get(), results.getAverageTransactionTime());
        
        return results;
    }

    /**
     * Test database resource utilization
     */
    private DatabaseResourceUtilization testDatabaseResourceUtilization() {
        logger.info("Testing database resource utilization");
        
        DatabaseResourceUtilization utilization = new DatabaseResourceUtilization();
        
        try {
            // Query database statistics
            List<Map<String, Object>> connectionStats = jdbcTemplate.queryForList(
                "SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active'");
            
            if (!connectionStats.isEmpty()) {
                Number activeConnections = (Number) connectionStats.get(0).get("active_connections");
                utilization.setActiveConnections(activeConnections.intValue());
            }
            
            // Query database size
            List<Map<String, Object>> sizeStats = jdbcTemplate.queryForList(
                "SELECT pg_size_pretty(pg_database_size(current_database())) as db_size");
            
            if (!sizeStats.isEmpty()) {
                String dbSize = (String) sizeStats.get(0).get("db_size");
                utilization.setDatabaseSize(dbSize);
            }
            
            // Query cache hit ratio
            List<Map<String, Object>> cacheStats = jdbcTemplate.queryForList(
                "SELECT sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) * 100 as cache_hit_ratio FROM pg_statio_user_tables");
            
            if (!cacheStats.isEmpty() && cacheStats.get(0).get("cache_hit_ratio") != null) {
                Number cacheHitRatio = (Number) cacheStats.get(0).get("cache_hit_ratio");
                utilization.setCacheHitRatio(cacheHitRatio.doubleValue());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to gather database resource utilization: {}", e.getMessage());
        }
        
        return utilization;
    }

    /**
     * Get average query time from recent performance tests
     */
    public double getAverageQueryTime() {
        // This would typically be calculated from stored metrics
        // For this implementation, return a simulated value
        return 45.0; // milliseconds
    }

    /**
     * Get connection pool utilization percentage
     */
    public double getConnectionPoolUtilization() {
        // This would typically query the actual connection pool
        // For this implementation, return a simulated value based on current load
        return 0.65; // 65% utilization
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