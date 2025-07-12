package com.amanahfi.platform.regulatory.domain;

import lombok.Getter;

/**
 * Regulatory authorities across different jurisdictions
 */
@Getter
public enum RegulatoryAuthority {
    // UAE Authorities
    CBUAE("Central Bank of the UAE", Jurisdiction.UAE, "Open Finance, Banking Regulations"),
    VARA("Virtual Assets Regulatory Authority", Jurisdiction.UAE, "Cryptocurrency, Digital Assets"),
    HSA("Higher Sharia Authority", Jurisdiction.UAE, "Islamic Finance, Sharia Compliance"),
    DFSA("Dubai Financial Services Authority", Jurisdiction.UAE, "DIFC Financial Services"),
    ADGM_FSRA("ADGM Financial Services Regulatory Authority", Jurisdiction.UAE, "ADGM Financial Services"),
    
    // Saudi Arabia Authorities
    SAMA("Saudi Central Bank", Jurisdiction.SAUDI_ARABIA, "Banking, Payment Systems"),
    CMA_SA("Capital Market Authority", Jurisdiction.SAUDI_ARABIA, "Capital Markets, Securities"),
    
    // Turkey Authorities
    BDDK("Banking Regulation and Supervision Agency", Jurisdiction.TURKEY, "Banking Supervision"),
    TCMB("Central Bank of Turkey", Jurisdiction.TURKEY, "Monetary Policy, Payment Systems"),
    
    // Pakistan Authorities
    SBP("State Bank of Pakistan", Jurisdiction.PAKISTAN, "Banking, Monetary Policy"),
    SECP("Securities and Exchange Commission", Jurisdiction.PAKISTAN, "Capital Markets"),
    
    // Azerbaijan Authorities
    CBAR("Central Bank of Azerbaijan", Jurisdiction.AZERBAIJAN, "Banking, Financial Stability"),
    
    // Iran Authorities
    CBI("Central Bank of Iran", Jurisdiction.IRAN, "Banking, Foreign Exchange"),
    
    // Israel Authorities
    BOI("Bank of Israel", Jurisdiction.ISRAEL, "Banking Supervision, Monetary Policy"),
    ISA("Israel Securities Authority", Jurisdiction.ISRAEL, "Securities, Capital Markets"),
    
    // International Bodies
    FATF("Financial Action Task Force", null, "AML/CFT Standards"),
    BIS("Bank for International Settlements", null, "International Banking Standards"),
    IFSB("Islamic Financial Services Board", null, "Islamic Finance Standards"),
    AAOIFI("Accounting and Auditing Organization for Islamic Financial Institutions", null, "Islamic Finance Accounting");
    
    private final String fullName;
    private final Jurisdiction primaryJurisdiction;
    private final String scope;
    
    RegulatoryAuthority(String fullName, Jurisdiction primaryJurisdiction, String scope) {
        this.fullName = fullName;
        this.primaryJurisdiction = primaryJurisdiction;
        this.scope = scope;
    }
    
    public boolean isInternational() {
        return primaryJurisdiction == null;
    }
    
    public boolean isIslamicFinanceAuthority() {
        return this == HSA || this == IFSB || this == AAOIFI;
    }
    
    public boolean isCryptocurrencyAuthority() {
        return this == VARA;
    }
}