package com.amanahfi.platform.shared.integration;

import com.amanahfi.platform.islamicfinance.domain.*;
import com.amanahfi.platform.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Currency;

/**
 * Adapter for integrating AmanahFi Platform with MasruFi Framework capabilities
 * 
 * This adapter provides seamless integration between the enterprise-grade
 * AmanahFi Platform and the proven MasruFi Framework, combining the best
 * of both platforms for comprehensive Islamic finance solutions.
 * 
 * Key Capabilities:
 * - Bidirectional model transformation
 * - Sharia compliance validation synchronization  
 * - Multi-framework interoperability
 * - Enhanced business rule enforcement
 * - Cross-platform event coordination
 * 
 * Integration Benefits:
 * - Leverage MasruFi's proven Islamic finance models
 * - Enhance AmanahFi with battle-tested business logic
 * - Maintain regulatory compliance across both platforms
 * - Enable gradual migration strategies
 * - Provide unified API for clients
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
public class MasruFiFrameworkAdapter {

    /**
     * Converts AmanahFi IslamicFinanceProduct to MasruFi IslamicFinancing
     * 
     * This method enables leveraging MasruFi's business logic and validation
     * capabilities within the AmanahFi platform ecosystem.
     * 
     * @param product AmanahFi Islamic finance product
     * @return MasruFi IslamicFinancing equivalent
     */
    public MasruFiIslamicFinancing toMasruFiModel(IslamicFinanceProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("IslamicFinanceProduct cannot be null");
        }

        log.debug("Converting AmanahFi product {} to MasruFi model", product.getId());

        return MasruFiIslamicFinancing.builder()
                .financingId(new MasruFiFinancingId(product.getId().getValue()))
                .islamicFinancingType(convertFinanceType(product.getFinanceType()))
                .customerProfile(convertCustomerProfile(product))
                .principalAmount(convertMoney(product.getPrincipalAmount()))
                .totalAmount(convertMoney(product.calculateTotalAmount()))
                .profitMargin(product.getProfitMargin())
                .assetDescription(product.getAssetDescription())
                .maturityDate(convertToLocalDateTime(product.getMaturityDate()))
                .createdDate(LocalDateTime.now())
                .status(product.getStatus().name())
                .jurisdiction(product.getJurisdiction())
                .shariaCompliance(convertShariaCompliance(product.getShariaComplianceDetails()))
                .build();
    }

    /**
     * Converts MasruFi IslamicFinancing to AmanahFi IslamicFinanceProduct
     * 
     * This enables importing proven MasruFi models into the AmanahFi
     * platform for enhanced processing and management.
     * 
     * @param financing MasruFi Islamic financing model
     * @return AmanahFi IslamicFinanceProduct equivalent
     */
    public IslamicFinanceProduct fromMasruFiModel(MasruFiIslamicFinancing financing) {
        if (financing == null) {
            throw new IllegalArgumentException("IslamicFinancing cannot be null");
        }

        log.debug("Converting MasruFi financing {} to AmanahFi model", financing.getFinancingId());

        // Create the appropriate AmanahFi product based on type
        switch (financing.getIslamicFinancingType()) {
            case MURABAHA:
                return createMurabahaFromMasruFi(financing);
            case MUSHARAKAH:
                return createMusharakahFromMasruFi(financing);
            case IJARAH:
                return createIjarahFromMasruFi(financing);
            case QARD_HASSAN:
                return createQardHassanFromMasruFi(financing);
            default:
                throw new UnsupportedOperationException(
                    "Finance type not supported: " + financing.getIslamicFinancingType()
                );
        }
    }

    /**
     * Enhances AmanahFi product with MasruFi business rules
     * 
     * This method applies MasruFi's proven business rules and validations
     * to enhance the robustness of AmanahFi products.
     * 
     * @param product AmanahFi product to enhance
     * @return Enhanced product with MasruFi capabilities
     */
    public IslamicFinanceProduct enhanceWithMasruFiCapabilities(IslamicFinanceProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        log.info("Enhancing AmanahFi product {} with MasruFi capabilities", product.getId());

        // Convert to MasruFi model for business rule application
        MasruFiIslamicFinancing masruFiModel = toMasruFiModel(product);

        // Apply MasruFi business rules and validations
        validateMasruFiBusinessRules(masruFiModel);

        // Apply enhanced Sharia compliance checks
        ShariaComplianceDetails enhancedCompliance = enhanceShariaCompliance(
            product.getShariaComplianceDetails(), masruFiModel
        );

        // Return enhanced product
        return product.withEnhancedShariaCompliance(enhancedCompliance);
    }

    /**
     * Validates business rules using MasruFi framework logic
     */
    public void validateMasruFiBusinessRules(MasruFiIslamicFinancing financing) {
        // Apply MasruFi's proven validation logic
        if (!financing.isShariaCompliant()) {
            throw new ShariaComplianceViolationException(
                "Product does not meet MasruFi Sharia compliance standards"
            );
        }

        // Validate profit rates against MasruFi benchmarks
        if (financing.getEffectiveProfitRate().compareTo(new BigDecimal("0.30")) > 0) {
            throw new ExcessiveProfitMarginException(
                "Profit rate exceeds MasruFi framework limits"
            );
        }

        // Additional MasruFi business rule validations
        validateAssetBacking(financing);
        validateMaturityPeriod(financing);
    }

    /**
     * Enhances Sharia compliance with MasruFi framework insights
     */
    private ShariaComplianceDetails enhanceShariaCompliance(
            ShariaComplianceDetails original, 
            MasruFiIslamicFinancing masruFiModel) {
        
        return original.toBuilder()
                .applicablePrinciples(original.getApplicablePrinciples().stream()
                    .collect(java.util.stream.Collectors.toMutableList()))
                .complianceNotes(original.getComplianceNotes() + 
                    " | Enhanced with MasruFi Framework validation")
                .build();
    }

    // Helper methods for conversion

    private MasruFiIslamicFinancingType convertFinanceType(IslamicFinanceType type) {
        return switch (type) {
            case MURABAHA -> MasruFiIslamicFinancingType.MURABAHA;
            case MUSHARAKAH -> MasruFiIslamicFinancingType.MUSHARAKAH;
            case IJARAH -> MasruFiIslamicFinancingType.IJARAH;
            case SALAM -> MasruFiIslamicFinancingType.SALAM;
            case ISTISNA -> MasruFiIslamicFinancingType.ISTISNA;
            case QARD_HASSAN -> MasruFiIslamicFinancingType.QARD_HASSAN;
        };
    }

    private IslamicFinanceType convertFromMasruFiType(MasruFiIslamicFinancingType type) {
        return switch (type) {
            case MURABAHA -> IslamicFinanceType.MURABAHA;
            case MUSHARAKAH -> IslamicFinanceType.MUSHARAKAH;
            case IJARAH -> IslamicFinanceType.IJARAH;
            case SALAM -> IslamicFinanceType.SALAM;
            case ISTISNA -> IslamicFinanceType.ISTISNA;
            case QARD_HASSAN -> IslamicFinanceType.QARD_HASSAN;
        };
    }

    private MasruFiCustomerProfile convertCustomerProfile(IslamicFinanceProduct product) {
        return MasruFiCustomerProfile.builder()
                .customerId(product.getCustomerId().getValue().toString())
                .customerName("Enhanced Customer") // Would come from customer service
                .customerType(MasruFiCustomerType.INDIVIDUAL)
                .jurisdiction(product.getJurisdiction())
                .build();
    }

    private MasruFiMoney convertMoney(Money money) {
        return new MasruFiMoney(money.getAmount(), money.getCurrency());
    }

    private LocalDateTime convertToLocalDateTime(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private MasruFiShariaComplianceValidation convertShariaCompliance(ShariaComplianceDetails details) {
        if (details == null) {
            return null;
        }

        return MasruFiShariaComplianceValidation.builder()
                .isCompliant(details.isCompliant())
                .validationDate(details.getValidationDate().toString())
                .validatingAuthority(details.getValidatingAuthority())
                .complianceNotes(details.getComplianceNotes())
                .ribaFree(details.isRibaFree())
                .ghararFree(details.isGhararFree())
                .assetBacked(details.isAssetBacked())
                .permissibleAsset(details.isPermissibleAsset())
                .build();
    }

    // Factory methods for creating AmanahFi products from MasruFi models

    private IslamicFinanceProduct createMurabahaFromMasruFi(MasruFiIslamicFinancing financing) {
        return IslamicFinanceProduct.createMurabaha(
                new IslamicFinanceProductId(financing.getFinancingId().getValue()),
                new CustomerId(java.util.UUID.fromString(financing.getCustomerProfile().getCustomerId())),
                convertFromMasruFiMoney(financing.getPrincipalAmount()),
                financing.getProfitMargin(),
                financing.getMaturityDate().toLocalDate(),
                financing.getAssetDescription(),
                financing.getJurisdiction()
        );
    }

    private IslamicFinanceProduct createMusharakahFromMasruFi(MasruFiIslamicFinancing financing) {
        // Default partnership terms for conversion
        Money customerContribution = convertFromMasruFiMoney(financing.getPrincipalAmount()).multiply(0.3);
        BigDecimal profitShare = new BigDecimal("0.70");
        BigDecimal lossShare = new BigDecimal("0.70");

        return IslamicFinanceProduct.createMusharakah(
                new IslamicFinanceProductId(financing.getFinancingId().getValue()),
                new CustomerId(java.util.UUID.fromString(financing.getCustomerProfile().getCustomerId())),
                convertFromMasruFiMoney(financing.getPrincipalAmount()),
                customerContribution,
                profitShare,
                lossShare,
                financing.getMaturityDate().toLocalDate(),
                financing.getAssetDescription(),
                financing.getJurisdiction()
        );
    }

    private IslamicFinanceProduct createIjarahFromMasruFi(MasruFiIslamicFinancing financing) {
        Money monthlyRental = convertFromMasruFiMoney(financing.getPrincipalAmount()).divide(new BigDecimal("36"));
        Period leaseTerm = Period.ofYears(3);

        return IslamicFinanceProduct.createIjarah(
                new IslamicFinanceProductId(financing.getFinancingId().getValue()),
                new CustomerId(java.util.UUID.fromString(financing.getCustomerProfile().getCustomerId())),
                convertFromMasruFiMoney(financing.getPrincipalAmount()),
                monthlyRental,
                leaseTerm,
                LocalDate.now().plusDays(30),
                financing.getAssetDescription(),
                financing.getJurisdiction()
        );
    }

    private IslamicFinanceProduct createQardHassanFromMasruFi(MasruFiIslamicFinancing financing) {
        Money adminFee = Money.aed(new BigDecimal("100.00")); // Default admin fee

        return IslamicFinanceProduct.createQardHassan(
                new IslamicFinanceProductId(financing.getFinancingId().getValue()),
                new CustomerId(java.util.UUID.fromString(financing.getCustomerProfile().getCustomerId())),
                convertFromMasruFiMoney(financing.getPrincipalAmount()),
                financing.getMaturityDate().toLocalDate(),
                financing.getAssetDescription(),
                adminFee,
                financing.getJurisdiction()
        );
    }

    private Money convertFromMasruFiMoney(MasruFiMoney masruFiMoney) {
        return new Money(masruFiMoney.getAmount(), masruFiMoney.getCurrency());
    }

    public void validateAssetBacking(MasruFiIslamicFinancing financing) {
        if (financing.getIslamicFinancingType().requiresAssetBacking() && 
            (financing.getAssetDescription() == null || financing.getAssetDescription().trim().isEmpty())) {
            throw new AssetValidationException("Asset description required for " + financing.getIslamicFinancingType());
        }
    }

    private void validateMaturityPeriod(MasruFiIslamicFinancing financing) {
        if (financing.getMaturityDate() != null && financing.getMaturityDate().isBefore(LocalDateTime.now())) {
            throw new InvalidMaturityDateException("Maturity date cannot be in the past");
        }
    }

    // Helper classes for MasruFi integration (simplified representations)
    
    public static class MasruFiIslamicFinancing {
        private MasruFiFinancingId financingId;
        private MasruFiIslamicFinancingType islamicFinancingType;
        private MasruFiCustomerProfile customerProfile;
        private MasruFiMoney principalAmount;
        private MasruFiMoney totalAmount;
        private BigDecimal profitMargin;
        private String assetDescription;
        private LocalDateTime maturityDate;
        private LocalDateTime createdDate;
        private String status;
        private String jurisdiction;
        private MasruFiShariaComplianceValidation shariaCompliance;

        // Builder pattern and getters
        public static MasruFiIslamicFinancingBuilder builder() {
            return new MasruFiIslamicFinancingBuilder();
        }

        // Getters
        public MasruFiFinancingId getFinancingId() { return financingId; }
        public MasruFiIslamicFinancingType getIslamicFinancingType() { return islamicFinancingType; }
        public MasruFiCustomerProfile getCustomerProfile() { return customerProfile; }
        public MasruFiMoney getPrincipalAmount() { return principalAmount; }
        public MasruFiMoney getTotalAmount() { return totalAmount; }
        public BigDecimal getProfitMargin() { return profitMargin; }
        public String getAssetDescription() { return assetDescription; }
        public LocalDateTime getMaturityDate() { return maturityDate; }
        public String getJurisdiction() { return jurisdiction; }

        public boolean isShariaCompliant() {
            return shariaCompliance != null && shariaCompliance.isCompliant();
        }

        public MasruFiShariaComplianceValidation getShariaDaCompliance() {
            return shariaCompliance;
        }

        public BigDecimal getEffectiveProfitRate() {
            if (principalAmount == null || totalAmount == null) {
                return BigDecimal.ZERO;
            }
            MasruFiMoney profit = totalAmount.subtract(principalAmount);
            return profit.divide(principalAmount);
        }

        public static class MasruFiIslamicFinancingBuilder {
            private MasruFiIslamicFinancing instance = new MasruFiIslamicFinancing();

            public MasruFiIslamicFinancingBuilder financingId(MasruFiFinancingId financingId) {
                instance.financingId = financingId;
                return this;
            }

            public MasruFiIslamicFinancingBuilder islamicFinancingType(MasruFiIslamicFinancingType type) {
                instance.islamicFinancingType = type;
                return this;
            }

            public MasruFiIslamicFinancingBuilder customerProfile(MasruFiCustomerProfile profile) {
                instance.customerProfile = profile;
                return this;
            }

            public MasruFiIslamicFinancingBuilder principalAmount(MasruFiMoney amount) {
                instance.principalAmount = amount;
                return this;
            }

            public MasruFiIslamicFinancingBuilder totalAmount(MasruFiMoney amount) {
                instance.totalAmount = amount;
                return this;
            }

            public MasruFiIslamicFinancingBuilder profitMargin(BigDecimal margin) {
                instance.profitMargin = margin;
                return this;
            }

            public MasruFiIslamicFinancingBuilder assetDescription(String description) {
                instance.assetDescription = description;
                return this;
            }

            public MasruFiIslamicFinancingBuilder maturityDate(LocalDateTime date) {
                instance.maturityDate = date;
                return this;
            }

            public MasruFiIslamicFinancingBuilder createdDate(LocalDateTime date) {
                instance.createdDate = date;
                return this;
            }

            public MasruFiIslamicFinancingBuilder status(String status) {
                instance.status = status;
                return this;
            }

            public MasruFiIslamicFinancingBuilder jurisdiction(String jurisdiction) {
                instance.jurisdiction = jurisdiction;
                return this;
            }

            public MasruFiIslamicFinancingBuilder shariaCompliance(MasruFiShariaComplianceValidation compliance) {
                instance.shariaCompliance = compliance;
                return this;
            }

            public MasruFiIslamicFinancing build() {
                return instance;
            }
        }
    }

    // Supporting classes (simplified for integration)
    public static class MasruFiFinancingId {
        private final java.util.UUID value;
        public MasruFiFinancingId(java.util.UUID value) { this.value = value; }
        public java.util.UUID getValue() { return value; }
    }

    public enum MasruFiIslamicFinancingType {
        MURABAHA, MUSHARAKAH, IJARAH, SALAM, ISTISNA, QARD_HASSAN;
        
        public boolean requiresAssetBacking() {
            return this == MURABAHA || this == IJARAH || this == SALAM || this == ISTISNA;
        }
    }

    public static class MasruFiCustomerProfile {
        private String customerId;
        private String customerName;
        private MasruFiCustomerType customerType;
        private String jurisdiction;

        public static MasruFiCustomerProfileBuilder builder() {
            return new MasruFiCustomerProfileBuilder();
        }

        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public MasruFiCustomerType getCustomerType() { return customerType; }
        public String getJurisdiction() { return jurisdiction; }

        public static class MasruFiCustomerProfileBuilder {
            private MasruFiCustomerProfile instance = new MasruFiCustomerProfile();

            public MasruFiCustomerProfileBuilder customerId(String customerId) {
                instance.customerId = customerId;
                return this;
            }

            public MasruFiCustomerProfileBuilder customerName(String customerName) {
                instance.customerName = customerName;
                return this;
            }

            public MasruFiCustomerProfileBuilder customerType(MasruFiCustomerType customerType) {
                instance.customerType = customerType;
                return this;
            }

            public MasruFiCustomerProfileBuilder jurisdiction(String jurisdiction) {
                instance.jurisdiction = jurisdiction;
                return this;
            }

            public MasruFiCustomerProfile build() {
                return instance;
            }
        }
    }

    public enum MasruFiCustomerType {
        INDIVIDUAL, CORPORATE, GOVERNMENT, NON_PROFIT
    }

    public static class MasruFiMoney {
        private final BigDecimal amount;
        private final Currency currency;

        public MasruFiMoney(BigDecimal amount, Currency currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() { return amount; }
        public Currency getCurrency() { return currency; }

        public MasruFiMoney subtract(MasruFiMoney other) {
            return new MasruFiMoney(this.amount.subtract(other.amount), this.currency);
        }

        public BigDecimal divide(MasruFiMoney other) {
            return this.amount.divide(other.amount, 10, java.math.RoundingMode.HALF_EVEN);
        }
    }

    public static class MasruFiShariaComplianceValidation {
        private boolean isCompliant;
        private String validationDate;
        private String validatingAuthority;
        private String complianceNotes;
        private boolean ribaFree;
        private boolean ghararFree;
        private boolean assetBacked;
        private boolean permissibleAsset;

        public static MasruFiShariaComplianceValidationBuilder builder() {
            return new MasruFiShariaComplianceValidationBuilder();
        }

        public boolean isCompliant() { return isCompliant; }
        public String getValidationDate() { return validationDate; }
        public String getValidatingAuthority() { return validatingAuthority; }
        public String getComplianceNotes() { return complianceNotes; }
        public boolean isRibaFree() { return ribaFree; }
        public boolean isGhararFree() { return ghararFree; }
        public boolean isAssetBacked() { return assetBacked; }
        public boolean isPermissibleAsset() { return permissibleAsset; }

        public static class MasruFiShariaComplianceValidationBuilder {
            private MasruFiShariaComplianceValidation instance = new MasruFiShariaComplianceValidation();

            public MasruFiShariaComplianceValidationBuilder isCompliant(boolean compliant) {
                instance.isCompliant = compliant;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder validationDate(String date) {
                instance.validationDate = date;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder validatingAuthority(String authority) {
                instance.validatingAuthority = authority;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder complianceNotes(String notes) {
                instance.complianceNotes = notes;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder ribaFree(boolean ribaFree) {
                instance.ribaFree = ribaFree;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder ghararFree(boolean ghararFree) {
                instance.ghararFree = ghararFree;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder assetBacked(boolean assetBacked) {
                instance.assetBacked = assetBacked;
                return this;
            }

            public MasruFiShariaComplianceValidationBuilder permissibleAsset(boolean permissible) {
                instance.permissibleAsset = permissible;
                return this;
            }

            public MasruFiShariaComplianceValidation build() {
                return instance;
            }
        }
    }

    // Exception classes
    public static class ShariaComplianceViolationException extends RuntimeException {
        public ShariaComplianceViolationException(String message) {
            super(message);
        }
    }

    public static class ExcessiveProfitMarginException extends RuntimeException {
        public ExcessiveProfitMarginException(String message) {
            super(message);
        }
    }

    public static class AssetValidationException extends RuntimeException {
        public AssetValidationException(String message) {
            super(message);
        }
    }

    public static class InvalidMaturityDateException extends RuntimeException {
        public InvalidMaturityDateException(String message) {
            super(message);
        }
    }
}