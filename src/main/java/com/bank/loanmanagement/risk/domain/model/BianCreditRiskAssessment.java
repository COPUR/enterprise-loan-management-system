package com.bank.loanmanagement.risk.domain.model;

import com.bank.loanmanagement.domain.shared.BianTypes.*;
import com.bank.loanmanagement.domain.shared.BerlinGroupTypes.*;
import com.bank.loanmanagement.sharedkernel.domain.model.AggregateRoot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BIAN Credit Risk Assessment Service Domain - Aggregate Root
 * Following BIAN Credit Risk Assessment service domain specification
 * Implements comprehensive credit risk analysis and scoring
 * Pure domain model following DDD principles
 */
@Getter
public class BianCreditRiskAssessment extends AggregateRoot<CreditRiskAssessmentInstanceReference> {

    // BIAN Service Domain Instance Reference
    private final CreditRiskAssessmentInstanceReference id;
    private final ServiceDomainInstanceReference serviceDomainInstanceReference;

    // BIAN Credit Risk Assessment Core
    private final CustomerReference customerReference;
    private final CreditRiskAssessmentRequest assessmentRequest;
    private final CreditRiskMethodology methodology;
    private final List<RiskFactor> riskFactors;
    private final List<CreditMetric> creditMetrics;

    // Risk Assessment Results
    private CreditRiskRating creditRiskRating;
    private BigDecimal creditScore;
    private BigDecimal probabilityOfDefault;
    private BigDecimal lossGivenDefault;
    private BigDecimal exposureAtDefault;
    private CurrencyAndAmount recommendedCreditLimit;

    // Assessment Status and Control
    private AssessmentStatus assessmentStatus;
    private final List<Action> assessmentActions;
    private final List<ServiceDomainActivityLog> activityLog;
    private final List<Assessment> riskAssessments;

    // BIAN Control Information
    private OffsetDateTime lastUpdated;
    private EmployeeBusinessUnitReference lastUpdatedBy;
    private LocalDate assessmentDate;
    private LocalDate nextReviewDate;
    private String assessmentVersion;

    // Private constructor following DDD aggregate pattern
    private BianCreditRiskAssessment(CreditRiskAssessmentInstanceReference id,
                                   ServiceDomainInstanceReference serviceDomainInstanceReference,
                                   CustomerReference customerReference,
                                   CreditRiskAssessmentRequest assessmentRequest,
                                   CreditRiskMethodology methodology) {
        super(id);
        this.id = id;
        this.serviceDomainInstanceReference = serviceDomainInstanceReference;
        this.customerReference = customerReference;
        this.assessmentRequest = assessmentRequest;
        this.methodology = methodology;

        // Initialize collections
        this.riskFactors = new ArrayList<>();
        this.creditMetrics = new ArrayList<>();
        this.assessmentActions = new ArrayList<>();
        this.activityLog = new ArrayList<>();
        this.riskAssessments = new ArrayList<>();

        // Set initial status
        this.assessmentStatus = AssessmentStatus.INITIATED;
        this.lastUpdated = OffsetDateTime.now();
        this.assessmentDate = LocalDate.now();
        this.assessmentVersion = "1.0";
    }

    /**
     * BIAN INITIATE behavior qualifier
     * Factory method to initiate credit risk assessment
     */
    public static BianCreditRiskAssessment initiate(InitiateCreditRiskAssessmentRequest request) {
        validateInitiateRequest(request);

        CreditRiskAssessmentInstanceReference instanceRef = CreditRiskAssessmentInstanceReference.generate();
        ServiceDomainInstanceReference serviceDomainRef = 
            ServiceDomainInstanceReference.of("CreditRiskAssessment", instanceRef.getValue(), "1.0");

        BianCreditRiskAssessment assessment = new BianCreditRiskAssessment(
            instanceRef,
            serviceDomainRef,
            request.customerReference(),
            request.assessmentRequest(),
            request.methodology()
        );

        // Initialize risk factors from request
        assessment.riskFactors.addAll(request.initialRiskFactors());

        // Add initial activity log
        assessment.addActivityLog(
            ServiceDomainActivityLog.of(
                "INITIATE",
                "INIT-" + instanceRef.getValue(),
                OffsetDateTime.now(),
                request.initiatedBy(),
                "Credit risk assessment initiated",
                "COMPLETED"
            )
        );

        return assessment;
    }

    /**
     * BIAN UPDATE behavior qualifier
     * Update credit risk assessment
     */
    public void update(UpdateCreditRiskAssessmentRequest request) {
        validateUpdateRequest(request);

        AssessmentStatus previousStatus = this.assessmentStatus;

        if (request.newStatus() != null) {
            this.assessmentStatus = request.newStatus();
        }

        if (request.creditScore() != null) {
            this.creditScore = request.creditScore();
        }

        if (request.creditRiskRating() != null) {
            this.creditRiskRating = request.creditRiskRating();
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.updatedBy();

        // Add status change action if applicable
        if (previousStatus != this.assessmentStatus) {
            Action statusChangeAction = Action.of(
                "STATUS_UPDATE",
                String.format("Assessment status changed from %s to %s", previousStatus, this.assessmentStatus),
                "COMPLETED",
                OffsetDateTime.now(),
                request.updatedBy()
            );
            this.assessmentActions.add(statusChangeAction);
        }

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "UPDATE",
                "UPD-" + id.getValue(),
                OffsetDateTime.now(),
                request.updatedBy(),
                "Credit risk assessment updated",
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN EXECUTE behavior qualifier
     * Execute credit risk assessment calculation
     */
    public CreditRiskAssessmentResult executeAssessment(ExecuteCreditRiskAssessmentRequest request) {
        validateExecuteRequest(request);

        if (this.assessmentStatus != AssessmentStatus.INITIATED && 
            this.assessmentStatus != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Assessment must be initiated or in progress to execute");
        }

        // Execute risk assessment calculations
        this.assessmentStatus = AssessmentStatus.IN_PROGRESS;
        calculateRiskMetrics(request);
        this.assessmentStatus = AssessmentStatus.COMPLETED;

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.executedBy();

        // Create execution action
        Action executionAction = Action.of(
            "EXECUTION",
            "Credit risk assessment calculation completed",
            "COMPLETED",
            OffsetDateTime.now(),
            request.executedBy()
        );
        this.assessmentActions.add(executionAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "EXECUTE",
                "EXE-" + id.getValue(),
                OffsetDateTime.now(),
                request.executedBy(),
                "Credit risk assessment executed",
                "COMPLETED"
            )
        );

        return CreditRiskAssessmentResult.builder()
            .creditRiskAssessmentInstanceReference(this.id)
            .customerReference(this.customerReference)
            .creditRiskRating(this.creditRiskRating)
            .creditScore(this.creditScore)
            .probabilityOfDefault(this.probabilityOfDefault)
            .recommendedCreditLimit(this.recommendedCreditLimit)
            .assessmentDateTime(OffsetDateTime.now())
            .executionAction(executionAction)
            .build();
    }

    /**
     * BIAN CONTROL behavior qualifier
     * Control assessment process (approve, reject, etc.)
     */
    public void control(ControlCreditRiskAssessmentRequest request) {
        validateControlRequest(request);

        AssessmentStatus previousStatus = this.assessmentStatus;

        switch (request.controlAction()) {
            case APPROVE -> {
                if (this.assessmentStatus == AssessmentStatus.COMPLETED) {
                    this.assessmentStatus = AssessmentStatus.APPROVED;
                } else {
                    throw new IllegalStateException("Can only approve completed assessments");
                }
            }
            case REJECT -> {
                if (this.assessmentStatus == AssessmentStatus.COMPLETED || 
                    this.assessmentStatus == AssessmentStatus.IN_PROGRESS) {
                    this.assessmentStatus = AssessmentStatus.REJECTED;
                } else {
                    throw new IllegalStateException("Cannot reject assessment in current status");
                }
            }
            case RECALCULATE -> {
                if (this.assessmentStatus == AssessmentStatus.COMPLETED || 
                    this.assessmentStatus == AssessmentStatus.APPROVED) {
                    this.assessmentStatus = AssessmentStatus.IN_PROGRESS;
                } else {
                    throw new IllegalStateException("Cannot recalculate assessment in current status");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported control action: " + request.controlAction());
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.controlledBy();

        // Add control action
        Action controlAction = Action.of(
            "CONTROL",
            String.format("Assessment status changed from %s to %s", previousStatus, this.assessmentStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.controlledBy()
        );
        this.assessmentActions.add(controlAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "CONTROL",
                "CTL-" + id.getValue(),
                OffsetDateTime.now(),
                request.controlledBy(),
                String.format("Control action %s executed", request.controlAction()),
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN CAPTURE behavior qualifier
     * Capture additional risk information
     */
    public void captureRiskInformation(CaptureRiskInformationRequest request) {
        validateCaptureRequest(request);

        // Add new risk factors
        if (request.additionalRiskFactors() != null) {
            this.riskFactors.addAll(request.additionalRiskFactors());
        }

        // Add new credit metrics
        if (request.additionalCreditMetrics() != null) {
            this.creditMetrics.addAll(request.additionalCreditMetrics());
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.capturedBy();

        // Create capture action
        Action captureAction = Action.of(
            "CAPTURE",
            "Additional risk information captured",
            "COMPLETED",
            OffsetDateTime.now(),
            request.capturedBy()
        );
        this.assessmentActions.add(captureAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "CAPTURE",
                "CAP-" + id.getValue(),
                OffsetDateTime.now(),
                request.capturedBy(),
                "Risk information captured",
                "COMPLETED"
            )
        );
    }

    // Private helper methods
    private void calculateRiskMetrics(ExecuteCreditRiskAssessmentRequest request) {
        // Simplified risk calculation logic
        // In real implementation, this would use sophisticated models

        // Calculate credit score based on risk factors
        BigDecimal scoreSum = riskFactors.stream()
            .map(RiskFactor::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.creditScore = scoreSum.multiply(BigDecimal.valueOf(10)); // Simplified calculation
        
        // Determine credit risk rating
        if (creditScore.compareTo(BigDecimal.valueOf(700)) >= 0) {
            this.creditRiskRating = CreditRiskRating.LOW;
        } else if (creditScore.compareTo(BigDecimal.valueOf(600)) >= 0) {
            this.creditRiskRating = CreditRiskRating.MEDIUM;
        } else {
            this.creditRiskRating = CreditRiskRating.HIGH;
        }

        // Calculate probability of default (simplified)
        this.probabilityOfDefault = BigDecimal.valueOf(1.0).subtract(
            creditScore.divide(BigDecimal.valueOf(1000), 4, BigDecimal.ROUND_HALF_UP));

        // Calculate recommended credit limit
        BigDecimal baseLimit = BigDecimal.valueOf(10000);
        BigDecimal ratingMultiplier = switch (creditRiskRating) {
            case LOW -> BigDecimal.valueOf(2.0);
            case MEDIUM -> BigDecimal.valueOf(1.0);
            case HIGH -> BigDecimal.valueOf(0.5);
            case VERY_HIGH -> BigDecimal.valueOf(0.2);
        };
        
        BigDecimal limitAmount = baseLimit.multiply(ratingMultiplier);
        this.recommendedCreditLimit = CurrencyAndAmount.of("EUR", limitAmount);
    }

    private void addActivityLog(ServiceDomainActivityLog log) {
        this.activityLog.add(log);
    }

    // Validation methods following DDD invariants
    private static void validateInitiateRequest(InitiateCreditRiskAssessmentRequest request) {
        Objects.requireNonNull(request, "Initiate request cannot be null");
        Objects.requireNonNull(request.customerReference(), "Customer reference is required");
        Objects.requireNonNull(request.assessmentRequest(), "Assessment request is required");
        Objects.requireNonNull(request.methodology(), "Methodology is required");
        Objects.requireNonNull(request.initiatedBy(), "Initiated by is required");
    }

    private void validateUpdateRequest(UpdateCreditRiskAssessmentRequest request) {
        Objects.requireNonNull(request, "Update request cannot be null");
        Objects.requireNonNull(request.updatedBy(), "Updated by is required");
    }

    private void validateExecuteRequest(ExecuteCreditRiskAssessmentRequest request) {
        Objects.requireNonNull(request, "Execute request cannot be null");
        Objects.requireNonNull(request.executedBy(), "Executed by is required");
    }

    private void validateControlRequest(ControlCreditRiskAssessmentRequest request) {
        Objects.requireNonNull(request, "Control request cannot be null");
        Objects.requireNonNull(request.controlAction(), "Control action is required");
        Objects.requireNonNull(request.controlledBy(), "Controlled by is required");
    }

    private void validateCaptureRequest(CaptureRiskInformationRequest request) {
        Objects.requireNonNull(request, "Capture request cannot be null");
        Objects.requireNonNull(request.capturedBy(), "Captured by is required");
    }

    @Override
    public CreditRiskAssessmentInstanceReference getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BianCreditRiskAssessment that = (BianCreditRiskAssessment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BianCreditRiskAssessment{" +
                "id=" + id +
                ", customerReference=" + customerReference +
                ", creditRiskRating=" + creditRiskRating +
                ", creditScore=" + creditScore +
                ", assessmentStatus=" + assessmentStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Enums and supporting types
    public enum AssessmentStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        APPROVED,
        REJECTED,
        EXPIRED
    }

    public enum CreditRiskRating {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }

    public enum AssessmentControlAction {
        APPROVE,
        REJECT,
        RECALCULATE
    }

    // Value objects for risk assessment
    public record CreditRiskAssessmentRequest(
        String assessmentType,
        String assessmentPurpose,
        CurrencyAndAmount requestedAmount,
        String assessmentScope
    ) {}

    public record CreditRiskMethodology(
        String methodologyName,
        String methodologyVersion,
        String methodologyDescription,
        List<String> assessmentCriteria
    ) {}

    public record RiskFactor(
        String factorType,
        String factorName,
        BigDecimal factorValue,
        BigDecimal weight,
        String impact
    ) {}

    public record CreditMetric(
        String metricType,
        String metricName,
        BigDecimal metricValue,
        String metricUnit,
        LocalDate measurementDate
    ) {}

    // Request/Response types for BIAN behavior qualifiers
    public record InitiateCreditRiskAssessmentRequest(
        CustomerReference customerReference,
        CreditRiskAssessmentRequest assessmentRequest,
        CreditRiskMethodology methodology,
        List<RiskFactor> initialRiskFactors,
        EmployeeBusinessUnitReference initiatedBy
    ) {}

    public record UpdateCreditRiskAssessmentRequest(
        AssessmentStatus newStatus,
        BigDecimal creditScore,
        CreditRiskRating creditRiskRating,
        EmployeeBusinessUnitReference updatedBy
    ) {}

    public record ExecuteCreditRiskAssessmentRequest(
        EmployeeBusinessUnitReference executedBy
    ) {}

    public record ControlCreditRiskAssessmentRequest(
        AssessmentControlAction controlAction,
        EmployeeBusinessUnitReference controlledBy
    ) {}

    public record CaptureRiskInformationRequest(
        List<RiskFactor> additionalRiskFactors,
        List<CreditMetric> additionalCreditMetrics,
        EmployeeBusinessUnitReference capturedBy
    ) {}

    public record CreditRiskAssessmentResult(
        CreditRiskAssessmentInstanceReference creditRiskAssessmentInstanceReference,
        CustomerReference customerReference,
        CreditRiskRating creditRiskRating,
        BigDecimal creditScore,
        BigDecimal probabilityOfDefault,
        CurrencyAndAmount recommendedCreditLimit,
        OffsetDateTime assessmentDateTime,
        Action executionAction
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CreditRiskAssessmentInstanceReference creditRiskAssessmentInstanceReference;
            private CustomerReference customerReference;
            private CreditRiskRating creditRiskRating;
            private BigDecimal creditScore;
            private BigDecimal probabilityOfDefault;
            private CurrencyAndAmount recommendedCreditLimit;
            private OffsetDateTime assessmentDateTime;
            private Action executionAction;

            public Builder creditRiskAssessmentInstanceReference(CreditRiskAssessmentInstanceReference creditRiskAssessmentInstanceReference) {
                this.creditRiskAssessmentInstanceReference = creditRiskAssessmentInstanceReference;
                return this;
            }

            public Builder customerReference(CustomerReference customerReference) {
                this.customerReference = customerReference;
                return this;
            }

            public Builder creditRiskRating(CreditRiskRating creditRiskRating) {
                this.creditRiskRating = creditRiskRating;
                return this;
            }

            public Builder creditScore(BigDecimal creditScore) {
                this.creditScore = creditScore;
                return this;
            }

            public Builder probabilityOfDefault(BigDecimal probabilityOfDefault) {
                this.probabilityOfDefault = probabilityOfDefault;
                return this;
            }

            public Builder recommendedCreditLimit(CurrencyAndAmount recommendedCreditLimit) {
                this.recommendedCreditLimit = recommendedCreditLimit;
                return this;
            }

            public Builder assessmentDateTime(OffsetDateTime assessmentDateTime) {
                this.assessmentDateTime = assessmentDateTime;
                return this;
            }

            public Builder executionAction(Action executionAction) {
                this.executionAction = executionAction;
                return this;
            }

            public CreditRiskAssessmentResult build() {
                return new CreditRiskAssessmentResult(creditRiskAssessmentInstanceReference, customerReference,
                        creditRiskRating, creditScore, probabilityOfDefault, recommendedCreditLimit,
                        assessmentDateTime, executionAction);
            }
        }
    }
}