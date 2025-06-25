package com.bank.loanmanagement.account.domain.model;

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
 * BIAN Account Information Services (AIS) Service Domain - Aggregate Root
 * Following BIAN Account Information Services specification
 * Implements Berlin Group PSD2 Account Information Services compliance
 * Pure domain model following DDD principles
 */
@Getter
public class BianAccountInformationServices extends AggregateRoot<AccountInformationInstanceReference> {

    // BIAN Service Domain Instance Reference
    private final AccountInformationInstanceReference id;
    private final ServiceDomainInstanceReference serviceDomainInstanceReference;

    // Berlin Group PSD2 Account Information Data
    private final AccountReference accountReference;
    private final String consentId;
    private final List<String> availableAccounts;
    private final List<String> allPsd2;
    private final boolean balances;
    private final boolean transactions;
    private final LocalDate validUntil;
    private final int frequencyPerDay;

    // BIAN Account Context
    private final CustomerReference customerReference;
    private final PsuData psuData;
    private final TppInfo tppInfo;
    private final List<AuthenticationObject> scaMethods;

    // Account Information Status and Control
    private ConsentStatus consentStatus;
    private ScaStatus scaStatus;
    private final List<Action> accountActions;
    private final List<ServiceDomainActivityLog> activityLog;
    private Links accountLinks;

    // BIAN Control Information
    private OffsetDateTime lastUpdated;
    private EmployeeBusinessUnitReference lastUpdatedBy;
    private String authorisationId;
    private LocalDate lastAccessDate;

    // Private constructor following DDD aggregate pattern
    private BianAccountInformationServices(AccountInformationInstanceReference id,
                                         ServiceDomainInstanceReference serviceDomainInstanceReference,
                                         AccountReference accountReference,
                                         String consentId,
                                         List<String> availableAccounts,
                                         List<String> allPsd2,
                                         boolean balances,
                                         boolean transactions,
                                         LocalDate validUntil,
                                         int frequencyPerDay,
                                         CustomerReference customerReference,
                                         PsuData psuData,
                                         TppInfo tppInfo,
                                         List<AuthenticationObject> scaMethods) {
        this.id = id;
        this.serviceDomainInstanceReference = serviceDomainInstanceReference;
        this.accountReference = accountReference;
        this.consentId = consentId;
        this.availableAccounts = new ArrayList<>(availableAccounts);
        this.allPsd2 = new ArrayList<>(allPsd2);
        this.balances = balances;
        this.transactions = transactions;
        this.validUntil = validUntil;
        this.frequencyPerDay = frequencyPerDay;
        this.customerReference = customerReference;
        this.psuData = psuData;
        this.tppInfo = tppInfo;
        this.scaMethods = new ArrayList<>(scaMethods);

        // Initialize collections
        this.accountActions = new ArrayList<>();
        this.activityLog = new ArrayList<>();

        // Set initial status
        this.consentStatus = ConsentStatus.RECEIVED;
        this.scaStatus = ScaStatus.RECEIVED;
        this.lastUpdated = OffsetDateTime.now();
    }

    /**
     * BIAN INITIATE behavior qualifier
     * Factory method to initiate account information consent
     */
    public static BianAccountInformationServices initiate(InitiateAccountInformationConsentRequest request) {
        validateInitiateRequest(request);

        AccountInformationInstanceReference instanceRef = AccountInformationInstanceReference.generate();
        ServiceDomainInstanceReference serviceDomainRef = 
            ServiceDomainInstanceReference.of("AccountInformationServices", instanceRef.getValue(), "1.0");

        String consentId = generateConsentId();

        BianAccountInformationServices ais = new BianAccountInformationServices(
            instanceRef,
            serviceDomainRef,
            request.getAccountReference(),
            consentId,
            request.getAvailableAccounts(),
            request.getAllPsd2(),
            request.isBalances(),
            request.isTransactions(),
            request.getValidUntil(),
            request.getFrequencyPerDay(),
            request.getCustomerReference(),
            request.getPsuData(),
            request.getTppInfo(),
            request.getScaMethods()
        );

        // Generate initial HATEOAS links
        ais.accountLinks = Links.builder()
            .self("/v1/consents/" + consentId)
            .status("/v1/consents/" + consentId + "/status")
            .startAuthorisation("/v1/consents/" + consentId + "/authorisations")
            .build();

        // Add initial activity log
        ais.addActivityLog(
            ServiceDomainActivityLog.of(
                "INITIATE",
                "INIT-" + instanceRef.getValue(),
                OffsetDateTime.now(),
                request.getInitiatedBy(),
                "Account information consent initiated",
                "COMPLETED"
            )
        );

        return ais;
    }

    /**
     * BIAN UPDATE behavior qualifier
     * Update account information consent
     */
    public void update(UpdateAccountInformationConsentRequest request) {
        validateUpdateRequest(request);

        ConsentStatus previousStatus = this.consentStatus;

        if (request.getNewConsentStatus() != null) {
            this.consentStatus = request.getNewConsentStatus();
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
        if (previousStatus != this.consentStatus) {
            Action statusChangeAction = Action.of(
                "STATUS_UPDATE",
                String.format("Consent status changed from %s to %s", previousStatus, this.consentStatus),
                "COMPLETED",
                OffsetDateTime.now(),
                request.getUpdatedBy()
            );
            this.accountActions.add(statusChangeAction);
        }

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "UPDATE",
                "UPD-" + id.getValue(),
                OffsetDateTime.now(),
                request.getUpdatedBy(),
                "Account information consent updated",
                "COMPLETED"
            )
        );
    }

    /**
     * BIAN RETRIEVE behavior qualifier
     * Retrieve account information
     */
    public AccountInformationRetrievalResult retrieve(RetrieveAccountInformationRequest request) {
        validateRetrieveRequest(request);

        if (this.consentStatus != ConsentStatus.VALID) {
            throw new IllegalStateException("Consent must be valid for information retrieval");
        }

        if (this.scaStatus != ScaStatus.FINALISED) {
            throw new IllegalStateException("SCA must be finalised for information retrieval");
        }

        // Check access frequency
        validateAccessFrequency();

        this.lastAccessDate = LocalDate.now();
        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getRetrievedBy();

        // Create retrieval action
        Action retrievalAction = Action.of(
            "RETRIEVAL",
            String.format("Account information retrieved: %s", request.getInformationType()),
            "COMPLETED",
            OffsetDateTime.now(),
            request.getRetrievedBy()
        );
        this.accountActions.add(retrievalAction);

        // Add activity log
        addActivityLog(
            ServiceDomainActivityLog.of(
                "RETRIEVE",
                "RET-" + id.getValue(),
                OffsetDateTime.now(),
                request.getRetrievedBy(),
                String.format("Account information %s retrieved", request.getInformationType()),
                "COMPLETED"
            )
        );

        return AccountInformationRetrievalResult.builder()
            .accountInformationInstanceReference(this.id)
            .consentId(this.consentId)
            .informationType(request.getInformationType())
            .retrievalDateTime(OffsetDateTime.now())
            .accountReference(this.accountReference)
            .retrievalAction(retrievalAction)
            .build();
    }

    /**
     * BIAN CONTROL behavior qualifier
     * Control consent (revoke, suspend, etc.)
     */
    public void control(ControlAccountInformationConsentRequest request) {
        validateControlRequest(request);

        ConsentStatus previousStatus = this.consentStatus;

        switch (request.getControlAction()) {
            case REVOKE -> {
                if (this.consentStatus == ConsentStatus.VALID || 
                    this.consentStatus == ConsentStatus.RECEIVED) {
                    this.consentStatus = ConsentStatus.REVOKEDBYPSU;
                } else {
                    throw new IllegalStateException("Cannot revoke consent in current status");
                }
            }
            case REJECT -> {
                if (this.consentStatus == ConsentStatus.RECEIVED) {
                    this.consentStatus = ConsentStatus.REJECTED;
                } else {
                    throw new IllegalStateException("Cannot reject consent in current status");
                }
            }
            case EXPIRE -> {
                this.consentStatus = ConsentStatus.EXPIRED;
            }
            default -> throw new IllegalArgumentException("Unsupported control action: " + request.getControlAction());
        }

        this.lastUpdated = OffsetDateTime.now();
        this.lastUpdatedBy = request.getControlledBy();

        // Add control action
        Action controlAction = Action.of(
            "CONTROL",
            String.format("Consent control action %s: status changed from %s to %s", 
                         request.getControlAction(), previousStatus, this.consentStatus),
            "COMPLETED",
            OffsetDateTime.now(),
            request.getControlledBy()
        );
        this.accountActions.add(controlAction);

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
     * Exchange SCA information for consent authorisation
     */
    public ConsentScaExchangeResult exchangeScaInformation(ExchangeConsentScaInformationRequest request) {
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
                    this.consentStatus = ConsentStatus.VALID;
                }
            }
            case FAIL -> {
                this.scaStatus = ScaStatus.FAILED;
                this.consentStatus = ConsentStatus.REJECTED;
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
        this.accountActions.add(scaAction);

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

        return ConsentScaExchangeResult.builder()
            .accountInformationInstanceReference(this.id)
            .consentId(this.consentId)
            .scaStatus(this.scaStatus)
            .consentStatus(this.consentStatus)
            .authorisationId(this.authorisationId)
            .scaAction(scaAction)
            .links(this.accountLinks)
            .build();
    }

    // Private helper methods
    private void addActivityLog(ServiceDomainActivityLog log) {
        this.activityLog.add(log);
    }

    private static String generateConsentId() {
        return "CNS-" + java.util.UUID.randomUUID().toString().toUpperCase().replace("-", "");
    }

    private void validateAccessFrequency() {
        // Implementation for frequency validation would go here
        // For now, we'll just update the access date
    }

    // Validation methods following DDD invariants
    private static void validateInitiateRequest(InitiateAccountInformationConsentRequest request) {
        Objects.requireNonNull(request, "Initiate consent request cannot be null");
        Objects.requireNonNull(request.getAccountReference(), "Account reference is required");
        Objects.requireNonNull(request.getValidUntil(), "Valid until date is required");
        Objects.requireNonNull(request.getPsuData(), "PSU data is required");
        Objects.requireNonNull(request.getTppInfo(), "TPP info is required");
        Objects.requireNonNull(request.getInitiatedBy(), "Initiated by is required");
        
        if (request.getFrequencyPerDay() <= 0) {
            throw new IllegalArgumentException("Frequency per day must be positive");
        }
    }

    private void validateUpdateRequest(UpdateAccountInformationConsentRequest request) {
        Objects.requireNonNull(request, "Update consent request cannot be null");
        Objects.requireNonNull(request.getUpdatedBy(), "Updated by is required");
    }

    private void validateRetrieveRequest(RetrieveAccountInformationRequest request) {
        Objects.requireNonNull(request, "Retrieve request cannot be null");
        Objects.requireNonNull(request.getInformationType(), "Information type is required");
        Objects.requireNonNull(request.getRetrievedBy(), "Retrieved by is required");
    }

    private void validateControlRequest(ControlAccountInformationConsentRequest request) {
        Objects.requireNonNull(request, "Control request cannot be null");
        Objects.requireNonNull(request.getControlAction(), "Control action is required");
        Objects.requireNonNull(request.getControlledBy(), "Controlled by is required");
    }

    private void validateScaExchangeRequest(ExchangeConsentScaInformationRequest request) {
        Objects.requireNonNull(request, "SCA exchange request cannot be null");
        Objects.requireNonNull(request.getScaAction(), "SCA action is required");
        Objects.requireNonNull(request.getExchangedBy(), "Exchanged by is required");
    }

    @Override
    public AccountInformationInstanceReference getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BianAccountInformationServices that = (BianAccountInformationServices) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BianAccountInformationServices{" +
                "id=" + id +
                ", consentId='" + consentId + '\'' +
                ", consentStatus=" + consentStatus +
                ", scaStatus=" + scaStatus +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    // Enums and supporting types
    public enum ConsentStatus {
        RECEIVED,
        REJECTED,
        VALID,
        REVOKEDBYPSU,
        EXPIRED,
        TERMINATEDBYTPP
    }

    public enum ConsentControlAction {
        REVOKE,
        REJECT,
        EXPIRE
    }

    public enum ScaAction {
        START_AUTHORISATION,
        SELECT_SCA_METHOD,
        AUTHENTICATE,
        FAIL
    }

    // Request/Response types for BIAN behavior qualifiers
    public record InitiateAccountInformationConsentRequest(
        AccountReference accountReference,
        List<String> availableAccounts,
        List<String> allPsd2,
        boolean balances,
        boolean transactions,
        LocalDate validUntil,
        int frequencyPerDay,
        CustomerReference customerReference,
        PsuData psuData,
        TppInfo tppInfo,
        List<AuthenticationObject> scaMethods,
        EmployeeBusinessUnitReference initiatedBy
    ) {}

    public record UpdateAccountInformationConsentRequest(
        ConsentStatus newConsentStatus,
        ScaStatus newScaStatus,
        String authorisationId,
        EmployeeBusinessUnitReference updatedBy
    ) {}

    public record RetrieveAccountInformationRequest(
        String informationType,
        EmployeeBusinessUnitReference retrievedBy
    ) {}

    public record ControlAccountInformationConsentRequest(
        ConsentControlAction controlAction,
        EmployeeBusinessUnitReference controlledBy
    ) {}

    public record ExchangeConsentScaInformationRequest(
        ScaAction scaAction,
        String authorisationId,
        EmployeeBusinessUnitReference exchangedBy
    ) {}

    public record AccountInformationRetrievalResult(
        AccountInformationInstanceReference accountInformationInstanceReference,
        String consentId,
        String informationType,
        OffsetDateTime retrievalDateTime,
        AccountReference accountReference,
        Action retrievalAction
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AccountInformationInstanceReference accountInformationInstanceReference;
            private String consentId;
            private String informationType;
            private OffsetDateTime retrievalDateTime;
            private AccountReference accountReference;
            private Action retrievalAction;

            public Builder accountInformationInstanceReference(AccountInformationInstanceReference accountInformationInstanceReference) {
                this.accountInformationInstanceReference = accountInformationInstanceReference;
                return this;
            }

            public Builder consentId(String consentId) {
                this.consentId = consentId;
                return this;
            }

            public Builder informationType(String informationType) {
                this.informationType = informationType;
                return this;
            }

            public Builder retrievalDateTime(OffsetDateTime retrievalDateTime) {
                this.retrievalDateTime = retrievalDateTime;
                return this;
            }

            public Builder accountReference(AccountReference accountReference) {
                this.accountReference = accountReference;
                return this;
            }

            public Builder retrievalAction(Action retrievalAction) {
                this.retrievalAction = retrievalAction;
                return this;
            }

            public AccountInformationRetrievalResult build() {
                return new AccountInformationRetrievalResult(accountInformationInstanceReference, consentId,
                        informationType, retrievalDateTime, accountReference, retrievalAction);
            }
        }
    }

    public record ConsentScaExchangeResult(
        AccountInformationInstanceReference accountInformationInstanceReference,
        String consentId,
        ScaStatus scaStatus,
        ConsentStatus consentStatus,
        String authorisationId,
        Action scaAction,
        Links links
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AccountInformationInstanceReference accountInformationInstanceReference;
            private String consentId;
            private ScaStatus scaStatus;
            private ConsentStatus consentStatus;
            private String authorisationId;
            private Action scaAction;
            private Links links;

            public Builder accountInformationInstanceReference(AccountInformationInstanceReference accountInformationInstanceReference) {
                this.accountInformationInstanceReference = accountInformationInstanceReference;
                return this;
            }

            public Builder consentId(String consentId) {
                this.consentId = consentId;
                return this;
            }

            public Builder scaStatus(ScaStatus scaStatus) {
                this.scaStatus = scaStatus;
                return this;
            }

            public Builder consentStatus(ConsentStatus consentStatus) {
                this.consentStatus = consentStatus;
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

            public ConsentScaExchangeResult build() {
                return new ConsentScaExchangeResult(accountInformationInstanceReference, consentId, scaStatus,
                        consentStatus, authorisationId, scaAction, links);
            }
        }
    }
}