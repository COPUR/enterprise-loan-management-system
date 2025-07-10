package com.loanmanagement.payment.infrastructure;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.payment.infrastructure.gateway.PaymentGatewayService;
import com.loanmanagement.payment.infrastructure.gateway.ACHGatewayService;
import com.loanmanagement.payment.infrastructure.gateway.CreditCardGatewayService;
import com.loanmanagement.payment.infrastructure.gateway.WireTransferGatewayService;
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
 * Tests for Payment Gateway Integration
 * Comprehensive testing of payment gateway connections
 */
@DisplayName("Payment Gateway Integration Tests")
class PaymentGatewayIntegrationTest {

    private PaymentGatewayService paymentGatewayService;
    private ACHGatewayService achGatewayService;
    private CreditCardGatewayService creditCardGatewayService;
    private WireTransferGatewayService wireTransferGatewayService;
    private LoanId testLoanId;
    private CustomerId testCustomerId;
    private Money testPaymentAmount;

    @BeforeEach
    void setUp() {
        paymentGatewayService = new PaymentGatewayService();
        achGatewayService = new ACHGatewayService();
        creditCardGatewayService = new CreditCardGatewayService();
        wireTransferGatewayService = new WireTransferGatewayService();
        testLoanId = LoanId.generate();
        testCustomerId = CustomerId.generate();
        testPaymentAmount = Money.of("USD", new BigDecimal("1200.00"));
    }

    @Nested
    @DisplayName("ACH Payment Gateway Tests")
    class ACHPaymentGatewayTests {

        @Test
        @DisplayName("Should process ACH debit payment successfully")
        void shouldProcessACHDebitPaymentSuccessfully() {
            // Given
            ACHPaymentRequest achRequest = ACHPaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .accountNumber("123456789")
                    .routingNumber("021000021")
                    .accountType(BankAccountType.CHECKING)
                    .transactionType(ACHTransactionType.DEBIT)
                    .secCode(SECCode.WEB)
                    .companyName("Loan Management Corp")
                    .paymentDate(LocalDateTime.now())
                    .build();

            // When
            ACHPaymentResult result = achGatewayService.processACHPayment(achRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.PENDING, result.getStatus());
            assertNotNull(result.getTransactionId());
            assertNotNull(result.getACHTraceNumber());
            assertTrue(result.getProcessingTime().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(achRequest.getPaymentAmount(), result.getProcessedAmount());
        }

        @Test
        @DisplayName("Should handle ACH payment validation errors")
        void shouldHandleACHPaymentValidationErrors() {
            // Given
            ACHPaymentRequest invalidRequest = ACHPaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .accountNumber("invalid")
                    .routingNumber("000000000") // Invalid routing number
                    .accountType(BankAccountType.CHECKING)
                    .transactionType(ACHTransactionType.DEBIT)
                    .secCode(SECCode.WEB)
                    .build();

            // When
            ACHPaymentResult result = achGatewayService.processACHPayment(invalidRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.FAILED, result.getStatus());
            assertNotNull(result.getErrorCode());
            assertEquals(ACHErrorCode.INVALID_ROUTING_NUMBER, result.getErrorCode());
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should handle ACH return processing")
        void shouldHandleACHReturnProcessing() {
            // Given
            ACHReturnNotification returnNotification = ACHReturnNotification.builder()
                    .originalTransactionId("TXN-123456")
                    .returnCode(ACHReturnCode.R01_INSUFFICIENT_FUNDS)
                    .returnReason("Insufficient Funds")
                    .returnDate(LocalDateTime.now())
                    .returnAmount(testPaymentAmount)
                    .build();

            // When
            ACHReturnResult returnResult = achGatewayService.processACHReturn(returnNotification);

            // Then
            assertNotNull(returnResult);
            assertTrue(returnResult.isReturnProcessed());
            assertEquals(ACHReturnCode.R01_INSUFFICIENT_FUNDS, returnResult.getReturnCode());
            assertNotNull(returnResult.getReversalTransactionId());
            assertEquals(testPaymentAmount, returnResult.getReturnedAmount());
        }

        @Test
        @DisplayName("Should verify bank account via micro-deposits")
        void shouldVerifyBankAccountViaMicroDeposits() {
            // Given
            BankAccountVerificationRequest verificationRequest = BankAccountVerificationRequest.builder()
                    .customerId(testCustomerId)
                    .accountNumber("123456789")
                    .routingNumber("021000021")
                    .accountType(BankAccountType.CHECKING)
                    .verificationMethod(AccountVerificationMethod.MICRO_DEPOSITS)
                    .build();

            // When
            BankAccountVerificationResult result = achGatewayService
                    .initiateAccountVerification(verificationRequest);

            // Then
            assertNotNull(result);
            assertEquals(AccountVerificationStatus.PENDING, result.getStatus());
            assertNotNull(result.getVerificationId());
            assertEquals(2, result.getMicroDepositCount());
            assertTrue(result.getVerificationTimeoutHours() > 0);
        }
    }

    @Nested
    @DisplayName("Credit Card Payment Gateway Tests")
    class CreditCardPaymentGatewayTests {

        @Test
        @DisplayName("Should process credit card payment successfully")
        void shouldProcessCreditCardPaymentSuccessfully() {
            // Given
            CreditCardPaymentRequest cardRequest = CreditCardPaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .cardNumber("4111111111111111") // Test Visa
                    .expiryMonth("12")
                    .expiryYear("2025")
                    .cvv("123")
                    .cardholderName("John Doe")
                    .billingAddress(createTestBillingAddress())
                    .transactionType(CreditCardTransactionType.SALE)
                    .build();

            // When
            CreditCardPaymentResult result = creditCardGatewayService
                    .processCreditCardPayment(cardRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.APPROVED, result.getStatus());
            assertNotNull(result.getTransactionId());
            assertNotNull(result.getAuthorizationCode());
            assertEquals(testPaymentAmount, result.getAuthorizedAmount());
            assertTrue(result.getProcessingFee().getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should handle credit card decline")
        void shouldHandleCreditCardDecline() {
            // Given
            CreditCardPaymentRequest declinedRequest = CreditCardPaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(Money.of("USD", new BigDecimal("50000.00"))) // Large amount to trigger decline
                    .cardNumber("4000000000000002") // Test decline card
                    .expiryMonth("12")
                    .expiryYear("2025")
                    .cvv("123")
                    .cardholderName("John Doe")
                    .billingAddress(createTestBillingAddress())
                    .transactionType(CreditCardTransactionType.SALE)
                    .build();

            // When
            CreditCardPaymentResult result = creditCardGatewayService
                    .processCreditCardPayment(declinedRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.DECLINED, result.getStatus());
            assertNotNull(result.getDeclineReason());
            assertEquals(CreditCardDeclineCode.INSUFFICIENT_FUNDS, result.getDeclineCode());
            assertNull(result.getAuthorizationCode());
        }

        @Test
        @DisplayName("Should handle tokenized card payments")
        void shouldHandleTokenizedCardPayments() {
            // Given
            TokenizedCardPaymentRequest tokenRequest = TokenizedCardPaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .cardToken("tok_1234567890")
                    .cvv("123")
                    .transactionType(CreditCardTransactionType.SALE)
                    .build();

            // When
            CreditCardPaymentResult result = creditCardGatewayService
                    .processTokenizedPayment(tokenRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.APPROVED, result.getStatus());
            assertNotNull(result.getTransactionId());
            assertTrue(result.isTokenizedTransaction());
        }

        @Test
        @DisplayName("Should process credit card refund")
        void shouldProcessCreditCardRefund() {
            // Given
            CreditCardRefundRequest refundRequest = CreditCardRefundRequest.builder()
                    .originalTransactionId("TXN-CC-123456")
                    .refundAmount(Money.of("USD", new BigDecimal("600.00")))
                    .refundReason("Customer dispute")
                    .customerId(testCustomerId)
                    .loanId(testLoanId)
                    .build();

            // When
            CreditCardRefundResult result = creditCardGatewayService
                    .processCreditCardRefund(refundRequest);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.APPROVED, result.getStatus());
            assertNotNull(result.getRefundTransactionId());
            assertEquals(refundRequest.getRefundAmount(), result.getRefundedAmount());
        }
    }

    @Nested
    @DisplayName("Wire Transfer Gateway Tests")
    class WireTransferGatewayTests {

        @Test
        @DisplayName("Should initiate domestic wire transfer")
        void shouldInitiateDomesticWireTransfer() {
            // Given
            WireTransferRequest wireRequest = WireTransferRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .transferAmount(testPaymentAmount)
                    .senderName("John Doe")
                    .senderAccount("123456789")
                    .senderBank("Sender Bank")
                    .senderRoutingNumber("021000021")
                    .receiverName("Loan Management Corp")
                    .receiverAccount("987654321")
                    .receiverBank("Receiver Bank")
                    .receiverRoutingNumber("021000022")
                    .wireType(WireTransferType.DOMESTIC)
                    .purpose("Loan payment")
                    .build();

            // When
            WireTransferResult result = wireTransferGatewayService.initiateWireTransfer(wireRequest);

            // Then
            assertNotNull(result);
            assertEquals(WireTransferStatus.PENDING, result.getStatus());
            assertNotNull(result.getWireTransferId());
            assertNotNull(result.getFedwireMessageId());
            assertTrue(result.getTransferFee().getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should handle international wire transfer")
        void shouldHandleInternationalWireTransfer() {
            // Given
            InternationalWireTransferRequest intlRequest = InternationalWireTransferRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .transferAmount(Money.of("EUR", new BigDecimal("1000.00")))
                    .senderName("John Doe")
                    .senderAccount("DE89370400440532013000") // IBAN
                    .senderBank("Deutsche Bank")
                    .senderSwiftCode("DEUTDEFF")
                    .receiverName("Loan Management Corp")
                    .receiverAccount("US12345678901234567890")
                    .receiverBank("Chase Bank")
                    .receiverSwiftCode("CHASUS33")
                    .wireType(WireTransferType.INTERNATIONAL)
                    .correspondentBank("JP Morgan Chase")
                    .correspondentSwiftCode("CHASUS33")
                    .exchangeRate(new BigDecimal("1.10"))
                    .build();

            // When
            WireTransferResult result = wireTransferGatewayService
                    .initiateInternationalWireTransfer(intlRequest);

            // Then
            assertNotNull(result);
            assertEquals(WireTransferStatus.PENDING_COMPLIANCE, result.getStatus());
            assertNotNull(result.getWireTransferId());
            assertTrue(result.requiresComplianceReview());
            assertNotNull(result.getExchangeRateUsed());
        }

        @Test
        @DisplayName("Should track wire transfer status")
        void shouldTrackWireTransferStatus() {
            // Given
            String wireTransferId = "WIRE-123456789";

            // When
            WireTransferStatus status = wireTransferGatewayService
                    .getWireTransferStatus(wireTransferId);

            // Then
            assertNotNull(status);
            assertTrue(List.of(WireTransferStatus.PENDING, WireTransferStatus.COMPLETED, 
                    WireTransferStatus.FAILED).contains(status));
        }
    }

    @Nested
    @DisplayName("Payment Gateway Orchestration Tests")
    class PaymentGatewayOrchestrationTests {

        @Test
        @DisplayName("Should route payment to appropriate gateway")
        void shouldRoutePaymentToAppropriateGateway() {
            // Given
            PaymentRoutingRequest routingRequest = PaymentRoutingRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .paymentMethod(PaymentMethod.ACH)
                    .paymentSource(createACHPaymentSource())
                    .build();

            // When
            PaymentGatewayRoute route = paymentGatewayService.determinePaymentRoute(routingRequest);

            // Then
            assertNotNull(route);
            assertEquals(PaymentGatewayType.ACH, route.getGatewayType());
            assertEquals(PaymentPriority.STANDARD, route.getPriority());
            assertTrue(route.getExpectedProcessingTimeHours() > 0);
        }

        @Test
        @DisplayName("Should handle payment gateway failover")
        void shouldHandlePaymentGatewayFailover() {
            // Given
            PaymentRequest failoverRequest = PaymentRequest.builder()
                    .loanId(testLoanId)
                    .customerId(testCustomerId)
                    .paymentAmount(testPaymentAmount)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .paymentSource(createCreditCardPaymentSource())
                    .enableFailover(true)
                    .build();

            // Simulate primary gateway failure
            PaymentGatewayFailure primaryFailure = PaymentGatewayFailure.builder()
                    .gatewayType(PaymentGatewayType.PRIMARY_CREDIT_CARD)
                    .failureReason("Gateway timeout")
                    .failureCode("GATEWAY_TIMEOUT")
                    .build();

            // When
            PaymentGatewayResult result = paymentGatewayService
                    .processPaymentWithFailover(failoverRequest, primaryFailure);

            // Then
            assertNotNull(result);
            assertEquals(PaymentGatewayStatus.APPROVED, result.getStatus());
            assertEquals(PaymentGatewayType.SECONDARY_CREDIT_CARD, result.getGatewayUsed());
            assertTrue(result.isFailoverTransaction());
            assertNotNull(result.getFailoverDetails());
        }

        @Test
        @DisplayName("Should aggregate gateway transaction fees")
        void shouldAggregateGatewayTransactionFees() {
            // Given
            List<PaymentGatewayTransaction> transactions = List.of(
                    createACHTransaction(Money.of("USD", new BigDecimal("1200.00"))),
                    createCreditCardTransaction(Money.of("USD", new BigDecimal("800.00"))),
                    createWireTransferTransaction(Money.of("USD", new BigDecimal("5000.00")))
            );

            // When
            PaymentGatewayFeeAnalysis feeAnalysis = paymentGatewayService
                    .analyzeFees(transactions, LocalDateTime.now().minusMonths(1), LocalDateTime.now());

            // Then
            assertNotNull(feeAnalysis);
            assertTrue(feeAnalysis.getTotalFees().getAmount().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(3, feeAnalysis.getTransactionCount());
            assertNotNull(feeAnalysis.getFeesByGateway());
            assertTrue(feeAnalysis.getAverageFeePerTransaction().getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should handle payment gateway rate limiting")
        void shouldHandlePaymentGatewayRateLimiting() {
            // Given
            List<PaymentRequest> rapidRequests = createRapidPaymentRequests(50); // 50 requests rapidly

            // When
            PaymentBatchResult batchResult = paymentGatewayService
                    .processPaymentBatch(rapidRequests);

            // Then
            assertNotNull(batchResult);
            assertTrue(batchResult.getThrottledRequests() > 0);
            assertTrue(batchResult.getSuccessfulPayments() > 0);
            assertTrue(batchResult.getProcessingTimeMs() > 0);
            assertNotNull(batchResult.getRateLimitingDetails());
        }
    }

    // Helper methods
    private BillingAddress createTestBillingAddress() {
        return BillingAddress.builder()
                .street("123 Main St")
                .city("Anytown")
                .state("NY")
                .zipCode("12345")
                .country("US")
                .build();
    }

    private PaymentSource createACHPaymentSource() {
        return PaymentSource.builder()
                .sourceType(PaymentSourceType.BANK_ACCOUNT)
                .accountNumber("123456789")
                .routingNumber("021000021")
                .bankName("Test Bank")
                .build();
    }

    private PaymentSource createCreditCardPaymentSource() {
        return PaymentSource.builder()
                .sourceType(PaymentSourceType.CREDIT_CARD)
                .cardNumber("4111111111111111")
                .expiryMonth("12")
                .expiryYear("2025")
                .cvv("123")
                .cardholderName("John Doe")
                .build();
    }

    private PaymentGatewayTransaction createACHTransaction(Money amount) {
        return PaymentGatewayTransaction.builder()
                .transactionId("ACH-" + System.currentTimeMillis())
                .gatewayType(PaymentGatewayType.ACH)
                .amount(amount)
                .fee(Money.of("USD", new BigDecimal("1.50")))
                .status(PaymentGatewayStatus.APPROVED)
                .build();
    }

    private PaymentGatewayTransaction createCreditCardTransaction(Money amount) {
        return PaymentGatewayTransaction.builder()
                .transactionId("CC-" + System.currentTimeMillis())
                .gatewayType(PaymentGatewayType.CREDIT_CARD)
                .amount(amount)
                .fee(amount.multiply(new BigDecimal("0.029"))) // 2.9% fee
                .status(PaymentGatewayStatus.APPROVED)
                .build();
    }

    private PaymentGatewayTransaction createWireTransferTransaction(Money amount) {
        return PaymentGatewayTransaction.builder()
                .transactionId("WIRE-" + System.currentTimeMillis())
                .gatewayType(PaymentGatewayType.WIRE_TRANSFER)
                .amount(amount)
                .fee(Money.of("USD", new BigDecimal("25.00")))
                .status(PaymentGatewayStatus.APPROVED)
                .build();
    }

    private List<PaymentRequest> createRapidPaymentRequests(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> PaymentRequest.builder()
                        .loanId(testLoanId)
                        .customerId(testCustomerId)
                        .paymentAmount(Money.of("USD", new BigDecimal("100.00")))
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentSource(createCreditCardPaymentSource())
                        .build())
                .toList();
    }
}