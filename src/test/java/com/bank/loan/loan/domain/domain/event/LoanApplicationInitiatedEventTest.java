package com.bank.loanmanagement.loan.domain.event;

import com.bank.loanmanagement.loan.domain.shared.BianTypes.*;
import com.bank.loanmanagement.loan.domain.shared.BerlinGroupTypes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for LoanApplicationInitiatedEvent
 * Tests BIAN compliance, Berlin Group data structures, and FAPI security
 * Ensures 85%+ test coverage for domain events
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationInitiatedEvent Tests")
class LoanApplicationInitiatedEventTest {

    private LoanApplicationInitiatedEvent.LoanApplicationInitiatedData validEventData;
    private String aggregateId;
    private long version;

    @BeforeEach
    void setUp() {
        aggregateId = "CLA-TEST123456789";
        version = 1L;
        
        validEventData = createValidEventData();
    }

    @Nested
    @DisplayName("Event Creation Tests")
    class EventCreationTests {

        @Test
        @DisplayName("Should create event with valid data successfully")
        void shouldCreateEventWithValidData() {
            // When
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // Then
            assertThat(event).isNotNull();
            assertThat(event.getAggregateId()).isEqualTo(aggregateId);
            assertThat(event.getAggregateType()).isEqualTo("BianConsumerLoan");
            assertThat(event.getVersion()).isEqualTo(version);
            assertThat(event.getEventType()).isEqualTo("LoanApplicationInitiated");
            assertThat(event.getServiceDomain()).isEqualTo("ConsumerLoan");
            assertThat(event.getBehaviorQualifier()).isEqualTo("INITIATE");
            assertThat(event.getData()).isEqualTo(validEventData);
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getOccurredOn()).isNotNull();
        }

        @Test
        @DisplayName("Should create event with custom event ID and timestamp")
        void shouldCreateEventWithCustomEventIdAndTimestamp() {
            // Given
            String eventId = "EVENT-123";
            OffsetDateTime occurredOn = OffsetDateTime.now().minusHours(1);

            // When
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                eventId, aggregateId, occurredOn, version, validEventData);

            // Then
            assertThat(event.getEventId()).isEqualTo(eventId);
            assertThat(event.getOccurredOn()).isEqualTo(occurredOn);
            assertThat(event.getAggregateId()).isEqualTo(aggregateId);
            assertThat(event.getVersion()).isEqualTo(version);
        }

        @Test
        @DisplayName("Should fail when aggregate ID is null")
        void shouldFailWhenAggregateIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> new LoanApplicationInitiatedEvent(
                null, version, validEventData))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should fail when event data is null")
        void shouldFailWhenEventDataIsNull() {
            // When & Then
            assertThatThrownBy(() -> new LoanApplicationInitiatedEvent(
                aggregateId, version, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Event Data Factory Tests")
    class EventDataFactoryTests {

        @Test
        @DisplayName("Should create event data from parameters successfully")
        void shouldCreateEventDataFromParameters() {
            // Given
            String arrangementRef = "CLA-TEST123";
            CustomerReference customer = CustomerReference.of("CUST001", "INDIVIDUAL", "RETAIL");
            ProductServiceType productType = ProductServiceType.of("PERSONAL_LOAN", "Personal Loan Product", "StandardFeatures");
            CurrencyAndAmount amount = CurrencyAndAmount.of("EUR", new BigDecimal("50000"));
            Rate rate = Rate.percentage("INTEREST", new BigDecimal("5.5"), "ANNUAL");
            Schedule schedule = Schedule.of("MONTHLY", "Monthly payments", LocalDate.now(), LocalDate.now().plusYears(5), "MONTHLY");
            DateType maturity = DateType.of("MATURITY", LocalDate.now().plusYears(5));
            Agreement agreement = Agreement.of("LOAN_AGREEMENT", "LA001", "Personal loan agreement", 
                LocalDate.now(), LocalDate.now().plusYears(5), "ACTIVE", 
                Text.english("TERMS", "Standard loan terms and conditions"));
            AccountReference debtorAccount = AccountReference.iban("DE89370400440532013000", "EUR");
            AccountReference creditorAccount = AccountReference.iban("DE89370400440532014000", "EUR");
            PsuData psuData = PsuData.builder().psuId("PSU001").psuIpAddress("192.168.1.1").build();
            TppInfo tppInfo = TppInfo.builder().tppId("TPP001").tppName("Test TPP").build();
            EmployeeBusinessUnitReference initiatedBy = EmployeeBusinessUnitReference.of("EMP001", "LOAN_OFFICER");
            String fapiInteractionId = "FAPI-123";
            String clientId = "CLIENT-001";

            // When
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData eventData = 
                LoanApplicationInitiatedEvent.LoanApplicationInitiatedData.from(
                    arrangementRef, customer, productType, amount, rate, schedule, maturity,
                    agreement, debtorAccount, creditorAccount, psuData, tppInfo, initiatedBy,
                    fapiInteractionId, clientId);

            // Then
            assertThat(eventData).isNotNull();
            assertThat(eventData.consumerLoanArrangementInstanceReference()).isEqualTo(arrangementRef);
            assertThat(eventData.customerReference()).isEqualTo(customer);
            assertThat(eventData.loanProductType()).isEqualTo(productType);
            assertThat(eventData.loanPrincipalAmount()).isEqualTo(amount);
            assertThat(eventData.interestRate()).isEqualTo(rate);
            assertThat(eventData.repaymentSchedule()).isEqualTo(schedule);
            assertThat(eventData.maturityDate()).isEqualTo(maturity);
            assertThat(eventData.loanAgreement()).isEqualTo(agreement);
            assertThat(eventData.debtorAccount()).isEqualTo(debtorAccount);
            assertThat(eventData.creditorAccount()).isEqualTo(creditorAccount);
            assertThat(eventData.psuData()).isEqualTo(psuData);
            assertThat(eventData.tppInfo()).isEqualTo(tppInfo);
            assertThat(eventData.initiatedBy()).isEqualTo(initiatedBy);
            assertThat(eventData.fapiInteractionId()).isEqualTo(fapiInteractionId);
            assertThat(eventData.clientId()).isEqualTo(clientId);
            assertThat(eventData.serviceDomainInstanceReference()).isNotNull();
            assertThat(eventData.initiationDateTime()).isNotNull();
            assertThat(eventData.requestCorrelationId()).isNotNull();
            assertThat(eventData.eventSource()).isEqualTo("BankingLoanManagementSystem");
            assertThat(eventData.eventVersion()).isEqualTo("1.0");
            assertThat(eventData.regulatoryContext()).isEqualTo("PSD2_BIAN_CompliantLoanOrigination");
        }
    }

    @Nested
    @DisplayName("BIAN Compliance Tests")
    class BianComplianceTests {

        @Test
        @DisplayName("Should validate BIAN service domain compliance")
        void shouldValidateBianServiceDomainCompliance() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When & Then
            assertThat(event.getServiceDomain()).isEqualTo("ConsumerLoan");
            assertThat(event.getBehaviorQualifier()).isEqualTo("INITIATE");
            assertThat(event.getData().serviceDomainInstanceReference()).isNotNull();
            assertThat(event.getData().serviceDomainInstanceReference().serviceDomainName())
                .isEqualTo("ConsumerLoan");
        }

        @Test
        @DisplayName("Should contain required BIAN data elements")
        void shouldContainRequiredBianDataElements() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData data = event.getData();

            // Then
            assertThat(data.consumerLoanArrangementInstanceReference()).isNotNull();
            assertThat(data.customerReference()).isNotNull();
            assertThat(data.loanProductType()).isNotNull();
            assertThat(data.serviceDomainInstanceReference()).isNotNull();
            assertThat(data.initiatedBy()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Berlin Group Compliance Tests")
    class BerlinGroupComplianceTests {

        @Test
        @DisplayName("Should validate Berlin Group PSD2 compliance")
        void shouldValidateBerlinGroupPsd2Compliance() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData data = event.getData();

            // Then
            assertThat(data.debtorAccount()).isNotNull();
            assertThat(data.creditorAccount()).isNotNull();
            assertThat(data.psuData()).isNotNull();
            assertThat(data.tppInfo()).isNotNull();
            assertThat(data.debtorAccount().iban()).isNotNull();
            assertThat(data.creditorAccount().iban()).isNotNull();
        }

        @Test
        @DisplayName("Should validate ISO 20022 amount structure")
        void shouldValidateIso20022AmountStructure() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            CurrencyAndAmount amount = event.getData().loanPrincipalAmount();

            // Then
            assertThat(amount).isNotNull();
            assertThat(amount.currencyCode()).isEqualTo("EUR");
            assertThat(amount.amount()).isEqualTo(new BigDecimal("50000"));
        }
    }

    @Nested
    @DisplayName("FAPI Security Compliance Tests")
    class FapiSecurityComplianceTests {

        @Test
        @DisplayName("Should contain FAPI security context")
        void shouldContainFapiSecurityContext() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData data = event.getData();

            // Then
            assertThat(data.fapiInteractionId()).isNotNull();
            assertThat(data.clientId()).isNotNull();
            assertThat(data.requestCorrelationId()).isNotNull();
        }

        @Test
        @DisplayName("Should validate compliance summary")
        void shouldValidateComplianceSummary() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            LoanApplicationInitiatedEvent.RegulatoryComplianceSummary summary = 
                event.getData().getComplianceSummary();

            // Then
            assertThat(summary).isNotNull();
            assertThat(summary.bianCompliant()).isTrue();
            assertThat(summary.berlinGroupCompliant()).isTrue();
            assertThat(summary.fapiCompliant()).isTrue();
            assertThat(summary.complianceNotes()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Event Data Validation Tests")
    class EventDataValidationTests {

        @Test
        @DisplayName("Should validate compliant event data")
        void shouldValidateCompliantEventData() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            boolean isCompliant = event.getData().isCompliant();

            // Then
            assertThat(isCompliant).isTrue();
        }

        @Test
        @DisplayName("Should reject non-compliant event data missing customer reference")
        void shouldRejectNonCompliantEventDataMissingCustomerReference() {
            // Given
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData invalidData = 
                new LoanApplicationInitiatedEvent.LoanApplicationInitiatedData(
                    "CLA-TEST", null, validEventData.loanProductType(), 
                    validEventData.loanPrincipalAmount(), validEventData.interestRate(),
                    validEventData.repaymentSchedule(), validEventData.maturityDate(),
                    validEventData.loanAgreement(), validEventData.debtorAccount(),
                    validEventData.creditorAccount(), validEventData.psuData(),
                    validEventData.tppInfo(), validEventData.serviceDomainInstanceReference(),
                    validEventData.initiatedBy(), validEventData.initiationDateTime(),
                    validEventData.fapiInteractionId(), validEventData.clientId(),
                    validEventData.requestCorrelationId(), validEventData.eventSource(),
                    validEventData.eventVersion(), validEventData.regulatoryContext());

            // When
            boolean isCompliant = invalidData.isCompliant();

            // Then
            assertThat(isCompliant).isFalse();
        }

        @Test
        @DisplayName("Should reject non-compliant event data missing PSU data")
        void shouldRejectNonCompliantEventDataMissingPsuData() {
            // Given
            LoanApplicationInitiatedEvent.LoanApplicationInitiatedData invalidData = 
                new LoanApplicationInitiatedEvent.LoanApplicationInitiatedData(
                    validEventData.consumerLoanArrangementInstanceReference(), 
                    validEventData.customerReference(), validEventData.loanProductType(), 
                    validEventData.loanPrincipalAmount(), validEventData.interestRate(),
                    validEventData.repaymentSchedule(), validEventData.maturityDate(),
                    validEventData.loanAgreement(), validEventData.debtorAccount(),
                    validEventData.creditorAccount(), null, validEventData.tppInfo(),
                    validEventData.serviceDomainInstanceReference(), validEventData.initiatedBy(),
                    validEventData.initiationDateTime(), validEventData.fapiInteractionId(),
                    validEventData.clientId(), validEventData.requestCorrelationId(),
                    validEventData.eventSource(), validEventData.eventVersion(), 
                    validEventData.regulatoryContext());

            // When
            boolean isCompliant = invalidData.isCompliant();

            // Then
            assertThat(isCompliant).isFalse();
        }
    }

    @Nested
    @DisplayName("Event Equality and Hash Tests")
    class EventEqualityAndHashTests {

        @Test
        @DisplayName("Should be equal when event IDs are equal")
        void shouldBeEqualWhenEventIdsAreEqual() {
            // Given
            String eventId = "EVENT-123";
            LoanApplicationInitiatedEvent event1 = new LoanApplicationInitiatedEvent(
                eventId, aggregateId, OffsetDateTime.now(), version, validEventData);
            LoanApplicationInitiatedEvent event2 = new LoanApplicationInitiatedEvent(
                eventId, "DIFFERENT-AGGREGATE", OffsetDateTime.now(), 2L, validEventData);

            // When & Then
            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when event IDs are different")
        void shouldNotBeEqualWhenEventIdsAreDifferent() {
            // Given
            LoanApplicationInitiatedEvent event1 = new LoanApplicationInitiatedEvent(
                "EVENT-1", aggregateId, OffsetDateTime.now(), version, validEventData);
            LoanApplicationInitiatedEvent event2 = new LoanApplicationInitiatedEvent(
                "EVENT-2", aggregateId, OffsetDateTime.now(), version, validEventData);

            // When & Then
            assertThat(event1).isNotEqualTo(event2);
            assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode());
        }
    }

    @Nested
    @DisplayName("Event String Representation Tests")
    class EventStringRepresentationTests {

        @Test
        @DisplayName("Should contain key information in string representation")
        void shouldContainKeyInformationInStringRepresentation() {
            // Given
            LoanApplicationInitiatedEvent event = new LoanApplicationInitiatedEvent(
                aggregateId, version, validEventData);

            // When
            String eventString = event.toString();

            // Then
            assertThat(eventString).contains("LoanApplicationInitiated");
            assertThat(eventString).contains(aggregateId);
            assertThat(eventString).contains("eventId");
            assertThat(eventString).contains("version=" + version);
        }
    }

    private LoanApplicationInitiatedEvent.LoanApplicationInitiatedData createValidEventData() {
        return LoanApplicationInitiatedEvent.LoanApplicationInitiatedData.from(
            "CLA-TEST123456789",
            CustomerReference.of("CUST001", "INDIVIDUAL", "RETAIL"),
            ProductServiceType.of("PERSONAL_LOAN", "Personal Loan Product", "StandardFeatures"),
            CurrencyAndAmount.of("EUR", new BigDecimal("50000")),
            Rate.percentage("INTEREST", new BigDecimal("5.5"), "ANNUAL"),
            Schedule.of("MONTHLY", "Monthly payments", LocalDate.now(), LocalDate.now().plusYears(5), "MONTHLY"),
            DateType.of("MATURITY", LocalDate.now().plusYears(5)),
            Agreement.of("LOAN_AGREEMENT", "LA001", "Personal loan agreement", 
                LocalDate.now(), LocalDate.now().plusYears(5), "ACTIVE", 
                Text.english("TERMS", "Standard loan terms and conditions")),
            AccountReference.iban("DE89370400440532013000", "EUR"),
            AccountReference.iban("DE89370400440532014000", "EUR"),
            PsuData.builder().psuId("PSU001").psuIpAddress("192.168.1.1").build(),
            TppInfo.builder().tppId("TPP001").tppName("Test TPP").build(),
            EmployeeBusinessUnitReference.of("EMP001", "LOAN_OFFICER"),
            "FAPI-123456789",
            "CLIENT-001"
        );
    }
}