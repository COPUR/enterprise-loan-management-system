package com.bank.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Banking Metrics Configuration
 * 
 * Provides enterprise-grade monitoring for banking operations:
 * - Business metrics (loans, customers, payments)
 * - Technical metrics (performance, errors, resources)
 * - Compliance metrics (audit trails, regulatory)
 * - Islamic banking metrics (Sharia compliance)
 * - Real-time alerting and dashboards
 */
@Configuration
public class BankingMetricsConfiguration {
    
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        CompositeMeterRegistry registry = new CompositeMeterRegistry();
        
        // Add Prometheus registry for metrics export
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.add(prometheusRegistry);
        
        // Configure common tags for all metrics
        registry.config()
            .commonTags(
                "application", "enterprise-banking-platform",
                "environment", getEnvironment(),
                "region", getRegion(),
                "version", getApplicationVersion()
            );
        
        // Configure distribution statistics
        registry.config()
            .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", Duration.ofSeconds(10)))
            .meterFilter(MeterFilter.maximumExpectedValue("database.query.time", Duration.ofSeconds(5)))
            .meterFilter(MeterFilter.maximumExpectedValue("cache.access.time", Duration.ofMillis(100)))
            .meterFilter(MeterFilter.maximumExpectedValue("event.processing.time", Duration.ofSeconds(2)));
        
        // Configure percentiles for important metrics
        registry.config()
            .meterFilter(MeterFilter.accept(id -> {
                String name = id.getName();
                if (name.startsWith("banking.") || name.startsWith("loan.") || 
                    name.startsWith("customer.") || name.startsWith("payment.")) {
                    return DistributionStatisticConfig.builder()
                        .percentiles(0.5, 0.75, 0.95, 0.99, 0.999)
                        .percentilesHistogram(true)
                        .build()
                        .merge(DistributionStatisticConfig.DEFAULT);
                }
                return DistributionStatisticConfig.DEFAULT;
            }));
        
        return registry;
    }
    
    @Bean
    public BankingBusinessMetrics bankingBusinessMetrics(MeterRegistry meterRegistry) {
        return new BankingBusinessMetrics(meterRegistry);
    }
    
    @Bean
    public BankingTechnicalMetrics bankingTechnicalMetrics(MeterRegistry meterRegistry) {
        return new BankingTechnicalMetrics(meterRegistry);
    }
    
    @Bean
    public BankingComplianceMetrics bankingComplianceMetrics(MeterRegistry meterRegistry) {
        return new BankingComplianceMetrics(meterRegistry);
    }
    
    @Bean
    public IslamicBankingMetrics islamicBankingMetrics(MeterRegistry meterRegistry) {
        return new IslamicBankingMetrics(meterRegistry);
    }
    
    @Bean
    public BankingAlertManager bankingAlertManager(MeterRegistry meterRegistry) {
        return new BankingAlertManager(meterRegistry);
    }
    
    @Bean
    public BankingMetricsDashboard bankingMetricsDashboard(
            BankingBusinessMetrics businessMetrics,
            BankingTechnicalMetrics technicalMetrics,
            BankingComplianceMetrics complianceMetrics,
            IslamicBankingMetrics islamicBankingMetrics) {
        return new BankingMetricsDashboard(businessMetrics, technicalMetrics, complianceMetrics, islamicBankingMetrics);
    }
    
    @Bean
    public PrometheusScrapeEndpoint prometheusScrapeEndpoint(MeterRegistry meterRegistry) {
        return new PrometheusScrapeEndpoint(meterRegistry);
    }
    
    // Helper methods for configuration
    
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "development");
    }
    
    private String getRegion() {
        return System.getProperty("banking.region", "uae-central");
    }
    
    private String getApplicationVersion() {
        return System.getProperty("banking.version", "1.0.0");
    }
    
    /**
     * Business Metrics for Banking Operations
     */
    public static class BankingBusinessMetrics {
        private final MeterRegistry meterRegistry;
        
        // Loan metrics
        private final Counter loanApplications;
        private final Counter loanApprovals;
        private final Counter loanRejections;
        private final Counter loanDisbursements;
        private final Counter loanPayments;
        private final Counter loanDefaults;
        private final Timer loanProcessingTime;
        private final Gauge activeLoanAmount;
        private final Gauge portfolioAtRisk;
        
        // Customer metrics
        private final Counter customerRegistrations;
        private final Counter customerActivations;
        private final Counter customerDeactivations;
        private final Gauge totalCustomers;
        private final Gauge averageCreditScore;
        private final Timer customerOnboardingTime;
        
        // Payment metrics
        private final Counter paymentTransactions;
        private final Counter paymentFailures;
        private final Counter paymentReversals;
        private final Timer paymentProcessingTime;
        private final Gauge dailyPaymentVolume;
        
        public BankingBusinessMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Initialize loan metrics
            this.loanApplications = Counter.builder("banking.loans.applications")
                .description("Total number of loan applications")
                .register(meterRegistry);
            
            this.loanApprovals = Counter.builder("banking.loans.approvals")
                .description("Total number of loan approvals")
                .register(meterRegistry);
            
            this.loanRejections = Counter.builder("banking.loans.rejections")
                .description("Total number of loan rejections")
                .register(meterRegistry);
            
            this.loanDisbursements = Counter.builder("banking.loans.disbursements")
                .description("Total number of loan disbursements")
                .register(meterRegistry);
            
            this.loanPayments = Counter.builder("banking.loans.payments")
                .description("Total number of loan payments")
                .register(meterRegistry);
            
            this.loanDefaults = Counter.builder("banking.loans.defaults")
                .description("Total number of loan defaults")
                .register(meterRegistry);
            
            this.loanProcessingTime = Timer.builder("banking.loans.processing.time")
                .description("Time taken to process loan applications")
                .register(meterRegistry);
            
            this.activeLoanAmount = Gauge.builder("banking.loans.active.amount")
                .description("Total amount of active loans")
                .register(meterRegistry, this, metrics -> getCurrentActiveLoanAmount());
            
            this.portfolioAtRisk = Gauge.builder("banking.portfolio.at.risk")
                .description("Portfolio at risk percentage")
                .register(meterRegistry, this, metrics -> getCurrentPortfolioAtRisk());
            
            // Initialize customer metrics
            this.customerRegistrations = Counter.builder("banking.customers.registrations")
                .description("Total number of customer registrations")
                .register(meterRegistry);
            
            this.customerActivations = Counter.builder("banking.customers.activations")
                .description("Total number of customer activations")
                .register(meterRegistry);
            
            this.customerDeactivations = Counter.builder("banking.customers.deactivations")
                .description("Total number of customer deactivations")
                .register(meterRegistry);
            
            this.totalCustomers = Gauge.builder("banking.customers.total")
                .description("Total number of active customers")
                .register(meterRegistry, this, metrics -> getCurrentTotalCustomers());
            
            this.averageCreditScore = Gauge.builder("banking.customers.credit.score.average")
                .description("Average credit score of customers")
                .register(meterRegistry, this, metrics -> getCurrentAverageCreditScore());
            
            this.customerOnboardingTime = Timer.builder("banking.customers.onboarding.time")
                .description("Time taken to onboard new customers")
                .register(meterRegistry);
            
            // Initialize payment metrics
            this.paymentTransactions = Counter.builder("banking.payments.transactions")
                .description("Total number of payment transactions")
                .register(meterRegistry);
            
            this.paymentFailures = Counter.builder("banking.payments.failures")
                .description("Total number of payment failures")
                .register(meterRegistry);
            
            this.paymentReversals = Counter.builder("banking.payments.reversals")
                .description("Total number of payment reversals")
                .register(meterRegistry);
            
            this.paymentProcessingTime = Timer.builder("banking.payments.processing.time")
                .description("Time taken to process payments")
                .register(meterRegistry);
            
            this.dailyPaymentVolume = Gauge.builder("banking.payments.daily.volume")
                .description("Daily payment volume in AED")
                .register(meterRegistry, this, metrics -> getCurrentDailyPaymentVolume());
        }
        
        // Public methods for recording metrics
        
        public void recordLoanApplication(String loanType, double amount) {
            loanApplications.increment("type", loanType, "amount_range", getAmountRange(amount));
        }
        
        public void recordLoanApproval(String loanType, double amount, Duration processingTime) {
            loanApprovals.increment("type", loanType, "amount_range", getAmountRange(amount));
            loanProcessingTime.record(processingTime);
        }
        
        public void recordLoanRejection(String loanType, String reason) {
            loanRejections.increment("type", loanType, "reason", reason);
        }
        
        public void recordLoanDisbursement(String loanType, double amount) {
            loanDisbursements.increment("type", loanType, "amount_range", getAmountRange(amount));
        }
        
        public void recordLoanPayment(String loanType, double amount) {
            loanPayments.increment("type", loanType, "amount_range", getAmountRange(amount));
        }
        
        public void recordLoanDefault(String loanType, double amount, String reason) {
            loanDefaults.increment("type", loanType, "amount_range", getAmountRange(amount), "reason", reason);
        }
        
        public void recordCustomerRegistration(String customerType) {
            customerRegistrations.increment("type", customerType);
        }
        
        public void recordCustomerActivation(String customerType, Duration onboardingTime) {
            customerActivations.increment("type", customerType);
            customerOnboardingTime.record(onboardingTime);
        }
        
        public void recordCustomerDeactivation(String customerType, String reason) {
            customerDeactivations.increment("type", customerType, "reason", reason);
        }
        
        public void recordPaymentTransaction(String paymentType, double amount, Duration processingTime) {
            paymentTransactions.increment("type", paymentType, "amount_range", getAmountRange(amount));
            paymentProcessingTime.record(processingTime);
        }
        
        public void recordPaymentFailure(String paymentType, String reason) {
            paymentFailures.increment("type", paymentType, "reason", reason);
        }
        
        public void recordPaymentReversal(String paymentType, double amount, String reason) {
            paymentReversals.increment("type", paymentType, "amount_range", getAmountRange(amount), "reason", reason);
        }
        
        // Helper methods for gauge values (would be implemented with actual data sources)
        
        private double getCurrentActiveLoanAmount() {
            // This would connect to actual data source
            return 0.0; // Placeholder
        }
        
        private double getCurrentPortfolioAtRisk() {
            // This would calculate current portfolio at risk
            return 0.0; // Placeholder
        }
        
        private double getCurrentTotalCustomers() {
            // This would get current active customer count
            return 0.0; // Placeholder
        }
        
        private double getCurrentAverageCreditScore() {
            // This would calculate average credit score
            return 0.0; // Placeholder
        }
        
        private double getCurrentDailyPaymentVolume() {
            // This would calculate daily payment volume
            return 0.0; // Placeholder
        }
        
        private String getAmountRange(double amount) {
            if (amount < 10000) return "small";
            if (amount < 50000) return "medium";
            if (amount < 100000) return "large";
            return "xlarge";
        }
    }
    
    /**
     * Technical Metrics for System Performance
     */
    public static class BankingTechnicalMetrics {
        private final MeterRegistry meterRegistry;
        
        // Database metrics
        private final Timer databaseQueryTime;
        private final Counter databaseErrors;
        private final Gauge databaseConnections;
        private final Gauge databasePoolUtilization;
        
        // Cache metrics
        private final Timer cacheAccessTime;
        private final Counter cacheHits;
        private final Counter cacheMisses;
        private final Gauge cacheMemoryUsage;
        
        // Event processing metrics
        private final Timer eventProcessingTime;
        private final Counter eventProcessingErrors;
        private final Gauge eventQueueSize;
        
        // JVM metrics
        private final Gauge jvmMemoryUsage;
        private final Gauge jvmCpuUsage;
        private final Counter jvmGcCollections;
        
        public BankingTechnicalMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Initialize database metrics
            this.databaseQueryTime = Timer.builder("banking.database.query.time")
                .description("Database query execution time")
                .register(meterRegistry);
            
            this.databaseErrors = Counter.builder("banking.database.errors")
                .description("Database errors")
                .register(meterRegistry);
            
            this.databaseConnections = Gauge.builder("banking.database.connections")
                .description("Current database connections")
                .register(meterRegistry, this, metrics -> getCurrentDatabaseConnections());
            
            this.databasePoolUtilization = Gauge.builder("banking.database.pool.utilization")
                .description("Database connection pool utilization")
                .register(meterRegistry, this, metrics -> getCurrentPoolUtilization());
            
            // Initialize cache metrics
            this.cacheAccessTime = Timer.builder("banking.cache.access.time")
                .description("Cache access time")
                .register(meterRegistry);
            
            this.cacheHits = Counter.builder("banking.cache.hits")
                .description("Cache hits")
                .register(meterRegistry);
            
            this.cacheMisses = Counter.builder("banking.cache.misses")
                .description("Cache misses")
                .register(meterRegistry);
            
            this.cacheMemoryUsage = Gauge.builder("banking.cache.memory.usage")
                .description("Cache memory usage in bytes")
                .register(meterRegistry, this, metrics -> getCurrentCacheMemoryUsage());
            
            // Initialize event processing metrics
            this.eventProcessingTime = Timer.builder("banking.events.processing.time")
                .description("Event processing time")
                .register(meterRegistry);
            
            this.eventProcessingErrors = Counter.builder("banking.events.processing.errors")
                .description("Event processing errors")
                .register(meterRegistry);
            
            this.eventQueueSize = Gauge.builder("banking.events.queue.size")
                .description("Event queue size")
                .register(meterRegistry, this, metrics -> getCurrentEventQueueSize());
            
            // Initialize JVM metrics
            this.jvmMemoryUsage = Gauge.builder("banking.jvm.memory.usage")
                .description("JVM memory usage percentage")
                .register(meterRegistry, this, metrics -> getCurrentJvmMemoryUsage());
            
            this.jvmCpuUsage = Gauge.builder("banking.jvm.cpu.usage")
                .description("JVM CPU usage percentage")
                .register(meterRegistry, this, metrics -> getCurrentJvmCpuUsage());
            
            this.jvmGcCollections = Counter.builder("banking.jvm.gc.collections")
                .description("JVM garbage collection count")
                .register(meterRegistry);
        }
        
        // Public methods for recording metrics
        
        public void recordDatabaseQuery(String queryType, Duration executionTime) {
            databaseQueryTime.record(executionTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        public void recordDatabaseError(String errorType, String operation) {
            databaseErrors.increment("type", errorType, "operation", operation);
        }
        
        public void recordCacheAccess(String cacheType, boolean hit, Duration accessTime) {
            cacheAccessTime.record(accessTime.toMillis(), TimeUnit.MILLISECONDS);
            if (hit) {
                cacheHits.increment("type", cacheType);
            } else {
                cacheMisses.increment("type", cacheType);
            }
        }
        
        public void recordEventProcessing(String eventType, Duration processingTime) {
            eventProcessingTime.record(processingTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        public void recordEventProcessingError(String eventType, String errorType) {
            eventProcessingErrors.increment("event_type", eventType, "error_type", errorType);
        }
        
        public void recordGcCollection(String gcType) {
            jvmGcCollections.increment("type", gcType);
        }
        
        // Helper methods for gauge values
        
        private double getCurrentDatabaseConnections() {
            return 0.0; // Placeholder
        }
        
        private double getCurrentPoolUtilization() {
            return 0.0; // Placeholder
        }
        
        private double getCurrentCacheMemoryUsage() {
            return 0.0; // Placeholder
        }
        
        private double getCurrentEventQueueSize() {
            return 0.0; // Placeholder
        }
        
        private double getCurrentJvmMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            return (double) usedMemory / totalMemory * 100;
        }
        
        private double getCurrentJvmCpuUsage() {
            return 0.0; // Placeholder - would use ManagementFactory
        }
    }
    
    /**
     * Compliance Metrics for Regulatory Requirements
     */
    public static class BankingComplianceMetrics {
        private final MeterRegistry meterRegistry;
        
        // Audit metrics
        private final Counter auditEvents;
        private final Counter auditFailures;
        private final Timer auditLogProcessingTime;
        
        // Regulatory metrics
        private final Counter regulatoryReports;
        private final Counter complianceViolations;
        private final Gauge dataRetentionCompliance;
        
        // Security metrics
        private final Counter securityEvents;
        private final Counter failedAuthentications;
        private final Counter unauthorizedAccess;
        
        public BankingComplianceMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Initialize audit metrics
            this.auditEvents = Counter.builder("banking.compliance.audit.events")
                .description("Total audit events")
                .register(meterRegistry);
            
            this.auditFailures = Counter.builder("banking.compliance.audit.failures")
                .description("Audit logging failures")
                .register(meterRegistry);
            
            this.auditLogProcessingTime = Timer.builder("banking.compliance.audit.processing.time")
                .description("Audit log processing time")
                .register(meterRegistry);
            
            // Initialize regulatory metrics
            this.regulatoryReports = Counter.builder("banking.compliance.regulatory.reports")
                .description("Regulatory reports generated")
                .register(meterRegistry);
            
            this.complianceViolations = Counter.builder("banking.compliance.violations")
                .description("Compliance violations detected")
                .register(meterRegistry);
            
            this.dataRetentionCompliance = Gauge.builder("banking.compliance.data.retention")
                .description("Data retention compliance percentage")
                .register(meterRegistry, this, metrics -> getCurrentDataRetentionCompliance());
            
            // Initialize security metrics
            this.securityEvents = Counter.builder("banking.compliance.security.events")
                .description("Security events")
                .register(meterRegistry);
            
            this.failedAuthentications = Counter.builder("banking.compliance.security.failed.auth")
                .description("Failed authentication attempts")
                .register(meterRegistry);
            
            this.unauthorizedAccess = Counter.builder("banking.compliance.security.unauthorized.access")
                .description("Unauthorized access attempts")
                .register(meterRegistry);
        }
        
        // Public methods for recording metrics
        
        public void recordAuditEvent(String eventType, String category) {
            auditEvents.increment("type", eventType, "category", category);
        }
        
        public void recordAuditFailure(String reason) {
            auditFailures.increment("reason", reason);
        }
        
        public void recordAuditProcessing(Duration processingTime) {
            auditLogProcessingTime.record(processingTime);
        }
        
        public void recordRegulatoryReport(String reportType) {
            regulatoryReports.increment("type", reportType);
        }
        
        public void recordComplianceViolation(String violationType, String severity) {
            complianceViolations.increment("type", violationType, "severity", severity);
        }
        
        public void recordSecurityEvent(String eventType, String severity) {
            securityEvents.increment("type", eventType, "severity", severity);
        }
        
        public void recordFailedAuthentication(String method, String reason) {
            failedAuthentications.increment("method", method, "reason", reason);
        }
        
        public void recordUnauthorizedAccess(String resource, String source) {
            unauthorizedAccess.increment("resource", resource, "source", source);
        }
        
        // Helper methods for gauge values
        
        private double getCurrentDataRetentionCompliance() {
            return 0.0; // Placeholder
        }
    }
    
    /**
     * Islamic Banking Specific Metrics
     */
    public static class IslamicBankingMetrics {
        private final MeterRegistry meterRegistry;
        
        // Murabaha metrics
        private final Counter murabahaContracts;
        private final Counter shariahApprovals;
        private final Counter shariahRejections;
        private final Timer shariahReviewTime;
        
        // Compliance metrics
        private final Counter shariahViolations;
        private final Gauge shariahComplianceRate;
        private final Counter assetDeliveries;
        
        public IslamicBankingMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // Initialize Murabaha metrics
            this.murabahaContracts = Counter.builder("banking.islamic.murabaha.contracts")
                .description("Total Murabaha contracts created")
                .register(meterRegistry);
            
            this.shariahApprovals = Counter.builder("banking.islamic.shariah.approvals")
                .description("Shariah board approvals")
                .register(meterRegistry);
            
            this.shariahRejections = Counter.builder("banking.islamic.shariah.rejections")
                .description("Shariah board rejections")
                .register(meterRegistry);
            
            this.shariahReviewTime = Timer.builder("banking.islamic.shariah.review.time")
                .description("Shariah review processing time")
                .register(meterRegistry);
            
            // Initialize compliance metrics
            this.shariahViolations = Counter.builder("banking.islamic.shariah.violations")
                .description("Shariah compliance violations")
                .register(meterRegistry);
            
            this.shariahComplianceRate = Gauge.builder("banking.islamic.shariah.compliance.rate")
                .description("Shariah compliance rate percentage")
                .register(meterRegistry, this, metrics -> getCurrentShariahComplianceRate());
            
            this.assetDeliveries = Counter.builder("banking.islamic.asset.deliveries")
                .description("Asset deliveries for Murabaha contracts")
                .register(meterRegistry);
        }
        
        // Public methods for recording metrics
        
        public void recordMurabahaContract(double contractValue) {
            murabahaContracts.increment("value_range", getAmountRange(contractValue));
        }
        
        public void recordShariahApproval(String contractType, Duration reviewTime) {
            shariahApprovals.increment("contract_type", contractType);
            shariahReviewTime.record(reviewTime);
        }
        
        public void recordShariahRejection(String contractType, String reason) {
            shariahRejections.increment("contract_type", contractType, "reason", reason);
        }
        
        public void recordShariahViolation(String violationType, String severity) {
            shariahViolations.increment("type", violationType, "severity", severity);
        }
        
        public void recordAssetDelivery(String assetType, double value) {
            assetDeliveries.increment("asset_type", assetType, "value_range", getAmountRange(value));
        }
        
        // Helper methods
        
        private double getCurrentShariahComplianceRate() {
            return 0.0; // Placeholder
        }
        
        private String getAmountRange(double amount) {
            if (amount < 10000) return "small";
            if (amount < 50000) return "medium";
            if (amount < 100000) return "large";
            return "xlarge";
        }
    }
}