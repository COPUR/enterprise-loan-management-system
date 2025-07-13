package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Payment Aggregate
 * Following CBDC settlement and Islamic finance requirements
 */
@DisplayName("Payment Aggregate Tests")
class PaymentTest {

    @Test
    @DisplayName("Should create standard payment with valid details")
    void shouldCreateStandardPaymentWithValidDetails() {
        // Given
        String fromAccountId = "ACC-12345678";
        String toAccountId = "ACC-87654321";
        Money amount = Money.of(new BigDecimal("500.00"), "AED");
        String reference = "Transfer to savings";

        // When
        Payment payment = Payment.create(
            fromAccountId,
            toAccountId,
            amount,
            PaymentType.TRANSFER,
            reference
        );

        // Then
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(payment.getToAccountId()).isEqualTo(toAccountId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.TRANSFER);
        assertThat(payment.getReference()).isEqualTo(reference);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.isCbdcPayment()).isFalse(); // Default
    }

    @Test
    @DisplayName("Should create CBDC payment for instant settlement")
    void shouldCreateCbdcPaymentForInstantSettlement() {
        // Given
        String fromAccountId = "ACC-CBDC-001";
        String toAccountId = "ACC-CBDC-002";
        Money cbdcAmount = Money.of(new BigDecimal("1000.00"), "AED");

        // When
        Payment cbdcPayment = Payment.createCbdcPayment(
            fromAccountId,
            toAccountId,
            cbdcAmount,
            "CBDC instant transfer"
        );

        // Then
        assertThat(cbdcPayment.getPaymentType()).isEqualTo(PaymentType.CBDC_TRANSFER);
        assertThat(cbdcPayment.isCbdcPayment()).isTrue();
        assertThat(cbdcPayment.canSettleInstantly()).isTrue();
        assertThat(cbdcPayment.getMaxSettlementTimeSeconds()).isEqualTo(5); // 5-second requirement
    }

    @Test
    @DisplayName("Should create Islamic-compliant payment")
    void shouldCreateIslamicCompliantPayment() {
        // Given
        Money amount = Money.of(new BigDecimal("750.00"), "AED");

        // When
        Payment islamicPayment = Payment.create(
            "ACC-12345678",
            "ACC-87654321",
            amount,
            PaymentType.MURABAHA_PAYMENT,
            "Murabaha installment payment"
        ).withIslamicCompliance(true);

        // Then
        assertThat(islamicPayment.isIslamicCompliant()).isTrue();
        assertThat(islamicPayment.canChargeInterest()).isFalse();
        assertThat(islamicPayment.canIncludeProfitSharing()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AED", "USD", "EUR", "SAR", "QAR"})
    @DisplayName("Should support cross-currency payments")
    void shouldSupportCrossCurrencyPayments(String currency) {
        // Given
        Money amount = Money.of(new BigDecimal("100.00"), currency);

        // When
        Payment payment = Payment.createCrossCurrency(
            "ACC-SOURCE-001",
            "ACC-" + currency + "-001",
            amount,
            "Cross-currency transfer"
        );

        // Then
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.CROSS_CURRENCY);
        assertThat(payment.requiresCurrencyConversion()).isTrue();
        assertThat(payment.getAmount().getCurrency()).isEqualTo(currency);
    }

    @Test
    @DisplayName("Should process payment successfully")
    void shouldProcessPaymentSuccessfully() {
        // Given
        Payment payment = createTestPayment();
        String processingReference = "PROC-12345";

        // When
        payment.process(processingReference);

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getProcessingReference()).isEqualTo(processingReference);
        assertThat(payment.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should complete payment and record settlement time")
    void shouldCompletePaymentAndRecordSettlementTime() {
        // Given
        Payment payment = createTestPayment();
        payment.process("PROC-12345");
        String settlementReference = "SETTLE-67890";

        // When
        payment.complete(settlementReference);

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getSettlementReference()).isEqualTo(settlementReference);
        assertThat(payment.getCompletedAt()).isNotNull();
        assertThat(payment.getSettlementTimeSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should fail payment with reason")
    void shouldFailPaymentWithReason() {
        // Given
        Payment payment = createTestPayment();
        payment.process("PROC-12345");
        String failureReason = "Insufficient funds in source account";

        // When
        payment.fail(failureReason);

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(failureReason);
        assertThat(payment.getFailedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject payment processing when already completed")
    void shouldRejectPaymentProcessingWhenAlreadyCompleted() {
        // Given
        Payment payment = createTestPayment();
        payment.process("PROC-12345");
        payment.complete("SETTLE-67890");

        // When & Then
        assertThatThrownBy(() -> payment.process("PROC-99999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment is already completed");
    }

    @Test
    @DisplayName("Should validate CBDC settlement within 5 seconds")
    void shouldValidateCbdcSettlementWithin5Seconds() {
        // Given
        Payment cbdcPayment = Payment.createCbdcPayment(
            "ACC-CBDC-001",
            "ACC-CBDC-002",
            Money.of(new BigDecimal("500.00"), "AED"),
            "Fast CBDC transfer"
        );

        // When
        cbdcPayment.process("CBDC-PROC-001");
        // Simulate instant settlement
        cbdcPayment.complete("CBDC-SETTLE-001");

        // Then
        assertThat(cbdcPayment.getSettlementTimeSeconds()).isLessThanOrEqualTo(5);
        assertThat(cbdcPayment.meetsInstantSettlementRequirement()).isTrue();
    }

    @Test
    @DisplayName("Should create stablecoin payment")
    void shouldCreateStablecoinPayment() {
        // Given
        Money usdcAmount = Money.of(new BigDecimal("100.00"), "USD");
        String stablecoinType = "USDC";

        // When
        Payment stablecoinPayment = Payment.createStablecoinPayment(
            "ACC-STABLE-001",
            "ACC-STABLE-002",
            usdcAmount,
            stablecoinType,
            "USDC transfer"
        );

        // Then
        assertThat(stablecoinPayment.getPaymentType()).isEqualTo(PaymentType.STABLECOIN_TRANSFER);
        assertThat(stablecoinPayment.getStablecoinType()).isEqualTo(stablecoinType);
        assertThat(stablecoinPayment.isStablecoinPayment()).isTrue();
    }

    @Test
    @DisplayName("Should validate payment amount limits")
    void shouldValidatePaymentAmountLimits() {
        // Given - Amount exceeding daily limit
        Money largeAmount = Money.of(new BigDecimal("1000000.00"), "AED");

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "ACC-12345678",
            "ACC-87654321",
            largeAmount,
            PaymentType.TRANSFER,
            "Large transfer"
        )).isInstanceOf(PaymentLimitExceededException.class)
          .hasMessageContaining("exceeds daily limit");
    }

    @Test
    @DisplayName("Should validate same account transfer prevention")
    void shouldValidateSameAccountTransferPrevention() {
        // Given - Same account for source and destination
        String sameAccountId = "ACC-12345678";
        Money amount = Money.of(new BigDecimal("100.00"), "AED");

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            sameAccountId,
            sameAccountId,
            amount,
            PaymentType.TRANSFER,
            "Self transfer"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot transfer to the same account");
    }

    @Test
    @DisplayName("Should add payment fees for non-Islamic accounts")
    void shouldAddPaymentFeesForNonIslamicAccounts() {
        // Given
        Payment conventionalPayment = createTestPayment();
        Money feeAmount = Money.of(new BigDecimal("5.00"), "AED");

        // When
        conventionalPayment.addFee(PaymentFeeType.PROCESSING_FEE, feeAmount, "Standard processing fee");

        // Then
        assertThat(conventionalPayment.getTotalFees()).isEqualTo(feeAmount);
        assertThat(conventionalPayment.getFees()).hasSize(1);
        
        PaymentFee fee = conventionalPayment.getFees().get(0);
        assertThat(fee.getFeeType()).isEqualTo(PaymentFeeType.PROCESSING_FEE);
        assertThat(fee.getAmount()).isEqualTo(feeAmount);
    }

    @Test
    @DisplayName("Should reject interest-based fees for Islamic payments")
    void shouldRejectInterestBasedFeesForIslamicPayments() {
        // Given
        Payment islamicPayment = createTestPayment().withIslamicCompliance(true);
        Money interestFee = Money.of(new BigDecimal("10.00"), "AED");

        // When & Then
        assertThatThrownBy(() -> islamicPayment.addFee(
            PaymentFeeType.INTEREST_CHARGE, 
            interestFee, 
            "Interest charge"
        )).isInstanceOf(IslamicComplianceViolationException.class)
          .hasMessageContaining("Interest-based fees are not allowed in Islamic banking");
    }

    @Test
    @DisplayName("Should calculate total payment amount including fees")
    void shouldCalculateTotalPaymentAmountIncludingFees() {
        // Given
        Payment payment = createTestPayment();
        Money processingFee = Money.of(new BigDecimal("5.00"), "AED");
        Money networkFee = Money.of(new BigDecimal("2.50"), "AED");

        // When
        payment.addFee(PaymentFeeType.PROCESSING_FEE, processingFee, "Processing fee");
        payment.addFee(PaymentFeeType.NETWORK_FEE, networkFee, "Network fee");

        // Then
        Money expectedTotal = Money.of(new BigDecimal("507.50"), "AED"); // 500 + 5 + 2.50
        assertThat(payment.getTotalAmountWithFees()).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("Should track payment compliance status")
    void shouldTrackPaymentComplianceStatus() {
        // Given
        Payment payment = createTestPayment();

        // When
        payment.markComplianceChecked("AML-CHECK-001", "Passed AML screening");

        // Then
        assertThat(payment.isComplianceChecked()).isTrue();
        assertThat(payment.getComplianceCheckReference()).isEqualTo("AML-CHECK-001");
        assertThat(payment.getComplianceNotes()).isEqualTo("Passed AML screening");
    }

    private Payment createTestPayment() {
        return Payment.create(
            "ACC-12345678",
            "ACC-87654321",
            Money.of(new BigDecimal("500.00"), "AED"),
            PaymentType.TRANSFER,
            "Test payment"
        );
    }
}