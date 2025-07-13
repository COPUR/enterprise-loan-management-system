package com.amanahfi.risk.domain.assessment;

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
 * Risk Assessment Aggregate Root
 * 
 * Manages comprehensive risk evaluation for:
 * - Credit risk assessment for loan applications
 * - Market risk for investment products
 * - Operational risk for platform operations
 * - Liquidity risk for cash management
 * - Regulatory risk for compliance
 */
@Entity
@Table(name = "risk_assessments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAssessment {

    // Risk thresholds based on UAE Central Bank guidelines
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("75.0");
    private static final BigDecimal MEDIUM_RISK_THRESHOLD = new BigDecimal("50.0");
    private static final BigDecimal ACCEPTABLE_RISK_THRESHOLD = new BigDecimal("25.0");

    @Id
    private String assessmentId;

    @NotBlank
    private String entityId; // Customer ID, Loan ID, Product ID, etc.

    @NotNull
    @Enumerated(EnumType.STRING)
    private RiskType riskType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AssessmentStatus status;

    @NotNull
    private LocalDateTime assessmentDate;

    private LocalDateTime completedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;

    // Risk Scores (0-100 scale)
    @Column(precision = 5, scale = 2)
    private BigDecimal creditRiskScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal marketRiskScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal operationalRiskScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal liquidityRiskScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal overallRiskScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RiskLevel overallRiskLevel;

    // Assessment Details
    @NotBlank
    private String assessmentReason;

    private String assessmentNotes;
    private String approvalNotes;
    private String rejectionReason;

    // Financial Context
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "assessment_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "assessment_currency"))
    })
    private Money assessmentAmount;

    // Risk Factors
    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RiskFactor> riskFactors = new ArrayList<>();

    // Mitigation Measures
    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RiskMitigation> mitigationMeasures = new ArrayList<>();

    // Officer Assignment
    private String assignedRiskOfficerId;
    private String assignedAnalystId;

    // Automation
    private boolean automatedAssessment;
    private boolean requiresManualReview;
    private boolean requiresSeniorApproval;

    // Domain Events
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates credit risk assessment for loan applications
     */
    public static RiskAssessment createCreditRiskAssessment(String customerId, Money loanAmount, String reason) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.assessmentId = generateAssessmentId();
        assessment.entityId = customerId;
        assessment.riskType = RiskType.CREDIT;
        assessment.status = AssessmentStatus.PENDING;
        assessment.assessmentDate = LocalDateTime.now();
        assessment.assessmentAmount = loanAmount;
        assessment.assessmentReason = reason;
        assessment.automatedAssessment = true;
        assessment.requiresManualReview = loanAmount.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0;
        assessment.requiresSeniorApproval = loanAmount.getAmount().compareTo(BigDecimal.valueOf(500000)) > 0;
        
        assessment.addDomainEvent(new RiskAssessmentCreatedEvent(assessment.assessmentId, customerId, RiskType.CREDIT));
        
        return assessment;
    }

    /**
     * Creates market risk assessment for investment products
     */
    public static RiskAssessment createMarketRiskAssessment(String productId, Money exposureAmount, String reason) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.assessmentId = generateAssessmentId();
        assessment.entityId = productId;
        assessment.riskType = RiskType.MARKET;
        assessment.status = AssessmentStatus.PENDING;
        assessment.assessmentDate = LocalDateTime.now();
        assessment.assessmentAmount = exposureAmount;
        assessment.assessmentReason = reason;
        assessment.automatedAssessment = false;
        assessment.requiresManualReview = true;
        assessment.requiresSeniorApproval = exposureAmount.getAmount().compareTo(BigDecimal.valueOf(1000000)) > 0;
        
        assessment.addDomainEvent(new RiskAssessmentCreatedEvent(assessment.assessmentId, productId, RiskType.MARKET));
        
        return assessment;
    }

    /**
     * Creates operational risk assessment
     */
    public static RiskAssessment createOperationalRiskAssessment(String operationId, String reason) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.assessmentId = generateAssessmentId();
        assessment.entityId = operationId;
        assessment.riskType = RiskType.OPERATIONAL;
        assessment.status = AssessmentStatus.PENDING;
        assessment.assessmentDate = LocalDateTime.now();
        assessment.assessmentReason = reason;
        assessment.automatedAssessment = false;
        assessment.requiresManualReview = true;
        assessment.requiresSeniorApproval = true;
        
        assessment.addDomainEvent(new RiskAssessmentCreatedEvent(assessment.assessmentId, operationId, RiskType.OPERATIONAL));
        
        return assessment;
    }

    /**
     * Performs automated risk scoring
     */
    public void performAutomatedScoring(RiskScoringResult scoringResult) {
        if (status != AssessmentStatus.PENDING) {
            throw new IllegalStateException("Can only perform scoring on pending assessments");
        }

        this.creditRiskScore = scoringResult.getCreditScore();
        this.marketRiskScore = scoringResult.getMarketScore();
        this.operationalRiskScore = scoringResult.getOperationalScore();
        this.liquidityRiskScore = scoringResult.getLiquidityScore();
        
        // Calculate overall risk score (weighted average)
        this.overallRiskScore = calculateOverallRiskScore();
        this.overallRiskLevel = determineRiskLevel(overallRiskScore);
        
        if (isHighRisk()) {
            this.status = AssessmentStatus.REQUIRES_REVIEW;
            this.requiresManualReview = true;
        } else {
            this.status = AssessmentStatus.IN_PROGRESS;
        }
        
        addDomainEvent(new RiskScoringCompletedEvent(assessmentId, overallRiskScore, overallRiskLevel));
    }

    /**
     * Assigns assessment to risk officer
     */
    public void assignToRiskOfficer(String officerId, String analystId) {
        if (status != AssessmentStatus.REQUIRES_REVIEW && status != AssessmentStatus.PENDING) {
            throw new IllegalStateException("Assessment must be pending or require review to be assigned");
        }
        
        this.status = AssessmentStatus.UNDER_REVIEW;
        this.assignedRiskOfficerId = officerId;
        this.assignedAnalystId = analystId;
        
        addDomainEvent(new RiskAssessmentAssignedEvent(assessmentId, officerId, analystId));
    }

    /**
     * Adds risk factor to assessment
     */
    public void addRiskFactor(RiskFactorType factorType, RiskLevel impact, String description, BigDecimal weighting) {
        RiskFactor riskFactor = RiskFactor.create(factorType, impact, description, weighting);
        riskFactor.assignToAssessment(this);
        this.riskFactors.add(riskFactor);
        
        // Recalculate overall risk score
        recalculateRiskScore();
        
        addDomainEvent(new RiskFactorAddedEvent(assessmentId, factorType, impact));
    }

    /**
     * Adds risk mitigation measure
     */
    public void addMitigationMeasure(MitigationType mitigationType, String description, 
                                   BigDecimal riskReduction, LocalDateTime implementationDate) {
        RiskMitigation mitigation = RiskMitigation.create(mitigationType, description, riskReduction, implementationDate);
        mitigation.assignToAssessment(this);
        this.mitigationMeasures.add(mitigation);
        
        addDomainEvent(new RiskMitigationAddedEvent(assessmentId, mitigationType, riskReduction));
    }

    /**
     * Approves risk assessment
     */
    public void approve(String approverId, String approvalNotes) {
        if (status != AssessmentStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Assessment must be under review to be approved");
        }
        
        this.status = AssessmentStatus.APPROVED;
        this.approvalNotes = approvalNotes;
        this.approvedDate = LocalDateTime.now();
        this.completedDate = LocalDateTime.now();
        
        addDomainEvent(new RiskAssessmentApprovedEvent(assessmentId, approverId, overallRiskLevel));
    }

    /**
     * Rejects risk assessment
     */
    public void reject(String rejectorId, String rejectionReason) {
        if (status == AssessmentStatus.APPROVED || status == AssessmentStatus.REJECTED) {
            throw new IllegalStateException("Assessment is already completed");
        }
        
        this.status = AssessmentStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.rejectedDate = LocalDateTime.now();
        this.completedDate = LocalDateTime.now();
        
        addDomainEvent(new RiskAssessmentRejectedEvent(assessmentId, rejectorId, rejectionReason));
    }

    // Business Logic Methods

    public boolean isHighRisk() {
        return overallRiskScore != null && overallRiskScore.compareTo(HIGH_RISK_THRESHOLD) >= 0;
    }

    public boolean isMediumRisk() {
        return overallRiskScore != null && 
               overallRiskScore.compareTo(MEDIUM_RISK_THRESHOLD) >= 0 && 
               overallRiskScore.compareTo(HIGH_RISK_THRESHOLD) < 0;
    }

    public boolean isLowRisk() {
        return overallRiskScore != null && overallRiskScore.compareTo(ACCEPTABLE_RISK_THRESHOLD) < 0;
    }

    public boolean requiresManualReview() {
        return requiresManualReview || isHighRisk();
    }

    public boolean requiresSeniorApproval() {
        return requiresSeniorApproval || overallRiskLevel == RiskLevel.CRITICAL;
    }

    public boolean hasRiskFactors() {
        return !riskFactors.isEmpty();
    }

    public boolean hasMitigationMeasures() {
        return !mitigationMeasures.isEmpty();
    }

    public BigDecimal calculateRiskAdjustedReturn() {
        if (assessmentAmount == null || overallRiskScore == null) {
            return BigDecimal.ZERO;
        }
        
        // Simple risk-adjusted return calculation
        BigDecimal riskAdjustment = BigDecimal.ONE.subtract(overallRiskScore.divide(BigDecimal.valueOf(100)));
        return assessmentAmount.getAmount().multiply(riskAdjustment);
    }

    // Private helper methods

    private BigDecimal calculateOverallRiskScore() {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        
        if (creditRiskScore != null) {
            total = total.add(creditRiskScore.multiply(BigDecimal.valueOf(0.4))); // 40% weight
            count++;
        }
        if (marketRiskScore != null) {
            total = total.add(marketRiskScore.multiply(BigDecimal.valueOf(0.25))); // 25% weight
            count++;
        }
        if (operationalRiskScore != null) {
            total = total.add(operationalRiskScore.multiply(BigDecimal.valueOf(0.2))); // 20% weight
            count++;
        }
        if (liquidityRiskScore != null) {
            total = total.add(liquidityRiskScore.multiply(BigDecimal.valueOf(0.15))); // 15% weight
            count++;
        }
        
        return count > 0 ? total : BigDecimal.ZERO;
    }

    private RiskLevel determineRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return RiskLevel.CRITICAL;
        } else if (riskScore.compareTo(HIGH_RISK_THRESHOLD) >= 0) {
            return RiskLevel.HIGH;
        } else if (riskScore.compareTo(MEDIUM_RISK_THRESHOLD) >= 0) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private void recalculateRiskScore() {
        // Recalculate based on risk factors
        BigDecimal factorAdjustment = riskFactors.stream()
            .map(factor -> factor.getWeighting())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (overallRiskScore != null) {
            overallRiskScore = overallRiskScore.add(factorAdjustment);
            overallRiskLevel = determineRiskLevel(overallRiskScore);
        }
    }

    private static String generateAssessmentId() {
        return "RISK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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