package com.bank.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Banking Health Indicators
 * Tests custom health indicators for banking-specific services
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Banking Health Indicators - TDD Test Suite")
class BankingHealthIndicatorsTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private Query query;
    
    private LoanProcessingHealthIndicator loanProcessingHealthIndicator;
    private PaymentSystemHealthIndicator paymentSystemHealthIndicator;
    private ComplianceServiceHealthIndicator complianceServiceHealthIndicator;
    private FraudDetectionHealthIndicator fraudDetectionHealthIndicator;
    private CustomerServiceHealthIndicator customerServiceHealthIndicator;
    
    @BeforeEach
    void setUp() {
        loanProcessingHealthIndicator = new LoanProcessingHealthIndicator(jdbcTemplate, entityManager);
        paymentSystemHealthIndicator = new PaymentSystemHealthIndicator(redisTemplate, jdbcTemplate);
        complianceServiceHealthIndicator = new ComplianceServiceHealthIndicator(jdbcTemplate);
        fraudDetectionHealthIndicator = new FraudDetectionHealthIndicator(redisTemplate, jdbcTemplate);
        customerServiceHealthIndicator = new CustomerServiceHealthIndicator(jdbcTemplate, entityManager);
    }
    
    @Test
    @DisplayName("Should report UP when loan processing is healthy")
    void shouldReportUpWhenLoanProcessingHealthy() {
        // Given - Healthy loan processing metrics
        when(jdbcTemplate.queryForObject(contains("COUNT"), eq(Integer.class)))
            .thenReturn(5); // 5 pending loans
        when(entityManager.createQuery(contains("SELECT COUNT")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(2L); // 2 processing loans
        
        // When
        Health health = loanProcessingHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("pendingLoans");
        assertThat(health.getDetails()).containsKey("processingLoans");
        assertThat(health.getDetails()).containsKey("averageProcessingTime");
        assertThat(health.getDetails().get("pendingLoans")).isEqualTo(5);
        assertThat(health.getDetails().get("processingLoans")).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should report DOWN when loan processing has high backlog")
    void shouldReportDownWhenLoanProcessingHighBacklog() {
        // Given - High backlog scenario
        when(jdbcTemplate.queryForObject(contains("COUNT"), eq(Integer.class)))
            .thenReturn(150); // 150 pending loans (threshold exceeded)
        when(entityManager.createQuery(contains("SELECT COUNT")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(25L); // 25 processing loans
        
        // When
        Health health = loanProcessingHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("pendingLoans");
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails().get("status")).isEqualTo("HIGH_BACKLOG");
        assertThat(health.getDetails().get("pendingLoans")).isEqualTo(150);
    }
    
    @Test
    @DisplayName("Should report UP when payment system is operational")
    void shouldReportUpWhenPaymentSystemOperational() {
        // Given - Operational payment system
        when(redisTemplate.hasKey("payment:heartbeat")).thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("payment"), eq(Integer.class)))
            .thenReturn(10); // 10 recent payments
        
        // When
        Health health = paymentSystemHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("redisConnectivity");
        assertThat(health.getDetails()).containsKey("recentPayments");
        assertThat(health.getDetails()).containsKey("lastHeartbeat");
        assertThat(health.getDetails().get("redisConnectivity")).isEqualTo("CONNECTED");
        assertThat(health.getDetails().get("recentPayments")).isEqualTo(10);
    }
    
    @Test
    @DisplayName("Should report DOWN when payment system Redis is unavailable")
    void shouldReportDownWhenPaymentSystemRedisUnavailable() {
        // Given - Redis connectivity issues
        when(redisTemplate.hasKey("payment:heartbeat"))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When
        Health health = paymentSystemHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("redisConnectivity");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("redisConnectivity")).isEqualTo("DISCONNECTED");
        assertThat(health.getDetails().get("error")).asString().contains("Redis connection failed");
    }
    
    @Test
    @DisplayName("Should report UP when compliance service is functioning")
    void shouldReportUpWhenComplianceServiceFunctioning() {
        // Given - Functioning compliance service
        when(jdbcTemplate.queryForObject(contains("compliance"), eq(Integer.class)))
            .thenReturn(5); // 5 compliance checks in last hour
        
        // When
        Health health = complianceServiceHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("complianceChecks");
        assertThat(health.getDetails()).containsKey("lastCheckTime");
        assertThat(health.getDetails()).containsKey("fapiCompliance");
        assertThat(health.getDetails().get("complianceChecks")).isEqualTo(5);
        assertThat(health.getDetails().get("fapiCompliance")).isEqualTo("ACTIVE");
    }
    
    @Test
    @DisplayName("Should report DOWN when compliance service has no recent activity")
    void shouldReportDownWhenComplianceServiceInactive() {
        // Given - No recent compliance activity
        when(jdbcTemplate.queryForObject(contains("compliance"), eq(Integer.class)))
            .thenReturn(0); // No compliance checks
        
        // When
        Health health = complianceServiceHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("complianceChecks");
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails().get("complianceChecks")).isEqualTo(0);
        assertThat(health.getDetails().get("status")).isEqualTo("INACTIVE");
    }
    
    @Test
    @DisplayName("Should report UP when fraud detection engine is active")
    void shouldReportUpWhenFraudDetectionActive() {
        // Given - Active fraud detection
        when(redisTemplate.hasKey("fraud:engine:heartbeat")).thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("fraud"), eq(Integer.class)))
            .thenReturn(25); // 25 fraud checks
        
        // When
        Health health = fraudDetectionHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("engineStatus");
        assertThat(health.getDetails()).containsKey("fraudChecks");
        assertThat(health.getDetails()).containsKey("mlModelStatus");
        assertThat(health.getDetails().get("engineStatus")).isEqualTo("ACTIVE");
        assertThat(health.getDetails().get("fraudChecks")).isEqualTo(25);
        assertThat(health.getDetails().get("mlModelStatus")).isEqualTo("OPERATIONAL");
    }
    
    @Test
    @DisplayName("Should report DOWN when fraud detection engine is inactive")
    void shouldReportDownWhenFraudDetectionInactive() {
        // Given - Inactive fraud detection
        when(redisTemplate.hasKey("fraud:engine:heartbeat")).thenReturn(false);
        when(jdbcTemplate.queryForObject(contains("fraud"), eq(Integer.class)))
            .thenReturn(0); // No fraud checks
        
        // When
        Health health = fraudDetectionHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("engineStatus");
        assertThat(health.getDetails()).containsKey("fraudChecks");
        assertThat(health.getDetails().get("engineStatus")).isEqualTo("INACTIVE");
        assertThat(health.getDetails().get("fraudChecks")).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should report UP when customer service is available")
    void shouldReportUpWhenCustomerServiceAvailable() {
        // Given - Available customer service
        when(jdbcTemplate.queryForObject(contains("customer"), eq(Integer.class)))
            .thenReturn(50); // 50 customer operations
        when(entityManager.createQuery(contains("SELECT AVG")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(2.5); // 2.5s average response time
        
        // When
        Health health = customerServiceHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("customerOperations");
        assertThat(health.getDetails()).containsKey("averageResponseTime");
        assertThat(health.getDetails()).containsKey("serviceStatus");
        assertThat(health.getDetails().get("customerOperations")).isEqualTo(50);
        assertThat(health.getDetails().get("averageResponseTime")).isEqualTo(2.5);
        assertThat(health.getDetails().get("serviceStatus")).isEqualTo("AVAILABLE");
    }
    
    @Test
    @DisplayName("Should report DOWN when customer service response time is high")
    void shouldReportDownWhenCustomerServiceSlowResponse() {
        // Given - Slow customer service
        when(jdbcTemplate.queryForObject(contains("customer"), eq(Integer.class)))
            .thenReturn(30); // 30 customer operations
        when(entityManager.createQuery(contains("SELECT AVG")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(15.0); // 15s average response time (too high)
        
        // When
        Health health = customerServiceHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("customerOperations");
        assertThat(health.getDetails()).containsKey("averageResponseTime");
        assertThat(health.getDetails()).containsKey("serviceStatus");
        assertThat(health.getDetails().get("customerOperations")).isEqualTo(30);
        assertThat(health.getDetails().get("averageResponseTime")).isEqualTo(15.0);
        assertThat(health.getDetails().get("serviceStatus")).isEqualTo("DEGRADED");
    }
    
    @Test
    @DisplayName("Should handle database connectivity issues gracefully")
    void shouldHandleDatabaseConnectivityIssuesGracefully() {
        // Given - Database connectivity issues
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When
        Health health = loanProcessingHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("databaseConnectivity");
        assertThat(health.getDetails().get("databaseConnectivity")).isEqualTo("FAILED");
        assertThat(health.getDetails().get("error")).asString().contains("Database connection failed");
    }
    
    @Test
    @DisplayName("Should include banking-specific metrics in health details")
    void shouldIncludeBankingSpecificMetricsInHealthDetails() {
        // Given - Various banking metrics
        when(jdbcTemplate.queryForObject(contains("COUNT"), eq(Integer.class)))
            .thenReturn(15); // loan count
        when(entityManager.createQuery(contains("SELECT COUNT")))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(3L); // processing count
        
        // When
        Health health = loanProcessingHealthIndicator.health();
        
        // Then
        assertThat(health.getDetails()).containsKey("pendingLoans");
        assertThat(health.getDetails()).containsKey("processingLoans");
        assertThat(health.getDetails()).containsKey("averageProcessingTime");
        assertThat(health.getDetails()).containsKey("systemLoad");
        assertThat(health.getDetails()).containsKey("timestamp");
        assertThat(health.getDetails()).containsKey("healthCheckVersion");
    }
    
    @Test
    @DisplayName("Should provide health check performance metrics")
    void shouldProvideHealthCheckPerformanceMetrics() {
        // Given - Mock fast health check
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
            .thenReturn(10);
        when(entityManager.createQuery(anyString()))
            .thenReturn(query);
        when(query.getSingleResult()).thenReturn(2L);
        
        // When
        long startTime = System.currentTimeMillis();
        Health health = loanProcessingHealthIndicator.health();
        long endTime = System.currentTimeMillis();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(endTime - startTime).isLessThan(1000); // Health check should be fast
        assertThat(health.getDetails()).containsKey("checkDurationMs");
    }
}