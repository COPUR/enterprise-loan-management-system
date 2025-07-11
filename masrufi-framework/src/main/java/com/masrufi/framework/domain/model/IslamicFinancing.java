package com.masrufi.framework.domain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core domain model for Islamic Financing arrangements
 * 
 * This is the central aggregate root for all Islamic finance operations
 * in the MasruFi Framework. It provides a unified interface for different
 * types of Islamic finance products while maintaining Sharia compliance.
 * 
 * Supported Islamic Finance Types:
 * - MURABAHA: Cost-plus financing
 * - MUSHARAKAH: Partnership financing
 * - IJARAH: Lease financing
 * - SALAM: Forward sale financing
 * - ISTISNA: Manufacturing financing
 * - QARD_HASSAN: Benevolent loan
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class IslamicFinancing {
    
    /**
     * Unique identifier for the Islamic financing arrangement
     */
    private IslamicFinancingId financingId;
    
    /**
     * Type of Islamic financing
     */
    private IslamicFinancingType islamicFinancingType;
    
    /**
     * Customer profile information
     */
    private CustomerProfile customerProfile;
    
    /**
     * Principal amount of financing
     */
    private Money principalAmount;
    
    /**
     * Total amount including profit/fees
     */
    private Money totalAmount;
    
    /**
     * Profit margin as decimal (e.g., 0.15 for 15%)
     */
    private BigDecimal profitMargin;
    
    /**
     * Description of underlying asset (for asset-based financing)
     */
    private String assetDescription;
    
    /**
     * Asset purchase identifier (for tracking asset ownership)
     */
    private String assetPurchaseId;
    
    /**
     * Maturity date of the financing
     */
    private LocalDateTime maturityDate;
    
    /**
     * Creation timestamp
     */
    private LocalDateTime createdDate;
    
    /**
     * Current status of the financing
     */
    private String status;
    
    /**
     * Jurisdiction for regulatory compliance
     */
    private String jurisdiction;
    
    /**
     * Sharia compliance validation result
     */
    private ShariaComplianceValidation shariaCompliance;

    /**
     * Types of Islamic Financing supported by MasruFi Framework
     */
    public enum IslamicFinancingType {
        /**
         * Cost-plus financing where the financier purchases goods 
         * and sells them to the customer at cost plus disclosed profit
         */
        MURABAHA,
        
        /**
         * Partnership financing where profits and losses are shared
         * based on agreed proportions
         */
        MUSHARAKAH,
        
        /**
         * Lease financing where the financier owns the asset
         * and leases it to the customer
         */
        IJARAH,
        
        /**
         * Forward sale financing for commodities to be delivered
         * at a future date
         */
        SALAM,
        
        /**
         * Manufacturing/construction financing for projects
         * built to customer specifications
         */
        ISTISNA,
        
        /**
         * Interest-free benevolent loan with no profit expectation
         */
        QARD_HASSAN
    }

    /**
     * Sharia compliance validation details
     */
    @Data
    @Builder
    public static class ShariaComplianceValidation {
        private boolean isCompliant;
        private String validationDate;
        private String validatingAuthority;
        private String complianceNotes;
        
        // Specific compliance checks
        private boolean ribaFree;        // Free from interest
        private boolean ghararFree;      // Free from uncertainty
        private boolean assetBacked;     // Backed by real assets
        private boolean permissibleAsset; // Asset is Sharia permissible
    }

    /**
     * Check if this financing is Sharia compliant
     */
    public boolean isShariaCompliant() {
        return shariaCompliance != null && shariaCompliance.isCompliant();
    }

    /**
     * Get the effective profit rate for the financing
     */
    public BigDecimal getEffectiveProfitRate() {
        if (principalAmount == null || totalAmount == null) {
            return BigDecimal.ZERO;
        }
        
        Money profit = totalAmount.subtract(principalAmount);
        return profit.divide(principalAmount);
    }

    /**
     * Check if the financing is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) || "DISBURSED".equals(status);
    }

    /**
     * Check if the financing has matured
     */
    public boolean isMatured() {
        return maturityDate != null && LocalDateTime.now().isAfter(maturityDate);
    }
}