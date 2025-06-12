package com.bank.loanmanagement.graphql.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Connection types for pagination
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConnection {
    private List<Object> nodes;
    private List<CustomerEdge> edges;
    private PageInfo pageInfo;
    private Integer totalCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEdge {
    private Object node;
    private String cursor;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
    private String startCursor;
    private String endCursor;
}

// Natural Language Processing DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NLQueryResult {
    private String query;
    private QueryIntent intent;
    private List<QueryEntity> entities;
    private Object result;
    private Float confidence;
    private List<String> suggestions;
    private Float executionTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryEntity {
    private EntityType type;
    private String value;
    private Float confidence;
    private Integer position;
}

// Enums for GraphQL schema
public enum QueryIntent {
    SEARCH, ANALYTICS, RECOMMENDATION, TRANSACTION, REPORT, HELP
}

public enum EntityType {
    CUSTOMER, LOAN, PAYMENT, DATE, AMOUNT, PERCENTAGE, STATUS
}

public enum RecommendationType {
    CREDIT_INCREASE, LOAN_RESTRUCTURE, EARLY_PAYMENT, RISK_MITIGATION, 
    PRODUCT_RECOMMENDATION, PROCESS_IMPROVEMENT
}

public enum RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

public enum ImplementationEffort {
    LOW, MEDIUM, HIGH
}

public enum AnalyticsPeriod {
    LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, LAST_YEAR, CUSTOM
}

public enum QueryDomain {
    GENERAL, CUSTOMER_SERVICE, LOAN_MANAGEMENT, PAYMENT_PROCESSING, 
    ANALYTICS, RISK_MANAGEMENT
}

public enum AlertSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

public enum HealthStatus {
    UP, DOWN, DEGRADED, UNKNOWN
}

public enum CircuitBreakerStatus {
    CLOSED, OPEN, HALF_OPEN
}

// Analytics DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalytics {
    private String customerId;
    private Integer totalLoans;
    private Integer activeLoans;
    private BigDecimal totalBorrowed;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingAmount;
    private Float averagePaymentDelay;
    private Float creditUtilization;
    private Float riskScore;
    private PaymentReliabilityScore paymentReliability;
    private List<CustomerRecommendation> recommendations;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAnalytics {
    private AnalyticsPeriod period;
    private Integer totalLoansCreated;
    private BigDecimal totalLoanAmount;
    private BigDecimal averageLoanAmount;
    private Float approvalRate;
    private Float defaultRate;
    private List<LoanTypeMetric> loanTypeDistribution;
    private List<InterestRateMetric> interestRateDistribution;
    private List<GeographicMetric> geographicDistribution;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAnalytics {
    private AnalyticsPeriod period;
    private Integer totalPayments;
    private BigDecimal totalPaymentAmount;
    private BigDecimal averagePaymentAmount;
    private Float onTimePaymentRate;
    private Float earlyPaymentRate;
    private Float latePaymentRate;
    private List<PaymentMethodMetric> paymentMethodDistribution;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {
    private String customerId;
    private Float overallRiskScore;
    private Float creditRisk;
    private Float incomeRisk;
    private Float behavioralRisk;
    private Float marketRisk;
    private List<RiskFactor> riskFactors;
    private List<RiskRecommendation> recommendations;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextReviewDate;
}

// Supporting DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReliabilityScore {
    private Float score;
    private Float onTimePercentage;
    private Float averageDelayDays;
    private Integer totalPayments;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRecommendation {
    private String type;
    private String description;
    private RecommendationPriority priority;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanTypeMetric {
    private String loanType;
    private Integer count;
    private BigDecimal totalAmount;
    private Float percentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateMetric {
    private String rateRange;
    private Integer count;
    private BigDecimal averageRate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeographicMetric {
    private String location;
    private Integer count;
    private BigDecimal totalAmount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodMetric {
    private String method;
    private Integer count;
    private BigDecimal totalAmount;
    private Float percentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactor {
    private String factor;
    private Float impact;
    private String description;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskRecommendation {
    private String action;
    private String description;
    private RecommendationPriority urgency;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private RecommendationType type;
    private String title;
    private String description;
    private RecommendationPriority priority;
    private String impact;
    private Boolean actionRequired;
    private String estimatedBenefit;
    private ImplementationEffort implementationEffort;
}

// System Health DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealth {
    private HealthStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private List<ServiceHealth> services;
    private DatabaseHealth database;
    private CacheHealth cache;
    private List<CircuitBreakerState> circuitBreakers;
    private SystemMetrics metrics;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealth {
    private String serviceName;
    private HealthStatus status;
    private Integer port;
    private Float responseTime;
    private Float errorRate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseHealth {
    private HealthStatus status;
    private Integer connectionCount;
    private Float responseTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheHealth {
    private HealthStatus status;
    private Float hitRate;
    private Float memoryUsage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerState {
    private String name;
    private CircuitBreakerStatus state;
    private Float failureRate;
    private Integer callsCount;
    private Integer failedCalls;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastFailureTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextAttemptTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {
    private Float cpuUsage;
    private Float memoryUsage;
    private Integer activeConnections;
    private Float requestsPerSecond;
}

// Business Configuration DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRulesConfig {
    private List<Integer> allowedInstallmentCounts;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private BigDecimal earlyPaymentDiscountRate;
    private BigDecimal latePaymentPenaltyRate;
    private BigDecimal maxLoanAmount;
    private Integer minCreditScore;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateConfig {
    private String loanType;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private BigDecimal baseRate;
    private BigDecimal riskAdjustment;
}

// Credit Transaction DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransaction {
    private Long id;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}

// Risk Profile DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskProfile {
    private String overallRisk;
    private Float paymentHistory;
    private Float creditUtilization;
    private Float incomeStability;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
}

// Real-time update DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatusUpdate {
    private String loanId;
    private String customerId;
    private String oldStatus;
    private String newStatus;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String reason;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotification {
    private String paymentId;
    private String loanId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaProgressUpdate {
    private String sagaId;
    private String currentStep;
    private String status;
    private Float progress;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedCompletion;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAlert {
    private String id;
    private AlertSeverity severity;
    private String message;
    private String component;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private Boolean resolved;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerUpdate {
    private String name;
    private CircuitBreakerStatus oldState;
    private CircuitBreakerStatus newState;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}