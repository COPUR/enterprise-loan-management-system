package com.banking.loans.domain.party;

/**
 * RoleSource enumeration for banking domain
 * Defines the source system where role assignments originate
 */
public enum RoleSource {
    /**
     * Role assigned directly in the party data management database
     * This is the authoritative source for banking system roles
     */
    DATABASE,
    
    /**
     * Role sourced from LDAP directory
     * Used for integration with corporate directory services
     */
    LDAP,
    
    /**
     * Role sourced from Keycloak identity provider
     * Used for OAuth2.1 integration and external identity providers
     */
    KEYCLOAK,
    
    /**
     * Role sourced from external system integration
     * Used for third-party identity and access management systems
     */
    EXTERNAL,
    
    /**
     * Role assigned through automated provisioning
     * Used for system-generated role assignments based on business rules
     */
    AUTOMATED,
    
    /**
     * Role imported from legacy system
     * Used during migration from legacy banking systems
     */
    LEGACY_IMPORT
}