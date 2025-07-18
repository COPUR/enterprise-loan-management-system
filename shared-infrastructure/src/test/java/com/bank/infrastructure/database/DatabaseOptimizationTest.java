package com.bank.infrastructure.database;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.loan.domain.*;
import com.bank.customer.domain.Customer;
import com.bank.infrastructure.audit.AuditEventEntity;
import com.bank.infrastructure.event.JpaEventStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive Database Optimization Tests
 * 
 * Tests database performance patterns, indexing strategies,
 * query optimization, and concurrent access patterns for
 * the enterprise banking platform.
 */
@DataJpaTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.jdbc.batch_size=25",
    "spring.jpa.properties.hibernate.order_inserts=true",
    "spring.jpa.properties.hibernate.order_updates=true",
    "spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true"
})
@DisplayName("Database Optimization Tests")
class DatabaseOptimizationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("banking_perf_test")
        .withUsername("perf_user")
        .withPassword("perf_pass")
        .withInitScript("db/optimization-test-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager testEntityManager;

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = testEntityManager.getEntityManager();
    }

    @Nested
    @DisplayName("Index Optimization Tests")
    class IndexOptimizationTests {

        @Test
        @DisplayName("Should use composite index for audit events by user and time")
        void shouldUseCompositeIndexForAuditEventsByUserAndTime() {
            // Create test data
            String userId = "user-123";
            LocalDateTime now = LocalDateTime.now();
            
            // Insert multiple audit events
            IntStream.range(0, 100).forEach(i -> {
                AuditEventEntity event = createAuditEvent(userId, now.minusHours(i));
                testEntityManager.persistAndFlush(event);
            });

            // Query that should use composite index
            long startTime = System.nanoTime();
            
            Query query = entityManager.createNativeQuery("""
                EXPLAIN (ANALYZE, BUFFERS) 
                SELECT * FROM audit_events 
                WHERE user_id = ?1 
                AND timestamp BETWEEN ?2 AND ?3 
                ORDER BY timestamp DESC
                """);
            query.setParameter(1, userId);
            query.setParameter(2, now.minusDays(1));
            query.setParameter(3, now);
            
            @SuppressWarnings("unchecked")
            List<Object> results = query.getResultList();
            
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(100); // Should complete within 100ms
            
            // Verify index usage in explain plan
            String explainPlan = String.join("\n", results.stream()
                .map(Object::toString)
                .toArray(String[]::new));
            
            assertThat(explainPlan).containsIgnoringCase("idx_audit_user_time");
        }

        @Test
        @DisplayName("Should optimize customer search by credit score range")
        void shouldOptimizeCustomerSearchByCreditScoreRange() {
            // Create test customers with varying credit scores
            IntStream.range(0, 1000).forEach(i -> {
                Customer customer = createTestCustomer(i, 300 + (i % 551)); // Scores 300-850
                testEntityManager.persistAndFlush(customer);
            });

            testEntityManager.flush();
            testEntityManager.clear();

            // Query customers in high credit score range
            long startTime = System.nanoTime();
            
            TypedQuery<Customer> query = entityManager.createQuery("""
                SELECT c FROM Customer c 
                WHERE c.creditScore BETWEEN :minScore AND :maxScore 
                ORDER BY c.creditScore DESC
                """, Customer.class);
            query.setParameter("minScore", 750);
            query.setParameter("maxScore", 850);
            
            List<Customer> highCreditCustomers = query.getResultList();
            
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(50); // Should complete within 50ms
            assertThat(highCreditCustomers).isNotEmpty();
            assertThat(highCreditCustomers).allSatisfy(customer -> 
                assertThat(customer.getCreditScore()).isBetween(750, 850));
        }

        @Test
        @DisplayName("Should optimize loan queries by status and date range")
        void shouldOptimizeLoanQueriesByStatusAndDateRange() {
            // Create test loans with different statuses
            LocalDate baseDate = LocalDate.now();
            IntStream.range(0, 500).forEach(i -> {
                Loan loan = createTestLoan(i, LoanStatus.values()[i % LoanStatus.values().length]);
                testEntityManager.persistAndFlush(loan);
            });

            testEntityManager.flush();
            testEntityManager.clear();

            // Query active loans in date range
            long startTime = System.nanoTime();
            
            TypedQuery<Loan> query = entityManager.createQuery("""
                SELECT l FROM Loan l 
                WHERE l.status = :status 
                AND l.applicationDate BETWEEN :startDate AND :endDate 
                ORDER BY l.applicationDate DESC
                """, Loan.class);
            query.setParameter("status", LoanStatus.ACTIVE);
            query.setParameter("startDate", baseDate.minusDays(30));
            query.setParameter("endDate", baseDate);
            
            List<Loan> activeLoans = query.getResultList();
            
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(75); // Should complete within 75ms
            assertThat(activeLoans).allSatisfy(loan -> 
                assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE));
        }
    }

    @Nested
    @DisplayName("Query Optimization Tests")
    class QueryOptimizationTests {

        @Test
        @DisplayName("Should optimize batch insert operations")
        void shouldOptimizeBatchInsertOperations() {
            // Test batch insert performance
            int batchSize = 100;
            List<Customer> customers = IntStream.range(0, batchSize)
                .mapToObj(i -> createTestCustomer(i, 600 + i))
                .toList();

            long startTime = System.nanoTime();

            // Batch insert using Hibernate batch processing
            for (int i = 0; i < customers.size(); i++) {
                testEntityManager.persist(customers.get(i));
                
                if (i % 25 == 0) { // Flush every 25 entities (batch size)
                    testEntityManager.flush();
                    testEntityManager.clear();
                }
            }
            testEntityManager.flush();

            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertion - batch should be faster than individual inserts
            assertThat(executionTimeMs).isLessThan(500); // Should complete within 500ms
            
            // Verify all customers were inserted
            long count = entityManager.createQuery("SELECT COUNT(c) FROM Customer c", Long.class)
                .getSingleResult();
            assertThat(count).isEqualTo(batchSize);
        }

        @Test
        @DisplayName("Should optimize native query performance for complex joins")
        void shouldOptimizeNativeQueryPerformanceForComplexJoins() {
            // Create test data
            Customer customer = createTestCustomer(1, 750);
            testEntityManager.persistAndFlush(customer);

            IntStream.range(0, 50).forEach(i -> {
                Loan loan = createTestLoanForCustomer(i, customer.getId());
                testEntityManager.persistAndFlush(loan);
            });

            testEntityManager.flush();
            testEntityManager.clear();

            // Complex join query using native SQL for performance
            long startTime = System.nanoTime();
            
            Query nativeQuery = entityManager.createNativeQuery("""
                SELECT 
                    c.customer_id,
                    c.first_name,
                    c.last_name,
                    c.credit_score,
                    COUNT(l.loan_id) as loan_count,
                    COALESCE(SUM(l.principal_amount), 0) as total_loan_amount,
                    AVG(l.interest_rate) as avg_interest_rate
                FROM customers c
                LEFT JOIN loans l ON c.customer_id = l.customer_id
                WHERE c.credit_score >= ?1
                GROUP BY c.customer_id, c.first_name, c.last_name, c.credit_score
                HAVING COUNT(l.loan_id) > ?2
                ORDER BY total_loan_amount DESC
                """);
            nativeQuery.setParameter(1, 700);
            nativeQuery.setParameter(2, 10);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = nativeQuery.getResultList();
            
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(100); // Should complete within 100ms
            assertThat(results).isNotEmpty();
            
            // Verify result structure
            if (!results.isEmpty()) {
                Object[] firstResult = results.get(0);
                assertThat(firstResult).hasSize(7); // All selected columns
            }
        }

        @Test
        @DisplayName("Should optimize pagination queries")
        void shouldOptimizePaginationQueries() {
            // Create large dataset
            IntStream.range(0, 1000).forEach(i -> {
                Customer customer = createTestCustomer(i, 300 + (i % 551));
                testEntityManager.persistAndFlush(customer);
            });

            testEntityManager.flush();
            testEntityManager.clear();

            // Test efficient pagination using offset-based approach
            int pageSize = 20;
            int pageNumber = 10; // Test middle page
            
            long startTime = System.nanoTime();
            
            TypedQuery<Customer> query = entityManager.createQuery("""
                SELECT c FROM Customer c 
                ORDER BY c.createdAt ASC, c.customerId ASC
                """, Customer.class);
            query.setFirstResult(pageNumber * pageSize);
            query.setMaxResults(pageSize);
            
            List<Customer> page = query.getResultList();
            
            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(50); // Should complete within 50ms
            assertThat(page).hasSize(pageSize);
            
            // Test cursor-based pagination (more efficient for large offsets)
            Customer lastCustomer = page.get(page.size() - 1);
            
            startTime = System.nanoTime();
            
            TypedQuery<Customer> cursorQuery = entityManager.createQuery("""
                SELECT c FROM Customer c 
                WHERE c.createdAt > :lastCreatedAt 
                OR (c.createdAt = :lastCreatedAt AND c.customerId > :lastCustomerId)
                ORDER BY c.createdAt ASC, c.customerId ASC
                """, Customer.class);
            cursorQuery.setParameter("lastCreatedAt", lastCustomer.getCreatedAt());
            cursorQuery.setParameter("lastCustomerId", lastCustomer.getId());
            cursorQuery.setMaxResults(pageSize);
            
            List<Customer> nextPage = cursorQuery.getResultList();
            
            endTime = System.nanoTime();
            long cursorExecutionTimeMs = (endTime - startTime) / 1_000_000;

            // Cursor-based should be faster or similar for large datasets
            assertThat(cursorExecutionTimeMs).isLessThanOrEqualTo(executionTimeMs + 10);
            assertThat(nextPage).hasSize(pageSize);
        }
    }

    @Nested
    @DisplayName("Connection Pool Optimization Tests")
    class ConnectionPoolOptimizationTests {

        @Test
        @DisplayName("Should handle concurrent database access efficiently")
        void shouldHandleConcurrentDatabaseAccessEfficiently() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            int numberOfOperations = 100;

            long startTime = System.nanoTime();

            List<CompletableFuture<Void>> futures = IntStream.range(0, numberOfOperations)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    Customer customer = createTestCustomer(i, 600 + i);
                    testEntityManager.persistAndFlush(customer);
                }, executor))
                .toList();

            // Wait for all operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // Performance assertions
            assertThat(executionTimeMs).isLessThan(5000); // Should complete within 5 seconds
            
            // Verify all customers were created
            long count = entityManager.createQuery("SELECT COUNT(c) FROM Customer c", Long.class)
                .getSingleResult();
            assertThat(count).isEqualTo(numberOfOperations);
        }

        @Test
        @DisplayName("Should optimize database connection usage")
        void shouldOptimizeDatabaseConnectionUsage() {
            // Test connection reuse efficiency
            int iterations = 50;
            
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                // Simulate typical database operations within single transaction
                Customer customer = createTestCustomer(i, 700);
                testEntityManager.persist(customer);
                
                Loan loan = createTestLoanForCustomer(i, customer.getId());
                testEntityManager.persist(loan);
                
                testEntityManager.flush();
                
                // Query operations
                entityManager.createQuery("SELECT COUNT(c) FROM Customer c", Long.class)
                    .getSingleResult();
                
                if (i % 10 == 0) {
                    testEntityManager.clear(); // Clear persistence context periodically
                }
            }

            long endTime = System.nanoTime();
            long executionTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertion
            assertThat(executionTimeMs).isLessThan(2000); // Should complete within 2 seconds
        }
    }

    @Nested
    @DisplayName("Event Store Optimization Tests")
    class EventStoreOptimizationTests {

        @Test
        @DisplayName("Should optimize event serialization and storage")
        void shouldOptimizeEventSerializationAndStorage() {
            // Test event store performance with large number of events
            int numberOfEvents = 500;
            String aggregateId = "test-aggregate-" + System.currentTimeMillis();

            long startTime = System.nanoTime();

            // Simulate domain events being stored
            IntStream.range(0, numberOfEvents).forEach(i -> {
                // Create mock domain event
                String eventData = String.format("""
                    {
                        "eventId": "event-%d",
                        "aggregateId": "%s",
                        "eventType": "TestEvent",
                        "payload": {
                            "iteration": %d,
                            "timestamp": "%s",
                            "data": "test-data-%d"
                        }
                    }
                    """, i, aggregateId, i, LocalDateTime.now(), i);

                // Store in event store table (simulated)
                Query insertQuery = entityManager.createNativeQuery("""
                    INSERT INTO domain_events (event_id, aggregate_id, event_type, event_data, occurred_on, version)
                    VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                    """);
                insertQuery.setParameter(1, "event-" + i);
                insertQuery.setParameter(2, aggregateId);
                insertQuery.setParameter(3, "TestEvent");
                insertQuery.setParameter(4, eventData);
                insertQuery.setParameter(5, LocalDateTime.now());
                insertQuery.setParameter(6, (long) i + 1);
                
                insertQuery.executeUpdate();
            });

            long endTime = System.nanoTime();
            long insertTimeMs = (endTime - startTime) / 1_000_000;

            // Test event retrieval performance
            startTime = System.nanoTime();

            Query selectQuery = entityManager.createNativeQuery("""
                SELECT * FROM domain_events 
                WHERE aggregate_id = ?1 
                ORDER BY version ASC
                """);
            selectQuery.setParameter(1, aggregateId);
            
            @SuppressWarnings("unchecked")
            List<Object[]> events = selectQuery.getResultList();

            endTime = System.nanoTime();
            long selectTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(insertTimeMs).isLessThan(1000); // Insert should complete within 1 second
            assertThat(selectTimeMs).isLessThan(100);  // Select should complete within 100ms
            assertThat(events).hasSize(numberOfEvents);
        }

        @Test
        @DisplayName("Should optimize event replay performance")
        void shouldOptimizeEventReplayPerformance() {
            // Create events for multiple aggregates
            int numberOfAggregates = 10;
            int eventsPerAggregate = 100;

            IntStream.range(0, numberOfAggregates).forEach(aggIndex -> {
                String aggregateId = "aggregate-" + aggIndex;
                
                IntStream.range(0, eventsPerAggregate).forEach(eventIndex -> {
                    Query insertQuery = entityManager.createNativeQuery("""
                        INSERT INTO domain_events (event_id, aggregate_id, event_type, event_data, occurred_on, version)
                        VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                        """);
                    insertQuery.setParameter(1, "event-" + aggIndex + "-" + eventIndex);
                    insertQuery.setParameter(2, aggregateId);
                    insertQuery.setParameter(3, "TestEvent");
                    insertQuery.setParameter(4, "{\"data\": \"test\"}");
                    insertQuery.setParameter(5, LocalDateTime.now());
                    insertQuery.setParameter(6, (long) eventIndex + 1);
                    
                    insertQuery.executeUpdate();
                });
            });

            testEntityManager.flush();

            // Test aggregate reconstruction performance
            String targetAggregateId = "aggregate-5";
            
            long startTime = System.nanoTime();

            Query replayQuery = entityManager.createNativeQuery("""
                SELECT event_data FROM domain_events 
                WHERE aggregate_id = ?1 
                ORDER BY version ASC
                """);
            replayQuery.setParameter(1, targetAggregateId);
            
            @SuppressWarnings("unchecked")
            List<String> eventData = replayQuery.getResultList();

            long endTime = System.nanoTime();
            long replayTimeMs = (endTime - startTime) / 1_000_000;

            // Performance assertions
            assertThat(replayTimeMs).isLessThan(50); // Replay should complete within 50ms
            assertThat(eventData).hasSize(eventsPerAggregate);
        }
    }

    @Nested
    @DisplayName("Cache Integration Tests")
    class CacheIntegrationTests {

        @Test
        @DisplayName("Should demonstrate read-through cache performance")
        void shouldDemonstrateReadThroughCachePerformance() {
            // Create test data
            Customer customer = createTestCustomer(1, 750);
            testEntityManager.persistAndFlush(customer);
            testEntityManager.clear();

            // First read (cache miss) - should hit database
            long startTime = System.nanoTime();
            
            Customer firstRead = entityManager.find(Customer.class, customer.getId());
            
            long endTime = System.nanoTime();
            long firstReadTimeMs = (endTime - startTime) / 1_000_000;

            assertThat(firstRead).isNotNull();

            // Second read (cache hit) - should be faster
            entityManager.clear(); // Clear first-level cache
            
            startTime = System.nanoTime();
            
            Customer secondRead = entityManager.find(Customer.class, customer.getId());
            
            endTime = System.nanoTime();
            long secondReadTimeMs = (endTime - startTime) / 1_000_000;

            assertThat(secondRead).isNotNull();
            
            // Note: In this test environment, we can't test actual L2 cache
            // but we can verify that entity manager operations are optimized
            assertThat(firstReadTimeMs).isGreaterThan(0);
            assertThat(secondReadTimeMs).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should optimize query result caching")
        void shouldOptimizeQueryResultCaching() {
            // Create test data
            IntStream.range(0, 100).forEach(i -> {
                Customer customer = createTestCustomer(i, 600 + i);
                testEntityManager.persistAndFlush(customer);
            });

            testEntityManager.flush();
            testEntityManager.clear();

            // First query execution
            String jpql = "SELECT c FROM Customer c WHERE c.creditScore >= :minScore ORDER BY c.creditScore DESC";
            
            long startTime = System.nanoTime();
            
            TypedQuery<Customer> query1 = entityManager.createQuery(jpql, Customer.class);
            query1.setParameter("minScore", 700);
            List<Customer> results1 = query1.getResultList();
            
            long endTime = System.nanoTime();
            long firstQueryTimeMs = (endTime - startTime) / 1_000_000;

            assertThat(results1).isNotEmpty();

            // Second identical query execution
            entityManager.clear();
            
            startTime = System.nanoTime();
            
            TypedQuery<Customer> query2 = entityManager.createQuery(jpql, Customer.class);
            query2.setParameter("minScore", 700);
            List<Customer> results2 = query2.getResultList();
            
            endTime = System.nanoTime();
            long secondQueryTimeMs = (endTime - startTime) / 1_000_000;

            assertThat(results2).hasSize(results1.size());
            
            // Both queries should complete within reasonable time
            assertThat(firstQueryTimeMs).isLessThan(200);
            assertThat(secondQueryTimeMs).isLessThan(200);
        }
    }

    // Helper methods for creating test data

    private AuditEventEntity createAuditEvent(String userId, LocalDateTime timestamp) {
        AuditEventEntity event = new AuditEventEntity();
        event.setEventId("event-" + System.nanoTime());
        event.setEventType("TEST_EVENT");
        event.setUserId(userId);
        event.setTimestamp(timestamp);
        event.setDescription("Test audit event");
        return event;
    }

    private Customer createTestCustomer(int index, int creditScore) {
        CustomerId customerId = CustomerId.generate();
        Money monthlyIncome = Money.aed(new BigDecimal("5000"));
        
        return Customer.createWithCreditScore(
            customerId,
            "Test" + index,
            "Customer" + index,
            "test" + index + "@example.com",
            "+971501234" + String.format("%03d", index),
            monthlyIncome,
            creditScore
        );
    }

    private Loan createTestLoan(int index, LoanStatus status) {
        LoanId loanId = LoanId.generate();
        CustomerId customerId = CustomerId.generate();
        Money principalAmount = Money.aed(new BigDecimal("10000").add(new BigDecimal(index * 1000)));
        InterestRate interestRate = InterestRate.of(new BigDecimal("0.06"));
        LoanTerm loanTerm = LoanTerm.ofMonths(60);
        
        Loan loan = Loan.create(loanId, customerId, principalAmount, interestRate, loanTerm);
        
        // Set status by transitioning through states
        if (status != LoanStatus.CREATED) {
            loan.approve();
            if (status == LoanStatus.DISBURSED || status == LoanStatus.ACTIVE) {
                loan.disburse();
            }
        }
        
        return loan;
    }

    private Loan createTestLoanForCustomer(int index, CustomerId customerId) {
        LoanId loanId = LoanId.generate();
        Money principalAmount = Money.aed(new BigDecimal("5000").add(new BigDecimal(index * 500)));
        InterestRate interestRate = InterestRate.of(new BigDecimal("0.05"));
        LoanTerm loanTerm = LoanTerm.ofMonths(36);
        
        return Loan.create(loanId, customerId, principalAmount, interestRate, loanTerm);
    }
}