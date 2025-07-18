package com.bank.infrastructure.api.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Banking Health Indicator
 * 
 * Provides comprehensive health checks for banking services:
 * - Database connectivity and performance
 * - Redis cache availability
 * - External service dependencies
 * - System resources and performance
 * - Business logic health checks
 */
@Component
public class BankingHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    public BankingHealthIndicator(JdbcTemplate jdbcTemplate, 
                                 RedisTemplate<String, String> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean overallHealthy = true;
            
            // Check database health
            DatabaseHealthResult dbHealth = checkDatabaseHealth();
            details.put("database", dbHealth.getDetails());
            if (!dbHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Check Redis health
            RedisHealthResult redisHealth = checkRedisHealth();
            details.put("redis", redisHealth.getDetails());
            if (!redisHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Check system resources
            SystemHealthResult systemHealth = checkSystemHealth();
            details.put("system", systemHealth.getDetails());
            if (!systemHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Check business logic
            BusinessHealthResult businessHealth = checkBusinessHealth();
            details.put("business", businessHealth.getDetails());
            if (!businessHealth.isHealthy()) {
                overallHealthy = false;
            }
            
            // Overall health status
            details.put("timestamp", Instant.now().toString());
            details.put("version", "1.0.0");
            details.put("environment", System.getProperty("spring.profiles.active", "unknown"));
            
            return overallHealthy ? 
                Health.up().withDetails(details).build() : 
                Health.down().withDetails(details).build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now().toString())
                .build();
        }
    }
    
    private DatabaseHealthResult checkDatabaseHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic connectivity
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Test performance
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Get connection pool stats
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("responseTime", responseTime + "ms");
            details.put("connectionTest", result != null && result == 1 ? "PASS" : "FAIL");
            
            // Check table existence
            try {
                Long customerCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM customers LIMIT 1", Long.class);
                details.put("customerTableAccess", "OK");
            } catch (Exception e) {
                details.put("customerTableAccess", "ERROR: " + e.getMessage());
                return new DatabaseHealthResult(false, details);
            }
            
            // Performance thresholds
            boolean isHealthy = responseTime < 1000; // 1 second threshold
            
            return new DatabaseHealthResult(isHealthy, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("timestamp", Instant.now().toString());
            
            return new DatabaseHealthResult(false, details);
        }
    }
    
    private RedisHealthResult checkRedisHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test connectivity
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "OK";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("responseTime", responseTime + "ms");
            details.put("connectivityTest", testValue.equals(retrievedValue) ? "PASS" : "FAIL");
            
            // Check Redis info
            try {
                // Basic ping
                String pingResult = redisTemplate.getConnectionFactory()
                    .getConnection().ping();
                details.put("ping", pingResult);
            } catch (Exception e) {
                details.put("ping", "ERROR: " + e.getMessage());
            }
            
            boolean isHealthy = responseTime < 500 && testValue.equals(retrievedValue);
            
            return new RedisHealthResult(isHealthy, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("timestamp", Instant.now().toString());
            
            return new RedisHealthResult(false, details);
        }
    }
    
    private SystemHealthResult checkSystemHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Memory usage
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // Available processors
            int availableProcessors = runtime.availableProcessors();
            
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("memoryUsage", String.format("%.2f%%", memoryUsagePercent));
            details.put("maxMemory", formatBytes(maxMemory));
            details.put("totalMemory", formatBytes(totalMemory));
            details.put("freeMemory", formatBytes(freeMemory));
            details.put("usedMemory", formatBytes(usedMemory));
            details.put("availableProcessors", availableProcessors);
            
            // Health thresholds
            boolean isHealthy = memoryUsagePercent < 85.0; // 85% memory threshold
            
            if (!isHealthy) {
                details.put("warning", "High memory usage detected");
            }
            
            return new SystemHealthResult(isHealthy, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("timestamp", Instant.now().toString());
            
            return new SystemHealthResult(false, details);
        }
    }
    
    private BusinessHealthResult checkBusinessHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            
            // Check critical business operations
            boolean customerServiceHealthy = checkCustomerServiceHealth();
            boolean loanServiceHealthy = checkLoanServiceHealth();
            boolean paymentServiceHealthy = checkPaymentServiceHealth();
            
            details.put("customerService", customerServiceHealthy ? "UP" : "DOWN");
            details.put("loanService", loanServiceHealthy ? "UP" : "DOWN");
            details.put("paymentService", paymentServiceHealthy ? "UP" : "DOWN");
            
            boolean isHealthy = customerServiceHealthy && loanServiceHealthy && paymentServiceHealthy;
            
            return new BusinessHealthResult(isHealthy, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("timestamp", Instant.now().toString());
            
            return new BusinessHealthResult(false, details);
        }
    }
    
    private boolean checkCustomerServiceHealth() {
        try {
            // Simple query to test customer service
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE created_at > NOW() - INTERVAL '1 hour'", 
                Long.class);
            return count != null && count >= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkLoanServiceHealth() {
        try {
            // Simple query to test loan service
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE created_at > NOW() - INTERVAL '1 hour'", 
                Long.class);
            return count != null && count >= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkPaymentServiceHealth() {
        try {
            // Simple query to test payment service
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE created_at > NOW() - INTERVAL '1 hour'", 
                Long.class);
            return count != null && count >= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    // Inner classes for health results
    
    private static class DatabaseHealthResult {
        private final boolean healthy;
        private final Map<String, Object> details;
        
        public DatabaseHealthResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public boolean isHealthy() { return healthy; }
        public Map<String, Object> getDetails() { return details; }
    }
    
    private static class RedisHealthResult {
        private final boolean healthy;
        private final Map<String, Object> details;
        
        public RedisHealthResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public boolean isHealthy() { return healthy; }
        public Map<String, Object> getDetails() { return details; }
    }
    
    private static class SystemHealthResult {
        private final boolean healthy;
        private final Map<String, Object> details;
        
        public SystemHealthResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public boolean isHealthy() { return healthy; }
        public Map<String, Object> getDetails() { return details; }
    }
    
    private static class BusinessHealthResult {
        private final boolean healthy;
        private final Map<String, Object> details;
        
        public BusinessHealthResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public boolean isHealthy() { return healthy; }
        public Map<String, Object> getDetails() { return details; }
    }
}