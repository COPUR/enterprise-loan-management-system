package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Domain entity representing a loan application with full business logic
 */
@Entity
@Table(name = "loan_applications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {
    
    @Id
    private String applicationId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;
    
    @Column(name = "requested_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal requestedAmount;
    
    @Column(name = "requested_term_months", nullable = false)
    private Integer requestedTermMonths;
    
    @Column(name = "purpose", nullable = false)
    private String purpose;
    
    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private ApplicationPriority priority;
    
    // Financial information
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "employment_years")
    private Integer employmentYears;
    
    @Column(name = "collateral_value", precision = 15, scale = 2)
    private BigDecimal collateralValue;
    
    @Column(name = "business_revenue", precision = 15, scale = 2)
    private BigDecimal businessRevenue;
    
    @Column(name = "property_value", precision = 15, scale = 2)
    private BigDecimal propertyValue;
    
    @Column(name = "down_payment", precision = 15, scale = 2)
    private BigDecimal downPayment;
    
    // Decision information
    @Column(name = "decision_date")
    private LocalDate decisionDate;
    
    @Column(name = "decision_reason")
    @Setter
    private String decisionReason;
    
    @Column(name = "approved_amount", precision = 15, scale = 2)
    @Setter
    private BigDecimal approvedAmount;
    
    @Column(name = "approved_rate", precision = 8, scale = 4)
    @Setter
    private BigDecimal approvedRate;
    
    @Column(name = "assigned_underwriter")
    @Setter
    private String assignedUnderwriter;
    
    // Audit fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Setter
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    @Version
    private Long version;
    
    // Domain events (transient)
    @Transient
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    private static final Pattern APPLICATION_ID_PATTERN = Pattern.compile("^APP\\d{7}$");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");
    private static final Integer MIN_TERM_MONTHS = 6;
    private static final Integer MAX_TERM_MONTHS = 480;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Factory method to create a new loan application
     */
    public static LoanApplication create(String applicationId, Long customerId, LoanType loanType,
                                       BigDecimal requestedAmount, Integer requestedTermMonths,
                                       String purpose, String submittedBy) {
        // Validation
        validateApplicationId(applicationId);
        validateCustomerId(customerId);
        validateRequestedAmount(requestedAmount);
        validateRequestedTerm(requestedTermMonths);
        Objects.requireNonNull(loanType, "Loan type cannot be null");
        Objects.requireNonNull(purpose, "Purpose cannot be null");
        Objects.requireNonNull(submittedBy, "Submitted by cannot be null");
        
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
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .version(0L)
            .build();
        
        // Publish domain event
        application.addEvent(new LoanApplicationSubmittedEvent(
            applicationId, customerId.toString(), loanType, requestedAmount, LocalDateTime.now()
        ));
        
        return application;
    }
    
    /**
     * Reconstruct application from infrastructure data
     */
    public static LoanApplication reconstruct(String applicationId, Long customerId, LoanType loanType,
                                            BigDecimal requestedAmount, Integer requestedTermMonths,
                                            String purpose, LocalDate applicationDate, ApplicationStatus status,
                                            ApplicationPriority priority, BigDecimal monthlyIncome,
                                            Integer employmentYears, BigDecimal collateralValue,
                                            BigDecimal businessRevenue, BigDecimal propertyValue,
                                            BigDecimal downPayment, LocalDate decisionDate,
                                            String decisionReason, BigDecimal approvedAmount,
                                            BigDecimal approvedRate, String assignedUnderwriter,
                                            LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        return LoanApplication.builder()
            .applicationId(applicationId)
            .customerId(customerId)
            .loanType(loanType)
            .requestedAmount(requestedAmount)
            .requestedTermMonths(requestedTermMonths)
            .purpose(purpose)
            .applicationDate(applicationDate)
            .status(status)
            .priority(priority)
            .monthlyIncome(monthlyIncome)
            .employmentYears(employmentYears)
            .collateralValue(collateralValue)
            .businessRevenue(businessRevenue)
            .propertyValue(propertyValue)
            .downPayment(downPayment)
            .decisionDate(decisionDate)
            .decisionReason(decisionReason)
            .approvedAmount(approvedAmount)
            .approvedRate(approvedRate)
            .assignedUnderwriter(assignedUnderwriter)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .version(version)
            .build();
    }
    
    /**
     * Assign underwriter to application
     */
    public void assignUnderwriter(String underwriterId, String assignedBy, String reason) {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Can only assign underwriter to pending applications");
        }
        
        this.assignedUnderwriter = underwriterId;
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.updatedAt = LocalDateTime.now();
        
        addEvent(new UnderwriterAssignedEvent(applicationId, underwriterId, assignedBy, reason, LocalDateTime.now()));
    }
    
    /**
     * Approve the application
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
        this.updatedAt = LocalDateTime.now();
        
        addEvent(new LoanApplicationApprovedEvent(applicationId, customerId.toString(), approvedAmount, 
                                                approvedRate, approverId, LocalDateTime.now()));
    }
    
    /**
     * Reject the application
     */
    public void reject(String rejectionReason) {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only reject applications under review");
        }
        
        this.status = ApplicationStatus.REJECTED;
        this.decisionDate = LocalDate.now();
        this.decisionReason = rejectionReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Request additional documents
     */
    public void requestDocuments(String documentReason) {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Can only request documents for applications under review");
        }
        
        this.status = ApplicationStatus.PENDING_DOCUMENTS;
        this.decisionReason = documentReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Resume review after documents received
     */
    public void resumeReview() {
        if (status != ApplicationStatus.PENDING_DOCUMENTS) {
            throw new IllegalStateException("Can only resume review for applications pending documents");
        }
        
        this.status = ApplicationStatus.UNDER_REVIEW;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Escalate application priority
     */
    public void escalatePriority() {
        this.priority = priority.escalate();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate debt-to-income ratio
     */
    public BigDecimal calculateDebtToIncomeRatio(BigDecimal monthlyDebt) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Monthly income is required for DTI calculation");
        }
        return monthlyDebt.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate loan-to-value ratio
     */
    public BigDecimal calculateLoanToValueRatio() {
        BigDecimal assetValue = getAssetValue();
        if (assetValue == null || assetValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return requestedAmount.divide(assetValue, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Get asset value based on loan type
     */
    public BigDecimal getAssetValue() {
        return switch (loanType) {
            case MORTGAGE, HOME_EQUITY -> propertyValue;
            case BUSINESS, AUTO -> collateralValue;
            case PERSONAL, EDUCATION -> null; // Unsecured loans
            default -> collateralValue;
        };
    }
    
    /**
     * Check if additional documentation is required
     */
    public boolean requiresAdditionalDocumentation() {
        return switch (loanType) {
            case MORTGAGE -> propertyValue == null || downPayment == null;
            case BUSINESS -> businessRevenue == null || collateralValue == null;
            case AUTO -> collateralValue == null;
            default -> false;
        };
    }
    
    /**
     * Check if application is overdue for review
     */
    public boolean isOverdueForReview() {
        if (status != ApplicationStatus.UNDER_REVIEW) {
            return false;
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(priority.getTargetProcessingDays());
        return applicationDate.isBefore(cutoffDate);
    }
    
    /**
     * Get ID for aggregate root
     */
    public String getId() {
        return applicationId;
    }
    
    /**
     * Domain event management
     */
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public int getUncommittedEventCount() {
        return uncommittedEvents.size();
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    private void addEvent(DomainEvent event) {
        uncommittedEvents.add(event);
    }
    
    // Validation methods
    private static void validateApplicationId(String applicationId) {
        if (applicationId == null || !APPLICATION_ID_PATTERN.matcher(applicationId).matches()) {
            throw new IllegalArgumentException("Application ID must follow pattern APP#######");
        }
    }
    
    private static void validateCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }
    }
    
    private static void validateRequestedAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Requested amount must be at least $1,000");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Requested amount cannot exceed $10,000,000");
        }
    }
    
    private static void validateRequestedTerm(Integer termMonths) {
        if (termMonths == null || termMonths < MIN_TERM_MONTHS) {
            throw new IllegalArgumentException("Loan term must be at least 6 months");
        }
        if (termMonths > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("Loan term cannot exceed 480 months");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApplication that = (LoanApplication) o;
        return Objects.equals(applicationId, that.applicationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(applicationId);
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplication{applicationId='%s', customerId=%s, loanType=%s, " +
                           "requestedAmount=%s, status=%s, priority=%s, uncommittedEvents=%d}",
                           applicationId, customerId, loanType, requestedAmount, status, priority, 
                           uncommittedEvents.size());
    }
}