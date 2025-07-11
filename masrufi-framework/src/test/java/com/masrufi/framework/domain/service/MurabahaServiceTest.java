package com.masrufi.framework.domain.service;

import com.masrufi.framework.domain.model.*;
import com.masrufi.framework.domain.port.out.IslamicFinancingRepository;
import com.masrufi.framework.domain.port.out.ShariaComplianceValidationPort;
import com.masrufi.framework.domain.port.out.AssetValidationPort;
import com.masrufi.framework.domain.port.out.CustomerValidationPort;
import com.masrufi.framework.domain.exception.ShariaViolationException;
import com.masrufi.framework.domain.exception.AssetValidationException;
import com.masrufi.framework.domain.exception.CustomerValidationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development Test Suite for Murabaha Service
 * 
 * This comprehensive test suite validates all Murabaha business logic following TDD principles:
 * - Red: Write failing tests first
 * - Green: Implement minimal code to pass tests
 * - Refactor: Improve code while keeping tests green
 * 
 * Test Coverage:
 * - Murabaha creation with valid data
 * - Sharia compliance validation
 * - Asset validation and permissibility
 * - Customer eligibility validation
 * - Profit margin validation
 * - Edge cases and error scenarios
 * - Business rule validation
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🛒 Murabaha Service TDD Test Suite")
class MurabahaServiceTest {

    @Mock
    private IslamicFinancingRepository islamicFinancingRepository;
    
    @Mock
    private ShariaComplianceValidationPort shariaComplianceValidation;
    
    @Mock
    private AssetValidationPort assetValidation;
    
    @Mock
    private CustomerValidationPort customerValidation;

    private MurabahaService murabahaService;
    private CreateMurabahaCommand validCommand;
    private CustomerProfile validCustomer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        murabahaService = new MurabahaService(
            islamicFinancingRepository,
            shariaComplianceValidation,
            assetValidation,
            customerValidation
        );
        
        setupValidCustomer();
        setupValidCommand();
        setupDefaultMocks();
    }

    private void setupValidCustomer() {
        validCustomer = CustomerProfile.builder()
            .customerId("CUST-001")
            .customerName("Ahmed Al-Rashid")
            .customerType(CustomerType.INDIVIDUAL)
            .creditScore(750)
            .monthlyIncome(Money.of("15000", "AED"))
            .jurisdiction("UAE")
            .build();
    }

    private void setupValidCommand() {
        validCommand = CreateMurabahaCommand.builder()
            .customerProfile(validCustomer)
            .assetDescription("Toyota Camry 2024 - Mid-size sedan with safety features")
            .assetCost(Money.of("120000", "AED"))
            .profitMargin(new BigDecimal("0.15")) // 15%
            .maturityDate(LocalDateTime.now().plusYears(3))
            .supplier("Toyota Dealer UAE")
            .jurisdiction("UAE")
            .paymentFrequency(CreateMurabahaCommand.PaymentFrequency.MONTHLY)
            .purpose("Personal transportation")
            .assetCategory(CreateMurabahaCommand.AssetCategory.VEHICLES)
            .immediateDelivery(true)
            .build();
    }

    private void setupDefaultMocks() {
        // Default successful validations
        when(customerValidation.isEligibleForIslamicFinancing(any(CustomerProfile.class)))
            .thenReturn(true);
        when(assetValidation.isAssetPermissible(anyString()))
            .thenReturn(true);
        when(assetValidation.validateAssetValue(any(Money.class)))
            .thenReturn(true);
        when(shariaComplianceValidation.validateMurabahaCompliance(any(CreateMurabahaCommand.class)))
            .thenReturn(ShariaComplianceResult.compliant());
        when(islamicFinancingRepository.save(any(IslamicFinancing.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // =====================================================
    // POSITIVE TEST CASES - TDD GREEN PHASE
    // =====================================================

    @Test
    @Order(1)
    @DisplayName("Should create valid Murabaha financing")
    void shouldCreateValidMurabahaFinancing() {
        // When
        IslamicFinancing result = murabahaService.createMurabaha(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIslamicFinancingType()).isEqualTo(IslamicFinancing.IslamicFinancingType.MURABAHA);
        assertThat(result.getCustomerProfile()).isEqualTo(validCustomer);
        assertThat(result.getPrincipalAmount()).isEqualTo(validCommand.getAssetCost());
        assertThat(result.getTotalAmount()).isEqualTo(validCommand.getSellingPrice());
        assertThat(result.getProfitMargin()).isEqualTo(validCommand.getProfitMargin());
        assertThat(result.getAssetDescription()).isEqualTo(validCommand.getAssetDescription());
        assertThat(result.getMaturityDate()).isEqualTo(validCommand.getMaturityDate());
        assertThat(result.isShariaCompliant()).isTrue();

        // Verify interactions
        verify(customerValidation).isEligibleForIslamicFinancing(validCustomer);
        verify(assetValidation).isAssetPermissible(validCommand.getAssetDescription());
        verify(shariaComplianceValidation).validateMurabahaCompliance(validCommand);
        verify(islamicFinancingRepository).save(any(IslamicFinancing.class));
    }

    @Test
    @Order(2)
    @DisplayName("Should create Murabaha with correct profit calculation")
    void shouldCreateMurabahaWithCorrectProfitCalculation() {
        // Given
        Money assetCost = Money.of("100000", "AED");
        BigDecimal profitMargin = new BigDecimal("0.20"); // 20%
        Money expectedSellingPrice = Money.of("120000", "AED"); // 100k + 20k profit

        CreateMurabahaCommand command = validCommand.toBuilder()
            .assetCost(assetCost)
            .profitMargin(profitMargin)
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result.getPrincipalAmount()).isEqualTo(assetCost);
        assertThat(result.getTotalAmount()).isEqualTo(expectedSellingPrice);
        assertThat(result.getProfitMargin()).isEqualTo(profitMargin);
        
        Money calculatedProfit = result.getTotalAmount().subtract(result.getPrincipalAmount());
        Money expectedProfit = Money.of("20000", "AED");
        assertThat(calculatedProfit).isEqualTo(expectedProfit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AED", "SAR", "USD", "EUR"})
    @Order(3)
    @DisplayName("Should support multiple currencies")
    void shouldSupportMultipleCurrencies(String currency) {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .assetCost(Money.of("50000", currency))
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result.getPrincipalAmount().getCurrency()).isEqualTo(currency);
        assertThat(result.getTotalAmount().getCurrency()).isEqualTo(currency);
    }

    @Test
    @Order(4)
    @DisplayName("Should create Murabaha with different asset categories")
    void shouldCreateMurabahaWithDifferentAssetCategories() {
        // Given
        CreateMurabahaCommand.AssetCategory[] categories = CreateMurabahaCommand.AssetCategory.values();

        for (CreateMurabahaCommand.AssetCategory category : categories) {
            // When
            CreateMurabahaCommand command = validCommand.toBuilder()
                .assetCategory(category)
                .assetDescription("Asset in category: " + category.name())
                .build();

            IslamicFinancing result = murabahaService.createMurabaha(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAssetDescription()).contains(category.name());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should create Murabaha with immediate delivery")
    void shouldCreateMurabahaWithImmediateDelivery() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .immediateDelivery(true)
            .expectedDeliveryDate(null)
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(command.isImmediateDelivery()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Should create Murabaha with future delivery")
    void shouldCreateMurabahaWithFutureDelivery() {
        // Given
        LocalDateTime futureDelivery = LocalDateTime.now().plusDays(30);
        CreateMurabahaCommand command = validCommand.toBuilder()
            .immediateDelivery(false)
            .expectedDeliveryDate(futureDelivery)
            .deliveryAddress("123 Business Street, Dubai, UAE")
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(command.isImmediateDelivery()).isFalse();
        assertThat(command.getExpectedDeliveryDate()).isEqualTo(futureDelivery);
    }

    // =====================================================
    // NEGATIVE TEST CASES - TDD RED PHASE
    // =====================================================

    @Test
    @Order(7)
    @DisplayName("Should reject Murabaha when customer not eligible")
    void shouldRejectMurabahaWhenCustomerNotEligible() {
        // Given
        when(customerValidation.isEligibleForIslamicFinancing(any(CustomerProfile.class)))
            .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(validCommand))
            .isInstanceOf(CustomerValidationException.class)
            .hasMessageContaining("Customer not eligible for Islamic financing");

        verify(customerValidation).isEligibleForIslamicFinancing(validCustomer);
        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(8)
    @DisplayName("Should reject Murabaha when asset not permissible")
    void shouldRejectMurabahaWhenAssetNotPermissible() {
        // Given
        when(assetValidation.isAssetPermissible(anyString()))
            .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(validCommand))
            .isInstanceOf(AssetValidationException.class)
            .hasMessageContaining("Asset not permissible under Sharia law");

        verify(assetValidation).isAssetPermissible(validCommand.getAssetDescription());
        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(9)
    @DisplayName("Should reject Murabaha when Sharia compliance fails")
    void shouldRejectMurabahaWhenShariaComplianceFails() {
        // Given
        when(shariaComplianceValidation.validateMurabahaCompliance(any(CreateMurabahaCommand.class)))
            .thenReturn(ShariaComplianceResult.nonCompliant("Excessive profit margin detected"));

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(validCommand))
            .isInstanceOf(ShariaViolationException.class)
            .hasMessageContaining("Excessive profit margin detected");

        verify(shariaComplianceValidation).validateMurabahaCompliance(validCommand);
        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(10)
    @DisplayName("Should reject Murabaha with null command")
    void shouldRejectMurabahaWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CreateMurabahaCommand cannot be null");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(11)
    @DisplayName("Should reject Murabaha with null customer profile")
    void shouldRejectMurabahaWithNullCustomerProfile() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .customerProfile(null)
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Customer profile is required");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(12)
    @DisplayName("Should reject Murabaha with zero asset cost")
    void shouldRejectMurabahaWithZeroAssetCost() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .assetCost(Money.of("0", "AED"))
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Asset cost must be positive");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(13)
    @DisplayName("Should reject Murabaha with negative profit margin")
    void shouldRejectMurabahaWithNegativeProfitMargin() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .profitMargin(new BigDecimal("-0.05"))
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Profit margin must be positive");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(14)
    @DisplayName("Should reject Murabaha with excessive profit margin")
    void shouldRejectMurabahaWithExcessiveProfitMargin() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .profitMargin(new BigDecimal("0.60")) // 60% - excessive
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Profit margin cannot exceed 50%");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(15)
    @DisplayName("Should reject Murabaha with past maturity date")
    void shouldRejectMurabahaWithPastMaturityDate() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .maturityDate(LocalDateTime.now().minusDays(1))
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maturity date must be in the future");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(16)
    @DisplayName("Should reject Murabaha with short term")
    void shouldRejectMurabahaWithShortTerm() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .maturityDate(LocalDateTime.now().plusDays(15)) // Less than 30 days
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Murabaha term must be at least 30 days");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(17)
    @DisplayName("Should reject Murabaha with excessive term")
    void shouldRejectMurabahaWithExcessiveTerm() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .maturityDate(LocalDateTime.now().plusYears(35)) // More than 30 years
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Murabaha term cannot exceed 30 years");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(18)
    @DisplayName("Should reject Murabaha with empty asset description")
    void shouldRejectMurabahaWithEmptyAssetDescription() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .assetDescription("")
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Asset description cannot be empty");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(19)
    @DisplayName("Should reject Murabaha with short asset description")
    void shouldRejectMurabahaWithShortAssetDescription() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .assetDescription("Car") // Too short
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Asset description too short");

        verify(islamicFinancingRepository, never()).save(any());
    }

    @Test
    @Order(20)
    @DisplayName("Should reject Murabaha with empty supplier")
    void shouldRejectMurabahaWithEmptySupplier() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .supplier("")
            .build();

        // When & Then
        assertThatThrownBy(() -> murabahaService.createMurabaha(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Supplier cannot be empty");

        verify(islamicFinancingRepository, never()).save(any());
    }

    // =====================================================
    // EDGE CASES AND BOUNDARY TESTS
    // =====================================================

    @Test
    @Order(21)
    @DisplayName("Should handle minimum valid profit margin")
    void shouldHandleMinimumValidProfitMargin() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .profitMargin(new BigDecimal("0.01")) // 1% - minimum
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProfitMargin()).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    @Order(22)
    @DisplayName("Should handle maximum valid profit margin")
    void shouldHandleMaximumValidProfitMargin() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .profitMargin(new BigDecimal("0.50")) // 50% - maximum
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProfitMargin()).isEqualTo(new BigDecimal("0.50"));
    }

    @Test
    @Order(23)
    @DisplayName("Should handle minimum term")
    void shouldHandleMinimumTerm() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .maturityDate(LocalDateTime.now().plusDays(30)) // Exactly 30 days
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMaturityDate()).isEqualTo(command.getMaturityDate());
    }

    @Test
    @Order(24)
    @DisplayName("Should handle maximum term")
    void shouldHandleMaximumTerm() {
        // Given
        CreateMurabahaCommand command = validCommand.toBuilder()
            .maturityDate(LocalDateTime.now().plusYears(30)) // Exactly 30 years
            .build();

        // When
        IslamicFinancing result = murabahaService.createMurabaha(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMaturityDate()).isEqualTo(command.getMaturityDate());
    }

    @Test
    @Order(25)
    @DisplayName("Should generate unique Islamic Financing IDs")
    void shouldGenerateUniqueIslamicFinancingIds() {
        // Given
        int numberOfMurabaha = 10;

        // When
        Set<String> generatedIds = new HashSet<>();
        for (int i = 0; i < numberOfMurabaha; i++) {
            IslamicFinancing result = murabahaService.createMurabaha(validCommand);
            generatedIds.add(result.getFinancingId().getValue());
        }

        // Then
        assertThat(generatedIds).hasSize(numberOfMurabaha);
        generatedIds.forEach(id -> {
            assertThat(id).startsWith("MURABAHA-");
            assertThat(id.length()).isGreaterThan(10);
        });
    }
}