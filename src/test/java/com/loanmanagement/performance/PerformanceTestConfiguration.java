package com.loanmanagement.performance;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration for Performance Testing
 * Provides specialized beans and configurations for performance testing scenarios
 */
@TestConfiguration
@Profile("performance")
public class PerformanceTestConfiguration {
    
    /**
     * Thread pool for concurrent load testing
     */
    @Bean
    public ExecutorService loadTestExecutorService() {
        return Executors.newFixedThreadPool(100); // Configurable based on test requirements
    }
    
    /**
     * Scheduled executor for performance monitoring
     */
    @Bean
    public ScheduledExecutorService performanceMonitoringExecutor() {
        return Executors.newScheduledThreadPool(5);
    }
    
    /**
     * Performance metrics collector
     */
    @Bean
    public PerformanceMetricsCollector performanceMetricsCollector() {
        return new PerformanceMetricsCollector();
    }
    
    /**
     * Load test data generator
     */
    @Bean
    public LoadTestDataGenerator loadTestDataGenerator() {
        return new LoadTestDataGenerator();
    }
}