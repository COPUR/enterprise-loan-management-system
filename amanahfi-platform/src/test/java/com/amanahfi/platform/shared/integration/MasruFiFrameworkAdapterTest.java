package com.amanahfi.platform.shared.integration;

import com.amanahfi.platform.islamicfinance.domain.*;
import com.amanahfi.platform.shared.domain.Money;
import com.amanahfi.platform.shared.integration.MasruFiFrameworkAdapter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for MasruFi Framework Adapter
 * 
 * This test class ensures the seamless integration between AmanahFi Platform
 * and MasruFi Framework capabilities, validating bidirectional model 
 * transformation and enhanced business rule enforcement.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MasruFi Framework Adapter Integration Tests")
class MasruFiFrameworkAdapterTest {

    @InjectMocks
    private MasruFiFrameworkAdapter adapter;

    private IslamicFinanceProductId productId;
    private CustomerId customerId;
    private Money principalAmount;
    private IslamicFinanceProduct sampleMurabaha;
    private IslamicFinanceProduct sampleQardHassan;

    @BeforeEach
    void setUp() {
        productId = new IslamicFinanceProductId(UUID.randomUUID());
        customerId = new CustomerId(UUID.randomUUID());
        principalAmount = Money.aed(new BigDecimal("100000.00"));

        sampleMurabaha = IslamicFinanceProduct.createMurabaha(
                productId,
                customerId,
                principalAmount,
                new BigDecimal("0.05"),
                LocalDate.now().plusYears(2),
                "Commercial vehicle for delivery business",
                "AE"
        );

        sampleQardHassan = IslamicFinanceProduct.createQardHassan(
                new IslamicFinanceProductId(UUID.randomUUID()),
                customerId,
                Money.aed(new BigDecimal("50000.00")),
                LocalDate.now().plusYears(1),
                "Emergency medical expenses",
                Money.aed(new BigDecimal("100.00")),
                "AE"
        );
    }

    @Nested
    @DisplayName("AmanahFi to MasruFi Model Conversion")
    class AmanahFiToMasruFiConversion {

        @Test
        @DisplayName("Should convert AmanahFi Murabaha to MasruFi model successfully")
        void shouldConvertAmanahFiMurabahaToMasruFiModelSuccessfully() {
            // When
            MasruFiIslamicFinancing masruFiModel = adapter.toMasruFiModel(sampleMurabaha);

            // Then
            assertNotNull(masruFiModel);
            assertEquals(productId.getValue(), masruFiModel.getFinancingId().getValue());
            assertEquals(MasruFiIslamicFinancingType.MURABAHA, masruFiModel.getIslamicFinancingType());
            assertEquals(principalAmount.getAmount(), masruFiModel.getPrincipalAmount().getAmount());
            assertEquals(principalAmount.getCurrency(), masruFiModel.getPrincipalAmount().getCurrency());
            assertEquals(new BigDecimal("0.05"), masruFiModel.getProfitMargin());
            assertEquals("Commercial vehicle for delivery business", masruFiModel.getAssetDescription());
            assertEquals("AE", masruFiModel.getJurisdiction());
            assertTrue(masruFiModel.isShariaCompliant());
        }

        @Test
        @DisplayName("Should convert AmanahFi Qard Hassan to MasruFi model successfully")
        void shouldConvertAmanahFiQardHassanToMasruFiModelSuccessfully() {
            // When
            MasruFiIslamicFinancing masruFiModel = adapter.toMasruFiModel(sampleQardHassan);

            // Then
            assertNotNull(masruFiModel);
            assertEquals(MasruFiIslamicFinancingType.QARD_HASSAN, masruFiModel.getIslamicFinancingType());
            assertEquals(Money.aed(new BigDecimal("50000.00")).getAmount(), masruFiModel.getPrincipalAmount().getAmount());
            assertEquals(BigDecimal.ZERO, masruFiModel.getProfitMargin());
            assertEquals("Emergency medical expenses", masruFiModel.getAssetDescription());
            assertTrue(masruFiModel.isShariaCompliant());
        }

        @Test
        @DisplayName("Should throw exception when converting null product")
        void shouldThrowExceptionWhenConvertingNullProduct() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> adapter.toMasruFiModel(null)
            );

            assertEquals("IslamicFinanceProduct cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should include Sharia compliance details in conversion")
        void shouldIncludeShariaComplianceDetailsInConversion() {
            // When
            MasruFiIslamicFinancing masruFiModel = adapter.toMasruFiModel(sampleMurabaha);

            // Then
            assertNotNull(masruFiModel.getShariaDaCompliance());
            assertTrue(masruFiModel.getShariaDaCompliance().isCompliant());
            assertEquals("UAE_HIGHER_SHARIA_AUTHORITY", masruFiModel.getShariaDaCompliance().getValidatingAuthority());
            assertTrue(masruFiModel.getShariaDaCompliance().isRibaFree());
            assertTrue(masruFiModel.getShariaDaCompliance().isGhararFree());
            assertTrue(masruFiModel.getShariaDaCompliance().isAssetBacked());
            assertTrue(masruFiModel.getShariaDaCompliance().isPermissibleAsset());
        }
    }

    @Nested
    @DisplayName("MasruFi to AmanahFi Model Conversion")
    class MasruFiToAmanahFiConversion {

        @Test
        @DisplayName("Should convert MasruFi Murabaha to AmanahFi model successfully")
        void shouldConvertMasruFiMurabahaToAmanahFiModelSuccessfully() {
            // Given
            MasruFiIslamicFinancing masruFiModel = createSampleMasruFiMurabaha();

            // When
            IslamicFinanceProduct amanahFiProduct = adapter.fromMasruFiModel(masruFiModel);

            // Then
            assertNotNull(amanahFiProduct);
            assertEquals(IslamicFinanceType.MURABAHA, amanahFiProduct.getFinanceType());
            assertEquals(principalAmount, amanahFiProduct.getPrincipalAmount());
            assertEquals(new BigDecimal("0.05"), amanahFiProduct.getProfitMargin());
            assertEquals("MasruFi test asset", amanahFiProduct.getAssetDescription());
            assertEquals("AE", amanahFiProduct.getJurisdiction());
            assertTrue(amanahFiProduct.isShariaCompliant());
        }

        @Test
        @DisplayName("Should convert MasruFi Qard Hassan to AmanahFi model successfully")
        void shouldConvertMasruFiQardHassanToAmanahFiModelSuccessfully() {
            // Given
            MasruFiIslamicFinancing masruFiModel = createSampleMasruFiQardHassan();

            // When
            IslamicFinanceProduct amanahFiProduct = adapter.fromMasruFiModel(masruFiModel);

            // Then
            assertNotNull(amanahFiProduct);
            assertEquals(IslamicFinanceType.QARD_HASSAN, amanahFiProduct.getFinanceType());
            assertEquals(Money.aed(new BigDecimal("25000.00")), amanahFiProduct.getPrincipalAmount());
            assertEquals(BigDecimal.ZERO, amanahFiProduct.getProfitMargin());
            assertEquals("MasruFi emergency fund", amanahFiProduct.getPurpose());
            assertTrue(amanahFiProduct.isShariaCompliant());
        }

        @Test
        @DisplayName("Should throw exception when converting null MasruFi model")
        void shouldThrowExceptionWhenConvertingNullMasruFiModel() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> adapter.fromMasruFiModel(null)
            );

            assertEquals("IslamicFinancing cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for unsupported finance type")
        void shouldThrowExceptionForUnsupportedFinanceType() {
            // Given
            MasruFiIslamicFinancing unsupportedModel = MasruFiIslamicFinancing.builder()
                    .financingId(new MasruFiFinancingId(UUID.randomUUID()))
                    .islamicFinancingType(MasruFiIslamicFinancingType.SALAM) // Not yet implemented
                    .build();

            // When & Then
            UnsupportedOperationException exception = assertThrows(
                    UnsupportedOperationException.class,
                    () -> adapter.fromMasruFiModel(unsupportedModel)
            );

            assertTrue(exception.getMessage().contains("Finance type not supported"));
        }
    }

    @Nested
    @DisplayName("MasruFi Framework Enhancement")
    class MasruFiFrameworkEnhancement {

        @Test
        @DisplayName("Should enhance AmanahFi product with MasruFi capabilities")
        void shouldEnhanceAmanahFiProductWithMasruFiCapabilities() {
            // When
            IslamicFinanceProduct enhancedProduct = adapter.enhanceWithMasruFiCapabilities(sampleMurabaha);

            // Then
            assertNotNull(enhancedProduct);
            assertTrue(enhancedProduct.isMasruFiEnhanced());
            assertNotNull(enhancedProduct.getShariaComplianceDetails());
            assertTrue(enhancedProduct.getShariaComplianceDetails().getComplianceNotes()
                    .contains("Enhanced with MasruFi Framework validation"));
        }

        @Test
        @DisplayName("Should throw exception when enhancing null product")
        void shouldThrowExceptionWhenEnhancingNullProduct() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> adapter.enhanceWithMasruFiCapabilities(null)
            );

            assertEquals("Product cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should validate MasruFi business rules during enhancement")
        void shouldValidateMasruFiBusinessRulesDuringEnhancement() {
            // Given - Create product with excessive profit margin
            IslamicFinanceProduct productWithExcessiveProfit = IslamicFinanceProduct.createMurabaha(
                    new IslamicFinanceProductId(UUID.randomUUID()),
                    customerId,
                    principalAmount,
                    new BigDecimal("0.25"), // 25% profit - within current limits but will be tested against MasruFi limits
                    LocalDate.now().plusYears(2),
                    "Test asset",
                    "AE"
            );

            // When & Then - Should succeed as 25% is within MasruFi Framework limits (30%)
            assertDoesNotThrow(() -> adapter.enhanceWithMasruFiCapabilities(productWithExcessiveProfit));
        }
    }

    @Nested
    @DisplayName("Business Rule Validation")
    class BusinessRuleValidation {

        @Test
        @DisplayName("Should enforce MasruFi profit margin limits")
        void shouldEnforceMasruFiProfitMarginLimits() {
            // Given - This would need to be created through the adapter to test the limits
            MasruFiIslamicFinancing modelWithExcessiveProfit = MasruFiIslamicFinancing.builder()
                    .financingId(new MasruFiFinancingId(UUID.randomUUID()))
                    .islamicFinancingType(MasruFiIslamicFinancingType.MURABAHA)
                    .customerProfile(createSampleCustomerProfile())
                    .principalAmount(new MasruFiMoney(new BigDecimal("100000.00"), 
                            java.util.Currency.getInstance("AED")))
                    .totalAmount(new MasruFiMoney(new BigDecimal("135000.00"), 
                            java.util.Currency.getInstance("AED"))) // 35% profit - exceeds limit
                    .profitMargin(new BigDecimal("0.35"))
                    .assetDescription("Test asset")
                    .maturityDate(java.time.LocalDateTime.now().plusYears(2))
                    .jurisdiction("AE")
                    .shariaCompliance(createCompliantShariaValidation())
                    .build();

            // When & Then
            ExcessiveProfitMarginException exception = assertThrows(
                    ExcessiveProfitMarginException.class,
                    () -> adapter.validateMasruFiBusinessRules(modelWithExcessiveProfit)
            );

            assertTrue(exception.getMessage().contains("Profit rate exceeds MasruFi framework limits"));
        }

        @Test
        @DisplayName("Should validate asset backing requirements")
        void shouldValidateAssetBackingRequirements() {
            // Given
            MasruFiIslamicFinancing modelWithoutAsset = MasruFiIslamicFinancing.builder()
                    .financingId(new MasruFiFinancingId(UUID.randomUUID()))
                    .islamicFinancingType(MasruFiIslamicFinancingType.MURABAHA)
                    .customerProfile(createSampleCustomerProfile())
                    .principalAmount(new MasruFiMoney(new BigDecimal("100000.00"), 
                            java.util.Currency.getInstance("AED")))
                    .profitMargin(new BigDecimal("0.05"))
                    .assetDescription("") // Empty asset description
                    .maturityDate(java.time.LocalDateTime.now().plusYears(2))
                    .jurisdiction("AE")
                    .shariaCompliance(createCompliantShariaValidation())
                    .build();

            // When & Then
            AssetValidationException exception = assertThrows(
                    AssetValidationException.class,
                    () -> adapter.validateAssetBacking(modelWithoutAsset)
            );

            assertTrue(exception.getMessage().contains("Asset description required"));
        }
    }

    @Nested
    @DisplayName("Integration Quality Assurance")
    class IntegrationQualityAssurance {

        @Test
        @DisplayName("Should maintain data integrity during round-trip conversion")
        void shouldMaintainDataIntegrityDuringRoundTripConversion() {
            // When - Convert AmanahFi -> MasruFi -> AmanahFi
            MasruFiIslamicFinancing masruFiModel = adapter.toMasruFiModel(sampleMurabaha);
            IslamicFinanceProduct convertedBack = adapter.fromMasruFiModel(masruFiModel);

            // Then - Key properties should be preserved
            assertEquals(sampleMurabaha.getFinanceType(), convertedBack.getFinanceType());
            assertEquals(sampleMurabaha.getPrincipalAmount(), convertedBack.getPrincipalAmount());
            assertEquals(sampleMurabaha.getProfitMargin(), convertedBack.getProfitMargin());
            assertEquals(sampleMurabaha.getJurisdiction(), convertedBack.getJurisdiction());
            assertEquals(sampleMurabaha.isShariaCompliant(), convertedBack.isShariaCompliant());
        }

        @Test
        @DisplayName("Should preserve Sharia compliance status across conversions")
        void shouldPreserveShariaComplianceStatusAcrossConversions() {
            // When
            MasruFiIslamicFinancing masruFiModel = adapter.toMasruFiModel(sampleMurabaha);
            IslamicFinanceProduct convertedBack = adapter.fromMasruFiModel(masruFiModel);

            // Then
            assertTrue(sampleMurabaha.isShariaCompliant());
            assertTrue(masruFiModel.isShariaCompliant());
            assertTrue(convertedBack.isShariaCompliant());
        }
    }

    // Helper methods

    private MasruFiIslamicFinancing createSampleMasruFiMurabaha() {
        return MasruFiIslamicFinancing.builder()
                .financingId(new MasruFiFinancingId(productId.getValue()))
                .islamicFinancingType(MasruFiIslamicFinancingType.MURABAHA)
                .customerProfile(createSampleCustomerProfile())
                .principalAmount(new MasruFiMoney(principalAmount.getAmount(), principalAmount.getCurrency()))
                .totalAmount(new MasruFiMoney(principalAmount.getAmount().multiply(new BigDecimal("1.05")), 
                        principalAmount.getCurrency()))
                .profitMargin(new BigDecimal("0.05"))
                .assetDescription("MasruFi test asset")
                .maturityDate(java.time.LocalDateTime.now().plusYears(2))
                .jurisdiction("AE")
                .shariaCompliance(createCompliantShariaValidation())
                .build();
    }

    private MasruFiIslamicFinancing createSampleMasruFiQardHassan() {
        return MasruFiIslamicFinancing.builder()
                .financingId(new MasruFiFinancingId(UUID.randomUUID()))
                .islamicFinancingType(MasruFiIslamicFinancingType.QARD_HASSAN)
                .customerProfile(createSampleCustomerProfile())
                .principalAmount(new MasruFiMoney(new BigDecimal("25000.00"), 
                        java.util.Currency.getInstance("AED")))
                .totalAmount(new MasruFiMoney(new BigDecimal("25100.00"), 
                        java.util.Currency.getInstance("AED")))
                .profitMargin(BigDecimal.ZERO)
                .assetDescription("MasruFi emergency fund")
                .maturityDate(java.time.LocalDateTime.now().plusYears(1))
                .jurisdiction("AE")
                .shariaCompliance(createCompliantShariaValidation())
                .build();
    }

    private MasruFiCustomerProfile createSampleCustomerProfile() {
        return MasruFiCustomerProfile.builder()
                .customerId(customerId.getValue().toString())
                .customerName("Test Customer")
                .customerType(MasruFiCustomerType.INDIVIDUAL)
                .jurisdiction("AE")
                .build();
    }

    private MasruFiShariaComplianceValidation createCompliantShariaValidation() {
        return MasruFiShariaComplianceValidation.builder()
                .isCompliant(true)
                .validationDate(java.time.LocalDateTime.now().toString())
                .validatingAuthority("UAE_HIGHER_SHARIA_AUTHORITY")
                .complianceNotes("Fully compliant with Sharia principles")
                .ribaFree(true)
                .ghararFree(true)
                .assetBacked(true)
                .permissibleAsset(true)
                .build();
    }
}