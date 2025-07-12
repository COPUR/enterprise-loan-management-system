package com.amanahfi.platform.tenant.domain;

/**
 * Tenant type enumeration for different market segments
 */
public enum TenantType {
    
    /**
     * Central Bank Digital Currency operations
     */
    CBDC_OPERATOR,
    
    /**
     * Islamic financial institution
     */
    ISLAMIC_BANK,
    
    /**
     * Conventional bank with Islamic finance services
     */
    CONVENTIONAL_BANK,
    
    /**
     * Fintech company providing Islamic finance
     */
    FINTECH,
    
    /**
     * Regulatory authority
     */
    REGULATOR,
    
    /**
     * Government entity
     */
    GOVERNMENT,
    
    /**
     * Third-party service provider
     */
    SERVICE_PROVIDER,
    
    /**
     * Partner organization
     */
    PARTNER;
    
    /**
     * Check if tenant type is a financial institution
     */
    public boolean isFinancialInstitution() {
        return this == ISLAMIC_BANK || this == CONVENTIONAL_BANK || this == FINTECH;
    }
    
    /**
     * Check if tenant type is a regulatory entity
     */
    public boolean isRegulatoryEntity() {
        return this == REGULATOR || this == GOVERNMENT;
    }
    
    /**
     * Check if tenant type requires full compliance
     */
    public boolean requiresFullCompliance() {
        return isFinancialInstitution() || this == CBDC_OPERATOR;
    }
}