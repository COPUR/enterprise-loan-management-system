package com.bank.loan.loan.domain.customer;

import com.bank.loan.domain.shared.BianTypes.*;
import com.bank.loan.domain.shared.BerlinGroupTypes.*;
import com.bank.loan.sharedkernel.domain.model.AggregateRoot;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BIAN Customer Management Service Domain - Aggregate Root
 * Following BIAN Customer Management service domain specification
 * Implements comprehensive customer lifecycle management
 * Pure domain model following DDD principles
 */
@Getter
public class BianCustomerManagement extends AggregateRoot<CustomerManagementInstanceReference> {

    // BIAN Service Domain Instance Reference
    private final CustomerManagementInstanceReference id;
    private final ServiceDomainInstanceReference serviceDomainInstanceReference;

    // BIAN Customer Core Information
    private final CustomerReference customerReference;
    private final CustomerPersonalDetails personalDetails;
    private final CustomerContactDetails contactDetails;
    private final CustomerIdentification identification;
    private final CustomerFinancialProfile financialProfile;

    // Customer Status and Segment
    private CustomerStatus customerStatus;
    private final CustomerSegment customerSegment;
    private final CustomerRiskProfile riskProfile;
    private final List<CustomerProduct> customerProducts;
    private final List<CustomerRelationship> customerRelationships;

    // BIAN Activity Tracking
    private final List<Action> customerActions;
    private final List<ServiceDomainActivityLog> activityLog;
    private final List<Assessment> customerAssessments;

    // BIAN Control Information
    private OffsetDateTime lastUpdated;
    private EmployeeBusinessUnitReference lastUpdatedBy;
    private LocalDate onboardingDate;
    private LocalDate lastReviewDate;

    // Private constructor following DDD aggregate pattern
    private BianCustomerManagement(CustomerManagementInstanceReference id,
                                 ServiceDomainInstanceReference serviceDomainInstanceReference,
                                 CustomerReference customerReference,
                                 CustomerPersonalDetails personalDetails,
                                 CustomerContactDetails contactDetails,
                                 CustomerIdentification identification,
                                 CustomerFinancialProfile financialProfile,
                                 CustomerSegment customerSegment,
                                 CustomerRiskProfile riskProfile) {
        super(id);
        this.id = id;
        this.serviceDomainInstanceReference = serviceDomainInstanceReference;
        this.customerReference = customerReference;
        this.personalDetails = personalDetails;
        this.contactDetails = contactDetails;
        this.identification = identification;
        this.financialProfile = financialProfile;
        this.customerSegment = customerSegment;
        this.riskProfile = riskProfile;

        // Initialize collections
        this.customerProducts = new ArrayList<>();
        this.customerRelationships = new ArrayList<>();
        this.customerActions = new ArrayList<>();
        this.activityLog = new ArrayList<>();
        this.customerAssessments = new ArrayList<>();

        // Set initial status
        this.customerStatus = CustomerStatus.PROSPECT;
        this.lastUpdated = OffsetDateTime.now();
        this.onboardingDate = LocalDate.now();
    }

    /**
     * BIAN INITIATE behavior qualifier
     * Factory method to initiate customer onboarding
     */
    public static BianCustomerManagement initiate(InitiateCustomerOnboardingRequest request) {
        validateInitiateRequest(request);

        CustomerManagementInstanceReference instanceRef = CustomerManagementInstanceReference.generate();
        ServiceDomainInstanceReference serviceDomainRef = 
            ServiceDomainInstanceReference.of("CustomerManagement", instanceRef.getValue(), "1.0");

        BianCustomerManagement customer = new BianCustomerManagement(
            instanceRef,
            serviceDomainRef,
            request.customerReference(),
            request.personalDetails(),
            request.contactDetails(),
            request.identification(),
            request.financialProfile(),
            request.customerSegment(),
            request.riskProfile()
        );

        // Add initial activity log
        customer.addActivityLog(
            ServiceDomainActivityLog.of(
                "INITIATE",
                "INIT-" + instanceRef.getValue(),
                OffsetDateTime.now(),
                request.initiatedBy(),
                "Customer onboarding initiated",
                "COMPLETED"
            )
        );

        return customer;
    }

    /**
     * BIAN UPDATE behavior qualifier
     * Update customer information
     */
    public void update(UpdateCustomerInformationRequest request) {
        validateUpdateRequest(request);

        CustomerStatus previousStatus = this.customerStatus;

        if (request.newStatus() != null) {
            this.customerStatus = request.newStatus();
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.updatedBy();

        // Add status change action if applicable
        if (previousStatus != this.customerStatus) {
            Action statusChangeAction = Action.of(
                "STATUS_UPDATE",
                String.format("Customer status changed from %s to %s", previousStatus, this.customerStatus),
                "COMPLETED",
                OffsetDateTime.now(),
                request.updatedBy()
            );
            this.customerActions.add(statusChangeAction);
        }

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "UPDATE",
                "UPD-" + id.getValue(),
                OffsetDateTime.now(),
                request.updatedBy(),
                "Customer information updated",
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN EXECUTE behavior qualifier
     * Execute customer onboarding completion
     */
    public CustomerOnboardingResult executeOnboarding(ExecuteCustomerOnboardingRequest request) {
        validateOnboardingRequest(request);

        if (this.customerStatus != CustomerStatus.PROSPECT && 
            this.customerStatus != CustomerStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Customer must be prospect or under review for onboarding");
        }

        // Execute onboarding
        this.customerStatus = CustomerStatus.ACTIVE;
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.executedBy();

        // Create onboarding action
        Action onboardingAction = Action.of(
            "ONBOARDING",
            "Customer onboarding completed successfully",
            "COMPLETED",
            OffsetDateTime.now(),
            request.executedBy()
        );
        this.customerActions.add(onboardingAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "EXECUTE",
                "EXE-" + id.getValue(),
                OffsetDateTime.now(),
                request.executedBy(),
                "Customer onboarding executed",
                "COMPLETED"
            )
        );

        return CustomerOnboardingResult.builder()
            .customerManagementInstanceReference(this.id)
            .customerReference(this.customerReference)
            .onboardingStatus("COMPLETED")
            .onboardingDateTime(OffsetDateTime.now())
            .onboardingAction(onboardingAction)
            .build();
    }

    /**
     * BIAN CONTROL behavior qualifier
     * Control customer status (suspend, close, etc.)
     */
    public void control(ControlCustomerRequest request) {
        validateControlRequest(request);

        CustomerStatus previousStatus = this.customerStatus;

        switch (request.controlAction()) {
            case SUSPEND -> {
                if (this.customerStatus == CustomerStatus.ACTIVE) {
                    this.customerStatus = CustomerStatus.SUSPENDED;
                } else {
                    throw new IllegalStateException("Can only suspend active customers");
                }
            }
            case REACTIVATE -> {
                if (this.customerStatus == CustomerStatus.SUSPENDED) {
                    this.customerStatus = CustomerStatus.ACTIVE;
                } else {
                    throw new IllegalStateException("Can only reactivate suspended customers");
                }
            }
            case CLOSE -> {
                if (this.customerStatus == CustomerStatus.ACTIVE || 
                    this.customerStatus == CustomerStatus.SUSPENDED) {
                    this.customerStatus = CustomerStatus.CLOSED;
                } else {
                    throw new IllegalStateException("Cannot close customer in current status");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported control action: " + request.controlAction());
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.controlledBy();

        // Add control action
        Action controlAction = Action.of(
            "CONTROL",
            String.format("Customer status changed from %s to %s", previousStatus, this.customerStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.controlledBy()
        );
        this.customerActions.add(controlAction);

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
     * Capture customer interaction or event
     */
    public void captureCustomerInteraction(CaptureCustomerInteractionRequest request) {
        validateCaptureRequest(request);

        // Create interaction action
        Action interactionAction = Action.of(
            "INTERACTION",
            String.format("Customer interaction: %s", request.interactionType()),
            "COMPLETED",
            OffsetDateTime.now(),
            request.capturedBy()
        );
        this.customerActions.add(interactionAction);

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.capturedBy();

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "CAPTURE",
                "CAP-" + id.getValue(),
                OffsetDateTime.now(),
                request.capturedBy(),
                String.format("Customer interaction %s captured", request.interactionType()),
                "COMPLETED"
            )
        );
    }

    /**
     * Add customer product
     */
    public void addCustomerProduct(CustomerProduct product) {
        Objects.requireNonNull(product, "Customer product cannot be null");
        this.customerProducts.add(product);
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * Add customer relationship
     */
    public void addCustomerRelationship(CustomerRelationship relationship) {
        Objects.requireNonNull(relationship, "Customer relationship cannot be null");
        this.customerRelationships.add(relationship);
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * Add customer assessment
     */
    public void addCustomerAssessment(Assessment assessment) {
        Objects.requireNonNull(assessment, "Customer assessment cannot be null");
        this.customerAssessments.add(assessment);
        this.lastUpdated = OffsetDateTime.now();
    }

    // Private helper methods
    private void addActivityLog(ServiceDomainActivityLog log) {
        this.activityLog.add(log);
    }

    // Validation methods following DDD invariants
    private static void validateInitiateRequest(InitiateCustomerOnboardingRequest request) {
        Objects.requireNonNull(request, "Initiate request cannot be null");
        Objects.requireNonNull(request.customerReference(), "Customer reference is required");
        Objects.requireNonNull(request.personalDetails(), "Personal details are required");
        Objects.requireNonNull(request.contactDetails(), "Contact details are required");
        Objects.requireNonNull(request.identification(), "Identification is required");
        Objects.requireNonNull(request.initiatedBy(), "Initiated by is required");
    }

    private void validateUpdateRequest(UpdateCustomerInformationRequest request) {
        Objects.requireNonNull(request, "Update request cannot be null");
        Objects.requireNonNull(request.updatedBy(), "Updated by is required");
    }

    private void validateOnboardingRequest(ExecuteCustomerOnboardingRequest request) {
        Objects.requireNonNull(request, "Onboarding request cannot be null");
        Objects.requireNonNull(request.executedBy(), "Executed by is required");
    }

    private void validateControlRequest(ControlCustomerRequest request) {
        Objects.requireNonNull(request, "Control request cannot be null");
        Objects.requireNonNull(request.controlAction(), "Control action is required");
        Objects.requireNonNull(request.controlledBy(), "Controlled by is required");
    }

    private void validateCaptureRequest(CaptureCustomerInteractionRequest request) {
        Objects.requireNonNull(request, "Capture request cannot be null");
        Objects.requireNonNull(request.interactionType(), "Interaction type is required");
        Objects.requireNonNull(request.capturedBy(), "Captured by is required");
    }

    @Override
    public CustomerManagementInstanceReference getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BianCustomerManagement that = (BianCustomerManagement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BianCustomerManagement{" +
                "id=" + id +
                ", customerReference=" + customerReference +
                ", customerStatus=" + customerStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Enums and supporting types
    public enum CustomerStatus {
        PROSPECT,
        UNDER_REVIEW,
        ACTIVE,
        SUSPENDED,
        CLOSED,
        DORMANT
    }

    public enum CustomerControlAction {
        SUSPEND,
        REACTIVATE,
        CLOSE
    }

    // Value objects for customer data
    public record CustomerPersonalDetails(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String nationality,
        String maritalStatus,
        String occupation
    ) {}

    public record CustomerContactDetails(
        String primaryPhone,
        String secondaryPhone,
        String emailAddress,
        Address primaryAddress,
        Address mailingAddress
    ) {}

    public record CustomerIdentification(
        String identificationType,
        String identificationNumber,
        String issuingAuthority,
        LocalDate issueDate,
        LocalDate expiryDate
    ) {}

    public record CustomerFinancialProfile(
        CurrencyAndAmount annualIncome,
        CurrencyAndAmount netWorth,
        String employmentStatus,
        String creditRating,
        String riskTolerance
    ) {}

    public record CustomerSegment(
        String segmentType,
        String segmentCategory,
        String segmentDescription
    ) {}

    public record CustomerRiskProfile(
        String riskCategory,
        String riskLevel,
        String riskDescription,
        LocalDate lastAssessmentDate
    ) {}

    public record CustomerProduct(
        String productType,
        String productReference,
        String productStatus,
        LocalDate openDate
    ) {}

    public record CustomerRelationship(
        String relationshipType,
        String relationshipManager,
        String relationshipStatus,
        LocalDate establishedDate
    ) {}

    // Request/Response types for BIAN behavior qualifiers
    public record InitiateCustomerOnboardingRequest(
        CustomerReference customerReference,
        CustomerPersonalDetails personalDetails,
        CustomerContactDetails contactDetails,
        CustomerIdentification identification,
        CustomerFinancialProfile financialProfile,
        CustomerSegment customerSegment,
        CustomerRiskProfile riskProfile,
        EmployeeBusinessUnitReference initiatedBy
    ) {}

    public record UpdateCustomerInformationRequest(
        CustomerStatus newStatus,
        EmployeeBusinessUnitReference updatedBy
    ) {}

    public record ExecuteCustomerOnboardingRequest(
        EmployeeBusinessUnitReference executedBy
    ) {}

    public record ControlCustomerRequest(
        CustomerControlAction controlAction,
        EmployeeBusinessUnitReference controlledBy
    ) {}

    public record CaptureCustomerInteractionRequest(
        String interactionType,
        String interactionDescription,
        EmployeeBusinessUnitReference capturedBy
    ) {}

    public record CustomerOnboardingResult(
        CustomerManagementInstanceReference customerManagementInstanceReference,
        CustomerReference customerReference,
        String onboardingStatus,
        OffsetDateTime onboardingDateTime,
        Action onboardingAction
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CustomerManagementInstanceReference customerManagementInstanceReference;
            private CustomerReference customerReference;
            private String onboardingStatus;
            private OffsetDateTime onboardingDateTime;
            private Action onboardingAction;

            public Builder customerManagementInstanceReference(CustomerManagementInstanceReference customerManagementInstanceReference) {
                this.customerManagementInstanceReference = customerManagementInstanceReference;
                return this;
            }

            public Builder customerReference(CustomerReference customerReference) {
                this.customerReference = customerReference;
                return this;
            }

            public Builder onboardingStatus(String onboardingStatus) {
                this.onboardingStatus = onboardingStatus;
                return this;
            }

            public Builder onboardingDateTime(OffsetDateTime onboardingDateTime) {
                this.onboardingDateTime = onboardingDateTime;
                return this;
            }

            public Builder onboardingAction(Action onboardingAction) {
                this.onboardingAction = onboardingAction;
                return this;
            }

            public CustomerOnboardingResult build() {
                return new CustomerOnboardingResult(customerManagementInstanceReference, customerReference,
                        onboardingStatus, onboardingDateTime, onboardingAction);
            }
        }
    }
}