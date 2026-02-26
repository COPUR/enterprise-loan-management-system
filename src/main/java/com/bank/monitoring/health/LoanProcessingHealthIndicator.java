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
 * Banking Health Indicator for Loan Processing Service
 * Monitors loan processing health, backlog, and performance metrics
 */
@Component("loanProcessing")
public class LoanProcessingHealthIndicator implements HealthIndicator {

    private static final int PENDING_LOANS_THRESHOLD = 100;
    private static final int PROCESSING_LOANS_THRESHOLD = 20;
    private static final String HEALTH_CHECK_VERSION = "1.0.0";
    
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    
    public LoanProcessingHealthIndicator(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }
    
    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get pending loans count
            Integer pendingLoans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE status = 'PENDING'", 
                Integer.class
            );
            
            // Get currently processing loans
            Query processingQuery = entityManager.createQuery(
                "SELECT COUNT(l) FROM Loan l WHERE l.status = 'PROCESSING'"
            );
            Long processingLoans = (Long) processingQuery.getSingleResult();
            
            // Calculate average processing time (mock calculation)
            double averageProcessingTime = calculateAverageProcessingTime();
            
            // Determine system load
            String systemLoad = determineSystemLoad(pendingLoans, processingLoans.intValue());
            
            long checkDuration = System.currentTimeMillis() - startTime;
            
            // Build health details
            Health.Builder healthBuilder = Health.up()
                .withDetail("pendingLoans", pendingLoans)
                .withDetail("processingLoans", processingLoans)
                .withDetail("averageProcessingTime", averageProcessingTime + "s")
                .withDetail("systemLoad", systemLoad)
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION);
            
            // Check thresholds
            if (pendingLoans > PENDING_LOANS_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("status", "HIGH_BACKLOG")
                    .withDetail("threshold", PENDING_LOANS_THRESHOLD)
                    .build();
            }
            
            if (processingLoans > PROCESSING_LOANS_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("status", "HIGH_PROCESSING_LOAD")
                    .withDetail("threshold", PROCESSING_LOANS_THRESHOLD)
                    .build();
            }
            
            return healthBuilder
                .withDetail("status", "HEALTHY")
                .build();
                
        } catch (Exception e) {
            long checkDuration = System.currentTimeMillis() - startTime;
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("databaseConnectivity", "FAILED")
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION)
                .build();
        }
    }
    
    private double calculateAverageProcessingTime() {
        try {
            // Mock calculation - in real implementation, this would query actual processing times
            Query avgQuery = entityManager.createQuery(
                "SELECT AVG(EXTRACT(EPOCH FROM (l.updatedAt - l.createdAt))) " +
                "FROM Loan l WHERE l.status = 'APPROVED' AND l.updatedAt > :since"
            );
            avgQuery.setParameter("since", LocalDateTime.now().minusHours(24));
            
            Double avgTime = (Double) avgQuery.getSingleResult();
            return avgTime != null ? avgTime : 0.0;
        } catch (Exception e) {
            // Return default if calculation fails
            return 3.5; // 3.5 seconds default
        }
    }
    
    private String determineSystemLoad(int pendingLoans, int processingLoans) {
        int totalLoad = pendingLoans + processingLoans;
        
        if (totalLoad > 120) {
            return "HIGH";
        } else if (totalLoad > 60) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}