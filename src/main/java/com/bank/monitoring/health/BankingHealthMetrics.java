package com.bank.monitoring.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.Status;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Banking Health Metrics
 * Provides Prometheus metrics for banking health indicators
 */
@Component
public class BankingHealthMetrics {

    private final MeterRegistry meterRegistry;
    
    // Health check timers
    private final Timer loanProcessingHealthTimer;
    private final Timer paymentSystemHealthTimer;
    private final Timer complianceServiceHealthTimer;
    private final Timer fraudDetectionHealthTimer;
    private final Timer customerServiceHealthTimer;
    
    // Health status counters
    private final Counter loanProcessingUpCounter;
    private final Counter loanProcessingDownCounter;
    private final Counter paymentSystemUpCounter;
    private final Counter paymentSystemDownCounter;
    private final Counter complianceServiceUpCounter;
    private final Counter complianceServiceDownCounter;
    private final Counter fraudDetectionUpCounter;
    private final Counter fraudDetectionDownCounter;
    private final Counter customerServiceUpCounter;
    private final Counter customerServiceDownCounter;
    
    // Health metrics gauges
    private final AtomicInteger pendingLoansGauge = new AtomicInteger(0);
    private final AtomicInteger recentPaymentsGauge = new AtomicInteger(0);
    private final AtomicInteger complianceChecksGauge = new AtomicInteger(0);
    private final AtomicInteger fraudChecksGauge = new AtomicInteger(0);
    private final AtomicInteger customerOperationsGauge = new AtomicInteger(0);
    private final AtomicLong healthCheckDurationGauge = new AtomicLong(0);
    
    public BankingHealthMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize timers
        this.loanProcessingHealthTimer = Timer.builder("banking.health.check.duration")
            .tag("service", "loan-processing")
            .description("Duration of loan processing health checks")
            .register(meterRegistry);
            
        this.paymentSystemHealthTimer = Timer.builder("banking.health.check.duration")
            .tag("service", "payment-system")
            .description("Duration of payment system health checks")
            .register(meterRegistry);
            
        this.complianceServiceHealthTimer = Timer.builder("banking.health.check.duration")
            .tag("service", "compliance-service")
            .description("Duration of compliance service health checks")
            .register(meterRegistry);
            
        this.fraudDetectionHealthTimer = Timer.builder("banking.health.check.duration")
            .tag("service", "fraud-detection")
            .description("Duration of fraud detection health checks")
            .register(meterRegistry);
            
        this.customerServiceHealthTimer = Timer.builder("banking.health.check.duration")
            .tag("service", "customer-service")
            .description("Duration of customer service health checks")
            .register(meterRegistry);
        
        // Initialize counters
        this.loanProcessingUpCounter = Counter.builder("banking.health.status.total")
            .tag("service", "loan-processing")
            .tag("status", "up")
            .description("Count of loan processing UP status")
            .register(meterRegistry);
            
        this.loanProcessingDownCounter = Counter.builder("banking.health.status.total")
            .tag("service", "loan-processing")
            .tag("status", "down")
            .description("Count of loan processing DOWN status")
            .register(meterRegistry);
            
        this.paymentSystemUpCounter = Counter.builder("banking.health.status.total")
            .tag("service", "payment-system")
            .tag("status", "up")
            .description("Count of payment system UP status")
            .register(meterRegistry);
            
        this.paymentSystemDownCounter = Counter.builder("banking.health.status.total")
            .tag("service", "payment-system")
            .tag("status", "down")
            .description("Count of payment system DOWN status")
            .register(meterRegistry);
            
        this.complianceServiceUpCounter = Counter.builder("banking.health.status.total")
            .tag("service", "compliance-service")
            .tag("status", "up")
            .description("Count of compliance service UP status")
            .register(meterRegistry);
            
        this.complianceServiceDownCounter = Counter.builder("banking.health.status.total")
            .tag("service", "compliance-service")
            .tag("status", "down")
            .description("Count of compliance service DOWN status")
            .register(meterRegistry);
            
        this.fraudDetectionUpCounter = Counter.builder("banking.health.status.total")
            .tag("service", "fraud-detection")
            .tag("status", "up")
            .description("Count of fraud detection UP status")
            .register(meterRegistry);
            
        this.fraudDetectionDownCounter = Counter.builder("banking.health.status.total")
            .tag("service", "fraud-detection")
            .tag("status", "down")
            .description("Count of fraud detection DOWN status")
            .register(meterRegistry);
            
        this.customerServiceUpCounter = Counter.builder("banking.health.status.total")
            .tag("service", "customer-service")
            .tag("status", "up")
            .description("Count of customer service UP status")
            .register(meterRegistry);
            
        this.customerServiceDownCounter = Counter.builder("banking.health.status.total")
            .tag("service", "customer-service")
            .tag("status", "down")
            .description("Count of customer service DOWN status")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("banking.loans.pending")
            .description("Number of pending loans")
            .register(meterRegistry, pendingLoansGauge, AtomicInteger::get);
            
        Gauge.builder("banking.payments.recent")
            .description("Number of recent payments")
            .register(meterRegistry, recentPaymentsGauge, AtomicInteger::get);
            
        Gauge.builder("banking.compliance.checks")
            .description("Number of compliance checks")
            .register(meterRegistry, complianceChecksGauge, AtomicInteger::get);
            
        Gauge.builder("banking.fraud.checks")
            .description("Number of fraud checks")
            .register(meterRegistry, fraudChecksGauge, AtomicInteger::get);
            
        Gauge.builder("banking.customer.operations")
            .description("Number of customer operations")
            .register(meterRegistry, customerOperationsGauge, AtomicInteger::get);
            
        Gauge.builder("banking.health.check.duration.ms")
            .description("Health check duration in milliseconds")
            .register(meterRegistry, healthCheckDurationGauge, AtomicLong::get);
    }
    
    public void recordLoanProcessingHealth(Health health, Duration duration) {
        loanProcessingHealthTimer.record(duration);
        
        if (Status.UP.equals(health.getStatus())) {
            loanProcessingUpCounter.increment();
        } else {
            loanProcessingDownCounter.increment();
        }
        
        // Update pending loans gauge
        Object pendingLoans = health.getDetails().get("pendingLoans");
        if (pendingLoans instanceof Integer) {
            pendingLoansGauge.set((Integer) pendingLoans);
        }
        
        recordHealthCheckDuration(health);
    }
    
    public void recordPaymentSystemHealth(Health health, Duration duration) {
        paymentSystemHealthTimer.record(duration);
        
        if (Status.UP.equals(health.getStatus())) {
            paymentSystemUpCounter.increment();
        } else {
            paymentSystemDownCounter.increment();
        }
        
        // Update recent payments gauge
        Object recentPayments = health.getDetails().get("recentPayments");
        if (recentPayments instanceof Integer) {
            recentPaymentsGauge.set((Integer) recentPayments);
        }
        
        recordHealthCheckDuration(health);
    }
    
    public void recordComplianceServiceHealth(Health health, Duration duration) {
        complianceServiceHealthTimer.record(duration);
        
        if (Status.UP.equals(health.getStatus())) {
            complianceServiceUpCounter.increment();
        } else {
            complianceServiceDownCounter.increment();
        }
        
        // Update compliance checks gauge
        Object complianceChecks = health.getDetails().get("complianceChecks");
        if (complianceChecks instanceof Integer) {
            complianceChecksGauge.set((Integer) complianceChecks);
        }
        
        recordHealthCheckDuration(health);
    }
    
    public void recordFraudDetectionHealth(Health health, Duration duration) {
        fraudDetectionHealthTimer.record(duration);
        
        if (Status.UP.equals(health.getStatus())) {
            fraudDetectionUpCounter.increment();
        } else {
            fraudDetectionDownCounter.increment();
        }
        
        // Update fraud checks gauge
        Object fraudChecks = health.getDetails().get("fraudChecks");
        if (fraudChecks instanceof Integer) {
            fraudChecksGauge.set((Integer) fraudChecks);
        }
        
        recordHealthCheckDuration(health);
    }
    
    public void recordCustomerServiceHealth(Health health, Duration duration) {
        customerServiceHealthTimer.record(duration);
        
        if (Status.UP.equals(health.getStatus())) {
            customerServiceUpCounter.increment();
        } else {
            customerServiceDownCounter.increment();
        }
        
        // Update customer operations gauge
        Object customerOperations = health.getDetails().get("customerOperations");
        if (customerOperations instanceof Integer) {
            customerOperationsGauge.set((Integer) customerOperations);
        }
        
        recordHealthCheckDuration(health);
    }
    
    private void recordHealthCheckDuration(Health health) {
        Object checkDuration = health.getDetails().get("checkDurationMs");
        if (checkDuration instanceof Long) {
            healthCheckDurationGauge.set((Long) checkDuration);
        }
    }
}