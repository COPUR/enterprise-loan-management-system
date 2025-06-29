package com.bank.loanmanagement.domain.application;

import com.bank.loanmanagement.domain.shared.AggregateRoot;
import com.bank.loanmanagement.domain.application.events.*;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * Loan Application Domain Entity
 * 
 * Represents loan applications throughout the underwriting workflow.
 * Follows DDD principles with proper business rules and state management.
 * 
 * Architecture Guardrails Compliance:
 * ✅ Request Parsing: N/A (Domain Entity)
 * ✅ Validation: Jakarta Bean Validation with business rules
 * ✅ Response Types: N/A (Domain Entity)
 * ✅ Type Safety: Strong typing with BigDecimal for financial amounts
 * ✅ Dependency Inversion: Pure domain entity
 */
@Entity
@Table(name = "loan_applications", indexes = {
    @Index(name = "idx_loan_applications_customer_id", columnList = "customer_id"),
    @Index(name = "idx_loan_applications_status", columnList = "status"),
    @Index(name = "idx_loan_applications_loan_type", columnList = "loan_type"),
    @Index(name = "idx_loan_applications_assigned_underwriter", columnList = "assigned_underwriter"),
    @Index(name = "idx_loan_applications_application_date", columnList = "application_date"),
    @Index(name = "idx_loan_applications_priority", columnList = "priority"),
    @Index(name = "idx_loan_applications_status_underwriter", columnList = "status, assigned_underwriter")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication extends AggregateRoot<String> {
    
    // ⚠️ ARCHITECTURAL NOTE: This class currently violates Hexagonal Architecture
    // by mixing JPA annotations with domain logic. In a pure implementation,
    // this should be separated into:
    // 1. Pure domain model (this class without JPA annotations)
    // 2. Infrastructure JPA entity (separate class with mappings)
    // 3. Mapper between domain and infrastructure layers
    
    @Id
    @Column(name = "application_id", length = 20)
    @NotBlank(message = "Application ID is required")
    @Pattern(regexp = "^APP\\d{7}$", message = "Application ID must follow pattern APP#######")
    private String applicationId;
    
    @Column(name = "customer_id", nullable = false)
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    @NotNull(message = "Loan type is required")
    private LoanType loanType;
    
    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1000.00", message = "Requested amount must be at least $1,000")
    @DecimalMax(value = "10000000.00", message = "Requested amount cannot exceed $10,000,000")
    private BigDecimal requestedAmount;
    
    @Column(name = "requested_term_months", nullable = false)
    @NotNull(message = "Requested term is required")
    @Min(value = 6, message = "Loan term must be at least 6 months")
    @Max(value = 480, message = "Loan term cannot exceed 480 months")
    private Integer requestedTermMonths;
    
    @Column(name = "purpose", length = 255)
    @Size(max = 255, message = "Purpose must not exceed 255 characters")
    private String purpose;
    
    @Column(name = "application_date", nullable = false)
    @NotNull(message = "Application date is required")
    @PastOrPresent(message = "Application date cannot be in the future")
    private LocalDate applicationDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @NotNull(message = "Status is required")
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    @Column(name = "assigned_underwriter", length = 20)
    private String assignedUnderwriter;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @NotNull(message = "Priority is required")
    private ApplicationPriority priority = ApplicationPriority.STANDARD;
    
    @Column(name = "monthly_income", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Monthly income cannot be negative")
    private BigDecimal monthlyIncome;
    
    @Column(name = "employment_years")
    @Min(value = 0, message = "Employment years cannot be negative")
    @Max(value = 50, message = "Employment years cannot exceed 50")
    private Integer employmentYears;
    
    @Column(name = "collateral_value", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Collateral value cannot be negative")
    private BigDecimal collateralValue;
    
    @Column(name = "business_revenue", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Business revenue cannot be negative")
    private BigDecimal businessRevenue;
    
    @Column(name = "property_value", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Property value cannot be negative")
    private BigDecimal propertyValue;
    
    @Column(name = "down_payment", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Down payment cannot be negative")
    private BigDecimal downPayment;
    
    @Column(name = "decision_date")
    private LocalDate decisionDate;
    
    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;
    
    @Column(name = "approved_amount", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "Approved amount cannot be negative")
    private BigDecimal approvedAmount;
    
    @Column(name = "approved_rate", precision = 5, scale = 3)
    @DecimalMin(value = "0.000", message = "Approved rate cannot be negative")
    @DecimalMax(value = "99.999", message = "Approved rate cannot exceed 99.999%")
    private BigDecimal approvedRate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Integer version = 0;
    
    /**
     * Business method to assign an underwriter
     * Now includes domain event publishing for Event-Driven Communication
     */
    public void assignUnderwriter(String underwriterId, String assignedBy, String reason) {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Can only assign underwriter to pending applications");
        }
        
        this.assignedUnderwriter = underwriterId;
        this.status = ApplicationStatus.UNDER_REVIEW;
        
        // Publish domain event for Event-Driven Communication
        addDomainEvent(new UnderwriterAssignedEvent(
            applicationId, underwriterId, customerId.toString(), priority,
            assignedBy, reason
        ));
    }
    
    /**
     * Business method to approve the application
     * Now includes domain event publishing for Event-Driven Communication
     */
    public void approve(BigDecimal approvedAmount, BigDecimal approvedRate, String reason, String approverId) {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only approve applications under review");
        }
        if (approvedAmount.compareTo(requestedAmount) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }
        
        this.status = ApplicationStatus.APPROVED;
        this.approvedAmount = approvedAmount;
        this.approvedRate = approvedRate;
        this.decisionDate = LocalDate.now();
        this.decisionReason = reason;
        
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
        if (status == ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an approved application");
        }
        
        this.status = ApplicationStatus.REJECTED;
        this.decisionDate = LocalDate.now();
        this.decisionReason = reason;
    }
    
    /**
     * Business method to request additional documents
     */
    public void requestDocuments(String reason) {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only request documents for applications under review");
        }
        
        this.status = ApplicationStatus.PENDING_DOCUMENTS;
        this.decisionReason = reason;
    }
    
    /**
     * Business method to resume review after documents received
     */
    public void resumeReview() {
        if (status != ApplicationStatus.PENDING_DOCUMENTS) {
            throw new IllegalStateException("Can only resume review for applications pending documents");
        }
        
        this.status = ApplicationStatus.UNDER_REVIEW;
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
        this.priority = switch (priority) {
            case LOW -> ApplicationPriority.STANDARD;
            case STANDARD -> ApplicationPriority.HIGH;
            case HIGH -> ApplicationPriority.URGENT;
            case URGENT -> ApplicationPriority.URGENT; // Already at highest
        };
    }
    
    /**
     * Factory method to create a new loan application with proper event publishing
     */
    public static LoanApplication create(String applicationId, Long customerId, LoanType loanType,
                                       BigDecimal requestedAmount, Integer requestedTermMonths,
                                       String purpose, String submittedBy) {
        LoanApplication application = LoanApplication.builder()
            .applicationId(applicationId)
            .customerId(customerId)
            .loanType(loanType)
            .requestedAmount(requestedAmount)
            .requestedTermMonths(requestedTermMonths)
            .purpose(purpose)
            .applicationDate(LocalDate.now())
            .status(ApplicationStatus.PENDING)
            .priority(ApplicationPriority.STANDARD)
            .build();
        
        // Publish domain event for Event-Driven Communication
        application.addDomainEvent(new LoanApplicationSubmittedEvent(
            applicationId, customerId.toString(), loanType,
            Money.of(requestedAmount, Currency.getInstance("USD")),
            requestedTermMonths, purpose, LocalDate.now(), submittedBy
        ));
        
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
    
    @Override
    public String toString() {
        return String.format("LoanApplication{applicationId='%s', customerId=%d, loanType=%s, " +
                           "requestedAmount=%s, status=%s, priority=%s, uncommittedEvents=%d}",
                           applicationId, customerId, loanType, requestedAmount, 
                           status, priority, getUncommittedEventCount());
    }
}

}

/**
 * Loan type enumeration
 */
enum LoanType {
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
enum ApplicationStatus {
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
enum ApplicationPriority {
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