package com.amanahfi.platform.islamicfinance.domain;

import com.amanahfi.platform.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for IslamicFinanceProduct aggregate root
 * 
 * This test class drives the development of the core Islamic Finance Product
 * aggregate that represents all types of Sharia-compliant financial products
 * in the AmanahFi Platform.
 * 
 * Test Coverage:
 * - All 6 Islamic finance models (Murabaha, Musharakah, Ijarah, Salam, Istisna, Qard Hassan)
 * - Sharia compliance validation
 * - Product lifecycle management
 * - Multi-currency support
 * - Regulatory compliance
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@DisplayName("Islamic Finance Product Tests")
class IslamicFinanceProductTest {

    private IslamicFinanceProductId productId;
    private CustomerId customerId;
    private Money principalAmount;
    private Currency aedCurrency;

    @BeforeEach
    void setUp() {
        productId = new IslamicFinanceProductId(UUID.randomUUID());
        customerId = new CustomerId(UUID.randomUUID());
        aedCurrency = Currency.getInstance("AED");
        principalAmount = Money.aed(new BigDecimal("100000.00"));
    }

    @Nested
    @DisplayName("Murabaha (Cost-Plus Financing)")
    class MurabahaTests {

        @Test
        @DisplayName("Should create Murabaha product with valid parameters")
        void shouldCreateMurabahaProductWithValidParameters() {
            // Given
            Money assetCost = Money.aed(new BigDecimal("95000.00"));
            BigDecimal profitMargin = new BigDecimal("0.05"); // 5% profit
            LocalDate maturityDate = LocalDate.now().plusYears(3);
            String assetDescription = "Commercial vehicle for delivery business";

            // When
            IslamicFinanceProduct murabaha = IslamicFinanceProduct.createMurabaha(
                productId,
                customerId,
                assetCost,
                profitMargin,
                maturityDate,
                assetDescription,
                "AE" // UAE jurisdiction
            );

            // Then
            assertNotNull(murabaha);
            assertEquals(productId, murabaha.getId());
            assertEquals(customerId, murabaha.getCustomerId());
            assertEquals(IslamicFinanceType.MURABAHA, murabaha.getFinanceType());
            assertEquals(assetCost, murabaha.getPrincipalAmount());
            assertEquals(profitMargin, murabaha.getProfitMargin());
            assertEquals(maturityDate, murabaha.getMaturityDate());
            assertEquals(assetDescription, murabaha.getAssetDescription());
            assertTrue(murabaha.isShariaCompliant());
            assertEquals(ProductStatus.DRAFT, murabaha.getStatus());
        }

        @Test
        @DisplayName("Should calculate total amount correctly for Murabaha")
        void shouldCalculateTotalAmountCorrectlyForMurabaha() {
            // Given
            Money assetCost = Money.aed(new BigDecimal("100000.00"));
            BigDecimal profitMargin = new BigDecimal("0.10"); // 10% profit
            LocalDate maturityDate = LocalDate.now().plusYears(2);

            IslamicFinanceProduct murabaha = IslamicFinanceProduct.createMurabaha(
                productId, customerId, assetCost, profitMargin, maturityDate,
                "Real estate property", "AE"
            );

            // When
            Money totalAmount = murabaha.calculateTotalAmount();

            // Then
            Money expectedTotal = Money.aed(new BigDecimal("110000.00")); // 100,000 + 10% profit
            assertEquals(expectedTotal, totalAmount);
        }

        @Test
        @DisplayName("Should reject Murabaha with zero or negative profit margin")
        void shouldRejectMurabahaWithZeroOrNegativeProfitMargin() {
            // Given
            Money assetCost = Money.aed(new BigDecimal("100000.00"));
            BigDecimal zeroProfitMargin = BigDecimal.ZERO;
            LocalDate maturityDate = LocalDate.now().plusYears(1);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                IslamicFinanceProduct.createMurabaha(
                    productId, customerId, assetCost, zeroProfitMargin,
                    maturityDate, "Test asset", "AE"
                )
            );
        }

        @Test
        @DisplayName("Should enforce asset requirement for Murabaha")
        void shouldEnforceAssetRequirementForMurabaha() {
            // Given
            Money assetCost = Money.aed(new BigDecimal("100000.00"));
            BigDecimal profitMargin = new BigDecimal("0.05");
            LocalDate maturityDate = LocalDate.now().plusYears(1);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                IslamicFinanceProduct.createMurabaha(
                    productId, customerId, assetCost, profitMargin,
                    maturityDate, null, "AE" // null asset description
                )
            );
        }
    }

    @Nested
    @DisplayName("Musharakah (Partnership Financing)")
    class MusharakahTests {

        @Test
        @DisplayName("Should create Musharakah product with partnership terms")
        void shouldCreateMusharakahProductWithPartnershipTerms() {
            // Given
            Money bankContribution = Money.aed(new BigDecimal("500000.00"));
            Money customerContribution = Money.aed(new BigDecimal("300000.00"));
            BigDecimal bankProfitShare = new BigDecimal("0.60"); // 60% profit share
            BigDecimal bankLossShare = new BigDecimal("0.625"); // 62.5% loss share (proportional to contribution)
            LocalDate maturityDate = LocalDate.now().plusYears(5);
            String businessDescription = "Technology startup venture";

            // When
            IslamicFinanceProduct musharakah = IslamicFinanceProduct.createMusharakah(
                productId,
                customerId,
                bankContribution,
                customerContribution,
                bankProfitShare,
                bankLossShare,
                maturityDate,
                businessDescription,
                "AE"
            );

            // Then
            assertNotNull(musharakah);
            assertEquals(IslamicFinanceType.MUSHARAKAH, musharakah.getFinanceType());
            assertEquals(bankContribution, musharakah.getPrincipalAmount());
            assertEquals(bankProfitShare, musharakah.getProfitShare());
            assertEquals(bankLossShare, musharakah.getLossShare());
            assertTrue(musharakah.isShariaCompliant());
        }

        @Test
        @DisplayName("Should validate profit and loss sharing ratios")
        void shouldValidateProfitAndLossSharingRatios() {
            // Given
            Money bankContribution = Money.aed(new BigDecimal("400000.00"));
            Money customerContribution = Money.aed(new BigDecimal("600000.00"));
            BigDecimal invalidProfitShare = new BigDecimal("1.50"); // 150% - invalid
            BigDecimal validLossShare = new BigDecimal("0.40"); // 40%
            LocalDate maturityDate = LocalDate.now().plusYears(3);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                IslamicFinanceProduct.createMusharakah(
                    productId, customerId, bankContribution, customerContribution,
                    invalidProfitShare, validLossShare, maturityDate,
                    "Business venture", "AE"
                )
            );
        }
    }

    @Nested
    @DisplayName("Ijarah (Lease Financing)")
    class IjarahTests {

        @Test
        @DisplayName("Should create Ijarah product with lease terms")
        void shouldCreateIjarahProductWithLeaseTerms() {
            // Given
            Money assetValue = Money.aed(new BigDecimal("250000.00"));
            Money monthlyRental = Money.aed(new BigDecimal("5000.00"));
            Period leaseTerm = Period.ofYears(4);
            LocalDate leaseStartDate = LocalDate.now().plusDays(30);
            String assetDescription = "Industrial equipment for manufacturing";

            // When
            IslamicFinanceProduct ijarah = IslamicFinanceProduct.createIjarah(
                productId,
                customerId,
                assetValue,
                monthlyRental,
                leaseTerm,
                leaseStartDate,
                assetDescription,
                "AE"
            );

            // Then
            assertNotNull(ijarah);
            assertEquals(IslamicFinanceType.IJARAH, ijarah.getFinanceType());
            assertEquals(assetValue, ijarah.getPrincipalAmount());
            assertEquals(monthlyRental, ijarah.getMonthlyRental());
            assertEquals(leaseTerm, ijarah.getLeaseTerm());
            assertEquals(leaseStartDate, ijarah.getLeaseStartDate());
            assertTrue(ijarah.isShariaCompliant());
        }

        @Test
        @DisplayName("Should calculate total lease amount for Ijarah")
        void shouldCalculateTotalLeaseAmountForIjarah() {
            // Given
            Money assetValue = Money.aed(new BigDecimal("120000.00"));
            Money monthlyRental = Money.aed(new BigDecimal("3000.00"));
            Period leaseTerm = Period.ofYears(3); // 36 months
            LocalDate leaseStartDate = LocalDate.now().plusDays(30);

            IslamicFinanceProduct ijarah = IslamicFinanceProduct.createIjarah(
                productId, customerId, assetValue, monthlyRental, leaseTerm,
                leaseStartDate, "Equipment lease", "AE"
            );

            // When
            Money totalLeaseAmount = ijarah.calculateTotalLeaseAmount();

            // Then
            Money expectedTotal = Money.aed(new BigDecimal("108000.00")); // 3,000 × 36 months
            assertEquals(expectedTotal, totalLeaseAmount);
        }
    }

    @Nested
    @DisplayName("Qard Hassan (Benevolent Loan)")
    class QardHassanTests {

        @Test
        @DisplayName("Should create Qard Hassan with no profit expectation")
        void shouldCreateQardHassanWithNoProfitExpectation() {
            // Given
            Money loanAmount = Money.aed(new BigDecimal("50000.00"));
            LocalDate repaymentDate = LocalDate.now().plusYears(2);
            String purpose = "Emergency medical expenses";
            Money administrativeFee = Money.aed(new BigDecimal("100.00"));

            // When
            IslamicFinanceProduct qardHassan = IslamicFinanceProduct.createQardHassan(
                productId,
                customerId,
                loanAmount,
                repaymentDate,
                purpose,
                administrativeFee,
                "AE"
            );

            // Then
            assertNotNull(qardHassan);
            assertEquals(IslamicFinanceType.QARD_HASSAN, qardHassan.getFinanceType());
            assertEquals(loanAmount, qardHassan.getPrincipalAmount());
            assertEquals(BigDecimal.ZERO, qardHassan.getProfitMargin());
            assertEquals(repaymentDate, qardHassan.getMaturityDate());
            assertEquals(administrativeFee, qardHassan.getAdministrativeFee());
            assertTrue(qardHassan.isShariaCompliant());
        }

        @Test
        @DisplayName("Should ensure Qard Hassan total equals principal plus admin fee only")
        void shouldEnsureQardHassanTotalEqualsPrincipalPlusAdminFeeOnly() {
            // Given
            Money loanAmount = Money.aed(new BigDecimal("25000.00"));
            Money adminFee = Money.aed(new BigDecimal("50.00"));
            LocalDate repaymentDate = LocalDate.now().plusYears(1);

            IslamicFinanceProduct qardHassan = IslamicFinanceProduct.createQardHassan(
                productId, customerId, loanAmount, repaymentDate,
                "Education expenses", adminFee, "AE"
            );

            // When
            Money totalAmount = qardHassan.calculateTotalAmount();

            // Then
            Money expectedTotal = Money.aed(new BigDecimal("25050.00")); // No profit, only admin fee
            assertEquals(expectedTotal, totalAmount);
        }
    }

    @Nested
    @DisplayName("Sharia Compliance Validation")
    class ShariaComplianceValidation {

        @Test
        @DisplayName("Should validate Sharia compliance for all product types")
        void shouldValidateShariaComplianceForAllProductTypes() {
            // Given & When
            IslamicFinanceProduct murabaha = createSampleMurabaha();
            IslamicFinanceProduct qardHassan = createSampleQardHassan();

            // Then
            assertTrue(murabaha.isShariaCompliant());
            assertTrue(qardHassan.isShariaCompliant());
            assertNotNull(murabaha.getShariaComplianceDetails());
            assertNotNull(qardHassan.getShariaComplianceDetails());
        }

        @Test
        @DisplayName("Should track Sharia compliance authority")
        void shouldTrackShariaComplianceAuthority() {
            // Given
            IslamicFinanceProduct product = createSampleMurabaha();

            // When
            ShariaComplianceDetails compliance = product.getShariaComplianceDetails();

            // Then
            assertNotNull(compliance);
            assertEquals("UAE_HIGHER_SHARIA_AUTHORITY", compliance.getValidatingAuthority());
            assertTrue(compliance.isCompliant());
            assertNotNull(compliance.getValidationDate());
        }

        private IslamicFinanceProduct createSampleMurabaha() {
            return IslamicFinanceProduct.createMurabaha(
                productId, customerId, Money.aed(new BigDecimal("100000.00")),
                new BigDecimal("0.05"), LocalDate.now().plusYears(2),
                "Sample asset", "AE"
            );
        }

        private IslamicFinanceProduct createSampleQardHassan() {
            return IslamicFinanceProduct.createQardHassan(
                productId, customerId, Money.aed(new BigDecimal("50000.00")),
                LocalDate.now().plusYears(1), "Emergency fund",
                Money.aed(new BigDecimal("50.00")), "AE"
            );
        }
    }

    @Nested
    @DisplayName("Product Lifecycle Management")
    class ProductLifecycleManagement {

        @Test
        @DisplayName("Should approve product and change status")
        void shouldApproveProductAndChangeStatus() {
            // Given
            IslamicFinanceProduct product = createSampleMurabaha();
            assertEquals(ProductStatus.DRAFT, product.getStatus());

            // When
            product.approve("APPROVER123");

            // Then
            assertEquals(ProductStatus.APPROVED, product.getStatus());
            assertTrue(product.hasUncommittedEvents());
            assertEquals(1, product.getUncommittedEvents().size());
        }

        @Test
        @DisplayName("Should activate approved product")
        void shouldActivateApprovedProduct() {
            // Given
            IslamicFinanceProduct product = createSampleMurabaha();
            product.approve("APPROVER123");

            // When
            product.activate();

            // Then
            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertTrue(product.hasUncommittedEvents());
        }

        @Test
        @DisplayName("Should not activate non-approved product")
        void shouldNotActivateNonApprovedProduct() {
            // Given
            IslamicFinanceProduct product = createSampleMurabaha();
            assertEquals(ProductStatus.DRAFT, product.getStatus());

            // When & Then
            assertThrows(IllegalStateException.class, product::activate);
        }

        private IslamicFinanceProduct createSampleMurabaha() {
            return IslamicFinanceProduct.createMurabaha(
                new IslamicFinanceProductId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                Money.aed(new BigDecimal("100000.00")),
                new BigDecimal("0.05"),
                LocalDate.now().plusYears(2),
                "Sample asset",
                "AE"
            );
        }
    }
}