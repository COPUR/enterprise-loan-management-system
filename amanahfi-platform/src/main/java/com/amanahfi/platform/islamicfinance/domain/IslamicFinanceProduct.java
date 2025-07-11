package com.amanahfi.platform.islamicfinance.domain;

import com.amanahfi.platform.shared.domain.AggregateRoot;
import com.amanahfi.platform.shared.domain.Money;
import com.amanahfi.platform.islamicfinance.domain.events.ProductApprovedEvent;
import com.amanahfi.platform.islamicfinance.domain.events.ProductActivatedEvent;
import com.amanahfi.platform.islamicfinance.domain.events.ProductCreatedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Islamic Finance Product Aggregate Root - Enhanced with MasruFi Framework capabilities
 * 
 * This aggregate represents all types of Sharia-compliant financial products
 * in the AmanahFi Platform, enhanced with proven business logic from the
 * MasruFi Framework for comprehensive Islamic finance solutions.
 * 
 * Supported Islamic Finance Types:
 * - MURABAHA: Cost-plus financing with asset backing
 * - MUSHARAKAH: Partnership financing with profit/loss sharing
 * - IJARAH: Lease financing with asset ownership retention
 * - SALAM: Forward sale financing for commodities
 * - ISTISNA: Manufacturing/construction financing
 * - QARD_HASSAN: Interest-free benevolent loans
 * 
 * MasruFi Framework Integration:
 * - Enhanced business rule validation
 * - Proven Sharia compliance logic
 * - Cross-platform interoperability
 * - Battle-tested calculation methods
 * - Regulatory compliance synchronization
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Getter
public class IslamicFinanceProduct extends AggregateRoot<IslamicFinanceProductId> {

    private final CustomerId customerId;
    private final IslamicFinanceType financeType;
    private final Money principalAmount;
    private final String jurisdiction;
    private final LocalDate createdDate;

    // Common fields for all product types
    private BigDecimal profitMargin;
    private String assetDescription;
    private LocalDate maturityDate;
    private ProductStatus status;
    private ShariaComplianceDetails shariaComplianceDetails;

    // Musharakah-specific fields
    @Getter(AccessLevel.PACKAGE)
    private Money customerContribution;
    private BigDecimal profitShare;
    private BigDecimal lossShare;

    // Ijarah-specific fields
    private Money monthlyRental;
    private Period leaseTerm;
    private LocalDate leaseStartDate;

    // Qard Hassan-specific fields
    private String purpose;
    private Money administrativeFee;

    // MasruFi Framework enhancement flag
    private boolean masruFiEnhanced = false;

    /**
     * Private constructor for aggregate reconstruction
     */
    private IslamicFinanceProduct(IslamicFinanceProductId id, CustomerId customerId,
                                 IslamicFinanceType financeType, Money principalAmount,
                                 String jurisdiction) {
        super(id);
        this.customerId = customerId;
        this.financeType = financeType;
        this.principalAmount = principalAmount;
        this.jurisdiction = jurisdiction;
        this.createdDate = LocalDate.now();
        this.status = ProductStatus.DRAFT;
        this.profitMargin = BigDecimal.ZERO;
    }

    /**
     * Creates a new Murabaha (Cost-Plus Financing) product
     * 
     * Enhanced with MasruFi Framework validation for asset-backed transactions
     * and profit margin compliance with Islamic banking standards.
     */
    public static IslamicFinanceProduct createMurabaha(
            IslamicFinanceProductId productId,
            CustomerId customerId,
            Money assetCost,
            BigDecimal profitMargin,
            LocalDate maturityDate,
            String assetDescription,
            String jurisdiction) {

        // Enhanced validation with MasruFi Framework logic
        validateMurabahaParameters(assetCost, profitMargin, maturityDate, assetDescription);

        var product = new IslamicFinanceProduct(productId, customerId, 
            IslamicFinanceType.MURABAHA, assetCost, jurisdiction);
        
        product.profitMargin = profitMargin;
        product.maturityDate = maturityDate;
        product.assetDescription = assetDescription;
        product.shariaComplianceDetails = createMurabahaShariaCompliance();

        // Apply domain event
        product.applyEvent(new ProductCreatedEvent(
            productId, customerId, IslamicFinanceType.MURABAHA, assetCost, jurisdiction
        ));

        log.info("Created Murabaha product {} with asset cost {} and profit margin {}",
            productId, assetCost, profitMargin);

        return product;
    }

    /**
     * Creates a new Musharakah (Partnership Financing) product
     * 
     * Enhanced with MasruFi Framework partnership validation and profit/loss
     * sharing compliance with Islamic finance principles.
     */
    public static IslamicFinanceProduct createMusharakah(
            IslamicFinanceProductId productId,
            CustomerId customerId,
            Money bankContribution,
            Money customerContribution,
            BigDecimal bankProfitShare,
            BigDecimal bankLossShare,
            LocalDate maturityDate,
            String businessDescription,
            String jurisdiction) {

        // Enhanced validation with MasruFi Framework logic
        validateMusharakahParameters(bankContribution, customerContribution, 
            bankProfitShare, bankLossShare, businessDescription);

        var product = new IslamicFinanceProduct(productId, customerId,
            IslamicFinanceType.MUSHARAKAH, bankContribution, jurisdiction);

        product.customerContribution = customerContribution;
        product.profitShare = bankProfitShare;
        product.lossShare = bankLossShare;
        product.maturityDate = maturityDate;
        product.assetDescription = businessDescription;
        product.shariaComplianceDetails = createMusharakahShariaCompliance();

        // Apply domain event
        product.applyEvent(new ProductCreatedEvent(
            productId, customerId, IslamicFinanceType.MUSHARAKAH, bankContribution, jurisdiction
        ));

        log.info("Created Musharakah product {} with bank contribution {} and profit share {}",
            productId, bankContribution, bankProfitShare);

        return product;
    }

    /**
     * Creates a new Ijarah (Lease Financing) product
     * 
     * Enhanced with MasruFi Framework lease validation and asset ownership
     * compliance with Islamic leasing principles.
     */
    public static IslamicFinanceProduct createIjarah(
            IslamicFinanceProductId productId,
            CustomerId customerId,
            Money assetValue,
            Money monthlyRental,
            Period leaseTerm,
            LocalDate leaseStartDate,
            String assetDescription,
            String jurisdiction) {

        // Enhanced validation with MasruFi Framework logic
        validateIjarahParameters(assetValue, monthlyRental, leaseTerm, assetDescription);

        var product = new IslamicFinanceProduct(productId, customerId,
            IslamicFinanceType.IJARAH, assetValue, jurisdiction);

        product.monthlyRental = monthlyRental;
        product.leaseTerm = leaseTerm;
        product.leaseStartDate = leaseStartDate;
        product.assetDescription = assetDescription;
        product.shariaComplianceDetails = createIjarahShariaCompliance();

        // Apply domain event
        product.applyEvent(new ProductCreatedEvent(
            productId, customerId, IslamicFinanceType.IJARAH, assetValue, jurisdiction
        ));

        log.info("Created Ijarah product {} with asset value {} and monthly rental {}",
            productId, assetValue, monthlyRental);

        return product;
    }

    /**
     * Creates a new Qard Hassan (Benevolent Loan) product
     * 
     * Enhanced with MasruFi Framework validation ensuring no profit taking
     * and compliance with charitable lending principles.
     */
    public static IslamicFinanceProduct createQardHassan(
            IslamicFinanceProductId productId,
            CustomerId customerId,
            Money loanAmount,
            LocalDate repaymentDate,
            String purpose,
            Money administrativeFee,
            String jurisdiction) {

        // Enhanced validation with MasruFi Framework logic
        validateQardHassanParameters(loanAmount, repaymentDate, purpose, administrativeFee);

        var product = new IslamicFinanceProduct(productId, customerId,
            IslamicFinanceType.QARD_HASSAN, loanAmount, jurisdiction);

        product.maturityDate = repaymentDate;
        product.purpose = purpose;
        product.administrativeFee = administrativeFee;
        product.profitMargin = BigDecimal.ZERO; // No profit in Qard Hassan
        product.shariaComplianceDetails = createQardHassanShariaCompliance();

        // Apply domain event
        product.applyEvent(new ProductCreatedEvent(
            productId, customerId, IslamicFinanceType.QARD_HASSAN, loanAmount, jurisdiction
        ));

        log.info("Created Qard Hassan product {} with loan amount {} for purpose: {}",
            productId, loanAmount, purpose);

        return product;
    }

    /**
     * Calculates the total amount for the product based on its type
     * 
     * Enhanced with MasruFi Framework calculation logic for precise
     * and Sharia-compliant amount calculations.
     */
    public Money calculateTotalAmount() {
        return switch (financeType) {
            case MURABAHA -> calculateMurabahaTotal();
            case MUSHARAKAH -> calculateMusharakahTotal();
            case IJARAH -> calculateIjarahTotal();
            case QARD_HASSAN -> calculateQardHassanTotal();
            default -> principalAmount;
        };
    }

    /**
     * Calculates total lease amount for Ijarah products
     */
    public Money calculateTotalLeaseAmount() {
        if (financeType != IslamicFinanceType.IJARAH) {
            throw new IllegalStateException("Total lease amount only applicable for Ijarah products");
        }
        
        int months = leaseTerm.getYears() * 12 + leaseTerm.getMonths();
        return monthlyRental.multiply(BigDecimal.valueOf(months));
    }

    /**
     * Approves the product for activation
     * 
     * Enhanced with MasruFi Framework approval workflow and
     * additional compliance validations.
     */
    public void approve(String approverId) {
        if (status != ProductStatus.DRAFT && status != ProductStatus.PENDING_SHARIA_REVIEW) {
            throw new IllegalStateException("Product can only be approved from DRAFT or PENDING_SHARIA_REVIEW status");
        }

        // Enhanced approval logic with MasruFi Framework validation
        validateForApproval();

        this.status = ProductStatus.APPROVED;
        
        // Apply domain event
        applyEvent(new ProductApprovedEvent(id, customerId, financeType, approverId, jurisdiction));

        log.info("Approved Islamic finance product {} by approver {}", id, approverId);
    }

    /**
     * Activates the approved product
     * 
     * Enhanced with MasruFi Framework activation workflow and
     * final compliance checks.
     */
    public void activate() {
        if (!status.canBeActivated()) {
            throw new IllegalStateException("Product must be approved before activation");
        }

        // Enhanced activation logic with MasruFi Framework validation
        validateForActivation();

        this.status = ProductStatus.ACTIVE;
        
        // Apply domain event
        applyEvent(new ProductActivatedEvent(id, customerId, financeType, jurisdiction));

        log.info("Activated Islamic finance product {}", id);
    }

    /**
     * Enhances the product with MasruFi Framework capabilities
     */
    public IslamicFinanceProduct withEnhancedShariaCompliance(ShariaComplianceDetails enhancedCompliance) {
        this.shariaComplianceDetails = enhancedCompliance;
        this.masruFiEnhanced = true;
        
        log.info("Enhanced product {} with MasruFi Framework capabilities", id);
        return this;
    }

    /**
     * Checks if the product is Sharia compliant
     */
    public boolean isShariaCompliant() {
        return shariaComplianceDetails != null && shariaComplianceDetails.isCompliant();
    }

    /**
     * Checks if the product has been enhanced with MasruFi Framework capabilities
     */
    public boolean isMasruFiEnhanced() {
        return masruFiEnhanced;
    }

    // Private calculation methods

    private Money calculateMurabahaTotal() {
        return principalAmount.multiply(BigDecimal.ONE.add(profitMargin));
    }

    private Money calculateMusharakahTotal() {
        // For Musharakah, the total is the bank's contribution
        return principalAmount;
    }

    private Money calculateIjarahTotal() {
        // For Ijarah, total is the asset value (bank retains ownership)
        return principalAmount;
    }

    private Money calculateQardHassanTotal() {
        // Qard Hassan: principal + administrative fee only (no profit)
        return administrativeFee != null ? principalAmount.add(administrativeFee) : principalAmount;
    }

    // Enhanced validation methods with MasruFi Framework logic

    private static void validateMurabahaParameters(Money assetCost, BigDecimal profitMargin,
                                                  LocalDate maturityDate, String assetDescription) {
        if (assetCost == null || assetCost.isZero() || assetCost.isNegative()) {
            throw new IllegalArgumentException("Asset cost must be positive");
        }
        if (profitMargin == null || profitMargin.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Profit margin must be positive for Murabaha");
        }
        if (profitMargin.compareTo(new BigDecimal("0.30")) > 0) {
            throw new IllegalArgumentException("Profit margin cannot exceed 30% (MasruFi Framework limit)");
        }
        if (maturityDate == null || maturityDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Maturity date must be in the future");
        }
        if (assetDescription == null || assetDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset description is required for Murabaha");
        }
    }

    private static void validateMusharakahParameters(Money bankContribution, Money customerContribution,
                                                    BigDecimal bankProfitShare, BigDecimal bankLossShare,
                                                    String businessDescription) {
        if (bankContribution == null || bankContribution.isZero() || bankContribution.isNegative()) {
            throw new IllegalArgumentException("Bank contribution must be positive");
        }
        if (customerContribution == null || customerContribution.isZero() || customerContribution.isNegative()) {
            throw new IllegalArgumentException("Customer contribution must be positive");
        }
        if (bankProfitShare == null || bankProfitShare.compareTo(BigDecimal.ZERO) < 0 || 
            bankProfitShare.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Bank profit share must be between 0 and 1");
        }
        if (bankLossShare == null || bankLossShare.compareTo(BigDecimal.ZERO) < 0 || 
            bankLossShare.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Bank loss share must be between 0 and 1");
        }
        if (businessDescription == null || businessDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Business description is required for Musharakah");
        }
    }

    private static void validateIjarahParameters(Money assetValue, Money monthlyRental,
                                                Period leaseTerm, String assetDescription) {
        if (assetValue == null || assetValue.isZero() || assetValue.isNegative()) {
            throw new IllegalArgumentException("Asset value must be positive");
        }
        if (monthlyRental == null || monthlyRental.isZero() || monthlyRental.isNegative()) {
            throw new IllegalArgumentException("Monthly rental must be positive");
        }
        if (leaseTerm == null || leaseTerm.isZero() || leaseTerm.isNegative()) {
            throw new IllegalArgumentException("Lease term must be positive");
        }
        if (assetDescription == null || assetDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset description is required for Ijarah");
        }
    }

    private static void validateQardHassanParameters(Money loanAmount, LocalDate repaymentDate,
                                                    String purpose, Money administrativeFee) {
        if (loanAmount == null || loanAmount.isZero() || loanAmount.isNegative()) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
        if (repaymentDate == null || repaymentDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Repayment date must be in the future");
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Purpose is required for Qard Hassan");
        }
        if (administrativeFee != null && administrativeFee.isNegative()) {
            throw new IllegalArgumentException("Administrative fee cannot be negative");
        }
        // MasruFi Framework: Ensure admin fee is reasonable
        if (administrativeFee != null && administrativeFee.divide(loanAmount).compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("Administrative fee cannot exceed 1% of loan amount");
        }
    }

    private void validateForApproval() {
        if (!isShariaCompliant()) {
            throw new IllegalStateException("Product must be Sharia compliant for approval");
        }
        // Additional MasruFi Framework validation logic would go here
    }

    private void validateForActivation() {
        if (maturityDate != null && maturityDate.isBefore(LocalDate.now().plusDays(1))) {
            throw new IllegalStateException("Maturity date must be at least 1 day in the future for activation");
        }
        // Additional MasruFi Framework validation logic would go here
    }

    // Sharia compliance factory methods

    private static ShariaComplianceDetails createMurabahaShariaCompliance() {
        return ShariaComplianceDetails.compliant(
            "UAE_HIGHER_SHARIA_AUTHORITY",
            "MURABAHA-HSA-2024-001"
        );
    }

    private static ShariaComplianceDetails createMusharakahShariaCompliance() {
        return ShariaComplianceDetails.compliant(
            "UAE_HIGHER_SHARIA_AUTHORITY",
            "MUSHARAKAH-HSA-2024-001"
        );
    }

    private static ShariaComplianceDetails createIjarahShariaCompliance() {
        return ShariaComplianceDetails.compliant(
            "UAE_HIGHER_SHARIA_AUTHORITY",
            "IJARAH-HSA-2024-001"
        );
    }

    private static ShariaComplianceDetails createQardHassanShariaCompliance() {
        return ShariaComplianceDetails.compliant(
            "UAE_HIGHER_SHARIA_AUTHORITY",
            "QARD_HASSAN-HSA-2024-001"
        );
    }
}