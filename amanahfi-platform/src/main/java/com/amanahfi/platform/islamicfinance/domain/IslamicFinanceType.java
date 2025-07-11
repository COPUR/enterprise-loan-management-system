package com.amanahfi.platform.islamicfinance.domain;

/**
 * Enumeration of Islamic Finance Types supported by AmanahFi Platform
 * 
 * These represent the six core Islamic finance models that are
 * Sharia-compliant and approved by Islamic banking authorities.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
public enum IslamicFinanceType {
    
    /**
     * Cost-plus financing where the financier purchases goods
     * and sells them to the customer at cost plus disclosed profit
     */
    MURABAHA("Murabaha", "Cost-Plus Financing"),
    
    /**
     * Partnership financing where profits and losses are shared
     * based on agreed proportions
     */
    MUSHARAKAH("Musharakah", "Partnership Financing"),
    
    /**
     * Lease financing where the financier owns the asset
     * and leases it to the customer
     */
    IJARAH("Ijarah", "Lease Financing"),
    
    /**
     * Forward sale financing for commodities to be delivered
     * at a future date
     */
    SALAM("Salam", "Forward Sale Financing"),
    
    /**
     * Manufacturing/construction financing for projects
     * built to customer specifications
     */
    ISTISNA("Istisna", "Manufacturing Financing"),
    
    /**
     * Interest-free benevolent loan with no profit expectation
     */
    QARD_HASSAN("Qard Hassan", "Benevolent Loan");

    private final String arabicName;
    private final String description;

    IslamicFinanceType(String arabicName, String description) {
        this.arabicName = arabicName;
        this.description = description;
    }

    public String getArabicName() {
        return arabicName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this finance type requires asset backing
     */
    public boolean requiresAssetBacking() {
        return this == MURABAHA || this == IJARAH || this == SALAM || this == ISTISNA;
    }

    /**
     * Checks if this finance type allows profit taking
     */
    public boolean allowsProfitTaking() {
        return this != QARD_HASSAN;
    }

    /**
     * Checks if this finance type requires partnership structure
     */
    public boolean requiresPartnership() {
        return this == MUSHARAKAH;
    }
}