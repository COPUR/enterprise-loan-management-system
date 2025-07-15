package com.bank.infrastructure.observability;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD Test Suite for Banking Metrics Service
 * 
 * Tests observability and monitoring functionality:
 * - Business event metrics (loans, payments, customers)
 * - Performance timing metrics
 * - Compliance and security metrics
 * - Infrastructure health metrics
 * - Custom metrics capabilities
 */
@DisplayName("Banking Metrics Service Tests")
class BankingMetricsServiceTest {
    
    private MeterRegistry meterRegistry;
    private BankingMetricsService metricsService;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BankingMetricsService(meterRegistry);
    }
    
    @Nested
    @DisplayName("Business Event Metrics Tests")
    class BusinessEventMetricsTests {
        
        @Test
        @DisplayName("Should record loan application metrics with correct tags")
        void shouldRecordLoanApplicationMetricsWithCorrectTags() {
            // Given
            String loanType = "PERSONAL";
            String customerType = "INDIVIDUAL";
            BigDecimal amount = new BigDecimal("50000.00");
            
            // When
            metricsService.recordLoanApplication(loanType, customerType, amount);
            
            // Then
            Counter counter = meterRegistry.find("banking_loan_applications_total")
                .tag("loan_type", loanType)
                .tag("customer_type", customerType)
                .tag("amount_bucket", "10000-100000")
                .tag("service", "banking-platform")
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
            
            // Verify active loan applications gauge
            Gauge activeLoanGauge = meterRegistry.find("banking_active_loan_applications").gauge();
            assertThat(activeLoanGauge).isNotNull();
            assertThat(activeLoanGauge.value()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record loan approval and update portfolio")
        void shouldRecordLoanApprovalAndUpdatePortfolio() {
            // Given
            String loanType = "MORTGAGE";
            String customerType = "INDIVIDUAL";
            BigDecimal amount = new BigDecimal("250000.00");
            
            // First create a loan application to decrement later
            metricsService.recordLoanApplication(loanType, customerType, amount);
            
            // When
            metricsService.recordLoanApproval(loanType, customerType, amount);
            
            // Then
            Counter approvalCounter = meterRegistry.find("banking_loan_approvals_total")
                .tag("loan_type", loanType)
                .tag("customer_type", customerType)
                .tag("amount_bucket", "100000+")
                .counter();
            
            assertThat(approvalCounter).isNotNull();
            assertThat(approvalCounter.count()).isEqualTo(1.0);
            
            // Verify active loan applications decremented back to 0
            Gauge activeLoanGauge = meterRegistry.find("banking_active_loan_applications").gauge();
            assertThat(activeLoanGauge.value()).isEqualTo(0.0);
            
            // Verify loan portfolio updated
            Gauge portfolioGauge = meterRegistry.find("banking_total_loan_portfolio_usd").gauge();
            assertThat(portfolioGauge).isNotNull();
            assertThat(portfolioGauge.value()).isEqualTo(250000.0);
        }
        
        @Test
        @DisplayName("Should record loan rejection with reason")
        void shouldRecordLoanRejectionWithReason() {
            // Given
            String loanType = "PERSONAL";
            String customerType = "INDIVIDUAL";
            String rejectionReason = "INSUFFICIENT_CREDIT";
            BigDecimal amount = new BigDecimal("75000.00");
            
            // First create a loan application to decrement later
            metricsService.recordLoanApplication(loanType, customerType, amount);
            
            // When
            metricsService.recordLoanRejection(loanType, customerType, rejectionReason);
            
            // Then
            Counter rejectionCounter = meterRegistry.find("banking_loan_rejections_total")
                .tag("loan_type", loanType)
                .tag("customer_type", customerType)
                .tag("rejection_reason", rejectionReason)
                .counter();
            
            assertThat(rejectionCounter).isNotNull();
            assertThat(rejectionCounter.count()).isEqualTo(1.0);
            
            // Verify active loan applications decremented back to 0
            Gauge activeLoanGauge = meterRegistry.find("banking_active_loan_applications").gauge();
            assertThat(activeLoanGauge.value()).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should record payment transaction metrics")
        void shouldRecordPaymentTransactionMetrics() {
            // Given
            String paymentMethod = "BANK_TRANSFER";
            String paymentType = "LOAN_PAYMENT";
            BigDecimal amount = new BigDecimal("1500.00");
            String status = "COMPLETED";
            
            // When
            metricsService.recordPaymentTransaction(paymentMethod, paymentType, amount, status);
            
            // Then
            Counter paymentCounter = meterRegistry.find("banking_payment_transactions_total")
                .tag("payment_method", paymentMethod)
                .tag("payment_type", paymentType)
                .tag("status", status)
                .tag("amount_bucket", "1000-10000")
                .counter();
            
            assertThat(paymentCounter).isNotNull();
            assertThat(paymentCounter.count()).isEqualTo(1.0);
            
            // Verify daily payment volume updated for completed payments
            Gauge dailyVolumeGauge = meterRegistry.find("banking_daily_payment_volume_usd").gauge();
            assertThat(dailyVolumeGauge).isNotNull();
            assertThat(dailyVolumeGauge.value()).isEqualTo(1500.0);
        }
        
        @Test
        @DisplayName("Should record payment failure separately")
        void shouldRecordPaymentFailureSeparately() {
            // Given
            String paymentMethod = "CREDIT_CARD";
            String paymentType = "TRANSFER";
            BigDecimal amount = new BigDecimal("500.00");
            String status = "FAILED";
            
            // When
            metricsService.recordPaymentTransaction(paymentMethod, paymentType, amount, status);
            
            // Then
            Counter paymentCounter = meterRegistry.find("banking_payment_transactions_total")
                .tag("status", status)
                .counter();
            assertThat(paymentCounter.count()).isEqualTo(1.0);
            
            Counter failureCounter = meterRegistry.find("banking_payment_failures_total")
                .tag("payment_method", paymentMethod)
                .tag("payment_type", paymentType)
                .counter();
            assertThat(failureCounter).isNotNull();
            assertThat(failureCounter.count()).isEqualTo(1.0);
            
            // Verify daily payment volume NOT updated for failed payments
            Gauge dailyVolumeGauge = meterRegistry.find("banking_daily_payment_volume_usd").gauge();
            assertThat(dailyVolumeGauge.value()).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should record customer creation metrics")
        void shouldRecordCustomerCreationMetrics() {
            // Given
            String customerType = "INDIVIDUAL";
            String onboardingChannel = "MOBILE_APP";
            
            // When
            metricsService.recordCustomerCreation(customerType, onboardingChannel);
            
            // Then
            Counter customerCounter = meterRegistry.find("banking_customer_creations_total")
                .tag("customer_type", customerType)
                .tag("onboarding_channel", onboardingChannel)
                .counter();
            
            assertThat(customerCounter).isNotNull();
            assertThat(customerCounter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record fraud detection with risk scoring")
        void shouldRecordFraudDetectionWithRiskScoring() {
            // Given
            String fraudType = "SUSPICIOUS_TRANSACTION";
            String severity = "HIGH";
            Double riskScore = 0.85; // High risk
            
            // When
            metricsService.recordFraudDetection(fraudType, severity, riskScore);
            
            // Then
            Counter fraudCounter = meterRegistry.find("banking_fraud_detections_total")
                .tag("fraud_type", fraudType)
                .tag("severity", severity)
                .tag("risk_bucket", "high")
                .counter();
            
            assertThat(fraudCounter).isNotNull();
            assertThat(fraudCounter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record compliance violations")
        void shouldRecordComplianceViolations() {
            // Given
            String violationType = "AML_VIOLATION";
            String severity = "CRITICAL";
            
            // When
            metricsService.recordComplianceViolation(violationType, severity);
            
            // Then
            Counter complianceCounter = meterRegistry.find("banking_compliance_violations_total")
                .tag("violation_type", violationType)
                .tag("severity", severity)
                .counter();
            
            assertThat(complianceCounter).isNotNull();
            assertThat(complianceCounter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("Performance Timing Metrics Tests")
    class PerformanceTimingMetricsTests {
        
        @Test
        @DisplayName("Should measure loan processing duration")
        void shouldMeasureLoanProcessingDuration() throws InterruptedException {
            // Given
            String loanType = "PERSONAL";
            String result = "APPROVED";
            
            // When
            Timer.Sample sample = metricsService.startLoanProcessingTimer();
            Thread.sleep(100); // Simulate processing time
            metricsService.recordLoanProcessingDuration(sample, loanType, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_loan_processing_duration_seconds")
                .tag("loan_type", loanType)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(90);
        }
        
        @Test
        @DisplayName("Should measure payment processing duration")
        void shouldMeasurePaymentProcessingDuration() throws InterruptedException {
            // Given
            String paymentMethod = "BANK_TRANSFER";
            String result = "COMPLETED";
            
            // When
            Timer.Sample sample = metricsService.startPaymentProcessingTimer();
            Thread.sleep(50); // Simulate processing time
            metricsService.recordPaymentProcessingDuration(sample, paymentMethod, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_payment_processing_duration_seconds")
                .tag("payment_method", paymentMethod)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(40);
        }
        
        @Test
        @DisplayName("Should record KYC verification duration")
        void shouldRecordKYCVerificationDuration() {
            // Given
            Duration duration = Duration.ofMillis(750);
            String verificationType = "IDENTITY_VERIFICATION";
            String result = "VERIFIED";
            
            // When
            metricsService.recordKYCVerificationDuration(duration, verificationType, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_kyc_verification_duration_seconds")
                .tag("verification_type", verificationType)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(750);
        }
        
        @Test
        @DisplayName("Should record fraud analysis duration")
        void shouldRecordFraudAnalysisDuration() {
            // Given
            Duration duration = Duration.ofMillis(1200);
            String analysisType = "TRANSACTION_ANALYSIS";
            String result = "CLEAN";
            
            // When
            metricsService.recordFraudAnalysisDuration(duration, analysisType, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_fraud_analysis_duration_seconds")
                .tag("analysis_type", analysisType)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(1200);
        }
        
        @Test
        @DisplayName("Should record authentication duration")
        void shouldRecordAuthenticationDuration() {
            // Given
            Duration duration = Duration.ofMillis(300);
            String authMethod = "OAUTH2";
            String result = "SUCCESS";
            
            // When
            metricsService.recordAuthenticationDuration(duration, authMethod, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_authentication_duration_seconds")
                .tag("auth_method", authMethod)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(300);
        }
    }
    
    @Nested
    @DisplayName("State Management Metrics Tests")
    class StateManagementMetricsTests {
        
        @Test
        @DisplayName("Should update pending payments gauge")
        void shouldUpdatePendingPaymentsGauge() {
            // When
            metricsService.updatePendingPayments(15);
            
            // Then
            Gauge pendingGauge = meterRegistry.find("banking_pending_payments").gauge();
            assertThat(pendingGauge).isNotNull();
            assertThat(pendingGauge.value()).isEqualTo(15.0);
        }
        
        @Test
        @DisplayName("Should increment and decrement pending payments")
        void shouldIncrementAndDecrementPendingPayments() {
            // When
            metricsService.incrementPendingPayments();
            metricsService.incrementPendingPayments();
            metricsService.decrementPendingPayments();
            
            // Then
            Gauge pendingGauge = meterRegistry.find("banking_pending_payments").gauge();
            assertThat(pendingGauge.value()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should manage SSE connections gauge")
        void shouldManageSSEConnectionsGauge() {
            // When
            metricsService.incrementSSEConnections();
            metricsService.incrementSSEConnections();
            metricsService.incrementSSEConnections();
            metricsService.decrementSSEConnections();
            
            // Then
            Gauge sseGauge = meterRegistry.find("banking_sse_connections").gauge();
            assertThat(sseGauge).isNotNull();
            assertThat(sseGauge.value()).isEqualTo(2.0);
        }
        
        @Test
        @DisplayName("Should update SSE connections directly")
        void shouldUpdateSSEConnectionsDirectly() {
            // When
            metricsService.updateSSEConnections(42);
            
            // Then
            Gauge sseGauge = meterRegistry.find("banking_sse_connections").gauge();
            assertThat(sseGauge.value()).isEqualTo(42.0);
        }
    }
    
    @Nested
    @DisplayName("Amount Bucketing Tests")
    class AmountBucketingTests {
        
        @Test
        @DisplayName("Should categorize small amounts correctly")
        void shouldCategorizeSmallAmountsCorrectly() {
            // When
            metricsService.recordLoanApplication("PERSONAL", "INDIVIDUAL", new BigDecimal("500.00"));
            
            // Then
            Counter counter = meterRegistry.find("banking_loan_applications_total")
                .tag("amount_bucket", "0-1000")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should categorize medium amounts correctly")
        void shouldCategorizeMediumAmountsCorrectly() {
            // When
            metricsService.recordLoanApplication("PERSONAL", "INDIVIDUAL", new BigDecimal("5000.00"));
            
            // Then
            Counter counter = meterRegistry.find("banking_loan_applications_total")
                .tag("amount_bucket", "1000-10000")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should categorize large amounts correctly")
        void shouldCategorizeLargeAmountsCorrectly() {
            // When
            metricsService.recordLoanApplication("MORTGAGE", "INDIVIDUAL", new BigDecimal("50000.00"));
            
            // Then
            Counter counter = meterRegistry.find("banking_loan_applications_total")
                .tag("amount_bucket", "10000-100000")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should categorize very large amounts correctly")
        void shouldCategorizeVeryLargeAmountsCorrectly() {
            // When
            metricsService.recordLoanApplication("COMMERCIAL", "BUSINESS", new BigDecimal("500000.00"));
            
            // Then
            Counter counter = meterRegistry.find("banking_loan_applications_total")
                .tag("amount_bucket", "100000+")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("Risk Scoring Tests")
    class RiskScoringTests {
        
        @Test
        @DisplayName("Should categorize low risk correctly")
        void shouldCategorizeLowRiskCorrectly() {
            // When
            metricsService.recordFraudDetection("SUSPICIOUS_LOGIN", "LOW", 0.2);
            
            // Then
            Counter counter = meterRegistry.find("banking_fraud_detections_total")
                .tag("risk_bucket", "low")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should categorize medium risk correctly")
        void shouldCategorizeMediumRiskCorrectly() {
            // When
            metricsService.recordFraudDetection("UNUSUAL_PATTERN", "MEDIUM", 0.5);
            
            // Then
            Counter counter = meterRegistry.find("banking_fraud_detections_total")
                .tag("risk_bucket", "medium")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should categorize high risk correctly")
        void shouldCategorizeHighRiskCorrectly() {
            // When
            metricsService.recordFraudDetection("KNOWN_FRAUD_PATTERN", "HIGH", 0.9);
            
            // Then
            Counter counter = meterRegistry.find("banking_fraud_detections_total")
                .tag("risk_bucket", "high")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should handle null risk score")
        void shouldHandleNullRiskScore() {
            // When
            metricsService.recordFraudDetection("UNKNOWN_PATTERN", "MEDIUM", null);
            
            // Then
            Counter counter = meterRegistry.find("banking_fraud_detections_total")
                .tag("risk_bucket", "unknown")
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("FAPI Compliance Metrics Tests")
    class FAPIComplianceMetricsTests {
        
        @Test
        @DisplayName("Should record FAPI requests")
        void shouldRecordFAPIRequests() {
            // Given
            String endpoint = "/api/v1/loans";
            String clientId = "client-12345";
            String result = "SUCCESS";
            
            // When
            metricsService.recordFAPIRequest(endpoint, clientId, result);
            
            // Then
            Counter counter = meterRegistry.find("banking_fapi_requests_total")
                .tag("endpoint", endpoint)
                .tag("client_id", clientId)
                .tag("result", result)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record mTLS validation")
        void shouldRecordMTLSValidation() {
            // Given
            String clientId = "client-67890";
            String result = "VALID";
            
            // When
            metricsService.recordMTLSValidation(clientId, result);
            
            // Then
            Counter counter = meterRegistry.find("banking_mtls_validations_total")
                .tag("client_id", clientId)
                .tag("result", result)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record idempotency checks")
        void shouldRecordIdempotencyChecks() {
            // Given
            String operation = "LOAN_CREATION";
            String result = "NEW_REQUEST";
            
            // When
            metricsService.recordIdempotencyCheck(operation, result);
            
            // Then
            Counter counter = meterRegistry.find("banking_idempotency_checks_total")
                .tag("operation", operation)
                .tag("result", result)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("Custom Metrics Tests")
    class CustomMetricsTests {
        
        @Test
        @DisplayName("Should record custom counter")
        void shouldRecordCustomCounter() {
            // Given
            String name = "custom_business_metric_total";
            String description = "Custom business metric";
            Tags tags = Tags.of("department", "risk", "metric_type", "business");
            
            // When
            metricsService.recordCustomCounter(name, description, tags);
            
            // Then
            Counter counter = meterRegistry.find(name)
                .tags(tags)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record custom timer")
        void shouldRecordCustomTimer() {
            // Given
            String name = "custom_operation_duration_seconds";
            String description = "Custom operation duration";
            Duration duration = Duration.ofMillis(1500);
            Tags tags = Tags.of("operation", "complex_calculation");
            
            // When
            metricsService.recordCustomTimer(name, description, duration, tags);
            
            // Then
            Timer timer = meterRegistry.find(name)
                .tags(tags)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(1500);
        }
        
        @Test
        @DisplayName("Should update custom gauge")
        void shouldUpdateCustomGauge() {
            // Given
            String name = "custom_queue_size";
            String description = "Custom queue size";
            double value = 42.5;
            Tags tags = Tags.of("queue", "processing", "environment", "prod");
            
            // When
            metricsService.updateCustomGauge(name, description, value, tags);
            
            // Then
            Gauge gauge = meterRegistry.find(name)
                .tags(tags)
                .gauge();
            
            assertThat(gauge).isNotNull();
            assertThat(gauge.value()).isEqualTo(42.5);
        }
        
        @Test
        @DisplayName("Should update existing custom gauge")
        void shouldUpdateExistingCustomGauge() {
            // Given
            String name = "dynamic_metric";
            String description = "Dynamic metric";
            Tags tags = Tags.of("type", "test");
            
            // When - Set initial value
            metricsService.updateCustomGauge(name, description, 10.0, tags);
            
            // Then - Verify initial value
            Gauge gauge = meterRegistry.find(name).tags(tags).gauge();
            assertThat(gauge.value()).isEqualTo(10.0);
            
            // When - Update value
            metricsService.updateCustomGauge(name, description, 25.0, tags);
            
            // Then - Verify updated value
            assertThat(gauge.value()).isEqualTo(25.0);
        }
    }
    
    @Nested
    @DisplayName("Infrastructure Metrics Tests")
    class InfrastructureMetricsTests {
        
        @Test
        @DisplayName("Should record health check metrics")
        void shouldRecordHealthCheckMetrics() {
            // Given
            String service = "loan-service";
            String status = "UP";
            Duration responseTime = Duration.ofMillis(150);
            
            // When
            metricsService.recordHealthCheck(service, status, responseTime);
            
            // Then
            Counter counter = meterRegistry.find("banking_health_checks_total")
                .tag("service", service)
                .tag("status", status)
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
            
            Timer timer = meterRegistry.find("banking_health_check_duration_seconds")
                .tag("service", service)
                .tag("status", status)
                .timer();
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(150);
        }
        
        @Test
        @DisplayName("Should record rate limit hits")
        void shouldRecordRateLimitHits() {
            // Given
            String endpoint = "/api/v1/payments";
            String clientId = "client-abc123";
            String limitType = "REQUESTS_PER_MINUTE";
            
            // When
            metricsService.recordRateLimitHit(endpoint, clientId, limitType);
            
            // Then
            Counter counter = meterRegistry.find("banking_rate_limit_hits_total")
                .tag("endpoint", endpoint)
                .tag("client_id", clientId)
                .tag("limit_type", limitType)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record database operations")
        void shouldRecordDatabaseOperations() {
            // Given
            String operation = "SELECT";
            String table = "loans";
            Duration duration = Duration.ofMillis(25);
            String result = "SUCCESS";
            
            // When
            metricsService.recordDatabaseOperation(operation, table, duration, result);
            
            // Then
            Timer timer = meterRegistry.find("banking_database_operation_duration_seconds")
                .tag("operation", operation)
                .tag("table", table)
                .tag("result", result)
                .timer();
            
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(25);
        }
        
        @Test
        @DisplayName("Should record cache operations")
        void shouldRecordCacheOperations() {
            // Given
            String cache = "customer-profile";
            String operation = "GET";
            String result = "HIT";
            
            // When
            metricsService.recordCacheOperation(cache, operation, result);
            
            // Then
            Counter counter = meterRegistry.find("banking_cache_operations_total")
                .tag("cache", cache)
                .tag("operation", operation)
                .tag("result", result)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
    
    @Nested
    @DisplayName("Event Streaming Metrics Tests")
    class EventStreamingMetricsTests {
        
        @Test
        @DisplayName("Should record event published")
        void shouldRecordEventPublished() {
            // Given
            String eventType = "LoanApprovedEvent";
            String context = "loan-context";
            
            // When
            metricsService.recordEventPublished(eventType, context);
            
            // Then
            Counter counter = meterRegistry.find("banking_events_published_total")
                .tag("event_type", eventType)
                .tag("context", context)
                .counter();
            
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record event processed with timing")
        void shouldRecordEventProcessedWithTiming() {
            // Given
            String eventType = "PaymentCompletedEvent";
            String handler = "LoanPaymentHandler";
            Duration processingTime = Duration.ofMillis(75);
            String result = "SUCCESS";
            
            // When
            metricsService.recordEventProcessed(eventType, handler, processingTime, result);
            
            // Then
            Counter counter = meterRegistry.find("banking_events_processed_total")
                .tag("event_type", eventType)
                .tag("handler", handler)
                .tag("result", result)
                .counter();
            assertThat(counter.count()).isEqualTo(1.0);
            
            Timer timer = meterRegistry.find("banking_event_processing_duration_seconds")
                .tag("event_type", eventType)
                .tag("handler", handler)
                .tag("result", result)
                .timer();
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(75);
        }
    }
    
    @Nested
    @DisplayName("Service Initialization Tests")
    class ServiceInitializationTests {
        
        @Test
        @DisplayName("Should initialize all required meters on construction")
        void shouldInitializeAllRequiredMetersOnConstruction() {
            // Then - Verify all expected meters are registered
            List<Meter> meters = meterRegistry.getMeters();
            
            // Check that we have the expected number of meters (counters, timers, gauges)
            assertThat(meters).hasSizeGreaterThan(10);
            
            // Verify specific meters exist
            assertThat(meterRegistry.find("banking_loan_applications_total").counter()).isNotNull();
            assertThat(meterRegistry.find("banking_payment_transactions_total").counter()).isNotNull();
            assertThat(meterRegistry.find("banking_loan_processing_duration_seconds").timer()).isNotNull();
            assertThat(meterRegistry.find("banking_active_loan_applications").gauge()).isNotNull();
            assertThat(meterRegistry.find("banking_total_loan_portfolio_usd").gauge()).isNotNull();
        }
        
        @Test
        @DisplayName("Should have correct service tag on all meters")
        void shouldHaveCorrectServiceTagOnAllMeters() {
            // Then
            List<Meter> meters = meterRegistry.getMeters();
            
            // Verify all meters have the service tag
            meters.forEach(meter -> {
                boolean hasServiceTag = meter.getId().getTags().stream()
                    .anyMatch(tag -> "service".equals(tag.getKey()) && "banking-platform".equals(tag.getValue()));
                assertThat(hasServiceTag)
                    .as("Meter %s should have service=banking-platform tag", meter.getId().getName())
                    .isTrue();
            });
        }
    }
}