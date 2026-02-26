package com.loanmanagement.party.domain;

/**
 * Group type enumeration for the banking system
 * Defines different types of party groups
 */
public enum GroupType {
    DEPARTMENT("Organizational department"),
    TEAM("Working team or unit"),
    ROLE_GROUP("Role-based grouping"),
    BUSINESS_UNIT("Business unit or division"),
    COMPLIANCE_GROUP("Compliance-based grouping"),
    GEOGRAPHIC("Geographic location-based group"),
    CUSTOMER_SEGMENT("Customer segmentation group"),
    SECURITY_GROUP("Security access group"),
    PROJECT_TEAM("Project-based team"),
    COMMITTEE("Committee or board");
    
    private final String description;
    
    GroupType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isOrganizational() {
        return this == DEPARTMENT || this == TEAM || this == BUSINESS_UNIT || this == PROJECT_TEAM;
    }
    
    public boolean isSecurityRelated() {
        return this == SECURITY_GROUP || this == COMPLIANCE_GROUP || this == ROLE_GROUP;
    }
    
    public boolean isBusinessRelated() {
        return this == CUSTOMER_SEGMENT || this == BUSINESS_UNIT || this == GEOGRAPHIC;
    }
}