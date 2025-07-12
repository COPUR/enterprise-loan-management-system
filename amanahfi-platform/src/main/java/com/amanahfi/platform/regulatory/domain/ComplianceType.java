package com.amanahfi.platform.regulatory.domain;

/**
 * Types of regulatory compliance
 */
public enum ComplianceType {
    // Financial compliance types
    OPEN_FINANCE_API,      // CBUAE Open Finance compliance
    ANTI_MONEY_LAUNDERING, // AML/CFT compliance
    KNOW_YOUR_CUSTOMER,    // KYC compliance
    DATA_PROTECTION,       // Data privacy and protection
    CONSUMER_PROTECTION,   // Consumer rights and protection
    
    // Cryptocurrency and digital asset compliance
    VIRTUAL_ASSET_REGULATION,  // VARA compliance
    CBDC_COMPLIANCE,          // Central Bank Digital Currency
    DIGITAL_ASSET_CUSTODY,    // Custody requirements
    
    // Islamic finance compliance
    SHARIA_GOVERNANCE,        // HSA Sharia compliance
    ISLAMIC_BANKING,          // Islamic banking regulations
    SUKUK_ISSUANCE,          // Islamic bond compliance
    ZAKAT_COMPLIANCE,        // Religious tax compliance
    
    // Operational compliance
    OPERATIONAL_RESILIENCE,   // Business continuity
    CYBER_SECURITY,          // Information security
    RISK_MANAGEMENT,         // Risk management framework
    CAPITAL_ADEQUACY,        // Capital requirements
    
    // Reporting compliance
    REGULATORY_REPORTING,    // Periodic reporting requirements
    TRANSACTION_REPORTING,   // Transaction-level reporting
    SUSPICIOUS_ACTIVITY,     // SAR reporting
    
    // Cross-border compliance
    CROSS_BORDER_PAYMENTS,   // International transfers
    SANCTIONS_SCREENING,     // Sanctions compliance
    TAX_REPORTING           // Tax compliance (FATCA, CRS)
}