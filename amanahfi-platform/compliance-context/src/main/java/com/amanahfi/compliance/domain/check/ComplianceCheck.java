package com.amanahfi.compliance.domain.check;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compliance Check Aggregate Root
 * 
 * Manages compliance verification for:
 * - AML (Anti-Money Laundering) checks following CBUAE regulations
 * - Sharia compliance for Islamic finance contracts
 * - Transaction monitoring and risk assessment
 * - KYC (Know Your Customer) validation
 */
@Entity
@Table(name = "compliance_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComplianceCheck {

    // Business Constants
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00"); // 10K AED
    private static final BigDecimal ENHANCED_DUE_DILIGENCE_THRESHOLD = new BigDecimal("50000.00"); // 50K AED
    
    @Id
    private String checkId;

    @NotBlank
    private String entityId; // Customer ID, Contract ID, Transaction ID, etc.

    @NotNull
    @Enumerated(EnumType.STRING)
    private CheckType checkType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ComplianceType complianceType;

    @NotBlank
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CheckStatus status;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime screeningCompletedAt;
    private LocalDateTime archivedAt;

    // Assignment and Processing
    private String assignedOfficerId;
    private String assignmentNotes;
    private String completionNotes;
    private String rejectionReason;
    private String archiveReason;

    // Automation and Review
    private boolean automated;
    private boolean requiresManualReview;
    private boolean requiresShariahBoardReview;

    // Risk Assessment
    @Enumerated(EnumType.STRING)
    private RiskScore finalRiskScore;

    // Transaction-specific fields
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "transaction_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "transaction_currency"))
    })
    private Money transactionAmount;

    // AML Screening
    @Embedded
    private AmlScreeningResult amlScreeningResult;

    // Violations
    @OneToMany(mappedBy = "complianceCheck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ComplianceViolation> violations = new ArrayList<>();

    // Archive status
    private boolean archived = false;

    // Domain Events
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates an AML compliance check
     */
    public static ComplianceCheck createAmlCheck(String entityId, CheckType checkType, String reason) {
        ComplianceCheck check = new ComplianceCheck();
        check.checkId = generateCheckId();
        check.entityId = entityId;
        check.checkType = checkType;
        check.complianceType = ComplianceType.AML;
        check.reason = reason;
        check.status = CheckStatus.PENDING;
        check.createdAt = LocalDateTime.now();
        check.automated = true; // AML checks are typically automated
        check.requiresManualReview = false;
        check.requiresShariahBoardReview = false;
        
        check.addDomainEvent(new ComplianceCheckCreatedEvent(check.checkId, entityId, ComplianceType.AML));
        
        return check;
    }

    /**
     * Creates a Sharia compliance check
     */
    public static ComplianceCheck createShariahCheck(String entityId, CheckType checkType, String reason) {
        ComplianceCheck check = new ComplianceCheck();
        check.checkId = generateCheckId();
        check.entityId = entityId;
        check.checkType = checkType;
        check.complianceType = ComplianceType.SHARIA;
        check.reason = reason;
        check.status = CheckStatus.PENDING;
        check.createdAt = LocalDateTime.now();
        check.automated = false; // Sharia checks require human review
        check.requiresManualReview = true;
        check.requiresShariahBoardReview = true;
        
        check.addDomainEvent(new ComplianceCheckCreatedEvent(check.checkId, entityId, ComplianceType.SHARIA));
        
        return check;
    }

    /**
     * Creates a transaction monitoring check
     */
    public static ComplianceCheck createTransactionCheck(String transactionId, Money amount, String reason) {
        ComplianceCheck check = new ComplianceCheck();
        check.checkId = generateCheckId();
        check.entityId = transactionId;
        check.checkType = CheckType.TRANSACTION_MONITORING;
        check.complianceType = ComplianceType.AML;
        check.reason = reason;
        check.status = CheckStatus.PENDING;
        check.createdAt = LocalDateTime.now();
        check.transactionAmount = amount;
        check.automated = true;
        check.requiresManualReview = check.isHighValueTransaction();
        check.requiresShariahBoardReview = false;
        
        check.addDomainEvent(new TransactionMonitoringStartedEvent(check.checkId, transactionId, amount));
        
        return check;
    }

    /**
     * Performs automated AML screening
     */
    public void performAutomatedScreening(AmlScreeningResult screeningResult) {
        if (status != CheckStatus.PENDING) {
            throw new IllegalStateException("Can only perform screening on pending checks");
        }
        
        this.amlScreeningResult = screeningResult;
        this.screeningCompletedAt = LocalDateTime.now();
        
        if (screeningResult.isHighRisk()) {
            this.status = CheckStatus.REQUIRES_REVIEW;
            this.requiresManualReview = true;
        } else {
            this.status = CheckStatus.IN_PROGRESS;
        }
        
        addDomainEvent(new AmlScreeningCompletedEvent(checkId, screeningResult.getRiskScore()));
    }

    /**
     * Assigns check to compliance officer for manual review
     */
    public void assignToOfficer(String officerId, String assignmentNotes) {
        if (status != CheckStatus.REQUIRES_REVIEW && status != CheckStatus.PENDING) {
            throw new IllegalStateException("Check must be pending or require review to be assigned");
        }
        
        this.status = CheckStatus.UNDER_INVESTIGATION;
        this.assignedOfficerId = officerId;
        this.assignmentNotes = assignmentNotes;
        this.assignedAt = LocalDateTime.now();
        
        addDomainEvent(new ComplianceCheckAssignedEvent(checkId, officerId));
    }

    /**
     * Completes check with approval
     */
    public void complete(String officerId, String completionNotes, RiskScore finalRiskScore) {
        if (status == CheckStatus.APPROVED || status == CheckStatus.REJECTED) {
            throw new IllegalStateException("Check is already completed");
        }
        
        this.status = CheckStatus.APPROVED;
        this.completionNotes = completionNotes;
        this.finalRiskScore = finalRiskScore;
        this.completedAt = LocalDateTime.now();
        
        addDomainEvent(new ComplianceCheckCompletedEvent(checkId, CheckStatus.APPROVED, finalRiskScore));
    }

    /**
     * Rejects check with reason
     */
    public void reject(String officerId, String rejectionReason) {
        if (status == CheckStatus.APPROVED || status == CheckStatus.REJECTED) {
            throw new IllegalStateException("Check is already completed");
        }
        
        this.status = CheckStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.rejectedAt = LocalDateTime.now();
        
        addDomainEvent(new ComplianceCheckRejectedEvent(checkId, rejectionReason));
    }

    /**
     * Adds a compliance violation
     */
    public void addViolation(ComplianceViolation violation) {
        violation.assignToCheck(this);
        this.violations.add(violation);
        
        addDomainEvent(new ComplianceViolationDetectedEvent(checkId, violation.getViolationType()));
    }

    /**
     * Archives completed check
     */
    public void archive(String archiveReason) {
        if (status != CheckStatus.APPROVED && status != CheckStatus.REJECTED) {
            throw new IllegalStateException("Only completed checks can be archived");
        }
        
        this.archived = true;
        this.archiveReason = archiveReason;
        this.archivedAt = LocalDateTime.now();
        
        addDomainEvent(new ComplianceCheckArchivedEvent(checkId, archiveReason));
    }

    // Business Logic Methods

    public boolean isHighValueTransaction() {
        return transactionAmount != null && 
               transactionAmount.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0;
    }

    public boolean requiresEnhancedDueDiligence() {
        return isHighValueTransaction() || requiresManualReview;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public SeverityLevel getHighestSeverityLevel() {
        return violations.stream()
            .map(ComplianceViolation::getSeverityLevel)
            .max((s1, s2) -> Integer.compare(s1.getLevel(), s2.getLevel()))
            .orElse(SeverityLevel.LOW);
    }

    public int calculateComplianceScore() {
        int baseScore = 100;
        
        // Deduct points for violations
        for (ComplianceViolation violation : violations) {
            switch (violation.getSeverityLevel()) {
                case LOW -> baseScore -= 5;
                case MEDIUM -> baseScore -= 15;
                case HIGH -> baseScore -= 30;
                case CRITICAL -> baseScore -= 50;
            }
        }
        
        return Math.max(0, baseScore);
    }

    public ComplianceReport generateReport() {
        if (status != CheckStatus.APPROVED && status != CheckStatus.REJECTED) {
            throw new IllegalStateException("Report can only be generated for completed checks");
        }
        
        return ComplianceReport.generate(this);
    }

    // Islamic Banking Compliance Methods

    public boolean isShariahCompliant() {
        return complianceType == ComplianceType.SHARIA;
    }

    public boolean allowsInterestBasedTransactions() {
        return !isShariahCompliant();
    }

    public boolean requiresAssetBacking() {
        return complianceType == ComplianceType.SHARIA;
    }

    public boolean prohibitsExcessiveUncertainty() {
        return complianceType == ComplianceType.SHARIA;
    }

    // CBUAE Regulatory Compliance Methods

    public boolean meetsCbuaeRequirements() {
        return complianceType == ComplianceType.AML && hasRequiredDocumentation();
    }

    public boolean hasRequiredDocumentation() {
        return true; // Simplified for demo - would check actual documentation
    }

    public boolean followsKycProcedures() {
        return checkType == CheckType.CUSTOMER_ONBOARDING || status == CheckStatus.APPROVED;
    }

    public boolean maintainsAuditTrail() {
        return createdAt != null; // All checks have audit trail
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean requiresManualReview() {
        return requiresManualReview;
    }

    public boolean requiresShariahBoardReview() {
        return requiresShariahBoardReview;
    }

    private static String generateCheckId() {
        return "CHK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}