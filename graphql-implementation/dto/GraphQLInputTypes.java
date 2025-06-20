package com.bank.loanmanagement.infrastructure.graphql.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GraphQL Input Types for mutations and complex queries
 * Following clean architecture with proper input validation
 */

// Filter Input Types
@Data
public class CustomerFilterGraphQL {
    private String name;
    private String email;
    private IntRangeGraphQL creditScore;
    private String accountStatus;
    private String riskLevel;
    private DateRangeInputGraphQL dateRange;
}

@Data
public class LoanFilterGraphQL {
    private String customerId;
    private String loanType;
    private String status;
    private BigDecimalRangeGraphQL amountRange;
    private DateRangeInputGraphQL dateRange;
    private Boolean overdueOnly;
}

@Data
public class PaymentFilterGraphQL {
    private String customerId;
    private String loanId;
    private String status;
    private String paymentMethod;
    private DateRangeInputGraphQL dateRange;
}

// Pagination Input Types
@Data
public class PageInputGraphQL {
    private Integer page = 0;
    private Integer size = 20;
    private List<SortInputGraphQL> sort;
}

@Data
public class SortInputGraphQL {
    private String field;
    private String direction;
}

// Range Input Types
@Data
public class DateRangeInputGraphQL {
    private LocalDate start;
    private LocalDate end;
}

@Data
public class IntRangeGraphQL {
    private Integer min;
    private Integer max;
}

@Data
public class BigDecimalRangeGraphQL {
    private BigDecimal min;
    private BigDecimal max;
}

// Customer Input Types
@Data
public class CreateCustomerInputGraphQL {
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private AddressInputGraphQL address;
    private BigDecimal creditLimit;
    private BigDecimal annualIncome;
    private String employmentStatus;
    private String identificationNumber;
    private String identificationType;
}

@Data
public class UpdateCustomerInputGraphQL {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressInputGraphQL address;
    private BigDecimal annualIncome;
    private String employmentStatus;
}

@Data
public class AddressInputGraphQL {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}

// Loan Input Types
@Data
public class CreateLoanInputGraphQL {
    private String customerId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer installmentCount;
    private String loanType;
    private String purpose;
    private String collateralDetails;
    private String employmentDetails;
}

@Data
public class LoanApprovalInputGraphQL {
    private BigDecimal approvedAmount;
    private BigDecimal approvedRate;
    private List<String> conditions;
    private String notes;
}

@Data
public class LoanRejectionInputGraphQL {
    private String reason;
    private String notes;
}

// Payment Input Types
@Data
public class ProcessPaymentInputGraphQL {
    private String loanId;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private String paymentReference;
    private List<Integer> installmentNumbers;
    private String notes;
}

@Data
public class SchedulePaymentInputGraphQL {
    private String loanId;
    private BigDecimal paymentAmount;
    private LocalDateTime scheduledDate;
    private String paymentMethod;
    private Boolean recurring = false;
}

@Data
public class PaymentCalculationInputGraphQL {
    private String loanId;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentDate;
    private List<Integer> installmentNumbers;
    private Boolean simulateOnly = true;
}

// Credit Operations Input Types
@Data
public class CreditReservationInputGraphQL {
    private BigDecimal amount;
    private String purpose;
    private Integer timeoutMinutes = 5;
}

@Data
public class CreditReleaseInputGraphQL {
    private String reason;
    private BigDecimal actualAmountUsed;
}

// Bulk Operations Input Types
@Data
public class BulkPaymentInputGraphQL {
    private List<ProcessPaymentInputGraphQL> payments;
    private String batchId;
}

@Data
public class BulkLoanUpdateInputGraphQL {
    private List<String> loanIds;
    private String status;
    private String reason;
}

// SAGA Input Types
@Data
public class LoanCreationSagaInputGraphQL {
    private String customerId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer installmentCount;
    private String loanType;
    private String purpose;
    private Integer sagaTimeout = 300;
}

// Natural Language Processing Input Types
@Data
public class NLContextGraphQL {
    private String userId;
    private String customerId;
    private String sessionId;
    private String language = "en";
    private String domain = "GENERAL";
}