package com.loanmanagement.openbanking.gateway;

import com.loanmanagement.openbanking.domain.model.*;
import com.loanmanagement.openbanking.gateway.service.OpenBankingStandingOrderService;
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
 * Tests for Open Banking Standing Order Service
 * Tests standing order creation, management, and lifecycle
 */
@DisplayName("Open Banking Standing Order Tests")
class OpenBankingStandingOrderTest {

    private OpenBankingStandingOrderService standingOrderService;
    private OpenBankingCustomerId testCustomerId;
    private BankAccountId testAccountId;
    private ConsentId testConsentId;
    private Money testAmount;

    @BeforeEach
    void setUp() {
        standingOrderService = new OpenBankingStandingOrderService();
        testCustomerId = OpenBankingCustomerId.generate();
        testAccountId = BankAccountId.generate();
        testConsentId = ConsentId.generate();
        testAmount = Money.of("GBP", new BigDecimal("750.00"));
    }

    @Nested
    @DisplayName("Standing Order Creation Tests")
    class StandingOrderCreationTests {

        @Test
        @DisplayName("Should create domestic standing order successfully")
        void shouldCreateDomesticStandingOrderSuccessfully() {
            // Given
            DomesticStandingOrderRequest standingOrderRequest = DomesticStandingOrderRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount())
                    .creditorAccount(createCreditorAccount())
                    .instructedAmount(testAmount)
                    .frequency(StandingOrderFrequency.MONTHLY)
                    .firstPaymentDate(LocalDate.now().plusDays(7))
                    .finalPaymentDate(LocalDate.now().plusYears(1))
                    .reference("Monthly Loan Payment")
                    .numberOfPayments(12)
                    .consentId(testConsentId)
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .build();

            // When
            StandingOrderResponse standingOrderResponse = standingOrderService
                    .createDomesticStandingOrder(standingOrderRequest);

            // Then
            assertNotNull(standingOrderResponse);
            assertEquals(OpenBankingStatus.SUCCESS, standingOrderResponse.getStatus());
            assertNotNull(standingOrderResponse.getStandingOrderId());
            assertEquals(StandingOrderStatus.AWAITING_AUTHORIZATION, standingOrderResponse.getStandingOrderStatus());
            assertNotNull(standingOrderResponse.getCreationDateTime());
            assertNotNull(standingOrderResponse.getAuthorizationUrl());
            assertEquals(12, standingOrderResponse.getNumberOfPayments());
        }

        @Test
        @DisplayName("Should create international standing order with currency conversion")
        void shouldCreateInternationalStandingOrderWithCurrencyConversion() {
            // Given
            InternationalStandingOrderRequest internationalRequest = InternationalStandingOrderRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(createTestAccount())
                    .creditorAccount(createInternationalCreditorAccount())
                    .instructedAmount(Money.of("EUR", new BigDecimal("650.00")))
                    .exchangeRateInformation(createExchangeRateInfo())
                    .frequency(StandingOrderFrequency.MONTHLY)
                    .firstPaymentDate(LocalDate.now().plusDays(14))
                    .finalPaymentDate(LocalDate.now().plusMonths(24))
                    .chargeBearer(ChargeBearer.SHARED)
                    .purpose(PaymentPurpose.LOAN_PAYMENT)
                    .consentId(testConsentId)
                    .build();

            // When
            StandingOrderResponse standingOrderResponse = standingOrderService
                    .createInternationalStandingOrder(internationalRequest);

            // Then
            assertNotNull(standingOrderResponse);
            assertEquals(OpenBankingStatus.SUCCESS, standingOrderResponse.getStatus());
            assertNotNull(standingOrderResponse.getStandingOrderId());
            assertEquals(StandingOrderStatus.AWAITING_AUTHORIZATION, standingOrderResponse.getStandingOrderStatus());
            assertNotNull(standingOrderResponse.getExchangeRateInformation());
            assertEquals(ChargeBearer.SHARED, standingOrderResponse.getChargeBearer());
        }

        @Test
        @DisplayName("Should authorize standing order with SCA")
        void shouldAuthorizeStandingOrderWithSCA() {
            // Given
            StandingOrderId standingOrderId = StandingOrderId.generate();
            StandingOrderAuthorizationRequest authRequest = StandingOrderAuthorizationRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .scaMethod(SCAMethod.BIOMETRIC)
                    .scaAuthenticationData("fingerprint_hash_12345")
                    .consentId(testConsentId)
                    .build();

            // When
            StandingOrderAuthorizationResponse authResponse = standingOrderService
                    .authorizeStandingOrder(authRequest);

            // Then
            assertNotNull(authResponse);
            assertEquals(OpenBankingStatus.SUCCESS, authResponse.getStatus());
            assertEquals(StandingOrderStatus.ACTIVE, authResponse.getStandingOrderStatus());
            assertNotNull(authResponse.getAuthorizationId());
            assertNotNull(authResponse.getAuthorizationDateTime());
            assertEquals(SCAStatus.FINALISED, authResponse.getScaStatus());
            assertNotNull(authResponse.getNextPaymentDate());
        }

        @Test
        @DisplayName("Should handle standing order creation with invalid account")
        void shouldHandleStandingOrderCreationWithInvalidAccount() {
            // Given
            BankAccount invalidAccount = BankAccount.builder()
                    .accountId(BankAccountId.generate())
                    .accountNumber("00000000")
                    .sortCode("000000")
                    .accountName("Invalid Account")
                    .currency("GBP")
                    .accountType(AccountType.CURRENT)
                    .build();

            DomesticStandingOrderRequest invalidRequest = DomesticStandingOrderRequest.builder()
                    .customerId(testCustomerId)
                    .debtorAccount(invalidAccount)
                    .creditorAccount(createCreditorAccount())
                    .instructedAmount(testAmount)
                    .frequency(StandingOrderFrequency.MONTHLY)
                    .firstPaymentDate(LocalDate.now().plusDays(7))
                    .consentId(testConsentId)
                    .build();

            // When
            StandingOrderResponse standingOrderResponse = standingOrderService
                    .createDomesticStandingOrder(invalidRequest);

            // Then
            assertNotNull(standingOrderResponse);
            assertEquals(OpenBankingStatus.INVALID_ACCOUNT, standingOrderResponse.getStatus());
            assertEquals(OpenBankingErrorCode.INVALID_DEBTOR_ACCOUNT, standingOrderResponse.getErrorCode());
            assertNotNull(standingOrderResponse.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Standing Order Management Tests")
    class StandingOrderManagementTests {

        @Test
        @DisplayName("Should retrieve standing order details")
        void shouldRetrieveStandingOrderDetails() {
            // Given
            StandingOrderId standingOrderId = StandingOrderId.generate();
            StandingOrderDetailsRequest detailsRequest = StandingOrderDetailsRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .consentId(testConsentId)
                    .includePaymentHistory(true)
                    .includeUpcomingPayments(true)
                    .build();

            // When
            StandingOrderDetailsResponse detailsResponse = standingOrderService
                    .getStandingOrderDetails(detailsRequest);

            // Then
            assertNotNull(detailsResponse);
            assertEquals(OpenBankingStatus.SUCCESS, detailsResponse.getStatus());
            assertNotNull(detailsResponse.getStandingOrderDetails());
            assertNotNull(detailsResponse.getStandingOrderDetails().getStandingOrderId());
            assertNotNull(detailsResponse.getPaymentHistory());
            assertNotNull(detailsResponse.getUpcomingPayments());
            assertTrue(detailsResponse.getUpcomingPayments().size() > 0);
        }

        @Test
        @DisplayName("Should list all standing orders for customer")
        void shouldListAllStandingOrdersForCustomer() {
            // Given
            StandingOrderListRequest listRequest = StandingOrderListRequest.builder()
                    .customerId(testCustomerId)
                    .accountId(testAccountId)
                    .filterByStatus(List.of(StandingOrderStatus.ACTIVE, StandingOrderStatus.PENDING))
                    .includeInactive(false)
                    .sortBy(StandingOrderSortBy.NEXT_PAYMENT_DATE)
                    .build();

            // When
            StandingOrderListResponse listResponse = standingOrderService
                    .listStandingOrders(listRequest);

            // Then
            assertNotNull(listResponse);
            assertEquals(OpenBankingStatus.SUCCESS, listResponse.getStatus());
            assertNotNull(listResponse.getStandingOrders());
            assertNotNull(listResponse.getTotalCount());
            
            if (!listResponse.getStandingOrders().isEmpty()) {
                StandingOrderSummary firstStandingOrder = listResponse.getStandingOrders().get(0);
                assertNotNull(firstStandingOrder.getStandingOrderId());
                assertNotNull(firstStandingOrder.getReference());
                assertNotNull(firstStandingOrder.getStatus());
                assertNotNull(firstStandingOrder.getNextPaymentDate());
            }
        }

        @Test
        @DisplayName("Should modify standing order amount and frequency")
        void shouldModifyStandingOrderAmountAndFrequency() {
            // Given
            StandingOrderId standingOrderId = StandingOrderId.generate();
            StandingOrderModificationRequest modificationRequest = StandingOrderModificationRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .newInstructedAmount(Money.of("GBP", new BigDecimal("850.00")))
                    .newFrequency(StandingOrderFrequency.BI_WEEKLY)
                    .effectiveDate(LocalDate.now().plusDays(14))
                    .modificationReason("Customer requested payment increase")
                    .consentId(testConsentId)
                    .build();

            // When
            StandingOrderModificationResponse modificationResponse = standingOrderService
                    .modifyStandingOrder(modificationRequest);

            // Then
            assertNotNull(modificationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, modificationResponse.getStatus());
            assertTrue(modificationResponse.isModificationSuccessful());
            assertNotNull(modificationResponse.getModificationId());
            assertEquals(Money.of("GBP", new BigDecimal("850.00")), 
                        modificationResponse.getNewInstructedAmount());
            assertEquals(StandingOrderFrequency.BI_WEEKLY, modificationResponse.getNewFrequency());
        }

        @Test
        @DisplayName("Should suspend and resume standing order")
        void shouldSuspendAndResumeStandingOrder() {
            // Given
            StandingOrderId standingOrderId = StandingOrderId.generate();
            
            // Suspend standing order
            StandingOrderSuspensionRequest suspensionRequest = StandingOrderSuspensionRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .suspensionReason("Temporary financial difficulty")
                    .suspensionStartDate(LocalDate.now().plusDays(5))
                    .suspensionEndDate(LocalDate.now().plusMonths(2))
                    .build();

            // When - Suspend
            StandingOrderSuspensionResponse suspensionResponse = standingOrderService
                    .suspendStandingOrder(suspensionRequest);

            // Then - Suspend
            assertNotNull(suspensionResponse);
            assertEquals(OpenBankingStatus.SUCCESS, suspensionResponse.getStatus());
            assertEquals(StandingOrderStatus.SUSPENDED, suspensionResponse.getNewStatus());
            assertNotNull(suspensionResponse.getSuspensionId());
            
            // Given - Resume
            StandingOrderResumeRequest resumeRequest = StandingOrderResumeRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .resumeDate(LocalDate.now().plusDays(30))
                    .resumeReason("Financial situation improved")
                    .build();

            // When - Resume
            StandingOrderResumeResponse resumeResponse = standingOrderService
                    .resumeStandingOrder(resumeRequest);

            // Then - Resume
            assertNotNull(resumeResponse);
            assertEquals(OpenBankingStatus.SUCCESS, resumeResponse.getStatus());
            assertEquals(StandingOrderStatus.ACTIVE, resumeResponse.getNewStatus());
            assertTrue(resumeResponse.isResumeSuccessful());
            assertNotNull(resumeResponse.getNextPaymentDate());
        }

        @Test
        @DisplayName("Should cancel standing order")
        void shouldCancelStandingOrder() {
            // Given
            StandingOrderId standingOrderId = StandingOrderId.generate();
            StandingOrderCancellationRequest cancellationRequest = StandingOrderCancellationRequest.builder()
                    .standingOrderId(standingOrderId)
                    .customerId(testCustomerId)
                    .cancellationReason("Loan paid off early")
                    .cancellationDate(LocalDate.now().plusDays(3))
                    .consentId(testConsentId)
                    .build();

            // When
            StandingOrderCancellationResponse cancellationResponse = standingOrderService
                    .cancelStandingOrder(cancellationRequest);

            // Then
            assertNotNull(cancellationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, cancellationResponse.getStatus());
            assertEquals(StandingOrderStatus.CANCELLED, cancellationResponse.getNewStatus());
            assertNotNull(cancellationResponse.getCancellationDateTime());
            assertTrue(cancellationResponse.isCancellationSuccessful());
        }
    }

    @Nested
    @DisplayName("Standing Order Payment Processing Tests")
    class StandingOrderPaymentProcessingTests {

        @Test
        @DisplayName("Should process scheduled standing order payment")
        void shouldProcessScheduledStandingOrderPayment() {
            // Given
            StandingOrderPaymentRequest paymentRequest = StandingOrderPaymentRequest.builder()
                    .standingOrderId(StandingOrderId.generate())
                    .customerId(testCustomerId)
                    .scheduledDate(LocalDate.now())
                    .instructedAmount(testAmount)
                    .paymentReference("SO-PAYMENT-" + System.currentTimeMillis())
                    .build();

            // When
            StandingOrderPaymentResponse paymentResponse = standingOrderService
                    .processStandingOrderPayment(paymentRequest);

            // Then
            assertNotNull(paymentResponse);
            assertEquals(OpenBankingStatus.SUCCESS, paymentResponse.getStatus());
            assertNotNull(paymentResponse.getPaymentId());
            assertEquals(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, paymentResponse.getPaymentStatus());
            assertNotNull(paymentResponse.getTransactionId());
            assertNotNull(paymentResponse.getProcessingDateTime());
        }

        @Test
        @DisplayName("Should handle standing order payment failure")
        void shouldHandleStandingOrderPaymentFailure() {
            // Given
            StandingOrderPaymentRequest failedPaymentRequest = StandingOrderPaymentRequest.builder()
                    .standingOrderId(StandingOrderId.generate())
                    .customerId(testCustomerId)
                    .scheduledDate(LocalDate.now())
                    .instructedAmount(Money.of("GBP", new BigDecimal("50000.00"))) // Large amount to trigger failure
                    .paymentReference("SO-LARGE-PAYMENT")
                    .build();

            // When
            StandingOrderPaymentResponse paymentResponse = standingOrderService
                    .processStandingOrderPayment(failedPaymentRequest);

            // Then
            assertNotNull(paymentResponse);
            if (paymentResponse.getPaymentStatus() == PaymentStatus.REJECTED) {
                assertEquals(OpenBankingStatus.REJECTED, paymentResponse.getStatus());
                assertNotNull(paymentResponse.getFailureReason());
                assertNotNull(paymentResponse.getErrorCode());
                assertNotNull(paymentResponse.getRetryEligible());
            }
        }

        @Test
        @DisplayName("Should retry failed standing order payment")
        void shouldRetryFailedStandingOrderPayment() {
            // Given
            PaymentId failedPaymentId = PaymentId.generate();
            StandingOrderPaymentRetryRequest retryRequest = StandingOrderPaymentRetryRequest.builder()
                    .originalPaymentId(failedPaymentId)
                    .standingOrderId(StandingOrderId.generate())
                    .customerId(testCustomerId)
                    .retryAmount(testAmount)
                    .retryReason("Retry after account funding")
                    .maxRetryAttempts(3)
                    .build();

            // When
            StandingOrderPaymentRetryResponse retryResponse = standingOrderService
                    .retryStandingOrderPayment(retryRequest);

            // Then
            assertNotNull(retryResponse);
            assertEquals(OpenBankingStatus.SUCCESS, retryResponse.getStatus());
            assertNotNull(retryResponse.getNewPaymentId());
            assertTrue(retryResponse.isRetryAccepted());
            assertNotNull(retryResponse.getRetryScheduledDate());
            assertTrue(retryResponse.getRemainingRetryAttempts() >= 0);
        }

        @Test
        @DisplayName("Should generate standing order payment schedule")
        void shouldGenerateStandingOrderPaymentSchedule() {
            // Given
            StandingOrderScheduleRequest scheduleRequest = StandingOrderScheduleRequest.builder()
                    .standingOrderId(StandingOrderId.generate())
                    .customerId(testCustomerId)
                    .fromDate(LocalDate.now())
                    .toDate(LocalDate.now().plusMonths(6))
                    .includeHolidayAdjustments(true)
                    .holidayAdjustmentRule(HolidayAdjustmentRule.NEXT_BUSINESS_DAY)
                    .build();

            // When
            StandingOrderScheduleResponse scheduleResponse = standingOrderService
                    .generateStandingOrderSchedule(scheduleRequest);

            // Then
            assertNotNull(scheduleResponse);
            assertEquals(OpenBankingStatus.SUCCESS, scheduleResponse.getStatus());
            assertNotNull(scheduleResponse.getScheduledPayments());
            assertFalse(scheduleResponse.getScheduledPayments().isEmpty());
            assertTrue(scheduleResponse.getTotalScheduledPayments() > 0);
            
            // Verify first scheduled payment
            ScheduledStandingOrderPayment firstPayment = scheduleResponse.getScheduledPayments().get(0);
            assertNotNull(firstPayment.getScheduledDate());
            assertNotNull(firstPayment.getAmount());
            assertNotNull(firstPayment.getReference());
        }
    }

    @Nested
    @DisplayName("Standing Order Analytics Tests")
    class StandingOrderAnalyticsTests {

        @Test
        @DisplayName("Should provide standing order performance analytics")
        void shouldProvideStandingOrderPerformanceAnalytics() {
            // Given
            StandingOrderAnalyticsRequest analyticsRequest = StandingOrderAnalyticsRequest.builder()
                    .customerId(testCustomerId)
                    .analysisFromDate(LocalDate.now().minusMonths(6))
                    .analysisToDate(LocalDate.now())
                    .includeSuccessRates(true)
                    .includeFailureAnalysis(true)
                    .groupByFrequency(true)
                    .build();

            // When
            StandingOrderAnalyticsResponse analyticsResponse = standingOrderService
                    .getStandingOrderAnalytics(analyticsRequest);

            // Then
            assertNotNull(analyticsResponse);
            assertEquals(OpenBankingStatus.SUCCESS, analyticsResponse.getStatus());
            assertNotNull(analyticsResponse.getPerformanceMetrics());
            assertTrue(analyticsResponse.getOverallSuccessRate() >= 0.0);
            assertTrue(analyticsResponse.getOverallSuccessRate() <= 100.0);
            assertNotNull(analyticsResponse.getFailureReasons());
            assertNotNull(analyticsResponse.getFrequencyAnalysis());
        }

        @Test
        @DisplayName("Should track standing order reliability metrics")
        void shouldTrackStandingOrderReliabilityMetrics() {
            // Given
            StandingOrderReliabilityRequest reliabilityRequest = StandingOrderReliabilityRequest.builder()
                    .customerId(testCustomerId)
                    .timeWindow(AnalysisTimeWindow.LAST_90_DAYS)
                    .includeUptime(true)
                    .includeLatencyMetrics(true)
                    .includeErrorRates(true)
                    .build();

            // When
            StandingOrderReliabilityResponse reliabilityResponse = standingOrderService
                    .getStandingOrderReliability(reliabilityRequest);

            // Then
            assertNotNull(reliabilityResponse);
            assertEquals(OpenBankingStatus.SUCCESS, reliabilityResponse.getStatus());
            assertNotNull(reliabilityResponse.getReliabilityMetrics());
            assertTrue(reliabilityResponse.getUptimePercentage() >= 0.0);
            assertTrue(reliabilityResponse.getUptimePercentage() <= 100.0);
            assertTrue(reliabilityResponse.getAverageLatencyMs() >= 0.0);
            assertTrue(reliabilityResponse.getErrorRate() >= 0.0);
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

    private BankAccount createInternationalCreditorAccount() {
        return BankAccount.builder()
                .accountId(BankAccountId.generate())
                .accountNumber("DE89370400440532013000")
                .sortCode("DEUTDEFF")
                .accountName("International Loan Services")
                .currency("EUR")
                .accountType(AccountType.BUSINESS)
                .build();
    }

    private ExchangeRateInformation createExchangeRateInfo() {
        return ExchangeRateInformation.builder()
                .unitCurrency("GBP")
                .exchangeRate(new BigDecimal("1.15"))
                .rateType(ExchangeRateType.AGREED)
                .contractIdentification("FX123456")
                .build();
    }
}