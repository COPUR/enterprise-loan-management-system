package com.bank.loanmanagement.payment.domain.model;

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
 * BIAN Payment Initiation Service Domain - Aggregate Root
 * Following BIAN Payment Initiation service domain specification
 * Implements Berlin Group PSD2 Payment Initiation Services (PIS) compliance
 * Pure domain model following DDD principles
 */
@Getter
public class BianPaymentInitiation extends AggregateRoot<PaymentInitiationInstanceReference> {

    // BIAN Service Domain Instance Reference
    private final PaymentInitiationInstanceReference id;
    private final ServiceDomainInstanceReference serviceDomainInstanceReference;

    // Berlin Group PSD2 Payment Initiation Data
    private final String paymentId;
    private final PaymentProduct paymentProduct;
    private final PaymentType paymentType;
    private final Amount instructedAmount;
    private final AccountReference debtorAccount;
    private final AccountReference creditorAccount;
    private final PartyIdentification creditor;
    private final RemittanceInformation remittanceInformation;
    private final LocalDate requestedExecutionDate;
    private final PurposeCode purposeCode;

    // BIAN Payment Execution Context
    private final CustomerReference customerReference;
    private final PsuData psuData;
    private final TppInfo tppInfo;
    private final List<AuthenticationObject> scaMethods;

    // Payment Status and Control
    private TransactionStatus transactionStatus;
    private ScaStatus scaStatus;
    private final List<Action> paymentActions;
    private final List<ServiceDomainActivityLog> activityLog;
    private Links paymentLinks;

    // BIAN Control Information
    private OffsetDateTime lastUpdated;
    private EmployeeBusinessUnitReference lastUpdatedBy;
    private String authorisationId;
    private LocalDate statusReasonDate;

    // Private constructor following DDD aggregate pattern
    private BianPaymentInitiation(PaymentInitiationInstanceReference id,
                                ServiceDomainInstanceReference serviceDomainInstanceReference,
                                String paymentId,
                                PaymentProduct paymentProduct,
                                PaymentType paymentType,
                                Amount instructedAmount,
                                AccountReference debtorAccount,
                                AccountReference creditorAccount,
                                PartyIdentification creditor,
                                RemittanceInformation remittanceInformation,
                                LocalDate requestedExecutionDate,
                                PurposeCode purposeCode,
                                CustomerReference customerReference,
                                PsuData psuData,
                                TppInfo tppInfo,
                                List<AuthenticationObject> scaMethods) {
        this.id = id;
        this.serviceDomainInstanceReference = serviceDomainInstanceReference;
        this.paymentId = paymentId;
        this.paymentProduct = paymentProduct;
        this.paymentType = paymentType;
        this.instructedAmount = instructedAmount;
        this.debtorAccount = debtorAccount;
        this.creditorAccount = creditorAccount;
        this.creditor = creditor;
        this.remittanceInformation = remittanceInformation;
        this.requestedExecutionDate = requestedExecutionDate;
        this.purposeCode = purposeCode;
        this.customerReference = customerReference;
        this.psuData = psuData;
        this.tppInfo = tppInfo;
        this.scaMethods = new ArrayList<>(scaMethods);

        // Initialize collections
        this.paymentActions = new ArrayList<>();
        this.activityLog = new ArrayList<>();

        // Set initial status
        this.transactionStatus = TransactionStatus.RCVD;
        this.scaStatus = ScaStatus.RECEIVED;
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * BIAN INITIATE behavior qualifier
     * Factory method to initiate a new payment following Berlin Group PSD2
     */
    public static BianPaymentInitiation initiate(InitiatePaymentRequest request) {
        validateInitiateRequest(request);

        PaymentInitiationInstanceReference instanceRef = PaymentInitiationInstanceReference.generate();
        ServiceDomainInstanceReference serviceDomainRef = 
            ServiceDomainInstanceReference.of("PaymentInitiation", instanceRef.getValue(), "1.0");

        String paymentId = generatePaymentId();

        BianPaymentInitiation payment = new BianPaymentInitiation(
            instanceRef,
            serviceDomainRef,
            paymentId,
            request.getPaymentProduct(),
            request.getPaymentType(),
            request.getInstructedAmount(),
            request.getDebtorAccount(),
            request.getCreditorAccount(),
            request.getCreditor(),
            request.getRemittanceInformation(),
            request.getRequestedExecutionDate(),
            request.getPurposeCode(),
            request.getCustomerReference(),
            request.getPsuData(),
            request.getTppInfo(),
            request.getScaMethods()
        );

        // Generate initial HATEOAS links
        payment.paymentLinks = Links.builder()
            .self("/v1/payments/" + paymentProduct.name().toLowerCase() + "/" + paymentId)
            .status("/v1/payments/" + paymentProduct.name().toLowerCase() + "/" + paymentId + "/status")
            .startAuthorisation("/v1/payments/" + paymentProduct.name().toLowerCase() + "/" + paymentId + "/authorisations")
            .build();

        // Add initial activity log
        payment.addActivityLog(
            ServiceDomainActivityLog.of(
                "INITIATE",
                "INIT-" + instanceRef.getValue(),
                OffsetDateTime.now(),
                request.getInitiatedBy(),
                "Payment initiation request received",
                "COMPLETED"
            )
        );

        return payment;
    }

    /**
     * BIAN UPDATE behavior qualifier
     * Update payment status or information
     */
    public void update(UpdatePaymentRequest request) {
        validateUpdateRequest(request);

        TransactionStatus previousStatus = this.transactionStatus;

        if (request.getNewTransactionStatus() != null) {
            this.transactionStatus = request.getNewTransactionStatus();
        }

        if (request.getNewScaStatus() != null) {
            this.scaStatus = request.getNewScaStatus();
        }

        if (request.getAuthorisationId() != null) {
            this.authorisationId = request.getAuthorisationId();
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getUpdatedBy();

        // Add status change action
        if (previousStatus != this.transactionStatus) {
            Action statusChangeAction = Action.of(
                "STATUS_UPDATE",
                String.format("Payment status changed from %s to %s", previousStatus, this.transactionStatus),
                "COMPLETED",
                OffsetDateTime.now(),
                request.getUpdatedBy()
            );
            this.paymentActions.add(statusChangeAction);
        }

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "UPDATE",
                "UPD-" + id.getValue(),
                OffsetDateTime.now(),
                request.getUpdatedBy(),
                "Payment information updated",
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN EXECUTE behavior qualifier
     * Execute payment processing following Berlin Group standards
     */
    public PaymentExecutionResult execute(ExecutePaymentRequest request) {
        validateExecuteRequest(request);

        if (this.transactionStatus != TransactionStatus.ACCP) {
            throw new IllegalStateException("Payment must be accepted before execution");
        }

        if (this.scaStatus != ScaStatus.FINALISED) {
            throw new IllegalStateException("SCA must be finalised before execution");
        }

        // Execute payment processing
        this.transactionStatus = TransactionStatus.ACSP; // Accepted Settlement In Process
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getExecutedBy();

        // Create execution action
        Action executionAction = Action.of(
            "EXECUTION",
            "Payment processing initiated",
            "IN_PROGRESS",
            OffsetDateTime.now(),
            request.getExecutedBy()
        );
        this.paymentActions.add(executionAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "EXECUTE",
                "EXE-" + id.getValue(),
                OffsetDateTime.now(),
                request.getExecutedBy(),
                "Payment execution started",
                "COMPLETED"
            )
        );

        return PaymentExecutionResult.builder()
            .paymentInitiationInstanceReference(this.id)
            .paymentId(this.paymentId)
            .transactionStatus(this.transactionStatus)
            .executionDateTime(OffsetDateTime.now())
            .instructedAmount(this.instructedAmount)
            .executionAction(executionAction)
            .build();
    }

    /**
     * BIAN CONTROL behavior qualifier
     * Control payment processing (cancel, suspend, etc.)
     */
    public void control(ControlPaymentRequest request) {
        validateControlRequest(request);

        TransactionStatus previousStatus = this.transactionStatus;

        switch (request.getControlAction()) {
            case CANCEL -> {
                if (this.transactionStatus == TransactionStatus.RCVD || 
                    this.transactionStatus == TransactionStatus.PDNG ||
                    this.transactionStatus == TransactionStatus.ACCP) {
                    this.transactionStatus = TransactionStatus.CANC;
                } else {
                    throw new IllegalStateException("Cannot cancel payment in current status");
                }
            }
            case REJECT -> {
                if (this.transactionStatus == TransactionStatus.RCVD || 
                    this.transactionStatus == TransactionStatus.PDNG) {
                    this.transactionStatus = TransactionStatus.RJCT;
                } else {
                    throw new IllegalStateException("Cannot reject payment in current status");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported control action: " + request.getControlAction());
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getControlledBy();

        // Add control action
        Action controlAction = Action.of(
            "CONTROL",
            String.format("Payment control action %s: status changed from %s to %s", 
                         request.getControlAction(), previousStatus, this.transactionStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.getControlledBy()
        );
        this.paymentActions.add(controlAction);

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
     * BIAN EXCHANGE behavior qualifier
     * Exchange SCA information
     */
    public ScaExchangeResult exchangeScaInformation(ExchangeScaInformationRequest request) {
        validateScaExchangeRequest(request);

        ScaStatus previousScaStatus = this.scaStatus;

        // Update SCA status based on exchange
        switch (request.getScaAction()) {
            case START_AUTHORISATION -> {
                if (this.scaStatus == ScaStatus.RECEIVED) {
                    this.scaStatus = ScaStatus.STARTED;
                    this.authorisationId = request.getAuthorisationId();
                }
            }
            case SELECT_SCA_METHOD -> {
                if (this.scaStatus == ScaStatus.STARTED) {
                    this.scaStatus = ScaStatus.SCAMETHODSELECTED;
                }
            }
            case AUTHENTICATE -> {
                if (this.scaStatus == ScaStatus.SCAMETHODSELECTED) {
                    this.scaStatus = ScaStatus.FINALISED;
                    this.transactionStatus = TransactionStatus.ACCP; // Accepted Customer Profile
                }
            }
            case FAIL -> {
                this.scaStatus = ScaStatus.FAILED;
                this.transactionStatus = TransactionStatus.RJCT;
            }
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getExchangedBy();

        // Add SCA action
        Action scaAction = Action.of(
            "SCA_EXCHANGE",
            String.format("SCA action %s: status changed from %s to %s", 
                         request.getScaAction(), previousScaStatus, this.scaStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.getExchangedBy()
        );
        this.paymentActions.add(scaAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "EXCHANGE",
                "EXC-" + id.getValue(),
                OffsetDateTime.now(),
                request.getExchangedBy(),
                String.format("SCA exchange action %s executed", request.getScaAction()),
                "COMPLETED"
            )
        );

        return ScaExchangeResult.builder()
            .paymentInitiationInstanceReference(this.id)
            .paymentId(this.paymentId)
            .scaStatus(this.scaStatus)
            .transactionStatus(this.transactionStatus)
            .authorisationId(this.authorisationId)
            .scaAction(scaAction)
            .links(this.paymentLinks)
            .build();
    }

    /**
     * Complete payment settlement
     */
    public void completeSettlement(EmployeeBusinessUnitReference completedBy) {
        if (this.transactionStatus != TransactionStatus.ACSP) {
            throw new IllegalStateException("Payment must be in settlement process to complete");
        }

        this.transactionStatus = TransactionStatus.ACSC; // Accepted Settlement Completed
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = completedBy;

        // Add completion action
        Action completionAction = Action.of(
            "SETTLEMENT_COMPLETION",
            "Payment settlement completed successfully",
            "COMPLETED",
            OffsetDateTime.now(),
            completedBy
        );
        this.paymentActions.add(completionAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "NOTIFY",
                "NOT-" + id.getValue(),
                OffsetDateTime.now(),
                completedBy,
                "Payment settlement completed",
                "COMPLETED"
            )
        );
    }

    // Private helper methods
    private void addActivityLog(ServiceDomainActivityLog log) {
        this.activityLog.add(log);
    }

    private static String generatePaymentId() {
        return "PMT-" + java.util.UUID.randomUUID().toString().toUpperCase().replace("-", "");
    }

    // Validation methods following DDD invariants
    private static void validateInitiateRequest(InitiatePaymentRequest request) {
        Objects.requireNonNull(request, "Initiate payment request cannot be null");
        Objects.requireNonNull(request.getPaymentProduct(), "Payment product is required");
        Objects.requireNonNull(request.getPaymentType(), "Payment type is required");
        Objects.requireNonNull(request.getInstructedAmount(), "Instructed amount is required");
        Objects.requireNonNull(request.getDebtorAccount(), "Debtor account is required");
        Objects.requireNonNull(request.getCreditorAccount(), "Creditor account is required");
        Objects.requireNonNull(request.getPsuData(), "PSU data is required");
        Objects.requireNonNull(request.getTppInfo(), "TPP info is required");
        Objects.requireNonNull(request.getInitiatedBy(), "Initiated by is required");
    }

    private void validateUpdateRequest(UpdatePaymentRequest request) {
        Objects.requireNonNull(request, "Update payment request cannot be null");
        Objects.requireNonNull(request.getUpdatedBy(), "Updated by is required");
    }

    private void validateExecuteRequest(ExecutePaymentRequest request) {
        Objects.requireNonNull(request, "Execute payment request cannot be null");
        Objects.requireNonNull(request.getExecutedBy(), "Executed by is required");
    }

    private void validateControlRequest(ControlPaymentRequest request) {
        Objects.requireNonNull(request, "Control payment request cannot be null");
        Objects.requireNonNull(request.getControlAction(), "Control action is required");
        Objects.requireNonNull(request.getControlledBy(), "Controlled by is required");
    }

    private void validateScaExchangeRequest(ExchangeScaInformationRequest request) {
        Objects.requireNonNull(request, "SCA exchange request cannot be null");
        Objects.requireNonNull(request.getScaAction(), "SCA action is required");
        Objects.requireNonNull(request.getExchangedBy(), "Exchanged by is required");
    }

    @Override
    public PaymentInitiationInstanceReference getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BianPaymentInitiation that = (BianPaymentInitiation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BianPaymentInitiation{" +
                "id=" + id +
                ", paymentId='" + paymentId + '\'' +
                ", instructedAmount=" + instructedAmount +
                ", transactionStatus=" + transactionStatus +
                ", scaStatus=" + scaStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Enums and supporting types
    public enum PaymentProduct {
        SEPA_CREDIT_TRANSFERS,
        INSTANT_SEPA_CREDIT_TRANSFERS,
        TARGET2_PAYMENTS,
        CROSS_BORDER_CREDIT_TRANSFERS
    }

    public enum PaymentType {
        SINGLE,
        BULK,
        PERIODIC
    }

    public enum PaymentControlAction {
        CANCEL,
        REJECT
    }

    public enum ScaAction {
        START_AUTHORISATION,
        SELECT_SCA_METHOD,
        AUTHENTICATE,
        FAIL
    }

    // Request/Response types for BIAN behavior qualifiers
    public record InitiatePaymentRequest(
        PaymentProduct paymentProduct,
        PaymentType paymentType,
        Amount instructedAmount,
        AccountReference debtorAccount,
        AccountReference creditorAccount,
        PartyIdentification creditor,
        RemittanceInformation remittanceInformation,
        LocalDate requestedExecutionDate,
        PurposeCode purposeCode,
        CustomerReference customerReference,
        PsuData psuData,
        TppInfo tppInfo,
        List<AuthenticationObject> scaMethods,
        EmployeeBusinessUnitReference initiatedBy
    ) {}

    public record UpdatePaymentRequest(
        TransactionStatus newTransactionStatus,
        ScaStatus newScaStatus,
        String authorisationId,
        EmployeeBusinessUnitReference updatedBy
    ) {}

    public record ExecutePaymentRequest(
        EmployeeBusinessUnitReference executedBy
    ) {}

    public record ControlPaymentRequest(
        PaymentControlAction controlAction,
        EmployeeBusinessUnitReference controlledBy
    ) {}

    public record ExchangeScaInformationRequest(
        ScaAction scaAction,
        String authorisationId,
        EmployeeBusinessUnitReference exchangedBy
    ) {}

    public record PaymentExecutionResult(
        PaymentInitiationInstanceReference paymentInitiationInstanceReference,
        String paymentId,
        TransactionStatus transactionStatus,
        OffsetDateTime executionDateTime,
        Amount instructedAmount,
        Action executionAction
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PaymentInitiationInstanceReference paymentInitiationInstanceReference;
            private String paymentId;
            private TransactionStatus transactionStatus;
            private OffsetDateTime executionDateTime;
            private Amount instructedAmount;
            private Action executionAction;

            public Builder paymentInitiationInstanceReference(PaymentInitiationInstanceReference paymentInitiationInstanceReference) {
                this.paymentInitiationInstanceReference = paymentInitiationInstanceReference;
                return this;
            }

            public Builder paymentId(String paymentId) {
                this.paymentId = paymentId;
                return this;
            }

            public Builder transactionStatus(TransactionStatus transactionStatus) {
                this.transactionStatus = transactionStatus;
                return this;
            }

            public Builder executionDateTime(OffsetDateTime executionDateTime) {
                this.executionDateTime = executionDateTime;
                return this;
            }

            public Builder instructedAmount(Amount instructedAmount) {
                this.instructedAmount = instructedAmount;
                return this;
            }

            public Builder executionAction(Action executionAction) {
                this.executionAction = executionAction;
                return this;
            }

            public PaymentExecutionResult build() {
                return new PaymentExecutionResult(paymentInitiationInstanceReference, paymentId,
                        transactionStatus, executionDateTime, instructedAmount, executionAction);
            }
        }
    }

    public record ScaExchangeResult(
        PaymentInitiationInstanceReference paymentInitiationInstanceReference,
        String paymentId,
        ScaStatus scaStatus,
        TransactionStatus transactionStatus,
        String authorisationId,
        Action scaAction,
        Links links
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PaymentInitiationInstanceReference paymentInitiationInstanceReference;
            private String paymentId;
            private ScaStatus scaStatus;
            private TransactionStatus transactionStatus;
            private String authorisationId;
            private Action scaAction;
            private Links links;

            public Builder paymentInitiationInstanceReference(PaymentInitiationInstanceReference paymentInitiationInstanceReference) {
                this.paymentInitiationInstanceReference = paymentInitiationInstanceReference;
                return this;
            }

            public Builder paymentId(String paymentId) {
                this.paymentId = paymentId;
                return this;
            }

            public Builder scaStatus(ScaStatus scaStatus) {
                this.scaStatus = scaStatus;
                return this;
            }

            public Builder transactionStatus(TransactionStatus transactionStatus) {
                this.transactionStatus = transactionStatus;
                return this;
            }

            public Builder authorisationId(String authorisationId) {
                this.authorisationId = authorisationId;
                return this;
            }

            public Builder scaAction(Action scaAction) {
                this.scaAction = scaAction;
                return this;
            }

            public Builder links(Links links) {
                this.links = links;
                return this;
            }

            public ScaExchangeResult build() {
                return new ScaExchangeResult(paymentInitiationInstanceReference, paymentId, scaStatus,
                        transactionStatus, authorisationId, scaAction, links);
            }
        }
    }
}