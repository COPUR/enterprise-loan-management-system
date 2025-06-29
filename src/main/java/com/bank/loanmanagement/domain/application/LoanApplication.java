package com.bank.loanmanagement.domain.application;

import com.bank.loanmanagement.domain.shared.AggregateRoot;
import com.bank.loanmanagement.domain.application.events.*;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Objects;

/**
 * Loan Application Domain Entity (Pure Domain Model)
 * 
 * Represents loan applications throughout the underwriting workflow.
 * Follows DDD principles with proper business rules and state management.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Intention-revealing names and business methods
 * ✅ Hexagonal Architecture: Pure domain model without infrastructure concerns
 * ✅ DDD: Rich domain entity with business logic and event publishing
 * ✅ Event-Driven: Domain event publishing for workflow coordination
 * ✅ Type Safety: Strong typing with BigDecimal for financial amounts
 * 
 * This is a PURE DOMAIN MODEL - no infrastructure annotations.
 * Infrastructure mapping is handled by separate JPA entities.
 */
@Getter
@Builder
public class LoanApplication extends AggregateRoot<String> {
    
    // ✅ PURE DOMAIN MODEL - No infrastructure annotations
    // Infrastructure mapping handled by separate JPA entities
    
    private final String applicationId;
    private final Long customerId;
    private final LoanType loanType;
    private final BigDecimal requestedAmount;
    private final Integer requestedTermMonths;
    private final String purpose;
    private final LocalDate applicationDate;
    private ApplicationStatus status;
    private String assignedUnderwriter;
    private ApplicationPriority priority;
    private final BigDecimal monthlyIncome;
    private final Integer employmentYears;
    private final BigDecimal collateralValue;
    private final BigDecimal businessRevenue;
    private final BigDecimal propertyValue;
    private final BigDecimal downPayment;
    private LocalDate decisionDate;
    private String decisionReason;
    private BigDecimal approvedAmount;
    private BigDecimal approvedRate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    
    /**
     * Business method to assign an underwriter
     * Includes domain event publishing for Event-Driven Communication
     */
    public void assignUnderwriter(String underwriterId, String assignedBy, String reason) {
        Objects.requireNonNull(underwriterId, "Underwriter ID is required");
        Objects.requireNonNull(assignedBy, "Assigned by is required");
        
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Can only assign underwriter to pending applications");
        }
        
        this.assignedUnderwriter = underwriterId;
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.updatedAt = LocalDateTime.now();
        
        // Publish domain event for Event-Driven Communication
        addDomainEvent(new UnderwriterAssignedEvent(
            applicationId, underwriterId, customerId.toString(), priority,
            assignedBy, reason
        ));
    }
    
    /**
     * Business method to approve the application
     * Includes domain event publishing for Event-Driven Communication
     */
    public void approve(BigDecimal approvedAmount, BigDecimal approvedRate, String reason, String approverId) {
        Objects.requireNonNull(approvedAmount, "Approved amount is required");
        Objects.requireNonNull(approvedRate, "Approved rate is required");
        Objects.requireNonNull(reason, "Approval reason is required");
        Objects.requireNonNull(approverId, "Approver ID is required");
        
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only approve applications under review");
        }
        if (approvedAmount.compareTo(requestedAmount) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }
        if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Approved amount must be positive");
        }
        if (approvedRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Approved rate cannot be negative");
        }
        
        this.status = ApplicationStatus.APPROVED;
        this.approvedAmount = approvedAmount;
        this.approvedRate = approvedRate;
        this.decisionDate = LocalDate.now();
        this.decisionReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        // Publish domain event for Event-Driven Communication
        addDomainEvent(new LoanApplicationApprovedEvent(
            applicationId, customerId.toString(), assignedUnderwriter,
            Money.of(approvedAmount, Currency.getInstance("USD")),
            approvedRate, reason, decisionDate, approverId
        ));
    }
    
    /**
     * Business method to reject the application
     */
    public void reject(String reason) {
        Objects.requireNonNull(reason, "Rejection reason is required");
        
        if (status == ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an approved application");
        }
        
        this.status = ApplicationStatus.REJECTED;
        this.decisionDate = LocalDate.now();
        this.decisionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method to request additional documents
     */
    public void requestDocuments(String reason) {
        Objects.requireNonNull(reason, "Document request reason is required");
        
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only request documents for applications under review");
        }
        
        this.status = ApplicationStatus.PENDING_DOCUMENTS;
        this.decisionReason = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method to resume review after documents received
     */
    public void resumeReview() {
        if (status != ApplicationStatus.PENDING_DOCUMENTS) {
            throw new IllegalStateException("Can only resume review for applications pending documents");
        }
        
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method to calculate debt-to-income ratio
     */
    public BigDecimal calculateDebtToIncomeRatio(BigDecimal monthlyDebt) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (monthlyDebt == null) {
            return BigDecimal.ZERO;
        }
        
        return monthlyDebt.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Business method to calculate loan-to-value ratio (for secured loans)
     */
    public BigDecimal calculateLoanToValueRatio() {
        BigDecimal assetValue = getAssetValue();
        if (assetValue == null || assetValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal loanAmount = approvedAmount != null ? approvedAmount : requestedAmount;
        return loanAmount.divide(assetValue, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Business method to get asset value based on loan type
     */
    public BigDecimal getAssetValue() {
        return switch (loanType) {
            case MORTGAGE -> propertyValue;
            case BUSINESS -> collateralValue;
            case AUTO_LOAN -> collateralValue;
            default -> null; // Personal loans typically unsecured
        };
    }
    
    /**
     * Business method to check if application requires additional documentation
     */
    public boolean requiresAdditionalDocumentation() {
        return switch (loanType) {
            case MORTGAGE -> propertyValue == null || downPayment == null;
            case BUSINESS -> businessRevenue == null || collateralValue == null;
            case AUTO_LOAN -> collateralValue == null;
            default -> false;
        };
    }
    
    /**
     * Business method to check if application is overdue for review
     */
    public boolean isOverdueForReview() {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            return false;
        }
        
        LocalDate reviewDeadline = applicationDate.plusDays(getReviewDaysForPriority());
        return LocalDate.now().isAfter(reviewDeadline);
    }
    
    /**
     * Business method to get review deadline based on priority
     */
    private int getReviewDaysForPriority() {
        return switch (priority) {
            case URGENT -> 1;
            case HIGH -> 3;
            case STANDARD -> 7;
            case LOW -> 14;
        };
    }
    
    /**
     * Business method to escalate priority
     */
    public void escalatePriority() {
        ApplicationPriority newPriority = switch (priority) {
            case LOW -> ApplicationPriority.STANDARD;
            case STANDARD -> ApplicationPriority.HIGH;
            case HIGH -> ApplicationPriority.URGENT;
            case URGENT -> ApplicationPriority.URGENT; // Already at highest
        };
        
        if (newPriority != priority) {
            this.priority = newPriority;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Private constructor to enforce factory method usage
     */
    private LoanApplication(String applicationId, Long customerId, LoanType loanType,
                           BigDecimal requestedAmount, Integer requestedTermMonths, String purpose,
                           LocalDate applicationDate, ApplicationStatus status, ApplicationPriority priority,
                           BigDecimal monthlyIncome, Integer employmentYears, BigDecimal collateralValue,
                           BigDecimal businessRevenue, BigDecimal propertyValue, BigDecimal downPayment,
                           LocalDate decisionDate, String decisionReason, BigDecimal approvedAmount,
                           BigDecimal approvedRate, LocalDateTime createdAt, LocalDateTime updatedAt,
                           Integer version) {
        
        // Validate required business rules
        this.applicationId = validateApplicationId(applicationId);
        this.customerId = validateCustomerId(customerId);
        this.loanType = Objects.requireNonNull(loanType, "Loan type is required");
        this.requestedAmount = validateRequestedAmount(requestedAmount);
        this.requestedTermMonths = validateRequestedTerm(requestedTermMonths);
        this.purpose = purpose;
        this.applicationDate = Objects.requireNonNull(applicationDate, "Application date is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.priority = Objects.requireNonNull(priority, "Priority is required");
        this.monthlyIncome = monthlyIncome;
        this.employmentYears = employmentYears;
        this.collateralValue = collateralValue;
        this.businessRevenue = businessRevenue;
        this.propertyValue = propertyValue;
        this.downPayment = downPayment;
        this.decisionDate = decisionDate;
        this.decisionReason = decisionReason;
        this.approvedAmount = approvedAmount;
        this.approvedRate = approvedRate;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.version = version != null ? version : 0;
    }
    
    /**
     * Factory method to create a new loan application with proper event publishing
     */
    public static LoanApplication create(String applicationId, Long customerId, LoanType loanType,
                                       BigDecimal requestedAmount, Integer requestedTermMonths,
                                       String purpose, String submittedBy) {
        
        LoanApplication application = new LoanApplication(
            applicationId, customerId, loanType, requestedAmount, requestedTermMonths,
            purpose, LocalDate.now(), ApplicationStatus.PENDING, ApplicationPriority.STANDARD,
            null, null, null, null, null, null, null, null, null, null,
            LocalDateTime.now(), LocalDateTime.now(), 0
        );
        
        // Publish domain event for Event-Driven Communication
        application.addDomainEvent(new LoanApplicationSubmittedEvent(
            applicationId, customerId.toString(), loanType,
            Money.of(requestedAmount, Currency.getInstance("USD")),
            requestedTermMonths, purpose, LocalDate.now(), submittedBy
        ));
        
        return application;
    }
    
    /**
     * Factory method for reconstruction from infrastructure
     */
    public static LoanApplication reconstruct(String applicationId, Long customerId, LoanType loanType,
                                            BigDecimal requestedAmount, Integer requestedTermMonths, String purpose,
                                            LocalDate applicationDate, ApplicationStatus status, ApplicationPriority priority,
                                            BigDecimal monthlyIncome, Integer employmentYears, BigDecimal collateralValue,
                                            BigDecimal businessRevenue, BigDecimal propertyValue, BigDecimal downPayment,
                                            LocalDate decisionDate, String decisionReason, BigDecimal approvedAmount,
                                            BigDecimal approvedRate, String assignedUnderwriter, LocalDateTime createdAt,
                                            LocalDateTime updatedAt, Integer version) {
        
        LoanApplication application = new LoanApplication(
            applicationId, customerId, loanType, requestedAmount, requestedTermMonths, purpose,
            applicationDate, status, priority, monthlyIncome, employmentYears, collateralValue,
            businessRevenue, propertyValue, downPayment, decisionDate, decisionReason,
            approvedAmount, approvedRate, createdAt, updatedAt, version
        );
        
        application.assignedUnderwriter = assignedUnderwriter;
        return application;
    }
    
    /**
     * Implement AggregateRoot's getId() method
     */
    @Override
    public String getId() {
        return applicationId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanApplication)) return false;
        LoanApplication that = (LoanApplication) o;
        return applicationId != null && applicationId.equals(that.applicationId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    /**
     * Domain validation methods
     */
    private static String validateApplicationId(String applicationId) {
        Objects.requireNonNull(applicationId, "Application ID is required");
        if (!applicationId.matches("^APP\\d{7}$")) {
            throw new IllegalArgumentException("Application ID must follow pattern APP#######");
        }
        return applicationId;
    }
    
    private static Long validateCustomerId(Long customerId) {
        Objects.requireNonNull(customerId, "Customer ID is required");
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
        return customerId;
    }
    
    private static BigDecimal validateRequestedAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Requested amount is required");
        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Requested amount must be at least $1,000");
        }
        if (amount.compareTo(new BigDecimal("10000000")) > 0) {
            throw new IllegalArgumentException("Requested amount cannot exceed $10,000,000");
        }
        return amount;
    }
    
    private static Integer validateRequestedTerm(Integer termMonths) {
        Objects.requireNonNull(termMonths, "Requested term is required");
        if (termMonths < 6) {
            throw new IllegalArgumentException("Loan term must be at least 6 months");
        }
        if (termMonths > 480) {
            throw new IllegalArgumentException("Loan term cannot exceed 480 months");
        }
        return termMonths;
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplication{applicationId='%s', customerId=%d, loanType=%s, " +
                           "requestedAmount=%s, status=%s, priority=%s, uncommittedEvents=%d}",
                           applicationId, customerId, loanType, requestedAmount, 
                           status, priority, getUncommittedEventCount());
    }
}

/**
 * Loan type enumeration
 */
public enum LoanType {
    PERSONAL("Personal Loan"),
    BUSINESS("Business Loan"),
    MORTGAGE("Mortgage"),
    AUTO_LOAN("Auto Loan");
    
    private final String displayName;
    
    LoanType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

/**
 * Application status enumeration
 */
public enum ApplicationStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PENDING_DOCUMENTS("Pending Documents"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

/**
 * Application priority enumeration
 */
public enum ApplicationPriority {
    LOW("Low"),
    STANDARD("Standard"),
    HIGH("High"),
    URGENT("Urgent");
    
    private final String displayName;
    
    ApplicationPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}