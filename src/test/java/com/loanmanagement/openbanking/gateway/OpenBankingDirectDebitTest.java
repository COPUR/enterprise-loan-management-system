package com.loanmanagement.openbanking.gateway;

import com.loanmanagement.openbanking.domain.model.*;
import com.loanmanagement.openbanking.gateway.service.OpenBankingDirectDebitService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Open Banking Direct Debit Service
 * Tests direct debit mandates, collections, and management
 */
@DisplayName("Open Banking Direct Debit Tests")
class OpenBankingDirectDebitTest {

    private OpenBankingDirectDebitService directDebitService;
    private OpenBankingCustomerId testCustomerId;
    private BankAccountId testAccountId;
    private ConsentId testConsentId;
    private Money testAmount;

    @BeforeEach
    void setUp() {
        directDebitService = new OpenBankingDirectDebitService();
        testCustomerId = OpenBankingCustomerId.generate();
        testAccountId = BankAccountId.generate();
        testConsentId = ConsentId.generate();
        testAmount = Money.of("GBP", new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Direct Debit Mandate Tests")
    class DirectDebitMandateTests {

        @Test
        @DisplayName("Should create direct debit mandate successfully")
        void shouldCreateDirectDebitMandateSuccessfully() {
            // Given
            DirectDebitMandateRequest mandateRequest = DirectDebitMandateRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount())
                    .creditorAccount(createCreditorAccount())
                    .mandateReference("MANDATE-" + System.currentTimeMillis())
                    .maximumIndividualAmount(Money.of("GBP", new BigDecimal("1000.00")))
                    .periodicLimits(createPeriodicLimits())
                    .mandate(createMandateDetails())
                    .consentId(testConsentId)
                    .build();

            // When
            DirectDebitMandateResponse mandateResponse = directDebitService
                    .createDirectDebitMandate(mandateRequest);

            // Then
            assertNotNull(mandateResponse);
            assertEquals(OpenBankingStatus.SUCCESS, mandateResponse.getStatus());
            assertNotNull(mandateResponse.getMandateId());
            assertNotNull(mandateResponse.getMandateReference());
            assertEquals(DirectDebitMandateStatus.AWAITING_AUTHORIZATION, mandateResponse.getMandateStatus());
            assertNotNull(mandateResponse.getCreationDateTime());
            assertNotNull(mandateResponse.getAuthorizationUrl());
        }

        @Test
        @DisplayName("Should authorize direct debit mandate with SCA")
        void shouldAuthorizeDirectDebitMandateWithSCA() {
            // Given
            DirectDebitMandateId mandateId = DirectDebitMandateId.generate();
            DirectDebitMandateAuthorizationRequest authRequest = DirectDebitMandateAuthorizationRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .scaMethod(SCAMethod.SMS_OTP)
                    .scaAuthenticationData("123456")
                    .consentId(testConsentId)
                    .build();

            // When
            DirectDebitMandateAuthorizationResponse authResponse = directDebitService
                    .authorizeDirectDebitMandate(authRequest);

            // Then
            assertNotNull(authResponse);
            assertEquals(OpenBankingStatus.SUCCESS, authResponse.getStatus());
            assertEquals(DirectDebitMandateStatus.AUTHORIZED, authResponse.getMandateStatus());
            assertNotNull(authResponse.getAuthorizationId());
            assertNotNull(authResponse.getAuthorizationDateTime());
            assertEquals(SCAStatus.FINALISED, authResponse.getScaStatus());
        }

        @Test
        @DisplayName("Should retrieve direct debit mandate details")
        void shouldRetrieveDirectDebitMandateDetails() {
            // Given
            DirectDebitMandateId mandateId = DirectDebitMandateId.generate();
            DirectDebitMandateDetailsRequest detailsRequest = DirectDebitMandateDetailsRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .consentId(testConsentId)
                    .includeCollectionHistory(true)
                    .build();

            // When
            DirectDebitMandateDetailsResponse detailsResponse = directDebitService
                    .getDirectDebitMandateDetails(detailsRequest);

            // Then
            assertNotNull(detailsResponse);
            assertEquals(OpenBankingStatus.SUCCESS, detailsResponse.getStatus());
            assertNotNull(detailsResponse.getMandateDetails());
            assertNotNull(detailsResponse.getMandateDetails().getMandateId());
            assertNotNull(detailsResponse.getMandateDetails().getDebtorAccount());
            assertNotNull(detailsResponse.getMandateDetails().getCreditorAccount());
            assertNotNull(detailsResponse.getCollectionHistory());
        }

        @Test
        @DisplayName("Should cancel direct debit mandate")
        void shouldCancelDirectDebitMandate() {
            // Given
            DirectDebitMandateId mandateId = DirectDebitMandateId.generate();
            DirectDebitMandateCancellationRequest cancellationRequest = DirectDebitMandateCancellationRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .cancellationReason("Customer request")
                    .consentId(testConsentId)
                    .build();

            // When
            DirectDebitMandateCancellationResponse cancellationResponse = directDebitService
                    .cancelDirectDebitMandate(cancellationRequest);

            // Then
            assertNotNull(cancellationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, cancellationResponse.getStatus());
            assertEquals(DirectDebitMandateStatus.CANCELLED, cancellationResponse.getNewMandateStatus());
            assertNotNull(cancellationResponse.getCancellationDateTime());
            assertTrue(cancellationResponse.isCancellationSuccessful());
        }
    }

    @Nested
    @DisplayName("Direct Debit Collection Tests")
    class DirectDebitCollectionTests {

        @Test
        @DisplayName("Should initiate direct debit collection")
        void shouldInitiateDirectDebitCollection() {
            // Given
            DirectDebitCollectionRequest collectionRequest = DirectDebitCollectionRequest.builder()
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .collectionAmount(testAmount)
                    .collectionDate(LocalDate.now().plusDays(3))
                    .collectionReference("COLLECTION-" + System.currentTimeMillis())
                    .ultimateCreditorName("Loan Management System")
                    .remittanceInformation("Monthly loan payment")
                    .build();

            // When
            DirectDebitCollectionResponse collectionResponse = directDebitService
                    .initiateDirectDebitCollection(collectionRequest);

            // Then
            assertNotNull(collectionResponse);
            assertEquals(OpenBankingStatus.SUCCESS, collectionResponse.getStatus());
            assertNotNull(collectionResponse.getCollectionId());
            assertNotNull(collectionResponse.getCollectionReference());
            assertEquals(DirectDebitCollectionStatus.ACCEPTED, collectionResponse.getCollectionStatus());
            assertNotNull(collectionResponse.getExpectedCollectionDate());
        }

        @Test
        @DisplayName("Should handle direct debit collection with insufficient funds")
        void shouldHandleDirectDebitCollectionWithInsufficientFunds() {
            // Given
            Money largeAmount = Money.of("GBP", new BigDecimal("10000.00"));
            DirectDebitCollectionRequest largeCollectionRequest = DirectDebitCollectionRequest.builder()
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .collectionAmount(largeAmount)
                    .collectionDate(LocalDate.now().plusDays(1))
                    .collectionReference("LARGE-COLLECTION-" + System.currentTimeMillis())
                    .build();

            // When
            DirectDebitCollectionResponse collectionResponse = directDebitService
                    .initiateDirectDebitCollection(largeCollectionRequest);

            // Then
            assertNotNull(collectionResponse);
            if (collectionResponse.getCollectionStatus() == DirectDebitCollectionStatus.REJECTED) {
                assertEquals(OpenBankingStatus.REJECTED, collectionResponse.getStatus());
                assertEquals(DirectDebitRejectionCode.INSUFFICIENT_FUNDS, collectionResponse.getRejectionCode());
                assertNotNull(collectionResponse.getRejectionReason());
            }
        }

        @Test
        @DisplayName("Should retrieve direct debit collection status")
        void shouldRetrieveDirectDebitCollectionStatus() {
            // Given
            DirectDebitCollectionId collectionId = DirectDebitCollectionId.generate();
            DirectDebitCollectionStatusRequest statusRequest = DirectDebitCollectionStatusRequest.builder()
                    .collectionId(collectionId)
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .build();

            // When
            DirectDebitCollectionStatusResponse statusResponse = directDebitService
                    .getDirectDebitCollectionStatus(statusRequest);

            // Then
            assertNotNull(statusResponse);
            assertEquals(OpenBankingStatus.SUCCESS, statusResponse.getStatus());
            assertNotNull(statusResponse.getCollectionStatus());
            assertNotNull(statusResponse.getLastStatusUpdate());
            assertNotNull(statusResponse.getCollectionDetails());
        }

        @Test
        @DisplayName("Should handle recurring direct debit collections")
        void shouldHandleRecurringDirectDebitCollections() {
            // Given
            RecurringDirectDebitRequest recurringRequest = RecurringDirectDebitRequest.builder()
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .collectionAmount(testAmount)
                    .frequency(CollectionFrequency.MONTHLY)
                    .startDate(LocalDate.now().plusDays(7))
                    .endDate(LocalDate.now().plusMonths(12))
                    .dayOfMonth(15)
                    .recurringReference("RECURRING-LOAN-PAYMENT")
                    .build();

            // When
            RecurringDirectDebitResponse recurringResponse = directDebitService
                    .setupRecurringDirectDebit(recurringRequest);

            // Then
            assertNotNull(recurringResponse);
            assertEquals(OpenBankingStatus.SUCCESS, recurringResponse.getStatus());
            assertNotNull(recurringResponse.getRecurringCollectionId());
            assertEquals(RecurringCollectionStatus.ACTIVE, recurringResponse.getRecurringStatus());
            assertNotNull(recurringResponse.getNextCollectionDate());
            assertTrue(recurringResponse.getScheduledCollections().size() > 0);
        }
    }

    @Nested
    @DisplayName("Direct Debit Management Tests")
    class DirectDebitManagementTests {

        @Test
        @DisplayName("Should list all direct debit mandates for customer")
        void shouldListAllDirectDebitMandatesForCustomer() {
            // Given
            DirectDebitMandateListRequest listRequest = DirectDebitMandateListRequest.builder()
                    .customerId(testCustomerId)
                    .includeInactive(false)
                    .filterByStatus(List.of(DirectDebitMandateStatus.AUTHORIZED, 
                                          DirectDebitMandateStatus.ACTIVE))
                    .build();

            // When
            DirectDebitMandateListResponse listResponse = directDebitService
                    .listDirectDebitMandates(listRequest);

            // Then
            assertNotNull(listResponse);
            assertEquals(OpenBankingStatus.SUCCESS, listResponse.getStatus());
            assertNotNull(listResponse.getMandates());
            assertNotNull(listResponse.getTotalCount());
            
            if (!listResponse.getMandates().isEmpty()) {
                DirectDebitMandateSummary firstMandate = listResponse.getMandates().get(0);
                assertNotNull(firstMandate.getMandateId());
                assertNotNull(firstMandate.getMandateReference());
                assertNotNull(firstMandate.getStatus());
                assertNotNull(firstMandate.getCreditorName());
            }
        }

        @Test
        @DisplayName("Should update direct debit mandate limits")
        void shouldUpdateDirectDebitMandateLimits() {
            // Given
            DirectDebitMandateId mandateId = DirectDebitMandateId.generate();
            DirectDebitMandateUpdateRequest updateRequest = DirectDebitMandateUpdateRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .newMaximumIndividualAmount(Money.of("GBP", new BigDecimal("1500.00")))
                    .newPeriodicLimits(createUpdatedPeriodicLimits())
                    .updateReason("Customer requested higher limits")
                    .build();

            // When
            DirectDebitMandateUpdateResponse updateResponse = directDebitService
                    .updateDirectDebitMandate(updateRequest);

            // Then
            assertNotNull(updateResponse);
            assertEquals(OpenBankingStatus.SUCCESS, updateResponse.getStatus());
            assertTrue(updateResponse.isUpdateSuccessful());
            assertNotNull(updateResponse.getUpdatedMandate());
            assertEquals(Money.of("GBP", new BigDecimal("1500.00")), 
                        updateResponse.getUpdatedMandate().getMaximumIndividualAmount());
        }

        @Test
        @DisplayName("Should suspend and resume direct debit mandate")
        void shouldSuspendAndResumeDirectDebitMandate() {
            // Given
            DirectDebitMandateId mandateId = DirectDebitMandateId.generate();
            
            // Suspend mandate
            DirectDebitMandateSuspensionRequest suspensionRequest = DirectDebitMandateSuspensionRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .suspensionReason("Temporary cash flow issues")
                    .suspensionPeriod(SuspensionPeriod.TEMPORARY)
                    .build();

            // When - Suspend
            DirectDebitMandateSuspensionResponse suspensionResponse = directDebitService
                    .suspendDirectDebitMandate(suspensionRequest);

            // Then - Suspend
            assertNotNull(suspensionResponse);
            assertEquals(OpenBankingStatus.SUCCESS, suspensionResponse.getStatus());
            assertEquals(DirectDebitMandateStatus.SUSPENDED, suspensionResponse.getNewMandateStatus());
            
            // Given - Resume
            DirectDebitMandateResumeRequest resumeRequest = DirectDebitMandateResumeRequest.builder()
                    .mandateId(mandateId)
                    .customerId(testCustomerId)
                    .resumeReason("Cash flow restored")
                    .build();

            // When - Resume
            DirectDebitMandateResumeResponse resumeResponse = directDebitService
                    .resumeDirectDebitMandate(resumeRequest);

            // Then - Resume
            assertNotNull(resumeResponse);
            assertEquals(OpenBankingStatus.SUCCESS, resumeResponse.getStatus());
            assertEquals(DirectDebitMandateStatus.ACTIVE, resumeResponse.getNewMandateStatus());
            assertTrue(resumeResponse.isResumeSuccessful());
        }

        @Test
        @DisplayName("Should generate direct debit collection reports")
        void shouldGenerateDirectDebitCollectionReports() {
            // Given
            DirectDebitReportRequest reportRequest = DirectDebitReportRequest.builder()
                    .customerId(testCustomerId)
                    .reportType(DirectDebitReportType.COLLECTION_SUMMARY)
                    .fromDate(LocalDate.now().minusMonths(3))
                    .toDate(LocalDate.now())
                    .includeRejections(true)
                    .includeCharges(true)
                    .groupByMandate(true)
                    .build();

            // When
            DirectDebitReportResponse reportResponse = directDebitService
                    .generateDirectDebitReport(reportRequest);

            // Then
            assertNotNull(reportResponse);
            assertEquals(OpenBankingStatus.SUCCESS, reportResponse.getStatus());
            assertNotNull(reportResponse.getReportData());
            assertNotNull(reportResponse.getSummaryStatistics());
            assertTrue(reportResponse.getTotalCollections() >= 0);
            assertTrue(reportResponse.getSuccessfulCollections() >= 0);
            assertTrue(reportResponse.getSuccessRate() >= 0.0);
            assertTrue(reportResponse.getSuccessRate() <= 100.0);
        }
    }

    @Nested
    @DisplayName("Direct Debit Compliance Tests")
    class DirectDebitComplianceTests {

        @Test
        @DisplayName("Should validate direct debit mandate compliance with regulations")
        void shouldValidateDirectDebitMandateComplianceWithRegulations() {
            // Given
            DirectDebitComplianceRequest complianceRequest = DirectDebitComplianceRequest.builder()
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .regulatoryFramework(RegulatoryFramework.PSD2)
                    .validateSCARequirements(true)
                    .validateMandateLimits(true)
                    .validateNotificationRequirements(true)
                    .build();

            // When
            DirectDebitComplianceResponse complianceResponse = directDebitService
                    .validateDirectDebitCompliance(complianceRequest);

            // Then
            assertNotNull(complianceResponse);
            assertEquals(OpenBankingStatus.SUCCESS, complianceResponse.getStatus());
            assertTrue(complianceResponse.isCompliant());
            assertNotNull(complianceResponse.getComplianceChecks());
            assertTrue(complianceResponse.getComplianceChecks().stream()
                    .allMatch(check -> check.getStatus() == ComplianceCheckStatus.PASSED));
        }

        @Test
        @DisplayName("Should enforce advance notification requirements")
        void shouldEnforceAdvanceNotificationRequirements() {
            // Given
            DirectDebitNotificationRequest notificationRequest = DirectDebitNotificationRequest.builder()
                    .mandateId(DirectDebitMandateId.generate())
                    .customerId(testCustomerId)
                    .collectionAmount(testAmount)
                    .collectionDate(LocalDate.now().plusDays(5))
                    .notificationType(NotificationType.ADVANCE_NOTICE)
                    .minimumNoticeDays(3)
                    .build();

            // When
            DirectDebitNotificationResponse notificationResponse = directDebitService
                    .sendDirectDebitNotification(notificationRequest);

            // Then
            assertNotNull(notificationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, notificationResponse.getStatus());
            assertTrue(notificationResponse.isNotificationSent());
            assertNotNull(notificationResponse.getNotificationId());
            assertNotNull(notificationResponse.getSentDateTime());
            assertTrue(notificationResponse.isComplianceRequirementsMet());
        }

        @Test
        @DisplayName("Should handle indemnity claims for direct debit errors")
        void shouldHandleIndemnityClaimsForDirectDebitErrors() {
            // Given
            DirectDebitIndemnityClaimRequest claimRequest = DirectDebitIndemnityClaimRequest.builder()
                    .customerId(testCustomerId)
                    .collectionId(DirectDebitCollectionId.generate())
                    .claimType(IndemnityClaimType.UNAUTHORIZED_COLLECTION)
                    .claimAmount(testAmount)
                    .claimReason("Collection made without proper authorization")
                    .supportingDocuments(List.of("bank_statement.pdf", "mandate_copy.pdf"))
                    .build();

            // When
            DirectDebitIndemnityClaimResponse claimResponse = directDebitService
                    .processIndemnityClaimRequest(claimRequest);

            // Then
            assertNotNull(claimResponse);
            assertEquals(OpenBankingStatus.SUCCESS, claimResponse.getStatus());
            assertNotNull(claimResponse.getClaimId());
            assertEquals(IndemnityClaimStatus.UNDER_REVIEW, claimResponse.getClaimStatus());
            assertNotNull(claimResponse.getEstimatedResolutionDate());
            assertNotNull(claimResponse.getClaimReference());
        }
    }

    // Helper methods
    private BankAccount createTestAccount() {
        return BankAccount.builder()
                .accountId(testAccountId)
                .accountNumber("12345678")
                .sortCode("123456")
                .accountName("Test Customer Account")
                .currency("GBP")
                .accountType(AccountType.CURRENT)
                .build();
    }

    private BankAccount createCreditorAccount() {
        return BankAccount.builder()
                .accountId(BankAccountId.generate())
                .accountNumber("87654321")
                .sortCode("654321")
                .accountName("Loan Management Ltd")
                .currency("GBP")
                .accountType(AccountType.BUSINESS)
                .build();
    }

    private PeriodicLimits createPeriodicLimits() {
        return PeriodicLimits.builder()
                .dailyLimit(Money.of("GBP", new BigDecimal("500.00")))
                .weeklyLimit(Money.of("GBP", new BigDecimal("2000.00")))
                .monthlyLimit(Money.of("GBP", new BigDecimal("5000.00")))
                .build();
    }

    private PeriodicLimits createUpdatedPeriodicLimits() {
        return PeriodicLimits.builder()
                .dailyLimit(Money.of("GBP", new BigDecimal("750.00")))
                .weeklyLimit(Money.of("GBP", new BigDecimal("3000.00")))
                .monthlyLimit(Money.of("GBP", new BigDecimal("7500.00")))
                .build();
    }

    private MandateDetails createMandateDetails() {
        return MandateDetails.builder()
                .creditorName("Loan Management System Ltd")
                .creditorIdentifier("GB98ZZZ123456789012")
                .mandateReference("MANDATE-LMS-" + System.currentTimeMillis())
                .scheme("BACS")
                .categoryPurpose("LOAN")
                .build();
    }
}