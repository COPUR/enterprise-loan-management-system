package com.loanmanagement.party.domain;

/**
 * Role source enumeration for the banking system
 * Defines the source or origin of a party role assignment
 */
public enum RoleSource {
    SYSTEM("System-assigned role"),
    ADMINISTRATOR("Administrator-assigned role"),
    EXTERNAL_SYSTEM("External system integration"),
    SELF_SERVICE("Self-service assignment"),
    COMPLIANCE("Compliance-driven assignment"),
    AUDIT("Audit-driven assignment"),
    MIGRATION("Data migration assignment");
    
    private final String description;
    
    RoleSource(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isSystemGenerated() {
        return this == SYSTEM || this == COMPLIANCE || this == AUDIT || this == MIGRATION;
    }
    
    public boolean isUserInitiated() {
        return this == ADMINISTRATOR || this == SELF_SERVICE;
    }
}