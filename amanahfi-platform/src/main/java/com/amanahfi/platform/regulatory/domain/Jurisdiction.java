package com.amanahfi.platform.regulatory.domain;

import lombok.Getter;

/**
 * Supported jurisdictions for regulatory compliance
 */
@Getter
public enum Jurisdiction {
    // Primary market
    UAE("AE", "United Arab Emirates", "AED", "en,ar"),
    
    // MENAT expansion markets
    SAUDI_ARABIA("SA", "Saudi Arabia", "SAR", "ar,en"),
    TURKEY("TR", "Turkey", "TRY", "tr,en"),
    PAKISTAN("PK", "Pakistan", "PKR", "ur,en"),
    AZERBAIJAN("AZ", "Azerbaijan", "AZN", "az,en"),
    IRAN("IR", "Iran", "IRR", "fa,en"),
    ISRAEL("IL", "Israel", "ILS", "he,ar,en"),
    
    // Additional GCC countries
    BAHRAIN("BH", "Bahrain", "BHD", "ar,en"),
    KUWAIT("KW", "Kuwait", "KWD", "ar,en"),
    OMAN("OM", "Oman", "OMR", "ar,en"),
    QATAR("QA", "Qatar", "QAR", "ar,en");
    
    private final String isoCode;
    private final String displayName;
    private final String defaultCurrency;
    private final String supportedLanguages;
    
    Jurisdiction(String isoCode, String displayName, String defaultCurrency, String supportedLanguages) {
        this.isoCode = isoCode;
        this.displayName = displayName;
        this.defaultCurrency = defaultCurrency;
        this.supportedLanguages = supportedLanguages;
    }
    
    public static Jurisdiction fromIsoCode(String isoCode) {
        for (Jurisdiction jurisdiction : values()) {
            if (jurisdiction.isoCode.equalsIgnoreCase(isoCode)) {
                return jurisdiction;
            }
        }
        throw new IllegalArgumentException("Unknown jurisdiction ISO code: " + isoCode);
    }
    
    public boolean isGCC() {
        return this == UAE || this == SAUDI_ARABIA || this == BAHRAIN || 
               this == KUWAIT || this == OMAN || this == QATAR;
    }
    
    public boolean requiresDataSovereignty() {
        // All jurisdictions require data sovereignty
        return true;
    }
    
    public boolean requiresComputationalSovereignty() {
        // Specific jurisdictions require computational sovereignty
        return this == UAE || this == SAUDI_ARABIA || this == IRAN;
    }
}