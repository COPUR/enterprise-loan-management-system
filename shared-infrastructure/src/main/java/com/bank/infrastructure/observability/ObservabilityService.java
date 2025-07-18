package com.bank.infrastructure.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Observability Service
 * 
 * Enterprise-grade observability for banking platform:
 * - Structured logging with correlation IDs
 * - Performance metrics and monitoring
 * - Distributed tracing
 * - Business metrics tracking
 * - Real-time health monitoring
 * - Custom dashboards and alerts
 * - Compliance and audit logging
 * - Error tracking and analysis
 */
@Service
public class ObservabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityService.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger businessLogger = LoggerFactory.getLogger("BUSINESS");
    private static final Logger errorLogger = LoggerFactory.getLogger("ERROR");
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Performance metrics
    private final Timer requestTimer;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Counter businessEventCounter;
    
    // Business metrics
    private final Counter loanApplicationCounter;
    private final Counter paymentProcessedCounter;
    private final Counter customerRegistrationCounter;
    private final Gauge activeSessionsGauge;
    
    // System metrics
    private final AtomicLong activeCustomers = new AtomicLong(0);
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    // Correlation tracking
    private final Map<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    // Redis keys for metrics
    private static final String METRICS_KEY_PREFIX = "metrics:";
    private static final String HEALTH_CHECK_KEY = "health:check";
    private static final String PERFORMANCE_KEY = "performance:";
    
    public ObservabilityService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize performance metrics
        this.requestTimer = Timer.builder("http.request.duration")
                .description("HTTP request duration")
                .register(meterRegistry);
        
        this.requestCounter = Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("http.errors.total")
                .description("Total HTTP errors")
                .register(meterRegistry);
        
        this.businessEventCounter = Counter.builder("business.events.total")
                .description("Total business events")
                .register(meterRegistry);
        
        // Initialize business metrics
        this.loanApplicationCounter = Counter.builder("loan.applications.total")
                .description("Total loan applications")
                .register(meterRegistry);
        
        this.paymentProcessedCounter = Counter.builder("payments.processed.total")
                .description("Total payments processed")
                .register(meterRegistry);
        
        this.customerRegistrationCounter = Counter.builder("customer.registrations.total")
                .description("Total customer registrations")
                .register(meterRegistry);
        
        this.activeSessionsGauge = Gauge.builder("sessions.active")
                .description("Active user sessions")
                .register(meterRegistry, activeCustomers, AtomicLong::get);
    }
    
    /**
     * Start request tracing
     */
    public TraceContext startTrace(String operationName, String customerId) {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        TraceContext context = new TraceContext(traceId, spanId, operationName, customerId, Instant.now());
        activeTraces.put(traceId, context);
        
        // Set MDC for structured logging
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("operation", operationName);
        MDC.put("customerId", customerId);
        
        // Start timer
        Timer.Sample sample = Timer.start(meterRegistry);
        context.setTimerSample(sample);
        
        logger.info("Starting trace: {} for operation: {} customer: {}", traceId, operationName, customerId);
        
        return context;
    }
    
    /**
     * End request tracing
     */
    public void endTrace(TraceContext context, boolean success) {
        try {
            Duration duration = Duration.between(context.getStartTime(), Instant.now());
            
            // Stop timer
            if (context.getTimerSample() != null) {
                context.getTimerSample().stop(requestTimer);
            }
            
            // Record metrics
            requestCounter.increment();
            if (!success) {
                errorCounter.increment();
                errorCount.incrementAndGet();
            }
            
            // Log performance
            performanceLogger.info("Trace completed: {} duration: {}ms success: {}", 
                context.getTraceId(), duration.toMillis(), success);
            
            // Store performance data
            storePerformanceMetrics(context, duration, success);
            
            // Send to Kafka for analysis
            publishTraceEvent(context, duration, success);
            
            // Clean up
            activeTraces.remove(context.getTraceId());
            MDC.clear();
            
        } catch (Exception e) {
            logger.error("Error ending trace", e);
        }
    }
    
    /**
     * Log business event
     */
    public void logBusinessEvent(BusinessEventType eventType, String customerId, Map<String, Object> data) {
        try {
            String eventId = generateEventId();
            
            BusinessEvent event = new BusinessEvent(
                eventId,
                eventType,
                customerId,
                data,
                Instant.now(),
                MDC.get("traceId")
            );
            
            // Record metrics
            businessEventCounter.increment();
            
            switch (eventType) {
                case LOAN_APPLICATION_SUBMITTED:
                    loanApplicationCounter.increment();
                    break;
                case PAYMENT_PROCESSED:
                    paymentProcessedCounter.increment();
                    totalTransactions.incrementAndGet();
                    break;
                case CUSTOMER_REGISTERED:
                    customerRegistrationCounter.increment();
                    break;
                case CUSTOMER_LOGIN:
                    activeCustomers.incrementAndGet();
                    break;
                case CUSTOMER_LOGOUT:
                    activeCustomers.decrementAndGet();
                    break;
            }
            
            // Log business event
            businessLogger.info("Business event: {} customer: {} data: {}", 
                eventType, customerId, objectMapper.writeValueAsString(data));
            
            // Store in Redis for real-time analytics
            storeBusinessEvent(event);
            
            // Send to Kafka for processing
            publishBusinessEvent(event);
            
        } catch (Exception e) {
            logger.error("Error logging business event", e);
        }
    }
    
    /**
     * Log error with context
     */
    public void logError(String operation, String customerId, Throwable error, Map<String, Object> context) {
        try {
            String errorId = generateErrorId();
            
            ErrorEvent errorEvent = new ErrorEvent(
                errorId,
                operation,
                customerId,
                error.getClass().getSimpleName(),
                error.getMessage(),
                getStackTrace(error),
                context,
                Instant.now(),
                MDC.get("traceId")
            );
            
            // Record metrics
            errorCounter.increment();
            errorCount.incrementAndGet();
            
            // Log error
            errorLogger.error("Error in operation: {} customer: {} error: {} context: {}", 
                operation, customerId, error.getMessage(), objectMapper.writeValueAsString(context), error);
            
            // Store error for analysis
            storeErrorEvent(errorEvent);
            
            // Send alert if critical
            if (isCriticalError(error)) {
                sendCriticalErrorAlert(errorEvent);
            }
            
        } catch (Exception e) {
            logger.error("Error logging error event", e);
        }
    }
    
    /**
     * Log audit event
     */
    public void logAuditEvent(AuditEventType eventType, String customerId, String resource, 
                             String action, Map<String, Object> details) {
        try {
            String auditId = generateAuditId();
            
            AuditEvent auditEvent = new AuditEvent(
                auditId,
                eventType,
                customerId,
                resource,
                action,
                details,
                Instant.now(),
                MDC.get("traceId")
            );
            
            // Log audit event
            auditLogger.info("Audit event: {} customer: {} resource: {} action: {} details: {}", 
                eventType, customerId, resource, action, objectMapper.writeValueAsString(details));
            
            // Store for compliance
            storeAuditEvent(auditEvent);
            
        } catch (Exception e) {
            logger.error("Error logging audit event", e);
        }
    }
    
    /**
     * Get system health metrics
     */
    public SystemHealthMetrics getSystemHealthMetrics() {
        try {
            return new SystemHealthMetrics(
                activeCustomers.get(),
                totalTransactions.get(),
                errorCount.get(),
                getAverageResponseTime(),
                getErrorRate(),
                getSystemUptime(),
                getDatabaseHealth(),
                getRedisHealth(),
                getKafkaHealth()
            );
            
        } catch (Exception e) {
            logger.error("Error getting system health metrics", e);
            return SystemHealthMetrics.unhealthy();
        }
    }
    
    /**
     * Get business metrics dashboard
     */
    public BusinessMetricsDashboard getBusinessMetricsDashboard() {
        try {
            return new BusinessMetricsDashboard(
                loanApplicationCounter.count(),
                paymentProcessedCounter.count(),
                customerRegistrationCounter.count(),
                activeCustomers.get(),
                getTotalRevenue(),
                getCustomerSatisfactionScore(),
                getConversionRate()
            );
            
        } catch (Exception e) {
            logger.error("Error getting business metrics dashboard", e);
            return BusinessMetricsDashboard.empty();
        }
    }
    
    // Private methods
    
    private void storePerformanceMetrics(TraceContext context, Duration duration, boolean success) {
        try {
            String key = PERFORMANCE_KEY + context.getOperation() + ":" + Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            
            PerformanceMetric metric = new PerformanceMetric(
                context.getOperation(),
                duration.toMillis(),
                success,
                context.getCustomerId(),
                Instant.now()
            );
            
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(metric));
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            
        } catch (Exception e) {
            logger.warn("Failed to store performance metrics", e);
        }
    }
    
    private void storeBusinessEvent(BusinessEvent event) {
        try {
            String key = METRICS_KEY_PREFIX + "business:" + event.getEventType();
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Failed to store business event", e);
        }
    }
    
    private void storeErrorEvent(ErrorEvent event) {
        try {
            String key = METRICS_KEY_PREFIX + "errors:" + event.getOperation();
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.warn("Failed to store error event", e);
        }
    }
    
    private void storeAuditEvent(AuditEvent event) {
        try {
            String key = METRICS_KEY_PREFIX + "audit:" + event.getEventType();
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
            redisTemplate.expire(key, 365, TimeUnit.DAYS); // Long retention for compliance
            
        } catch (Exception e) {
            logger.warn("Failed to store audit event", e);
        }
    }
    
    private void publishTraceEvent(TraceContext context, Duration duration, boolean success) {
        try {
            Map<String, Object> traceEvent = Map.of(
                "eventType", "TraceCompleted",
                "traceId", context.getTraceId(),
                "operation", context.getOperation(),
                "customerId", context.getCustomerId(),
                "duration", duration.toMillis(),
                "success", success,
                "timestamp", Instant.now().toString()
            );
            
            kafkaTemplate.send("observability-traces", context.getTraceId(), objectMapper.writeValueAsString(traceEvent));
            
        } catch (Exception e) {
            logger.warn("Failed to publish trace event", e);
        }
    }
    
    private void publishBusinessEvent(BusinessEvent event) {
        try {
            kafkaTemplate.send("business-events", event.getCustomerId(), objectMapper.writeValueAsString(event));
            
        } catch (Exception e) {
            logger.warn("Failed to publish business event", e);
        }
    }
    
    private void sendCriticalErrorAlert(ErrorEvent errorEvent) {
        try {
            Map<String, Object> alert = Map.of(
                "alertType", "CRITICAL_ERROR",
                "errorId", errorEvent.getErrorId(),
                "operation", errorEvent.getOperation(),
                "customerId", errorEvent.getCustomerId(),
                "errorType", errorEvent.getErrorType(),
                "message", errorEvent.getMessage(),
                "timestamp", errorEvent.getTimestamp()
            );
            
            kafkaTemplate.send("critical-alerts", errorEvent.getErrorId(), objectMapper.writeValueAsString(alert));
            
        } catch (Exception e) {
            logger.warn("Failed to send critical error alert", e);
        }
    }
    
    private boolean isCriticalError(Throwable error) {
        // Define critical error types
        return error instanceof java.sql.SQLException ||
               error instanceof java.lang.OutOfMemoryError ||
               error instanceof java.lang.SecurityException ||
               error.getMessage().contains("database") ||
               error.getMessage().contains("security");
    }
    
    private double getAverageResponseTime() {
        return requestTimer.mean(TimeUnit.MILLISECONDS);
    }
    
    private double getErrorRate() {
        double totalRequests = requestCounter.count();
        double totalErrors = errorCounter.count();
        return totalRequests > 0 ? (totalErrors / totalRequests) * 100 : 0;
    }
    
    private long getSystemUptime() {
        // Mock implementation - in production, track actual uptime
        return System.currentTimeMillis();
    }
    
    private boolean getDatabaseHealth() {
        // Mock implementation - in production, check database connectivity
        return true;
    }
    
    private boolean getRedisHealth() {
        try {
            redisTemplate.opsForValue().set(HEALTH_CHECK_KEY, "ok", 1, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean getKafkaHealth() {
        // Mock implementation - in production, check Kafka connectivity
        return true;
    }
    
    private double getTotalRevenue() {
        // Mock implementation - calculate from business events
        return 1000000.0;
    }
    
    private double getCustomerSatisfactionScore() {
        // Mock implementation - calculate from feedback
        return 4.5;
    }
    
    private double getConversionRate() {
        // Mock implementation - calculate conversion rate
        return 0.15;
    }
    
    private String generateTraceId() {
        return "trace-" + java.util.UUID.randomUUID().toString();
    }
    
    private String generateSpanId() {
        return "span-" + System.nanoTime();
    }
    
    private String generateEventId() {
        return "event-" + java.util.UUID.randomUUID().toString();
    }
    
    private String generateErrorId() {
        return "error-" + java.util.UUID.randomUUID().toString();
    }
    
    private String generateAuditId() {
        return "audit-" + java.util.UUID.randomUUID().toString();
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    // Inner classes for data structures
    
    public static class TraceContext {
        private final String traceId;
        private final String spanId;
        private final String operation;
        private final String customerId;
        private final Instant startTime;
        private Timer.Sample timerSample;
        
        public TraceContext(String traceId, String spanId, String operation, String customerId, Instant startTime) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.operation = operation;
            this.customerId = customerId;
            this.startTime = startTime;
        }
        
        // Getters and setters
        public String getTraceId() { return traceId; }
        public String getSpanId() { return spanId; }
        public String getOperation() { return operation; }
        public String getCustomerId() { return customerId; }
        public Instant getStartTime() { return startTime; }
        public Timer.Sample getTimerSample() { return timerSample; }
        public void setTimerSample(Timer.Sample timerSample) { this.timerSample = timerSample; }
    }
    
    public static class BusinessEvent {
        private final String eventId;
        private final BusinessEventType eventType;
        private final String customerId;
        private final Map<String, Object> data;
        private final Instant timestamp;
        private final String traceId;
        
        public BusinessEvent(String eventId, BusinessEventType eventType, String customerId, 
                           Map<String, Object> data, Instant timestamp, String traceId) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.customerId = customerId;
            this.data = data;
            this.timestamp = timestamp;
            this.traceId = traceId;
        }
        
        // Getters
        public String getEventId() { return eventId; }
        public BusinessEventType getEventType() { return eventType; }
        public String getCustomerId() { return customerId; }
        public Map<String, Object> getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
        public String getTraceId() { return traceId; }
    }
    
    public static class ErrorEvent {
        private final String errorId;
        private final String operation;
        private final String customerId;
        private final String errorType;
        private final String message;
        private final String stackTrace;
        private final Map<String, Object> context;
        private final Instant timestamp;
        private final String traceId;
        
        public ErrorEvent(String errorId, String operation, String customerId, String errorType, 
                         String message, String stackTrace, Map<String, Object> context, 
                         Instant timestamp, String traceId) {
            this.errorId = errorId;
            this.operation = operation;
            this.customerId = customerId;
            this.errorType = errorType;
            this.message = message;
            this.stackTrace = stackTrace;
            this.context = context;
            this.timestamp = timestamp;
            this.traceId = traceId;
        }
        
        // Getters
        public String getErrorId() { return errorId; }
        public String getOperation() { return operation; }
        public String getCustomerId() { return customerId; }
        public String getErrorType() { return errorType; }
        public String getMessage() { return message; }
        public String getStackTrace() { return stackTrace; }
        public Map<String, Object> getContext() { return context; }
        public Instant getTimestamp() { return timestamp; }
        public String getTraceId() { return traceId; }
    }
    
    public static class AuditEvent {
        private final String auditId;
        private final AuditEventType eventType;
        private final String customerId;
        private final String resource;
        private final String action;
        private final Map<String, Object> details;
        private final Instant timestamp;
        private final String traceId;
        
        public AuditEvent(String auditId, AuditEventType eventType, String customerId, 
                         String resource, String action, Map<String, Object> details, 
                         Instant timestamp, String traceId) {
            this.auditId = auditId;
            this.eventType = eventType;
            this.customerId = customerId;
            this.resource = resource;
            this.action = action;
            this.details = details;
            this.timestamp = timestamp;
            this.traceId = traceId;
        }
        
        // Getters
        public String getAuditId() { return auditId; }
        public AuditEventType getEventType() { return eventType; }
        public String getCustomerId() { return customerId; }
        public String getResource() { return resource; }
        public String getAction() { return action; }
        public Map<String, Object> getDetails() { return details; }
        public Instant getTimestamp() { return timestamp; }
        public String getTraceId() { return traceId; }
    }
    
    public static class PerformanceMetric {
        private final String operation;
        private final long durationMs;
        private final boolean success;
        private final String customerId;
        private final Instant timestamp;
        
        public PerformanceMetric(String operation, long durationMs, boolean success, String customerId, Instant timestamp) {
            this.operation = operation;
            this.durationMs = durationMs;
            this.success = success;
            this.customerId = customerId;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getOperation() { return operation; }
        public long getDurationMs() { return durationMs; }
        public boolean isSuccess() { return success; }
        public String getCustomerId() { return customerId; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    public static class SystemHealthMetrics {
        private final long activeCustomers;
        private final long totalTransactions;
        private final long errorCount;
        private final double averageResponseTime;
        private final double errorRate;
        private final long systemUptime;
        private final boolean databaseHealthy;
        private final boolean redisHealthy;
        private final boolean kafkaHealthy;
        
        public SystemHealthMetrics(long activeCustomers, long totalTransactions, long errorCount, 
                                 double averageResponseTime, double errorRate, long systemUptime,
                                 boolean databaseHealthy, boolean redisHealthy, boolean kafkaHealthy) {
            this.activeCustomers = activeCustomers;
            this.totalTransactions = totalTransactions;
            this.errorCount = errorCount;
            this.averageResponseTime = averageResponseTime;
            this.errorRate = errorRate;
            this.systemUptime = systemUptime;
            this.databaseHealthy = databaseHealthy;
            this.redisHealthy = redisHealthy;
            this.kafkaHealthy = kafkaHealthy;
        }
        
        public static SystemHealthMetrics unhealthy() {
            return new SystemHealthMetrics(0, 0, Long.MAX_VALUE, 0, 100, 0, false, false, false);
        }
        
        // Getters
        public long getActiveCustomers() { return activeCustomers; }
        public long getTotalTransactions() { return totalTransactions; }
        public long getErrorCount() { return errorCount; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getErrorRate() { return errorRate; }
        public long getSystemUptime() { return systemUptime; }
        public boolean isDatabaseHealthy() { return databaseHealthy; }
        public boolean isRedisHealthy() { return redisHealthy; }
        public boolean isKafkaHealthy() { return kafkaHealthy; }
        
        public boolean isHealthy() {
            return databaseHealthy && redisHealthy && kafkaHealthy && errorRate < 5.0;
        }
    }
    
    public static class BusinessMetricsDashboard {
        private final double totalLoanApplications;
        private final double totalPaymentsProcessed;
        private final double totalCustomerRegistrations;
        private final long activeCustomers;
        private final double totalRevenue;
        private final double customerSatisfactionScore;
        private final double conversionRate;
        
        public BusinessMetricsDashboard(double totalLoanApplications, double totalPaymentsProcessed,
                                      double totalCustomerRegistrations, long activeCustomers,
                                      double totalRevenue, double customerSatisfactionScore,
                                      double conversionRate) {
            this.totalLoanApplications = totalLoanApplications;
            this.totalPaymentsProcessed = totalPaymentsProcessed;
            this.totalCustomerRegistrations = totalCustomerRegistrations;
            this.activeCustomers = activeCustomers;
            this.totalRevenue = totalRevenue;
            this.customerSatisfactionScore = customerSatisfactionScore;
            this.conversionRate = conversionRate;
        }
        
        public static BusinessMetricsDashboard empty() {
            return new BusinessMetricsDashboard(0, 0, 0, 0, 0, 0, 0);
        }
        
        // Getters
        public double getTotalLoanApplications() { return totalLoanApplications; }
        public double getTotalPaymentsProcessed() { return totalPaymentsProcessed; }
        public double getTotalCustomerRegistrations() { return totalCustomerRegistrations; }
        public long getActiveCustomers() { return activeCustomers; }
        public double getTotalRevenue() { return totalRevenue; }
        public double getCustomerSatisfactionScore() { return customerSatisfactionScore; }
        public double getConversionRate() { return conversionRate; }
    }
    
    public enum BusinessEventType {
        CUSTOMER_REGISTERED,
        CUSTOMER_LOGIN,
        CUSTOMER_LOGOUT,
        LOAN_APPLICATION_SUBMITTED,
        LOAN_APPLICATION_APPROVED,
        LOAN_APPLICATION_REJECTED,
        PAYMENT_PROCESSED,
        PAYMENT_FAILED,
        ACCOUNT_CREATED,
        ACCOUNT_CLOSED,
        FRAUD_DETECTED,
        SECURITY_ALERT
    }
    
    public enum AuditEventType {
        DATA_ACCESS,
        DATA_MODIFICATION,
        SECURITY_EVENT,
        CONFIGURATION_CHANGE,
        ADMINISTRATIVE_ACTION,
        COMPLIANCE_EVENT
    }
}