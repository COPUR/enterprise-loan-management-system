package com.bank.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.time.Duration;

/**
 * Virtual Thread Configuration for Java 21+ Banking System
 * 
 * This configuration enables virtual threads for async processing in the banking system,
 * providing better scalability and resource utilization for I/O-bound operations.
 * 
 * Virtual threads are particularly beneficial for:
 * - Database operations
 * - External API calls  
 * - Event processing
 * - Audit logging
 * - Real-time notifications
 * 
 * @author Banking System Migration Team
 * @since Java 21
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(VirtualThreadConfig.VirtualThreadProperties.class)
public class VirtualThreadConfig {

    private final VirtualThreadProperties properties;

    public VirtualThreadConfig(VirtualThreadProperties properties) {
        this.properties = properties;
    }

    /**
     * Primary async executor using virtual threads for general async operations
     */
    @Bean(name = "bankingAsyncExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public TaskExecutor virtualThreadTaskExecutor() {
        return new VirtualThreadTaskExecutor("banking-async");
    }

    /**
     * Virtual thread executor for audit operations
     */
    @Bean(name = "auditExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public Executor auditVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Virtual thread executor for event processing
     */
    @Bean(name = "eventExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public Executor eventVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Virtual thread executor for SSE (Server-Sent Events) operations
     */
    @Bean(name = "sseExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public Executor sseVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Virtual thread executor for payment processing
     */
    @Bean(name = "paymentExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public Executor paymentVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Virtual thread executor for fraud detection
     */
    @Bean(name = "fraudExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public Executor fraudVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Scheduled executor service using virtual threads
     */
    @Bean(name = "scheduledVirtualThreadExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public ScheduledExecutorService scheduledVirtualThreadExecutor() {
        return Executors.newScheduledThreadPool(
            properties.getScheduledPoolSize(),
            Thread.ofVirtual()
                .name("scheduled-", 0)
                .factory()
        );
    }

    /**
     * Fallback traditional thread pool executor when virtual threads are disabled
     */
    @Bean(name = "bankingAsyncExecutor")
    @ConditionalOnProperty(name = "banking.threads.virtual.enabled", havingValue = "false")
    public TaskExecutor traditionalThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getFallbackCorePoolSize());
        executor.setMaxPoolSize(properties.getFallbackMaxPoolSize());
        executor.setQueueCapacity(properties.getFallbackQueueCapacity());
        executor.setThreadNamePrefix("banking-async-");
        executor.setKeepAliveSeconds(properties.getFallbackKeepAliveSeconds());
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Configuration properties for virtual thread settings
     */
    @ConfigurationProperties(prefix = "banking.threads.virtual")
    public static class VirtualThreadProperties {
        private boolean enabled = true;
        private int scheduledPoolSize = 10;
        private int fallbackCorePoolSize = 10;
        private int fallbackMaxPoolSize = 50;
        private int fallbackQueueCapacity = 100;
        private int fallbackKeepAliveSeconds = 60;
        private Duration shutdownTimeout = Duration.ofSeconds(30);

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getScheduledPoolSize() { return scheduledPoolSize; }
        public void setScheduledPoolSize(int scheduledPoolSize) { this.scheduledPoolSize = scheduledPoolSize; }

        public int getFallbackCorePoolSize() { return fallbackCorePoolSize; }
        public void setFallbackCorePoolSize(int fallbackCorePoolSize) { this.fallbackCorePoolSize = fallbackCorePoolSize; }

        public int getFallbackMaxPoolSize() { return fallbackMaxPoolSize; }
        public void setFallbackMaxPoolSize(int fallbackMaxPoolSize) { this.fallbackMaxPoolSize = fallbackMaxPoolSize; }

        public int getFallbackQueueCapacity() { return fallbackQueueCapacity; }
        public void setFallbackQueueCapacity(int fallbackQueueCapacity) { this.fallbackQueueCapacity = fallbackQueueCapacity; }

        public int getFallbackKeepAliveSeconds() { return fallbackKeepAliveSeconds; }
        public void setFallbackKeepAliveSeconds(int fallbackKeepAliveSeconds) { this.fallbackKeepAliveSeconds = fallbackKeepAliveSeconds; }

        public Duration getShutdownTimeout() { return shutdownTimeout; }
        public void setShutdownTimeout(Duration shutdownTimeout) { this.shutdownTimeout = shutdownTimeout; }
    }

    /**
     * Custom TaskExecutor implementation using virtual threads
     */
    public static class VirtualThreadTaskExecutor implements TaskExecutor {
        private final String threadNamePrefix;
        private final Executor virtualThreadExecutor;

        public VirtualThreadTaskExecutor(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        }

        @Override
        public void execute(Runnable task) {
            virtualThreadExecutor.execute(task);
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
    }
}