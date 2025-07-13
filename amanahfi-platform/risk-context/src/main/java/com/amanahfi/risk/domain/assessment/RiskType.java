package com.amanahfi.risk.domain.assessment;

/**
 * Types of risk assessment in Islamic banking
 */
public enum RiskType {
    
    /**
     * Credit risk - risk of borrower default
     */
    CREDIT("Credit Risk", "Risk of counterparty default on financial obligations"),
    
    /**
     * Market risk - risk from market price movements
     */
    MARKET("Market Risk", "Risk from adverse movements in market prices"),
    
    /**
     * Operational risk - risk from internal processes, people, systems
     */
    OPERATIONAL("Operational Risk", "Risk from inadequate internal processes, people and systems"),
    
    /**
     * Liquidity risk - risk of inability to meet cash flow obligations
     */
    LIQUIDITY("Liquidity Risk", "Risk of inability to meet short-term financial obligations"),
    
    /**
     * Regulatory risk - risk from regulatory changes or non-compliance
     */
    REGULATORY("Regulatory Risk", "Risk from regulatory changes or compliance failures"),
    
    /**
     * Sharia compliance risk - specific to Islamic banking
     */
    SHARIA_COMPLIANCE("Sharia Compliance Risk", "Risk of non-compliance with Islamic law principles"),
    
    /**
     * Technology risk - risk from IT systems and cyber threats
     */
    TECHNOLOGY("Technology Risk", "Risk from IT systems failures or cyber security threats"),
    
    /**
     * Reputation risk - risk to institution's reputation
     */
    REPUTATION("Reputation Risk", "Risk of damage to institution's reputation and brand"),
    
    /**
     * Concentration risk - risk from lack of diversification
     */
    CONCENTRATION("Concentration Risk", "Risk from excessive concentration in particular sectors or counterparties");

    private final String displayName;
    private final String description;

    RiskType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCreditRelated() {
        return this == CREDIT || this == CONCENTRATION;
    }

    public boolean isMarketRelated() {
        return this == MARKET || this == LIQUIDITY;
    }

    public boolean isOperationalRelated() {
        return this == OPERATIONAL || this == TECHNOLOGY || this == REGULATORY;
    }

    public boolean isIslamicBankingSpecific() {
        return this == SHARIA_COMPLIANCE;
    }
}