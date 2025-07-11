package com.masrufi.framework.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command for creating Murabaha financing
 * 
 * This immutable command object encapsulates all the information required
 * to create a Murabaha financing arrangement following CQRS patterns
 * and Domain-Driven Design principles.
 * 
 * Validation Rules:
 * - Customer profile must be provided and valid
 * - Asset cost must be positive
 * - Profit margin must be between 0 and 50%
 * - Asset description must be provided
 * - Supplier must be specified
 * - Maturity date must be in the future
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Value
@Builder
public class CreateMurabahaCommand {
    
    CustomerProfile customerProfile;
    String assetDescription;
    Money assetCost;
    BigDecimal profitMargin;
    LocalDateTime maturityDate;
    String supplier;
    String jurisdiction;
    PaymentFrequency paymentFrequency;
    String purpose;
    
    // Additional Murabaha-specific fields
    AssetCategory assetCategory;
    String assetLocation;
    String purchaseOrderNumber;
    Boolean immediateDelivery;
    String deliveryAddress;
    LocalDateTime expectedDeliveryDate;

    /**
     * Validate the command before processing
     */
    public void validate() {
        validateCustomerProfile();
        validateAsset();
        validateFinancialTerms();
        validateDates();
        validateSupplier();
    }

    private void validateCustomerProfile() {
        Objects.requireNonNull(customerProfile, "Customer profile is required for Murabaha");
        
        if (customerProfile.getCustomerId() == null || customerProfile.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (customerProfile.getCustomerName() == null || customerProfile.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        if (customerProfile.getCustomerType() == null) {
            throw new IllegalArgumentException("Customer type is required");
        }
    }

    private void validateAsset() {
        Objects.requireNonNull(assetDescription, "Asset description is required for Murabaha");
        
        if (assetDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset description cannot be empty");
        }
        
        if (assetDescription.length() < 10) {
            throw new IllegalArgumentException("Asset description too short (minimum 10 characters)");
        }
        
        if (assetDescription.length() > 500) {
            throw new IllegalArgumentException("Asset description too long (maximum 500 characters)");
        }
        
        Objects.requireNonNull(assetCost, "Asset cost is required for Murabaha");
        
        if (assetCost.isNegative() || assetCost.isZero()) {
            throw new IllegalArgumentException("Asset cost must be positive");
        }
        
        // Validate minimum asset value (equivalent to $1,000)
        if (assetCost.isLessThan(Money.of("1000", "USD"))) {
            throw new IllegalArgumentException("Asset cost too low for Murabaha financing");
        }
    }

    private void validateFinancialTerms() {
        Objects.requireNonNull(profitMargin, "Profit margin is required for Murabaha");
        
        if (profitMargin.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Profit margin must be positive");
        }
        
        if (profitMargin.compareTo(new BigDecimal("0.50")) > 0) {
            throw new IllegalArgumentException("Profit margin cannot exceed 50%");
        }
        
        // Check for reasonable profit margin (warn if above 30%)
        if (profitMargin.compareTo(new BigDecimal("0.30")) > 0) {
            // This could be logged as a warning in real implementation
            System.out.println("Warning: High profit margin detected: " + 
                profitMargin.multiply(new BigDecimal("100")) + "%");
        }
    }

    private void validateDates() {
        Objects.requireNonNull(maturityDate, "Maturity date is required for Murabaha");
        
        LocalDateTime now = LocalDateTime.now();
        if (maturityDate.isBefore(now)) {
            throw new IllegalArgumentException("Maturity date must be in the future");
        }
        
        // Minimum term: 30 days
        if (maturityDate.isBefore(now.plusDays(30))) {
            throw new IllegalArgumentException("Murabaha term must be at least 30 days");
        }
        
        // Maximum term: 30 years
        if (maturityDate.isAfter(now.plusYears(30))) {
            throw new IllegalArgumentException("Murabaha term cannot exceed 30 years");
        }
        
        // Validate delivery date if specified
        if (expectedDeliveryDate != null) {
            if (expectedDeliveryDate.isBefore(now)) {
                throw new IllegalArgumentException("Expected delivery date must be in the future");
            }
            
            if (expectedDeliveryDate.isAfter(maturityDate)) {
                throw new IllegalArgumentException("Expected delivery date cannot be after maturity date");
            }
        }
    }

    private void validateSupplier() {
        Objects.requireNonNull(supplier, "Supplier is required for Murabaha");
        
        if (supplier.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier cannot be empty");
        }
        
        if (supplier.length() < 2) {
            throw new IllegalArgumentException("Supplier name too short");
        }
        
        if (supplier.length() > 100) {
            throw new IllegalArgumentException("Supplier name too long");
        }
    }

    /**
     * Calculate the total selling price (cost + profit)
     */
    public Money getSellingPrice() {
        if (assetCost == null || profitMargin == null) {
            throw new IllegalStateException("Cannot calculate selling price without asset cost and profit margin");
        }
        
        Money profit = assetCost.multiply(profitMargin);
        return assetCost.add(profit);
    }

    /**
     * Calculate the profit amount
     */
    public Money getProfitAmount() {
        if (assetCost == null || profitMargin == null) {
            throw new IllegalStateException("Cannot calculate profit amount without asset cost and profit margin");
        }
        
        return assetCost.multiply(profitMargin);
    }

    /**
     * Get the financing term in days
     */
    public long getTermInDays() {
        if (maturityDate == null) {
            throw new IllegalStateException("Cannot calculate term without maturity date");
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), maturityDate);
    }

    /**
     * Check if this is an immediate delivery Murabaha
     */
    public boolean isImmediateDelivery() {
        return Boolean.TRUE.equals(immediateDelivery);
    }

    /**
     * Check if delivery address is required
     */
    public boolean isDeliveryAddressRequired() {
        return !isImmediateDelivery() && expectedDeliveryDate != null;
    }

    /**
     * Asset categories for Murabaha financing
     */
    public enum AssetCategory {
        REAL_ESTATE,
        VEHICLES,
        MACHINERY,
        EQUIPMENT,
        COMMODITIES,
        CONSUMER_GOODS,
        TECHNOLOGY,
        MEDICAL_EQUIPMENT,
        CONSTRUCTION_MATERIALS,
        OTHER
    }

    /**
     * Payment frequency options
     */
    public enum PaymentFrequency {
        MONTHLY,
        QUARTERLY,
        SEMI_ANNUALLY,
        ANNUALLY,
        BALLOON,
        CUSTOM
    }
}