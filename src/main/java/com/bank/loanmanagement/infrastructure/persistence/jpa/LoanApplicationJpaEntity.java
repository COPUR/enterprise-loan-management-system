package com.bank.loanmanagement.infrastructure.persistence.jpa;

import com.bank.loanmanagement.domain.application.ApplicationPriority;
import com.bank.loanmanagement.domain.application.ApplicationStatus;
import com.bank.loanmanagement.domain.application.LoanType;
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

/**
 * JPA Infrastructure Entity for Loan Application
 * 
 * This class handles ONLY infrastructure concerns (persistence mapping).
 * Domain logic is kept in the pure domain model.
 * 
 * Architecture Compliance:
 * ✅ Hexagonal Architecture: Infrastructure adapter (JPA) separated from domain
 * ✅ Clean Code: Single responsibility for persistence mapping
 * ✅ DDD: Infrastructure layer properly separated from domain
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
public class LoanApplicationJpaEntity {
    
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanApplicationJpaEntity)) return false;
        LoanApplicationJpaEntity that = (LoanApplicationJpaEntity) o;
        return applicationId != null && applicationId.equals(that.applicationId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplicationJpaEntity{applicationId='%s', customerId=%d, loanType=%s, " +
                           "requestedAmount=%s, status=%s, priority=%s}",
                           applicationId, customerId, loanType, requestedAmount, status, priority);
    }
}