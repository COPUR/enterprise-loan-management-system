package com.bank.loanmanagement.infrastructure.graphql.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL DTOs following clean architecture principles
 * These types map to the GraphQL schema while maintaining separation from domain
 */

// Customer Types
@Data
@Builder
class CustomerGraphQL {
    private String id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private AddressGraphQL address;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal usedCredit;
    private BigDecimal annualIncome;
    private String employmentStatus;
    private Integer creditScore;
    private String accountStatus;
    private String kycStatus;
    private String riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LoanGraphQL> loans;
    private List<PaymentGraphQL> payments;
    private RiskProfileGraphQL riskProfile;
}

@Data
@Builder
public class AddressGraphQL {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}

// Loan Types
@Data
@Builder
public class LoanGraphQL {
    private String id;
    private String loanId;
    private CustomerGraphQL customer;
    private BigDecimal loanAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal interestRate;
    private Integer installmentCount;
    private BigDecimal installmentAmount;
    private BigDecimal totalRepaymentAmount;
    private String loanType;
    private String purpose;
    private String status;
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime disbursementDate;
    private LocalDate maturityDate;
    private List<LoanInstallmentGraphQL> installments;
    private List<PaymentGraphQL> payments;
    private PaymentSummaryGraphQL paymentHistory;
    private LoanInstallmentGraphQL nextInstallment;
    private BigDecimal overdueAmount;
    private Integer daysOverdue;
}

@Data
@Builder
public class LoanInstallmentGraphQL {
    private String id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime paidDate;
    private BigDecimal paidAmount;
    private BigDecimal discountApplied;
    private BigDecimal penaltyApplied;
    private BigDecimal remainingAmount;
}

// Payment Types
@Data
@Builder
public class PaymentGraphQL {
    private String id;
    private String paymentId;
    private LoanGraphQL loan;
    private CustomerGraphQL customer;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String paymentReference;
    private String status;
    private BigDecimal processingFee;
    private BigDecimal totalAmount;
    private List<InstallmentPaymentGraphQL> installmentPayments;
}

@Data
@Builder
public class InstallmentPaymentGraphQL {
    private Integer installmentNumber;
    private BigDecimal paidAmount;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal penaltyPaid;
    private BigDecimal discountApplied;
}

// Analytics Types
@Data
@Builder
public class CustomerAnalyticsGraphQL {
    private String customerId;
    private Integer totalLoans;
    private Integer activeLoans;
    private BigDecimal totalBorrowed;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingAmount;
    private Double averagePaymentDelay;
    private Double creditUtilization;
    private Double riskScore;
    private PaymentReliabilityScoreGraphQL paymentReliability;
    private List<CustomerRecommendationGraphQL> recommendations;
}

@Data
@Builder
public class RiskAssessmentGraphQL {
    private String customerId;
    private Double overallRiskScore;
    private Double creditRisk;
    private Double incomeRisk;
    private Double behavioralRisk;
    private Double marketRisk;
    private List<String> riskFactors;
    private List<String> recommendations;
    private LocalDate nextReviewDate;
}

// System Health Types
@Data
@Builder
public class SystemHealthGraphQL {
    private String status;
    private LocalDateTime timestamp;
    private List<ServiceHealthGraphQL> services;
    private DatabaseHealthGraphQL database;
    private CacheHealthGraphQL cache;
    private List<CircuitBreakerStateGraphQL> circuitBreakers;
    private SystemMetricsGraphQL metrics;
}

@Data
@Builder
public class ServiceHealthGraphQL {
    private String serviceName;
    private String status;
    private Integer port;
    private Double responseTime;
    private Double errorRate;
}

@Data
@Builder
public class DatabaseHealthGraphQL {
    private String status;
    private Integer connectionCount;
    private Double responseTime;
}

@Data
@Builder
public class CacheHealthGraphQL {
    private String status;
    private Double hitRate;
    private Double memoryUsage;
}

@Data
@Builder
public class CircuitBreakerStateGraphQL {
    private String name;
    private String state;
    private Double failureRate;
    private Integer callsCount;
    private Integer failedCalls;
    private LocalDateTime lastFailureTime;
    private LocalDateTime nextAttemptTime;
}

@Data
@Builder
public class SystemMetricsGraphQL {
    private Double cpuUsage;
    private Double memoryUsage;
    private Integer activeConnections;
    private Double requestsPerSecond;
}

// Recommendation Types
@Data
@Builder
public class LoanRecommendationGraphQL {
    private String type;
    private String title;
    private String description;
    private String priority;
    private String impact;
    private Boolean actionRequired;
    private String estimatedBenefit;
    private String implementationEffort;
}

// Supporting Types
@Data
@Builder
public class RiskProfileGraphQL {
    private String overallRisk;
    private Double paymentHistory;
    private Double creditUtilization;
    private Double incomeStability;
    private LocalDateTime lastUpdated;
}

@Data
@Builder
public class PaymentSummaryGraphQL {
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private LocalDateTime lastPaymentDate;
    private LocalDate nextDueDate;
}

@Data
@Builder
public class PaymentReliabilityScoreGraphQL {
    private Double score;
    private Double onTimePercentage;
    private Double averageDelayDays;
    private Integer totalPayments;
}

@Data
@Builder
public class CustomerRecommendationGraphQL {
    private String type;
    private String description;
    private String priority;
}

// Connection Types for Pagination
@Data
@Builder
public class CustomerConnectionGraphQL {
    private List<CustomerGraphQL> nodes;
    private List<CustomerEdgeGraphQL> edges;
    private PageInfoGraphQL pageInfo;
    private Integer totalCount;
}

@Data
@Builder
public class CustomerEdgeGraphQL {
    private CustomerGraphQL node;
    private String cursor;
}

@Data
@Builder
public class LoanConnectionGraphQL {
    private List<LoanGraphQL> nodes;
    private List<LoanEdgeGraphQL> edges;
    private PageInfoGraphQL pageInfo;
    private Integer totalCount;
}

@Data
@Builder
public class LoanEdgeGraphQL {
    private LoanGraphQL node;
    private String cursor;
}

@Data
@Builder
public class PaymentConnectionGraphQL {
    private List<PaymentGraphQL> nodes;
    private List<PaymentEdgeGraphQL> edges;
    private PageInfoGraphQL pageInfo;
    private Integer totalCount;
}

@Data
@Builder
public class PaymentEdgeGraphQL {
    private PaymentGraphQL node;
    private String cursor;
}

@Data
@Builder
public class PageInfoGraphQL {
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
    private String startCursor;
    private String endCursor;
}