package com.bank.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Banking Metrics Service for Financial Platform Observability
 * 
 * Provides comprehensive metrics collection for banking operations:
 * - Business metrics (loan applications, payment volumes, etc.)
 * - Performance metrics (response times, throughput)
 * - Compliance metrics (AML checks, fraud detection)
 * - Security metrics (authentication, authorization)
 * - Infrastructure metrics (health, capacity)
 */
@Service
public class BankingMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Counters for business events
    private final Counter loanApplicationsTotal;
    private final Counter loanApprovalsTotal;
    private final Counter loanRejectionsTotal;
    private final Counter paymentTransactionsTotal;
    private final Counter paymentFailuresTotal;
    private final Counter customerCreationsTotal;
    private final Counter fraudDetectionsTotal;
    private final Counter complianceViolationsTotal;
    
    // Timers for operation durations
    private final Timer loanProcessingDuration;
    private final Timer paymentProcessingDuration;
    private final Timer kycVerificationDuration;
    private final Timer fraudAnalysisDuration;
    private final Timer authenticationDuration;
    
    // Gauges for current state
    private final AtomicLong activeLoanApplications = new AtomicLong(0);
    private final AtomicLong pendingPayments = new AtomicLong(0);
    private final AtomicLong sseConnections = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalLoanPortfolio = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> dailyPaymentVolume = new AtomicReference<>(BigDecimal.ZERO);
    
    // Cache for dynamic gauges
    private final Map<String, AtomicLong> dynamicCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> dynamicGauges = new ConcurrentHashMap<>();
    
    public BankingMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.loanApplicationsTotal = Counter.builder("banking_loan_applications_total")
            .description("Total number of loan applications submitted")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.loanApprovalsTotal = Counter.builder("banking_loan_approvals_total")
            .description("Total number of loans approved")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.loanRejectionsTotal = Counter.builder("banking_loan_rejections_total")
            .description("Total number of loans rejected")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.paymentTransactionsTotal = Counter.builder("banking_payment_transactions_total")
            .description("Total number of payment transactions processed")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.paymentFailuresTotal = Counter.builder("banking_payment_failures_total")
            .description("Total number of failed payment transactions")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.customerCreationsTotal = Counter.builder("banking_customer_creations_total")
            .description("Total number of customers created")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.fraudDetectionsTotal = Counter.builder("banking_fraud_detections_total")
            .description("Total number of fraud cases detected")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.complianceViolationsTotal = Counter.builder("banking_compliance_violations_total")
            .description("Total number of compliance violations detected")
            .tag("service", "banking-platform")
            .register(meterRegistry);
        
        // Initialize timers
        this.loanProcessingDuration = Timer.builder("banking_loan_processing_duration_seconds")
            .description("Time taken to process loan applications")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.paymentProcessingDuration = Timer.builder("banking_payment_processing_duration_seconds")
            .description("Time taken to process payments")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.kycVerificationDuration = Timer.builder("banking_kyc_verification_duration_seconds")
            .description("Time taken for KYC verification")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.fraudAnalysisDuration = Timer.builder("banking_fraud_analysis_duration_seconds")
            .description("Time taken for fraud analysis")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        this.authenticationDuration = Timer.builder("banking_authentication_duration_seconds")
            .description("Time taken for user authentication")
            .tag("service", "banking-platform")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("banking_active_loan_applications", activeLoanApplications, AtomicLong::get)
            .description("Number of active loan applications")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        Gauge.builder("banking_pending_payments", pendingPayments, AtomicLong::get)
            .description("Number of pending payment transactions")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        Gauge.builder("banking_sse_connections", sseConnections, AtomicLong::get)
            .description("Number of active Server-Sent Event connections")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        Gauge.builder("banking_total_loan_portfolio_usd", totalLoanPortfolio, ref -> ref.get().doubleValue())
            .description("Total loan portfolio value in USD")
            .tag("service", "banking-platform")
            .register(meterRegistry);
            
        Gauge.builder("banking_daily_payment_volume_usd", dailyPaymentVolume, ref -> ref.get().doubleValue())
            .description("Daily payment volume in USD")
            .tag("service", "banking-platform")
            .register(meterRegistry);
    }
    
    // Business Event Metrics
    
    public void recordLoanApplication(String loanType, String customerType, BigDecimal amount) {
        Counter.builder("banking_loan_applications_total")
            .description("Total number of loan applications submitted")
            .tags("loan_type", loanType,
                  "customer_type", customerType,
                  "amount_bucket", getAmountBucket(amount),
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
        activeLoanApplications.incrementAndGet();
    }
    
    public void recordLoanApproval(String loanType, String customerType, BigDecimal amount) {
        Counter.builder("banking_loan_approvals_total")
            .description("Total number of loans approved")
            .tags("loan_type", loanType,
                  "customer_type", customerType,
                  "amount_bucket", getAmountBucket(amount),
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
        activeLoanApplications.decrementAndGet();
        updateTotalLoanPortfolio(amount, true);
    }
    
    public void recordLoanRejection(String loanType, String customerType, String rejectionReason) {
        Counter.builder("banking_loan_rejections_total")
            .description("Total number of loans rejected")
            .tags("loan_type", loanType,
                  "customer_type", customerType,
                  "rejection_reason", rejectionReason,
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
        activeLoanApplications.decrementAndGet();
    }
    
    public void recordPaymentTransaction(String paymentMethod, String paymentType, 
                                       BigDecimal amount, String status) {
        Counter.builder("banking_payment_transactions_total")
            .description("Total number of payment transactions processed")
            .tags("payment_method", paymentMethod,
                  "payment_type", paymentType,
                  "status", status,
                  "amount_bucket", getAmountBucket(amount),
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
        
        if ("COMPLETED".equals(status)) {
            updateDailyPaymentVolume(amount);
        } else if ("FAILED".equals(status)) {
            Counter.builder("banking_payment_failures_total")
                .description("Total number of failed payment transactions")
                .tags("payment_method", paymentMethod,
                      "payment_type", paymentType,
                      "service", "banking-platform")
                .register(meterRegistry)
                .increment();
        }
    }
    
    public void recordCustomerCreation(String customerType, String onboardingChannel) {
        Counter.builder("banking_customer_creations_total")
            .description("Total number of customers created")
            .tags("customer_type", customerType,
                  "onboarding_channel", onboardingChannel,
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordFraudDetection(String fraudType, String severity, Double riskScore) {
        Counter.builder("banking_fraud_detections_total")
            .description("Total fraud detections")
            .tags("fraud_type", fraudType,
                  "severity", severity,
                  "risk_bucket", getRiskBucket(riskScore),
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordComplianceViolation(String violationType, String severity) {
        Counter.builder("banking_compliance_violations_total")
            .description("Total compliance violations detected")
            .tags("violation_type", violationType,
                  "severity", severity,
                  "service", "banking-platform")
            .register(meterRegistry)
            .increment();
    }
    
    // Performance Metrics
    
    public Timer.Sample startLoanProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordLoanProcessingDuration(Timer.Sample sample, String loanType, String result) {
        sample.stop(Timer.builder("banking_loan_processing_duration_seconds")
            .description("Time taken to process loan applications")
            .tag("loan_type", loanType)
            .tag("result", result)
            .register(meterRegistry));
    }
    
    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordPaymentProcessingDuration(Timer.Sample sample, String paymentMethod, String result) {
        sample.stop(Timer.builder("banking_payment_processing_duration_seconds")
            .description("Time taken to process payments")
            .tag("payment_method", paymentMethod)
            .tag("result", result)
            .register(meterRegistry));
    }
    
    public void recordKYCVerificationDuration(Duration duration, String verificationType, String result) {
        Timer.builder("banking_kyc_verification_duration_seconds")
            .description("Time taken for KYC verification")
            .tags("verification_type", verificationType,
                  "result", result,
                  "service", "banking-platform")
            .register(meterRegistry)
            .record(duration);
    }
    
    public void recordFraudAnalysisDuration(Duration duration, String analysisType, String result) {
        Timer.builder("banking_fraud_analysis_duration_seconds")
            .description("Time taken for fraud analysis")
            .tags("analysis_type", analysisType,
                  "result", result,
                  "service", "banking-platform")
            .register(meterRegistry)
            .record(duration);
    }
    
    public void recordAuthenticationDuration(Duration duration, String authMethod, String result) {
        Timer.builder("banking_authentication_duration_seconds")
            .description("Time taken for user authentication")
            .tags("auth_method", authMethod,
                  "result", result,
                  "service", "banking-platform")
            .register(meterRegistry)
            .record(duration);
    }
    
    // State Metrics
    
    public void updatePendingPayments(long count) {
        pendingPayments.set(count);
    }
    
    public void incrementPendingPayments() {
        pendingPayments.incrementAndGet();
    }
    
    public void decrementPendingPayments() {
        pendingPayments.decrementAndGet();
    }
    
    public void updateSSEConnections(long count) {
        sseConnections.set(count);
    }
    
    public void incrementSSEConnections() {
        sseConnections.incrementAndGet();
    }
    
    public void decrementSSEConnections() {
        sseConnections.decrementAndGet();
    }
    
    // Custom Metrics
    
    public void recordCustomCounter(String name, String description, Tags tags) {
        Counter.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCustomTimer(String name, String description, Duration duration, Tags tags) {
        Timer.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry)
            .record(duration);
    }
    
    public void updateCustomGauge(String name, String description, double value, Tags tags) {
        String key = name + "_" + tags.toString();
        AtomicReference<Double> gauge = dynamicGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> ref = new AtomicReference<>(value);
            Gauge.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry, ref, AtomicReference::get);
            return ref;
        });
        gauge.set(value);
    }
    
    // FAPI Compliance Metrics
    
    public void recordFAPIRequest(String endpoint, String clientId, String result) {
        Counter.builder("banking_fapi_requests_total")
            .description("Total FAPI requests processed")
            .tag("endpoint", endpoint)
            .tag("client_id", clientId)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordMTLSValidation(String clientId, String result) {
        Counter.builder("banking_mtls_validations_total")
            .description("Total mTLS certificate validations")
            .tag("client_id", clientId)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordIdempotencyCheck(String operation, String result) {
        Counter.builder("banking_idempotency_checks_total")
            .description("Total idempotency key checks")
            .tag("operation", operation)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    // Utility Methods
    
    private String getAmountBucket(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) {
            return "0-1000";
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) <= 0) {
            return "1000-10000";
        } else if (amount.compareTo(BigDecimal.valueOf(100000)) <= 0) {
            return "10000-100000";
        } else {
            return "100000+";
        }
    }
    
    private String getRiskBucket(Double riskScore) {
        if (riskScore == null) return "unknown";
        if (riskScore <= 0.3) return "low";
        if (riskScore <= 0.7) return "medium";
        return "high";
    }
    
    private void updateTotalLoanPortfolio(BigDecimal amount, boolean add) {
        totalLoanPortfolio.updateAndGet(current -> 
            add ? current.add(amount) : current.subtract(amount));
    }
    
    private void updateDailyPaymentVolume(BigDecimal amount) {
        dailyPaymentVolume.updateAndGet(current -> current.add(amount));
    }
    
    // Health Check Metrics
    
    public void recordHealthCheck(String service, String status, Duration responseTime) {
        Counter.builder("banking_health_checks_total")
            .description("Total health check requests")
            .tag("service", service)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
            
        Timer.builder("banking_health_check_duration_seconds")
            .description("Health check response time")
            .tag("service", service)
            .tag("status", status)
            .register(meterRegistry)
            .record(responseTime);
    }
    
    // API Rate Limiting Metrics
    
    public void recordRateLimitHit(String endpoint, String clientId, String limitType) {
        Counter.builder("banking_rate_limit_hits_total")
            .description("Total rate limit violations")
            .tag("endpoint", endpoint)
            .tag("client_id", clientId)
            .tag("limit_type", limitType)
            .register(meterRegistry)
            .increment();
    }
    
    // Database Metrics
    
    public void recordDatabaseOperation(String operation, String table, Duration duration, String result) {
        Timer.builder("banking_database_operation_duration_seconds")
            .description("Database operation execution time")
            .tag("operation", operation)
            .tag("table", table)
            .tag("result", result)
            .register(meterRegistry)
            .record(duration);
    }
    
    // Cache Metrics
    
    public void recordCacheOperation(String cache, String operation, String result) {
        Counter.builder("banking_cache_operations_total")
            .description("Total cache operations")
            .tag("cache", cache)
            .tag("operation", operation)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
    }
    
    // Event Streaming Metrics
    
    public void recordEventPublished(String eventType, String context) {
        Counter.builder("banking_events_published_total")
            .description("Total domain events published")
            .tag("event_type", eventType)
            .tag("context", context)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordEventProcessed(String eventType, String handler, Duration processingTime, String result) {
        Counter.builder("banking_events_processed_total")
            .description("Total domain events processed")
            .tag("event_type", eventType)
            .tag("handler", handler)
            .tag("result", result)
            .register(meterRegistry)
            .increment();
            
        Timer.builder("banking_event_processing_duration_seconds")
            .description("Event processing time")
            .tag("event_type", eventType)
            .tag("handler", handler)
            .tag("result", result)
            .register(meterRegistry)
            .record(processingTime);
    }
}