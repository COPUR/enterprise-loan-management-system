package com.bank.loanmanagement.loan.domain.model;

import com.bank.loanmanagement.domain.shared.BianTypes.*;
import com.bank.loanmanagement.domain.shared.BerlinGroupTypes.*;
import com.bank.loanmanagement.sharedkernel.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BIAN Consumer Loan Service Domain - Aggregate Root
 * Following BIAN Consumer Loan service domain specification
 * Implements Berlin Group compliance for regulatory adherence
 * Pure domain model without infrastructure concerns
 */
@Getter
public class BianConsumerLoan extends AggregateRoot<ConsumerLoanArrangementInstanceReference> {

    // BIAN Service Domain Instance Reference
    private final ConsumerLoanArrangementInstanceReference id;
    private final ServiceDomainInstanceReference serviceDomainInstanceReference;

    // BIAN Consumer Loan Arrangement
    private final CustomerReference customerReference;
    private final ProductServiceType loanProductType;
    private final CurrencyAndAmount loanPrincipalAmount;
    private final Rate interestRate;
    private final Schedule repaymentSchedule;
    private final DateType maturityDate;
    private final Agreement loanAgreement;
    private final List<Feature> loanFeatures;

    // Berlin Group Compliance
    private final AccountReference debtorAccount;
    private final AccountReference creditorAccount;
    private final PsuData psuData;
    private final TppInfo tppInfo;

    // Loan Status and Control
    private ConsumerLoanStatus loanStatus;
    private final List<Assessment> creditAssessments;
    private final List<Action> loanActions;
    private final List<ServiceDomainActivityLog> activityLog;

    // BIAN Behavior Qualifiers Support
    private OffsetDateTime lastUpdated;
    private EmployeeBusinessUnitReference lastUpdatedBy;

    // Private constructor following DDD aggregate pattern
    private BianConsumerLoan(ConsumerLoanArrangementInstanceReference id,
                           ServiceDomainInstanceReference serviceDomainInstanceReference,
                           CustomerReference customerReference,
                           ProductServiceType loanProductType,
                           CurrencyAndAmount loanPrincipalAmount,
                           Rate interestRate,
                           Schedule repaymentSchedule,
                           DateType maturityDate,
                           Agreement loanAgreement,
                           AccountReference debtorAccount,
                           AccountReference creditorAccount,
                           PsuData psuData,
                           TppInfo tppInfo) {
        this.id = id;
        this.serviceDomainInstanceReference = serviceDomainInstanceReference;
        this.customerReference = customerReference;
        this.loanProductType = loanProductType;
        this.loanPrincipalAmount = loanPrincipalAmount;
        this.interestRate = interestRate;
        this.repaymentSchedule = repaymentSchedule;
        this.maturityDate = maturityDate;
        this.loanAgreement = loanAgreement;
        this.debtorAccount = debtorAccount;
        this.creditorAccount = creditorAccount;
        this.psuData = psuData;
        this.tppInfo = tppInfo;
        
        // Initialize collections
        this.loanFeatures = new ArrayList<>();
        this.creditAssessments = new ArrayList<>();
        this.loanActions = new ArrayList<>();
        this.activityLog = new ArrayList<>();
        
        // Set initial status
        this.loanStatus = ConsumerLoanStatus.INITIATED;
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * BIAN INITIATE behavior qualifier
     * Factory method to initiate a new consumer loan arrangement
     */
    public static BianConsumerLoan initiate(InitiateConsumerLoanArrangementRequest request) {
        validateInitiateRequest(request);
        
        ConsumerLoanArrangementInstanceReference arrangementRef = 
            ConsumerLoanArrangementInstanceReference.generate();
        
        ServiceDomainInstanceReference serviceDomainRef = 
            ServiceDomainInstanceReference.of("ConsumerLoan", arrangementRef.getValue(), "1.0");

        BianConsumerLoan loan = new BianConsumerLoan(
            arrangementRef,
            serviceDomainRef,
            request.getCustomerReference(),
            request.getLoanProductType(),
            request.getLoanPrincipalAmount(),
            request.getInterestRate(),
            request.getRepaymentSchedule(),
            request.getMaturityDate(),
            request.getLoanAgreement(),
            request.getDebtorAccount(),
            request.getCreditorAccount(),
            request.getPsuData(),
            request.getTppInfo()
        );

        // Add initial activity log
        loan.addActivityLog(
            ServiceDomainActivityLog.of(
                "INITIATE",
                "INIT-" + arrangementRef.getValue(),
                OffsetDateTime.now(),
                request.getInitiatedBy(),
                "Consumer loan arrangement initiated",
                "COMPLETED"
            )
        );

        return loan;
    }

    /**
     * BIAN UPDATE behavior qualifier
     * Update consumer loan arrangement
     */
    public void update(UpdateConsumerLoanArrangementRequest request) {
        validateUpdateRequest(request);
        
        // Update loan status if provided
        if (request.getNewStatus() != null) {
            this.loanStatus = request.getNewStatus();
        }
        
        // Add any new features
        if (request.getAdditionalFeatures() != null) {
            this.loanFeatures.addAll(request.getAdditionalFeatures());
        }
        
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getUpdatedBy();
        
        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "UPDATE",
                "UPD-" + id.getValue(),
                OffsetDateTime.now(),
                request.getUpdatedBy(),
                "Consumer loan arrangement updated",
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN EXECUTE behavior qualifier
     * Execute loan fulfillment
     */
    public LoanFulfillmentResult executeFulfillment(ExecuteLoanFulfillmentRequest request) {
        validateFulfillmentRequest(request);
        
        if (this.loanStatus != ConsumerLoanStatus.APPROVED) {
            throw new IllegalStateException("Loan must be approved before fulfillment");
        }
        
        // Execute fulfillment logic
        this.loanStatus = ConsumerLoanStatus.ACTIVE;
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getExecutedBy();
        
        // Create fulfillment action
        Action fulfillmentAction = Action.of(
            "FULFILLMENT",
            "Loan funds disbursed to customer account",
            "COMPLETED",
            OffsetDateTime.now(),
            request.getExecutedBy()
        );
        
        this.loanActions.add(fulfillmentAction);
        
        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "EXECUTE",
                "EXE-" + id.getValue(),
                OffsetDateTime.now(),
                request.getExecutedBy(),
                "Loan fulfillment executed",
                "COMPLETED"
            )
        );
        
        return LoanFulfillmentResult.builder()
            .arrangementInstanceReference(this.id)
            .fulfillmentStatus("COMPLETED")
            .disbursementAmount(this.loanPrincipalAmount)
            .disbursementDateTime(OffsetDateTime.now())
            .fulfillmentAction(fulfillmentAction)
            .build();
    }

    /**
     * BIAN CONTROL behavior qualifier
     * Control loan arrangement (suspend, reactivate, etc.)
     */
    public void control(ControlConsumerLoanRequest request) {
        validateControlRequest(request);
        
        ConsumerLoanStatus previousStatus = this.loanStatus;
        
        switch (request.getControlAction()) {
            case SUSPEND -> {
                if (this.loanStatus == ConsumerLoanStatus.ACTIVE) {
                    this.loanStatus = ConsumerLoanStatus.SUSPENDED;
                } else {
                    throw new IllegalStateException("Can only suspend active loans");
                }
            }
            case REACTIVATE -> {
                if (this.loanStatus == ConsumerLoanStatus.SUSPENDED) {
                    this.loanStatus = ConsumerLoanStatus.ACTIVE;
                } else {
                    throw new IllegalStateException("Can only reactivate suspended loans");
                }
            }
            case TERMINATE -> {
                if (this.loanStatus == ConsumerLoanStatus.ACTIVE || this.loanStatus == ConsumerLoanStatus.SUSPENDED) {
                    this.loanStatus = ConsumerLoanStatus.TERMINATED;
                } else {
                    throw new IllegalStateException("Cannot terminate loan in current status");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported control action: " + request.getControlAction());
        }
        
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getControlledBy();
        
        // Add control action
        Action controlAction = Action.of(
            "CONTROL",
            String.format("Loan status changed from %s to %s", previousStatus, this.loanStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.getControlledBy()
        );
        
        this.loanActions.add(controlAction);
        
        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "CONTROL",
                "CTL-" + id.getValue(),
                OffsetDateTime.now(),
                request.getControlledBy(),
                String.format("Control action %s executed", request.getControlAction()),
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN GRANT behavior qualifier
     * Grant loan approval
     */
    public void grant(GrantLoanApprovalRequest request) {
        validateGrantRequest(request);
        
        if (this.loanStatus != ConsumerLoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Loan must be under review to grant approval");
        }
        
        this.loanStatus = ConsumerLoanStatus.APPROVED;
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getGrantedBy();
        
        // Add approval assessment
        Assessment approvalAssessment = Assessment.of(
            "CREDIT_APPROVAL",
            "Loan application approved based on credit assessment",
            "APPROVED",
            OffsetDateTime.now(),
            request.getGrantedBy()
        );
        
        this.creditAssessments.add(approvalAssessment);
        
        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "GRANT",
                "GRT-" + id.getValue(),
                OffsetDateTime.now(),
                request.getGrantedBy(),
                "Loan approval granted",
                "COMPLETED"
            )
        );
    }

    /**
     * Add credit assessment
     */
    public void addCreditAssessment(Assessment assessment) {
        Objects.requireNonNull(assessment, "Assessment cannot be null");
        this.creditAssessments.add(assessment);
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * Add loan feature
     */
    public void addLoanFeature(Feature feature) {
        Objects.requireNonNull(feature, "Feature cannot be null");
        this.loanFeatures.add(feature);
        this.lastUpdated = OffsetDateTime.now();
    }

    // Private helper methods
    private void addActivityLog(ServiceDomainActivityLog log) {
        this.activityLog.add(log);
    }

    // Validation methods following DDD invariants
    private static void validateInitiateRequest(InitiateConsumerLoanArrangementRequest request) {
        Objects.requireNonNull(request, "Initiate request cannot be null");
        Objects.requireNonNull(request.getCustomerReference(), "Customer reference is required");
        Objects.requireNonNull(request.getLoanProductType(), "Loan product type is required");
        Objects.requireNonNull(request.getLoanPrincipalAmount(), "Loan principal amount is required");
        Objects.requireNonNull(request.getInterestRate(), "Interest rate is required");
        Objects.requireNonNull(request.getRepaymentSchedule(), "Repayment schedule is required");
        Objects.requireNonNull(request.getMaturityDate(), "Maturity date is required");
        Objects.requireNonNull(request.getInitiatedBy(), "Initiated by is required");
    }

    private void validateUpdateRequest(UpdateConsumerLoanArrangementRequest request) {
        Objects.requireNonNull(request, "Update request cannot be null");
        Objects.requireNonNull(request.getUpdatedBy(), "Updated by is required");
    }

    private void validateFulfillmentRequest(ExecuteLoanFulfillmentRequest request) {
        Objects.requireNonNull(request, "Fulfillment request cannot be null");
        Objects.requireNonNull(request.getExecutedBy(), "Executed by is required");
    }

    private void validateControlRequest(ControlConsumerLoanRequest request) {
        Objects.requireNonNull(request, "Control request cannot be null");
        Objects.requireNonNull(request.getControlAction(), "Control action is required");
        Objects.requireNonNull(request.getControlledBy(), "Controlled by is required");
    }

    private void validateGrantRequest(GrantLoanApprovalRequest request) {
        Objects.requireNonNull(request, "Grant request cannot be null");
        Objects.requireNonNull(request.getGrantedBy(), "Granted by is required");
    }

    @Override
    public ConsumerLoanArrangementInstanceReference getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BianConsumerLoan that = (BianConsumerLoan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BianConsumerLoan{" +
                "id=" + id +
                ", customerReference=" + customerReference +
                ", loanPrincipalAmount=" + loanPrincipalAmount +
                ", loanStatus=" + loanStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Enums and supporting types
    public enum ConsumerLoanStatus {
        INITIATED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        ACTIVE,
        SUSPENDED,
        TERMINATED,
        COMPLETED,
        DEFAULTED
    }

    public enum ControlAction {
        SUSPEND,
        REACTIVATE,
        TERMINATE
    }

    // Request/Response types for BIAN behavior qualifiers
    public record InitiateConsumerLoanArrangementRequest(
        CustomerReference customerReference,
        ProductServiceType loanProductType,
        CurrencyAndAmount loanPrincipalAmount,
        Rate interestRate,
        Schedule repaymentSchedule,
        DateType maturityDate,
        Agreement loanAgreement,
        AccountReference debtorAccount,
        AccountReference creditorAccount,
        PsuData psuData,
        TppInfo tppInfo,
        EmployeeBusinessUnitReference initiatedBy
    ) {}

    public record UpdateConsumerLoanArrangementRequest(
        ConsumerLoanStatus newStatus,
        List<Feature> additionalFeatures,
        EmployeeBusinessUnitReference updatedBy
    ) {}

    public record ExecuteLoanFulfillmentRequest(
        EmployeeBusinessUnitReference executedBy
    ) {}

    public record ControlConsumerLoanRequest(
        ControlAction controlAction,
        EmployeeBusinessUnitReference controlledBy
    ) {}

    public record GrantLoanApprovalRequest(
        EmployeeBusinessUnitReference grantedBy
    ) {}

    public record LoanFulfillmentResult(
        ConsumerLoanArrangementInstanceReference arrangementInstanceReference,
        String fulfillmentStatus,
        CurrencyAndAmount disbursementAmount,
        OffsetDateTime disbursementDateTime,
        Action fulfillmentAction
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ConsumerLoanArrangementInstanceReference arrangementInstanceReference;
            private String fulfillmentStatus;
            private CurrencyAndAmount disbursementAmount;
            private OffsetDateTime disbursementDateTime;
            private Action fulfillmentAction;

            public Builder arrangementInstanceReference(ConsumerLoanArrangementInstanceReference arrangementInstanceReference) {
                this.arrangementInstanceReference = arrangementInstanceReference;
                return this;
            }

            public Builder fulfillmentStatus(String fulfillmentStatus) {
                this.fulfillmentStatus = fulfillmentStatus;
                return this;
            }

            public Builder disbursementAmount(CurrencyAndAmount disbursementAmount) {
                this.disbursementAmount = disbursementAmount;
                return this;
            }

            public Builder disbursementDateTime(OffsetDateTime disbursementDateTime) {
                this.disbursementDateTime = disbursementDateTime;
                return this;
            }

            public Builder fulfillmentAction(Action fulfillmentAction) {
                this.fulfillmentAction = fulfillmentAction;
                return this;
            }

            public LoanFulfillmentResult build() {
                return new LoanFulfillmentResult(arrangementInstanceReference, fulfillmentStatus,
                        disbursementAmount, disbursementDateTime, fulfillmentAction);
            }
        }
    }
}