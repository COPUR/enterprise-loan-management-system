package com.loanmanagement.openbanking.gateway;

import com.loanmanagement.openbanking.domain.model.*;
import com.loanmanagement.openbanking.gateway.service.OpenBankingPaymentStatusService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Open Banking Payment Status Service
 * Tests payment status tracking, polling, and lifecycle management
 */
@DisplayName("Open Banking Payment Status Tests")
class OpenBankingPaymentStatusTest {

    private OpenBankingPaymentStatusService paymentStatusService;
    private PaymentId testPaymentId;
    private ConsentId testConsentId;
    private OpenBankingCustomerId testCustomerId;

    @BeforeEach
    void setUp() {
        paymentStatusService = new OpenBankingPaymentStatusService();
        testPaymentId = PaymentId.generate();
        testConsentId = ConsentId.generate();
        testCustomerId = OpenBankingCustomerId.generate();
    }

    @Nested
    @DisplayName("Payment Status Inquiry Tests")
    class PaymentStatusInquiryTests {

        @Test
        @DisplayName("Should retrieve payment status for valid payment ID")
        void shouldRetrievePaymentStatusForValidPaymentId() {
            // Given
            PaymentStatusRequest statusRequest = PaymentStatusRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .requestId(RequestId.generate())
                    .fapiInteractionId(FAPIInteractionId.generate())
                    .build();

            // When
            PaymentStatusResponse statusResponse = paymentStatusService
                    .getPaymentStatus(statusRequest);

            // Then
            assertNotNull(statusResponse);
            assertEquals(OpenBankingStatus.SUCCESS, statusResponse.getStatus());
            assertNotNull(statusResponse.getPaymentStatus());
            assertNotNull(statusResponse.getLastStatusUpdate());
            assertEquals(testPaymentId, statusResponse.getPaymentId());
            assertNotNull(statusResponse.getStatusHistory());
            assertFalse(statusResponse.getStatusHistory().isEmpty());
        }

        @Test
        @DisplayName("Should handle payment status inquiry for non-existent payment")
        void shouldHandlePaymentStatusInquiryForNonExistentPayment() {
            // Given
            PaymentId nonExistentPaymentId = PaymentId.generate();
            PaymentStatusRequest statusRequest = PaymentStatusRequest.builder()
                    .paymentId(nonExistentPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .build();

            // When
            PaymentStatusResponse statusResponse = paymentStatusService
                    .getPaymentStatus(statusRequest);

            // Then
            assertNotNull(statusResponse);
            assertEquals(OpenBankingStatus.NOT_FOUND, statusResponse.getStatus());
            assertEquals(OpenBankingErrorCode.PAYMENT_NOT_FOUND, statusResponse.getErrorCode());
            assertNotNull(statusResponse.getErrorMessage());
        }

        @Test
        @DisplayName("Should retrieve detailed payment status with transaction information")
        void shouldRetrieveDetailedPaymentStatusWithTransactionInformation() {
            // Given
            PaymentStatusDetailRequest detailRequest = PaymentStatusDetailRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .includeTransactionDetails(true)
                    .includeStatusHistory(true)
                    .includeCharges(true)
                    .build();

            // When
            PaymentStatusDetailResponse detailResponse = paymentStatusService
                    .getDetailedPaymentStatus(detailRequest);

            // Then
            assertNotNull(detailResponse);
            assertEquals(OpenBankingStatus.SUCCESS, detailResponse.getStatus());
            assertNotNull(detailResponse.getPaymentDetails());
            assertNotNull(detailResponse.getTransactionReference());
            assertNotNull(detailResponse.getCharges());
            assertNotNull(detailResponse.getStatusHistory());
            assertTrue(detailResponse.getStatusHistory().size() > 0);
            assertNotNull(detailResponse.getExpectedExecutionDateTime());
        }

        @Test
        @DisplayName("Should validate payment status access permissions")
        void shouldValidatePaymentStatusAccessPermissions() {
            // Given
            PaymentStatusRequest unauthorizedRequest = PaymentStatusRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(ConsentId.generate()) // Different consent ID
                    .customerId(testCustomerId)
                    .build();

            // When
            PaymentStatusResponse statusResponse = paymentStatusService
                    .getPaymentStatus(unauthorizedRequest);

            // Then
            assertNotNull(statusResponse);
            assertEquals(OpenBankingStatus.UNAUTHORIZED, statusResponse.getStatus());
            assertEquals(OpenBankingErrorCode.INVALID_CONSENT, statusResponse.getErrorCode());
            assertNotNull(statusResponse.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Payment Status Polling Tests")
    class PaymentStatusPollingTests {

        @Test
        @DisplayName("Should handle payment status polling with proper intervals")
        void shouldHandlePaymentStatusPollingWithProperIntervals() {
            // Given
            PaymentStatusPollingRequest pollingRequest = PaymentStatusPollingRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .pollingIntervalSeconds(30)
                    .maxPollingAttempts(10)
                    .targetStatuses(List.of(PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED, 
                                          PaymentStatus.REJECTED))
                    .build();

            // When
            PaymentStatusPollingResult pollingResult = paymentStatusService
                    .startPaymentStatusPolling(pollingRequest);

            // Then
            assertNotNull(pollingResult);
            assertTrue(pollingResult.isPollingStarted());
            assertNotNull(pollingResult.getPollingId());
            assertNotNull(pollingResult.getNextPollingTime());
            assertEquals(PaymentPollingStatus.ACTIVE, pollingResult.getPollingStatus());
            assertNotNull(pollingResult.getEstimatedCompletionTime());
        }

        @Test
        @DisplayName("Should stop payment status polling when target status reached")
        void shouldStopPaymentStatusPollingWhenTargetStatusReached() {
            // Given
            PollingId pollingId = PollingId.generate();
            PaymentStatusPollingStopRequest stopRequest = PaymentStatusPollingStopRequest.builder()
                    .pollingId(pollingId)
                    .paymentId(testPaymentId)
                    .reason(PollingStopReason.TARGET_STATUS_REACHED)
                    .build();

            // When
            PaymentStatusPollingStopResult stopResult = paymentStatusService
                    .stopPaymentStatusPolling(stopRequest);

            // Then
            assertNotNull(stopResult);
            assertTrue(stopResult.isPollingStopped());
            assertEquals(PaymentPollingStatus.COMPLETED, stopResult.getFinalPollingStatus());
            assertNotNull(stopResult.getFinalPaymentStatus());
            assertNotNull(stopResult.getStoppedAt());
        }

        @Test
        @DisplayName("Should handle polling timeout for long-running payments")
        void shouldHandlePollingTimeoutForLongRunningPayments() {
            // Given
            PaymentStatusPollingRequest longPollingRequest = PaymentStatusPollingRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .pollingIntervalSeconds(60)
                    .maxPollingAttempts(5)
                    .timeoutMinutes(10)
                    .targetStatuses(List.of(PaymentStatus.ACCEPTED_SETTLEMENT_COMPLETED))
                    .build();

            // When
            PaymentStatusPollingResult pollingResult = paymentStatusService
                    .startPaymentStatusPolling(longPollingRequest);

            // Then
            assertNotNull(pollingResult);
            assertTrue(pollingResult.isPollingStarted());
            assertNotNull(pollingResult.getTimeoutAt());
            assertTrue(pollingResult.getTimeoutAt().isAfter(LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("Payment Lifecycle Management Tests")
    class PaymentLifecycleManagementTests {

        @Test
        @DisplayName("Should track payment lifecycle from initiation to completion")
        void shouldTrackPaymentLifecycleFromInitiationToCompletion() {
            // Given
            PaymentLifecycleRequest lifecycleRequest = PaymentLifecycleRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .includeIntermediateSteps(true)
                    .build();

            // When
            PaymentLifecycleResponse lifecycleResponse = paymentStatusService
                    .getPaymentLifecycle(lifecycleRequest);

            // Then
            assertNotNull(lifecycleResponse);
            assertEquals(OpenBankingStatus.SUCCESS, lifecycleResponse.getStatus());
            assertNotNull(lifecycleResponse.getLifecycleSteps());
            assertFalse(lifecycleResponse.getLifecycleSteps().isEmpty());
            
            // Verify lifecycle steps are in chronological order
            PaymentLifecycleStep firstStep = lifecycleResponse.getLifecycleSteps().get(0);
            assertEquals(PaymentStatus.ACCEPTED_TECHNICAL_VALIDATION, firstStep.getStatus());
            assertNotNull(firstStep.getTimestamp());
            assertNotNull(firstStep.getDescription());
        }

        @Test
        @DisplayName("Should handle payment cancellation during processing")
        void shouldHandlePaymentCancellationDuringProcessing() {
            // Given
            PaymentCancellationRequest cancellationRequest = PaymentCancellationRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .cancellationReason("Customer requested cancellation")
                    .requestId(RequestId.generate())
                    .build();

            // When
            PaymentCancellationResponse cancellationResponse = paymentStatusService
                    .cancelPayment(cancellationRequest);

            // Then
            assertNotNull(cancellationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, cancellationResponse.getStatus());
            assertEquals(PaymentStatus.CANCELLED, cancellationResponse.getNewPaymentStatus());
            assertNotNull(cancellationResponse.getCancellationId());
            assertNotNull(cancellationResponse.getCancellationDateTime());
            assertTrue(cancellationResponse.isCancellationSuccessful());
        }

        @Test
        @DisplayName("Should handle payment rejection with detailed reasons")
        void shouldHandlePaymentRejectionWithDetailedReasons() {
            // Given
            PaymentId rejectedPaymentId = PaymentId.generate();
            PaymentStatusRequest rejectionStatusRequest = PaymentStatusRequest.builder()
                    .paymentId(rejectedPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .build();

            // When
            PaymentStatusResponse statusResponse = paymentStatusService
                    .getPaymentStatus(rejectionStatusRequest);

            // Then
            assertNotNull(statusResponse);
            if (statusResponse.getPaymentStatus() == PaymentStatus.REJECTED) {
                assertNotNull(statusResponse.getRejectionReason());
                assertNotNull(statusResponse.getRejectionCode());
                assertNotNull(statusResponse.getRejectionDetails());
                assertTrue(statusResponse.getRejectionDetails().containsKey("reason_code"));
                assertTrue(statusResponse.getRejectionDetails().containsKey("rejection_description"));
            }
        }

        @Test
        @DisplayName("Should provide payment status notifications and webhooks")
        void shouldProvidePaymentStatusNotificationsAndWebhooks() {
            // Given
            PaymentStatusNotificationRequest notificationRequest = PaymentStatusNotificationRequest.builder()
                    .paymentId(testPaymentId)
                    .consentId(testConsentId)
                    .customerId(testCustomerId)
                    .webhookUrl("https://client-app.example.com/webhooks/payment-status")
                    .notificationEvents(List.of(
                            PaymentStatusEvent.PAYMENT_COMPLETED,
                            PaymentStatusEvent.PAYMENT_FAILED,
                            PaymentStatusEvent.PAYMENT_CANCELLED
                    ))
                    .build();

            // When
            PaymentStatusNotificationResponse notificationResponse = paymentStatusService
                    .setupPaymentStatusNotifications(notificationRequest);

            // Then
            assertNotNull(notificationResponse);
            assertEquals(OpenBankingStatus.SUCCESS, notificationResponse.getStatus());
            assertNotNull(notificationResponse.getNotificationId());
            assertTrue(notificationResponse.isNotificationSetupSuccessful());
            assertNotNull(notificationResponse.getWebhookSecret());
            assertEquals(3, notificationResponse.getConfiguredEvents().size());
        }
    }

    @Nested
    @DisplayName("Payment Status Analytics Tests")
    class PaymentStatusAnalyticsTests {

        @Test
        @DisplayName("Should provide payment processing time analytics")
        void shouldProvidePaymentProcessingTimeAnalytics() {
            // Given
            PaymentAnalyticsRequest analyticsRequest = PaymentAnalyticsRequest.builder()
                    .customerId(testCustomerId)
                    .fromDate(LocalDateTime.now().minusDays(30))
                    .toDate(LocalDateTime.now())
                    .paymentTypes(List.of(PaymentType.DOMESTIC_PAYMENT, PaymentType.INTERNATIONAL_PAYMENT))
                    .includeProcessingTimes(true)
                    .build();

            // When
            PaymentAnalyticsResponse analyticsResponse = paymentStatusService
                    .getPaymentAnalytics(analyticsRequest);

            // Then
            assertNotNull(analyticsResponse);
            assertEquals(OpenBankingStatus.SUCCESS, analyticsResponse.getStatus());
            assertNotNull(analyticsResponse.getProcessingTimeStats());
            assertTrue(analyticsResponse.getProcessingTimeStats().getAverageProcessingMinutes() > 0);
            assertTrue(analyticsResponse.getProcessingTimeStats().getMedianProcessingMinutes() > 0);
            assertNotNull(analyticsResponse.getStatusDistribution());
            assertTrue(analyticsResponse.getTotalPaymentsAnalyzed() > 0);
        }

        @Test
        @DisplayName("Should provide payment success rate metrics")
        void shouldProvidePaymentSuccessRateMetrics() {
            // Given
            PaymentSuccessRateRequest successRateRequest = PaymentSuccessRateRequest.builder()
                    .customerId(testCustomerId)
                    .analysisWindow(AnalysisWindow.LAST_30_DAYS)
                    .groupByPaymentType(true)
                    .groupByAmount(true)
                    .includeReasonAnalysis(true)
                    .build();

            // When
            PaymentSuccessRateResponse successRateResponse = paymentStatusService
                    .getPaymentSuccessRate(successRateRequest);

            // Then
            assertNotNull(successRateResponse);
            assertEquals(OpenBankingStatus.SUCCESS, successRateResponse.getStatus());
            assertTrue(successRateResponse.getOverallSuccessRate() >= 0.0);
            assertTrue(successRateResponse.getOverallSuccessRate() <= 100.0);
            assertNotNull(successRateResponse.getSuccessRateByType());
            assertNotNull(successRateResponse.getFailureReasons());
            assertNotNull(successRateResponse.getRecommendations());
        }

        @Test
        @DisplayName("Should identify payment processing bottlenecks")
        void shouldIdentifyPaymentProcessingBottlenecks() {
            // Given
            PaymentBottleneckAnalysisRequest bottleneckRequest = PaymentBottleneckAnalysisRequest.builder()
                    .customerId(testCustomerId)
                    .analysisTimeframe(AnalysisTimeframe.LAST_7_DAYS)
                    .thresholdMinutes(60) // Consider >60 min as bottleneck
                    .includeRecommendations(true)
                    .build();

            // When
            PaymentBottleneckAnalysisResponse bottleneckResponse = paymentStatusService
                    .analyzePaymentBottlenecks(bottleneckRequest);

            // Then
            assertNotNull(bottleneckResponse);
            assertEquals(OpenBankingStatus.SUCCESS, bottleneckResponse.getStatus());
            assertNotNull(bottleneckResponse.getIdentifiedBottlenecks());
            assertNotNull(bottleneckResponse.getBottleneckStages());
            assertNotNull(bottleneckResponse.getImpactAssessment());
            if (!bottleneckResponse.getIdentifiedBottlenecks().isEmpty()) {
                assertNotNull(bottleneckResponse.getRecommendations());
                assertFalse(bottleneckResponse.getRecommendations().isEmpty());
            }
        }
    }
}