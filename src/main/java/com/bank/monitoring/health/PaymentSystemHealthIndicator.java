package com.bank.monitoring.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Banking Health Indicator for Payment System
 * Monitors payment processing connectivity, Redis health, and transaction throughput
 */
@Component("paymentSystem")
public class PaymentSystemHealthIndicator implements HealthIndicator {

    private static final String PAYMENT_HEARTBEAT_KEY = "payment:heartbeat";
    private static final int RECENT_PAYMENTS_THRESHOLD = 5;
    private static final String HEALTH_CHECK_VERSION = "1.0.0";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    
    public PaymentSystemHealthIndicator(RedisTemplate<String, Object> redisTemplate, 
                                      JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check Redis connectivity
            String redisStatus = checkRedisConnectivity();
            
            // Get recent payments count
            Integer recentPayments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE created_at > NOW() - INTERVAL '1 HOUR'", 
                Integer.class
            );
            
            // Check payment processing queue
            String queueStatus = checkPaymentQueue();
            
            // Get last heartbeat
            String lastHeartbeat = getLastHeartbeat();
            
            // Calculate payment throughput
            double paymentThroughput = calculatePaymentThroughput();
            
            long checkDuration = System.currentTimeMillis() - startTime;
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("redisConnectivity", redisStatus)
                .withDetail("recentPayments", recentPayments)
                .withDetail("paymentQueue", queueStatus)
                .withDetail("lastHeartbeat", lastHeartbeat)
                .withDetail("paymentThroughput", paymentThroughput + " payments/min")
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION);
            
            // Check if Redis is disconnected
            if (!"CONNECTED".equals(redisStatus)) {
                return healthBuilder
                    .down()
                    .withDetail("status", "REDIS_DISCONNECTED")
                    .build();
            }
            
            // Check payment activity
            if (recentPayments < RECENT_PAYMENTS_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("status", "LOW_PAYMENT_ACTIVITY")
                    .withDetail("threshold", RECENT_PAYMENTS_THRESHOLD)
                    .build();
            }
            
            return healthBuilder
                .withDetail("status", "OPERATIONAL")
                .build();
                
        } catch (Exception e) {
            long checkDuration = System.currentTimeMillis() - startTime;
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("redisConnectivity", "DISCONNECTED")
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION)
                .build();
        }
    }
    
    private String checkRedisConnectivity() {
        try {
            // Update heartbeat
            redisTemplate.opsForValue().set(PAYMENT_HEARTBEAT_KEY, 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 
                5, TimeUnit.MINUTES);
            
            // Verify we can read it back
            Boolean exists = redisTemplate.hasKey(PAYMENT_HEARTBEAT_KEY);
            
            return exists ? "CONNECTED" : "DISCONNECTED";
        } catch (Exception e) {
            return "DISCONNECTED";
        }
    }
    
    private String checkPaymentQueue() {
        try {
            Long queueSize = redisTemplate.opsForList().size("payment:processing:queue");
            if (queueSize == null) queueSize = 0L;
            
            if (queueSize > 100) {
                return "HIGH_BACKLOG (" + queueSize + " items)";
            } else if (queueSize > 20) {
                return "MODERATE_LOAD (" + queueSize + " items)";
            } else {
                return "NORMAL (" + queueSize + " items)";
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getLastHeartbeat() {
        try {
            Object heartbeat = redisTemplate.opsForValue().get(PAYMENT_HEARTBEAT_KEY);
            return heartbeat != null ? heartbeat.toString() : "NONE";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    private double calculatePaymentThroughput() {
        try {
            Integer paymentsLastHour = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE created_at > NOW() - INTERVAL '1 HOUR'", 
                Integer.class
            );
            
            return paymentsLastHour != null ? paymentsLastHour / 60.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}