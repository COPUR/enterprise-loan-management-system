package com.bank.monitoring.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Banking Health Indicator for Customer Service
 * Monitors customer service availability, response times, and service quality metrics
 */
@Component("customerService")
public class CustomerServiceHealthIndicator implements HealthIndicator {

    private static final double RESPONSE_TIME_THRESHOLD = 10.0; // 10 seconds
    private static final int CUSTOMER_OPERATIONS_THRESHOLD = 10;
    private static final String HEALTH_CHECK_VERSION = "1.0.0";
    
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    
    public CustomerServiceHealthIndicator(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }
    
    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get customer operations count
            Integer customerOperations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_operations WHERE created_at > NOW() - INTERVAL '1 HOUR'", 
                Integer.class
            );
            
            // Calculate average response time
            Double averageResponseTime = calculateAverageResponseTime();
            
            // Check service availability
            String serviceAvailability = checkServiceAvailability();
            
            // Get active customer sessions
            Integer activeSessions = getActiveCustomerSessions();
            
            // Check customer satisfaction metrics
            Double customerSatisfaction = getCustomerSatisfactionScore();
            
            // Get service queue status
            String queueStatus = getServiceQueueStatus();
            
            long checkDuration = System.currentTimeMillis() - startTime;
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("customerOperations", customerOperations)
                .withDetail("averageResponseTime", averageResponseTime + "s")
                .withDetail("serviceAvailability", serviceAvailability)
                .withDetail("activeSessions", activeSessions)
                .withDetail("customerSatisfaction", customerSatisfaction + "/5.0")
                .withDetail("queueStatus", queueStatus)
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION);
            
            // Check response time threshold
            if (averageResponseTime > RESPONSE_TIME_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("serviceStatus", "DEGRADED")
                    .withDetail("reason", "High response time")
                    .withDetail("threshold", RESPONSE_TIME_THRESHOLD + "s")
                    .build();
            }
            
            // Check customer operations activity
            if (customerOperations < CUSTOMER_OPERATIONS_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("serviceStatus", "LOW_ACTIVITY")
                    .withDetail("threshold", CUSTOMER_OPERATIONS_THRESHOLD)
                    .build();
            }
            
            // Check service availability
            if (!"AVAILABLE".equals(serviceAvailability)) {
                return healthBuilder
                    .down()
                    .withDetail("serviceStatus", serviceAvailability)
                    .build();
            }
            
            return healthBuilder
                .withDetail("serviceStatus", "AVAILABLE")
                .build();
                
        } catch (Exception e) {
            long checkDuration = System.currentTimeMillis() - startTime;
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("serviceStatus", "ERROR")
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION)
                .build();
        }
    }
    
    private Double calculateAverageResponseTime() {
        try {
            Query avgQuery = entityManager.createQuery(
                "SELECT AVG(co.responseTimeMs) FROM CustomerOperation co " +
                "WHERE co.createdAt > :since AND co.responseTimeMs IS NOT NULL"
            );
            avgQuery.setParameter("since", LocalDateTime.now().minusHours(1));
            
            Double avgTime = (Double) avgQuery.getSingleResult();
            return avgTime != null ? Math.round(avgTime / 1000.0 * 100.0) / 100.0 : 2.5; // Convert to seconds
        } catch (Exception e) {
            return 2.5; // Default response time
        }
    }
    
    private String checkServiceAvailability() {
        try {
            // Check if customer service endpoints are responding
            Integer healthyEndpoints = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_endpoints WHERE service_type = 'CUSTOMER' " +
                "AND status = 'HEALTHY' AND last_check > NOW() - INTERVAL '5 MINUTES'", 
                Integer.class
            );
            
            Integer totalEndpoints = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_endpoints WHERE service_type = 'CUSTOMER'", 
                Integer.class
            );
            
            if (healthyEndpoints != null && totalEndpoints != null) {
                if (healthyEndpoints.equals(totalEndpoints) && totalEndpoints > 0) {
                    return "AVAILABLE";
                } else if (healthyEndpoints > 0) {
                    return "PARTIALLY_AVAILABLE";
                } else {
                    return "UNAVAILABLE";
                }
            } else {
                return "AVAILABLE"; // Default if no endpoint monitoring
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private Integer getActiveCustomerSessions() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_sessions WHERE status = 'ACTIVE' " +
                "AND last_activity > NOW() - INTERVAL '30 MINUTES'", 
                Integer.class
            );
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Double getCustomerSatisfactionScore() {
        try {
            Double avgScore = jdbcTemplate.queryForObject(
                "SELECT AVG(satisfaction_score) FROM customer_feedback " +
                "WHERE created_at > NOW() - INTERVAL '24 HOURS'", 
                Double.class
            );
            
            return avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 4.2;
        } catch (Exception e) {
            return 4.2; // Default satisfaction score
        }
    }
    
    private String getServiceQueueStatus() {
        try {
            Integer queueLength = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_service_queue WHERE status = 'WAITING'", 
                Integer.class
            );
            
            if (queueLength == null) queueLength = 0;
            
            if (queueLength > 50) {
                return "HIGH_QUEUE (" + queueLength + " waiting)";
            } else if (queueLength > 20) {
                return "MODERATE_QUEUE (" + queueLength + " waiting)";
            } else {
                return "NORMAL_QUEUE (" + queueLength + " waiting)";
            }
        } catch (Exception e) {
            return "QUEUE_UNKNOWN";
        }
    }
}