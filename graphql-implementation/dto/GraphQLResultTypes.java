package com.bank.loanmanagement.infrastructure.graphql.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GraphQL Result Types for mutations and complex operations
 * Following clean architecture with proper error handling
 */

// Customer Result Types
interface CustomerMutationResultGraphQL {}

@Data
@Builder
class CustomerSuccessGraphQL implements CustomerMutationResultGraphQL {
    private CustomerGraphQL customer;
    private String message;
}

@Data
@Builder
class CustomerErrorGraphQL implements CustomerMutationResultGraphQL {
    private String message;
    private String code;
    private String field;
}

// Loan Result Types
interface LoanMutationResultGraphQL {}

@Data
@Builder
class LoanSuccessGraphQL implements LoanMutationResultGraphQL {
    private LoanGraphQL loan;
    private String message;
}

@Data
@Builder
class LoanErrorGraphQL implements LoanMutationResultGraphQL {
    private String message;
    private String code;
    private String field;
}

// Payment Result Types
interface PaymentMutationResultGraphQL {}

@Data
@Builder
class PaymentSuccessGraphQL implements PaymentMutationResultGraphQL {
    private PaymentGraphQL payment;
    private String message;
}

@Data
@Builder
class PaymentErrorGraphQL implements PaymentMutationResultGraphQL {
    private String message;
    private String code;
    private String field;
}

// SAGA Result Types
interface SagaMutationResultGraphQL {}

@Data
@Builder
class SagaSuccessGraphQL implements SagaMutationResultGraphQL {
    private String sagaId;
    private String status;
    private String message;
}

@Data
@Builder
class SagaErrorGraphQL implements SagaMutationResultGraphQL {
    private String message;
    private String code;
    private String sagaId;
}

// Credit Operation Result Types
@Data
@Builder
class CreditReservationResultGraphQL {
    private String reservationId;
    private BigDecimal amount;
    private LocalDateTime expiresAt;
    private Boolean success;
    private String message;
}

@Data
@Builder
class CreditReleaseResultGraphQL {
    private String reservationId;
    private BigDecimal releasedAmount;
    private Boolean success;
    private String message;
}

// Bulk Operation Result Types
@Data
@Builder
class BulkPaymentResultGraphQL {
    private String batchId;
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private java.util.List<PaymentMutationResultGraphQL> results;
}

@Data
@Builder
class BulkLoanUpdateResultGraphQL {
    private String batchId;
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private java.util.List<LoanMutationResultGraphQL> results;
}

// Payment Calculation Result Types
@Data
@Builder
class PaymentCalculationGraphQL {
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal finalAmount;
    private Integer earlyPaymentDays;
    private Integer latePaymentDays;
    private java.util.List<InstallmentCalculationGraphQL> installmentBreakdown;
}

@Data
@Builder
class InstallmentCalculationGraphQL {
    private Integer installmentNumber;
    private BigDecimal originalAmount;
    private BigDecimal discountApplied;
    private BigDecimal penaltyApplied;
    private BigDecimal amountToPay;
}

@Data
@Builder
class PaymentCalculationResultGraphQL {
    private PaymentCalculationGraphQL calculation;
    private PaymentAdviceGraphQL paymentAdvice;
}

@Data
@Builder
class PaymentAdviceGraphQL {
    private BigDecimal recommendedAmount;
    private BigDecimal savingsOpportunity;
    private PaymentWindowGraphQL paymentWindow;
}

@Data
@Builder
class PaymentWindowGraphQL {
    private java.time.LocalDate earlyPaymentUntil;
    private java.time.LocalDate gracePeriodUntil;
    private java.time.LocalDate penaltyStartsFrom;
}

// Natural Language Query Result Types
@Data
@Builder
class NLQueryResultGraphQL {
    private String query;
    private String intent;
    private java.util.List<QueryEntityGraphQL> entities;
    private String result;
    private Double confidence;
    private java.util.List<String> suggestions;
    private Double executionTime;
}

@Data
@Builder
class QueryEntityGraphQL {
    private String type;
    private String value;
    private Double confidence;
    private Integer position;
}

// Configuration Result Types
@Data
@Builder
class BusinessRulesConfigGraphQL {
    private java.util.List<Integer> allowedInstallmentCounts;
    private BigDecimal minInterestRate;
    private BigDecimal maxInterestRate;
    private BigDecimal earlyPaymentDiscountRate;
    private BigDecimal latePaymentPenaltyRate;
    private BigDecimal maxLoanAmount;
    private Integer minCreditScore;
}

@Data
@Builder
class InterestRateConfigGraphQL {
    private String loanType;
    private BigDecimal minRate;
    private BigDecimal maxRate;
    private BigDecimal baseRate;
    private BigDecimal riskAdjustment;
}