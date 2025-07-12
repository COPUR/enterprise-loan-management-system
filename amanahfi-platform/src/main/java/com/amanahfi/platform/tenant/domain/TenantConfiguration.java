package com.amanahfi.platform.tenant.domain;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * Tenant configuration value object
 */
@Value
@Builder
public class TenantConfiguration {
    
    /**
     * Primary jurisdiction for the tenant
     */
    Jurisdiction primaryJurisdiction;
    
    /**
     * Additional jurisdictions where tenant operates
     */
    Set<Jurisdiction> additionalJurisdictions;
    
    /**
     * Supported languages (ISO 639-1 codes)
     */
    Set<String> supportedLanguages;
    
    /**
     * Default language for the tenant
     */
    String defaultLanguage;
    
    /**
     * Timezone for the tenant
     */
    String timezone;
    
    /**
     * Currency codes supported by the tenant
     */
    Set<String> supportedCurrencies;
    
    /**
     * Default currency for the tenant
     */
    String defaultCurrency;
    
    /**
     * Maximum number of users allowed
     */
    int maxUsers;
    
    /**
     * Maximum storage quota in GB
     */
    long maxStorageGB;
    
    /**
     * Maximum API calls per minute
     */
    int maxApiCallsPerMinute;
    
    /**
     * Data retention period in days
     */
    int dataRetentionDays;
    
    /**
     * Whether tenant has access to advanced features
     */
    boolean advancedFeaturesEnabled;
    
    /**
     * Whether tenant requires enhanced security
     */
    boolean enhancedSecurityEnabled;
    
    /**
     * Whether tenant supports white-label branding
     */
    boolean whiteLabelEnabled;
    
    /**
     * Custom domain for the tenant
     */
    String customDomain;
    
    /**
     * Validate tenant configuration
     */
    public void validate() {
        if (primaryJurisdiction == null) {
            throw new IllegalArgumentException("Primary jurisdiction is required");
        }
        
        if (supportedLanguages == null || supportedLanguages.isEmpty()) {
            throw new IllegalArgumentException("At least one supported language is required");
        }
        
        if (defaultLanguage == null || !supportedLanguages.contains(defaultLanguage)) {
            throw new IllegalArgumentException("Default language must be one of the supported languages");
        }
        
        if (supportedCurrencies == null || supportedCurrencies.isEmpty()) {
            throw new IllegalArgumentException("At least one supported currency is required");
        }
        
        if (defaultCurrency == null || !supportedCurrencies.contains(defaultCurrency)) {
            throw new IllegalArgumentException("Default currency must be one of the supported currencies");
        }
        
        if (maxUsers <= 0) {
            throw new IllegalArgumentException("Max users must be positive");
        }
        
        if (maxStorageGB <= 0) {
            throw new IllegalArgumentException("Max storage must be positive");
        }
        
        if (maxApiCallsPerMinute <= 0) {
            throw new IllegalArgumentException("Max API calls per minute must be positive");
        }
        
        if (dataRetentionDays <= 0) {
            throw new IllegalArgumentException("Data retention period must be positive");
        }
    }
    
    /**
     * Check if tenant operates in multiple jurisdictions
     */
    public boolean isMultiJurisdiction() {
        return additionalJurisdictions != null && !additionalJurisdictions.isEmpty();
    }
    
    /**
     * Check if tenant supports multiple languages
     */
    public boolean isMultiLanguage() {
        return supportedLanguages.size() > 1;
    }
    
    /**
     * Check if tenant supports multiple currencies
     */
    public boolean isMultiCurrency() {
        return supportedCurrencies.size() > 1;
    }
    
    /**
     * Get all jurisdictions (primary + additional)
     */
    public Set<Jurisdiction> getAllJurisdictions() {
        Set<Jurisdiction> all = Set.of(primaryJurisdiction);
        if (additionalJurisdictions != null) {
            all.addAll(additionalJurisdictions);
        }
        return all;
    }
}