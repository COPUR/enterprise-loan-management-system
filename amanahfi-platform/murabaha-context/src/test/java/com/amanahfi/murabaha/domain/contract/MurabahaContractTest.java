package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Murabaha Contract Aggregate
 * Following Islamic finance principles and Sharia compliance
 */
@DisplayName("Murabaha Contract Tests")
class MurabahaContractTest {

    @Test
    @DisplayName("Should create new Murabaha contract with valid details")
    void shouldCreateNewMurabahaContractWithValidDetails() {
        // Given
        String customerId = "CUST-12345678";
        String assetDescription = "Honda Civic 2024";
        Money assetCost = Money.of(new BigDecimal("80000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("8000.00"), "AED");
        int termMonths = 36;
        LocalDate deliveryDate = LocalDate.now().plusDays(7);

        // When
        MurabahaContract contract = MurabahaContract.create(
            customerId,
            assetDescription,
            assetCost,
            profitAmount,
            termMonths,
            deliveryDate
        );

        // Then
        assertThat(contract.getContractId()).isNotNull();
        assertThat(contract.getCustomerId()).isEqualTo(customerId);
        assertThat(contract.getAssetDescription()).isEqualTo(assetDescription);
        assertThat(contract.getAssetCost()).isEqualTo(assetCost);
        assertThat(contract.getProfitAmount()).isEqualTo(profitAmount);
        assertThat(contract.getTermMonths()).isEqualTo(termMonths);
        assertThat(contract.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.getCreatedAt()).isNotNull();
        assertThat(contract.isShariahCompliant()).isTrue(); // Always true for Murabaha
    }

    @Test
    @DisplayName("Should calculate total selling price correctly")
    void shouldCalculateTotalSellingPriceCorrectly() {
        // Given
        Money assetCost = Money.of(new BigDecimal("100000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("15000.00"), "AED");
        
        MurabahaContract contract = createTestContract(assetCost, profitAmount);

        // When
        Money totalSellingPrice = contract.getTotalSellingPrice();

        // Then
        Money expectedTotal = Money.of(new BigDecimal("115000.00"), "AED");
        assertThat(totalSellingPrice).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("Should calculate monthly installment amount")
    void shouldCalculateMonthlyInstallmentAmount() {
        // Given
        Money assetCost = Money.of(new BigDecimal("120000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("18000.00"), "AED");
        int termMonths = 36;
        
        MurabahaContract contract = createTestContract(assetCost, profitAmount, termMonths);

        // When
        Money monthlyInstallment = contract.getMonthlyInstallmentAmount();

        // Then
        // Total = 138000, divided by 36 months = 3833.33
        Money expectedInstallment = Money.of(new BigDecimal("3833.33"), "AED");
        assertThat(monthlyInstallment).isEqualTo(expectedInstallment);
    }

    @Test
    @DisplayName("Should validate profit rate within Islamic limits")
    void shouldValidateProfitRateWithinIslamicLimits() {
        // Given
        Money assetCost = Money.of(new BigDecimal("100000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("8000.00"), "AED"); // 8% profit rate
        
        MurabahaContract contract = createTestContract(assetCost, profitAmount);

        // When
        BigDecimal profitRate = contract.getProfitRate();

        // Then
        assertThat(profitRate).isEqualByComparingTo(new BigDecimal("0.08")); // 8%
        assertThat(contract.isProfitRateWithinLimits()).isTrue();
        assertThat(contract.isShariahCompliant()).isTrue();
    }

    @Test
    @DisplayName("Should reject excessive profit rates")
    void shouldRejectExcessiveProfitRates() {
        // Given - Profit rate over 25% (excessive)
        Money assetCost = Money.of(new BigDecimal("100000.00"), "AED");
        Money excessiveProfit = Money.of(new BigDecimal("30000.00"), "AED"); // 30% profit

        // When & Then
        assertThatThrownBy(() -> MurabahaContract.create(
            "CUST-12345678",
            "Test Asset",
            assetCost,
            excessiveProfit,
            24,
            LocalDate.now().plusDays(7)
        )).isInstanceOf(ExcessiveProfitRateException.class)
          .hasMessageContaining("exceeds maximum allowed");
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 24, 36, 48, 60})
    @DisplayName("Should support standard Islamic finance terms")
    void shouldSupportStandardIslamicFinanceTerms(int termMonths) {
        // Given
        Money assetCost = Money.of(new BigDecimal("50000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("5000.00"), "AED");

        // When
        MurabahaContract contract = createTestContract(assetCost, profitAmount, termMonths);

        // Then
        assertThat(contract.getTermMonths()).isEqualTo(termMonths);
        assertThat(contract.isTermWithinLimits()).isTrue();
    }

    @Test
    @DisplayName("Should approve contract by Sharia board")
    void shouldApproveContractByShariahBoard() {
        // Given
        MurabahaContract contract = createTestContract();
        String boardMemberId = "SHARIA-BOARD-001";
        String approvalNotes = "Contract complies with Islamic finance principles";

        // When
        contract.approveByShariahBoard(boardMemberId, approvalNotes);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.SHARIA_APPROVED);
        assertThat(contract.getShariahApprovalDate()).isNotNull();
        assertThat(contract.getShariahBoardMemberId()).isEqualTo(boardMemberId);
        assertThat(contract.getShariahApprovalNotes()).isEqualTo(approvalNotes);
    }

    @Test
    @DisplayName("Should activate contract after all approvals")
    void shouldActivateContractAfterAllApprovals() {
        // Given
        MurabahaContract contract = createTestContract();
        contract.approveByShariahBoard("SHARIA-BOARD-001", "Approved");
        String activationReference = "ACTIVATION-001";

        // When
        contract.activate(activationReference);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getActivationDate()).isNotNull();
        assertThat(contract.getActivationReference()).isEqualTo(activationReference);
        assertThat(contract.getInstallmentSchedule()).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate installment schedule upon activation")
    void shouldGenerateInstallmentScheduleUponActivation() {
        // Given
        MurabahaContract contract = createTestContract(
            Money.of(new BigDecimal("60000.00"), "AED"),
            Money.of(new BigDecimal("6000.00"), "AED"),
            12 // 12 months
        );
        contract.approveByShariahBoard("SHARIA-BOARD-001", "Approved");

        // When
        contract.activate("ACTIVATION-001");

        // Then
        assertThat(contract.getInstallmentSchedule()).hasSize(12);
        
        // Check first installment
        InstallmentSchedule firstInstallment = contract.getInstallmentSchedule().get(0);
        assertThat(firstInstallment.getInstallmentNumber()).isEqualTo(1);
        assertThat(firstInstallment.getAmount()).isEqualTo(Money.of(new BigDecimal("5500.00"), "AED"));
        assertThat(firstInstallment.getDueDate()).isAfter(LocalDate.now());
        assertThat(firstInstallment.getStatus()).isEqualTo(InstallmentStatus.PENDING);
    }

    @Test
    @DisplayName("Should record installment payment")
    void shouldRecordInstallmentPayment() {
        // Given
        MurabahaContract contract = createActiveContract();
        String paymentReference = "PAY-12345";
        Money paymentAmount = Money.of(new BigDecimal("5500.00"), "AED");

        // When
        contract.recordInstallmentPayment(1, paymentAmount, paymentReference);

        // Then
        InstallmentSchedule firstInstallment = contract.getInstallmentSchedule().get(0);
        assertThat(firstInstallment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(firstInstallment.getPaidAmount()).isEqualTo(paymentAmount);
        assertThat(firstInstallment.getPaymentReference()).isEqualTo(paymentReference);
        assertThat(firstInstallment.getPaidDate()).isNotNull();
    }

    @Test
    @DisplayName("Should handle early settlement with discount")
    void shouldHandleEarlySettlementWithDiscount() {
        // Given
        MurabahaContract contract = createActiveContract();
        Money settlementAmount = Money.of(new BigDecimal("60000.00"), "AED"); // Early settlement discount
        String settlementReference = "EARLY-SETTLE-001";

        // When
        contract.settleEarly(settlementAmount, settlementReference);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.SETTLED);
        assertThat(contract.getEarlySettlementAmount()).isEqualTo(settlementAmount);
        assertThat(contract.getSettlementDate()).isNotNull();
        assertThat(contract.getSettlementReference()).isEqualTo(settlementReference);
    }

    @Test
    @DisplayName("Should handle contract default")
    void shouldHandleContractDefault() {
        // Given
        MurabahaContract contract = createActiveContract();
        String defaultReason = "Customer failed to make payments for 3 consecutive months";

        // When
        contract.markAsDefault(defaultReason);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DEFAULTED);
        assertThat(contract.getDefaultReason()).isEqualTo(defaultReason);
        assertThat(contract.getDefaultDate()).isNotNull();
    }

    @Test
    @DisplayName("Should validate asset delivery requirements")
    void shouldValidateAssetDeliveryRequirements() {
        // Given
        MurabahaContract contract = createActiveContract();
        String deliveryReference = "DELIVERY-001";
        String deliveryNotes = "Asset delivered to customer address";

        // When
        contract.confirmAssetDelivery(deliveryReference, deliveryNotes);

        // Then
        assertThat(contract.isAssetDelivered()).isTrue();
        assertThat(contract.getAssetDeliveryDate()).isNotNull();
        assertThat(contract.getDeliveryReference()).isEqualTo(deliveryReference);
        assertThat(contract.getDeliveryNotes()).isEqualTo(deliveryNotes);
    }

    @Test
    @DisplayName("Should calculate outstanding balance")
    void shouldCalculateOutstandingBalance() {
        // Given
        MurabahaContract contract = createActiveContract();
        
        // Pay first 3 installments
        Money installmentAmount = Money.of(new BigDecimal("5500.00"), "AED");
        contract.recordInstallmentPayment(1, installmentAmount, "PAY-001");
        contract.recordInstallmentPayment(2, installmentAmount, "PAY-002");
        contract.recordInstallmentPayment(3, installmentAmount, "PAY-003");

        // When
        Money outstandingBalance = contract.getOutstandingBalance();

        // Then
        // Total: 66000, Paid: 16500 (3 x 5500), Outstanding: 49500
        Money expectedOutstanding = Money.of(new BigDecimal("49500.00"), "AED");
        assertThat(outstandingBalance).isEqualTo(expectedOutstanding);
    }

    @Test
    @DisplayName("Should enforce Islamic finance business rules")
    void shouldEnforceIslamicFinanceBusinessRules() {
        // Given
        MurabahaContract contract = createTestContract();

        // Then - Islamic finance principles
        assertThat(contract.isShariahCompliant()).isTrue();
        assertThat(contract.hasInterestBasedCharges()).isFalse();
        assertThat(contract.hasAssetBacking()).isTrue();
        assertThat(contract.allowsSpeculation()).isFalse();
        assertThat(contract.requiresActualOwnership()).isTrue();
    }

    @Test
    @DisplayName("Should validate minimum and maximum contract amounts")
    void shouldValidateMinimumAndMaximumContractAmounts() {
        // Given - Amount below minimum
        Money tooSmallAmount = Money.of(new BigDecimal("5000.00"), "AED"); // Below 10K minimum
        Money smallProfit = Money.of(new BigDecimal("500.00"), "AED");

        // When & Then
        assertThatThrownBy(() -> MurabahaContract.create(
            "CUST-12345678",
            "Small Asset",
            tooSmallAmount,
            smallProfit,
            12,
            LocalDate.now().plusDays(7)
        )).isInstanceOf(InvalidContractAmountException.class)
          .hasMessageContaining("below minimum");
    }

    @Test
    @DisplayName("Should reject contracts without asset backing")
    void shouldRejectContractsWithoutAssetBacking() {
        // Given - Empty asset description
        Money assetCost = Money.of(new BigDecimal("50000.00"), "AED");
        Money profitAmount = Money.of(new BigDecimal("5000.00"), "AED");

        // When & Then
        assertThatThrownBy(() -> MurabahaContract.create(
            "CUST-12345678",
            "", // Empty asset description
            assetCost,
            profitAmount,
            24,
            LocalDate.now().plusDays(7)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Asset description cannot be empty");
    }

    private MurabahaContract createTestContract() {
        return createTestContract(
            Money.of(new BigDecimal("60000.00"), "AED"),
            Money.of(new BigDecimal("6000.00"), "AED"),
            12
        );
    }

    private MurabahaContract createTestContract(Money assetCost, Money profitAmount) {
        return createTestContract(assetCost, profitAmount, 24);
    }

    private MurabahaContract createTestContract(Money assetCost, Money profitAmount, int termMonths) {
        return MurabahaContract.create(
            "CUST-12345678",
            "Test Asset - Honda Civic 2024",
            assetCost,
            profitAmount,
            termMonths,
            LocalDate.now().plusDays(7)
        );
    }

    private MurabahaContract createActiveContract() {
        MurabahaContract contract = createTestContract();
        contract.approveByShariahBoard("SHARIA-BOARD-001", "Approved");
        contract.activate("ACTIVATION-001");
        return contract;
    }
}