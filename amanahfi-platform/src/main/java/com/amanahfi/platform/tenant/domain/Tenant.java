package com.amanahfi.platform.tenant.domain;

import com.amanahfi.platform.shared.domain.AggregateRoot;
import com.amanahfi.platform.tenant.domain.events.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Tenant aggregate root
 * Represents a tenant in the multi-tenant architecture
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Tenant extends AggregateRoot<TenantId> {
    
    private TenantId id;
    private String name;
    private String description;
    private TenantType type;
    private TenantStatus status;
    private TenantConfiguration configuration;
    private String contactEmail;
    private String contactPhone;
    private String administratorUserId;
    private Instant createdAt;
    private Instant lastUpdated;
    private String createdBy;
    private String lastUpdatedBy;
    private Long version;
    
    /**
     * Create a new tenant
     */
    public static Tenant create(
            String name,
            String description,
            TenantType type,
            TenantConfiguration configuration,
            String contactEmail,
            String contactPhone,
            String administratorUserId,
            String createdBy
    ) {
        // Validate inputs
        validateName(name);
        validateContactEmail(contactEmail);
        validateAdministratorUserId(administratorUserId);
        Objects.requireNonNull(type, "Tenant type cannot be null");
        Objects.requireNonNull(configuration, "Tenant configuration cannot be null");
        Objects.requireNonNull(createdBy, "Creator cannot be null");
        
        // Validate configuration
        configuration.validate();
        
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.now();
        
        Tenant tenant = new Tenant(
            tenantId,
            name,
            description,
            type,
            TenantStatus.PROVISIONING,
            configuration,
            contactEmail,
            contactPhone,
            administratorUserId,
            now,
            now,
            createdBy,
            createdBy,
            0L
        );
        
        // Raise domain event
        tenant.raiseEvent(TenantCreatedEvent.builder()
            .tenantId(tenantId)
            .name(name)
            .type(type)
            .primaryJurisdiction(configuration.getPrimaryJurisdiction())
            .contactEmail(contactEmail)
            .administratorUserId(administratorUserId)
            .createdBy(createdBy)
            .createdAt(now)
            .build());
        
        return tenant;
    }
    
    /**
     * Activate the tenant
     */
    public void activate(String updatedBy) {
        if (status == TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is already active");
        }
        
        if (status == TenantStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot activate deactivated tenant");
        }
        
        TenantStatus previousStatus = this.status;
        this.status = TenantStatus.ACTIVE;
        this.lastUpdated = Instant.now();
        this.lastUpdatedBy = updatedBy;
        this.version++;
        
        raiseEvent(TenantActivatedEvent.builder()
            .tenantId(id)
            .previousStatus(previousStatus)
            .newStatus(TenantStatus.ACTIVE)
            .updatedBy(updatedBy)
            .updatedAt(lastUpdated)
            .build());
    }
    
    /**
     * Suspend the tenant
     */
    public void suspend(String reason, String updatedBy) {
        if (status == TenantStatus.SUSPENDED) {
            throw new IllegalStateException("Tenant is already suspended");
        }
        
        if (!status.isAccessible()) {
            throw new IllegalStateException("Cannot suspend tenant in status: " + status);
        }
        
        Objects.requireNonNull(reason, "Suspension reason cannot be null");
        
        TenantStatus previousStatus = this.status;
        this.status = TenantStatus.SUSPENDED;
        this.lastUpdated = Instant.now();
        this.lastUpdatedBy = updatedBy;
        this.version++;
        
        raiseEvent(TenantSuspendedEvent.builder()
            .tenantId(id)
            .previousStatus(previousStatus)
            .reason(reason)
            .updatedBy(updatedBy)
            .updatedAt(lastUpdated)
            .build());
    }
    
    /**
     * Update tenant configuration
     */
    public void updateConfiguration(TenantConfiguration newConfiguration, String updatedBy) {
        Objects.requireNonNull(newConfiguration, "Configuration cannot be null");
        Objects.requireNonNull(updatedBy, "Updater cannot be null");
        
        // Validate new configuration
        newConfiguration.validate();
        
        TenantConfiguration previousConfiguration = this.configuration;
        this.configuration = newConfiguration;
        this.lastUpdated = Instant.now();
        this.lastUpdatedBy = updatedBy;
        this.version++;
        
        raiseEvent(TenantConfigurationUpdatedEvent.builder()
            .tenantId(id)
            .previousConfiguration(previousConfiguration)
            .newConfiguration(newConfiguration)
            .updatedBy(updatedBy)
            .updatedAt(lastUpdated)
            .build());
    }
    
    /**
     * Update tenant contact information
     */
    public void updateContactInfo(String contactEmail, String contactPhone, String updatedBy) {
        validateContactEmail(contactEmail);
        Objects.requireNonNull(updatedBy, "Updater cannot be null");
        
        String previousEmail = this.contactEmail;
        String previousPhone = this.contactPhone;
        
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.lastUpdated = Instant.now();
        this.lastUpdatedBy = updatedBy;
        this.version++;
        
        raiseEvent(TenantContactInfoUpdatedEvent.builder()
            .tenantId(id)
            .previousEmail(previousEmail)
            .previousPhone(previousPhone)
            .newEmail(contactEmail)
            .newPhone(contactPhone)
            .updatedBy(updatedBy)
            .updatedAt(lastUpdated)
            .build());
    }
    
    /**
     * Deactivate the tenant
     */
    public void deactivate(String reason, String updatedBy) {
        if (status == TenantStatus.DEACTIVATED) {
            throw new IllegalStateException("Tenant is already deactivated");
        }
        
        Objects.requireNonNull(reason, "Deactivation reason cannot be null");
        Objects.requireNonNull(updatedBy, "Updater cannot be null");
        
        TenantStatus previousStatus = this.status;
        this.status = TenantStatus.DEACTIVATED;
        this.lastUpdated = Instant.now();
        this.lastUpdatedBy = updatedBy;
        this.version++;
        
        raiseEvent(TenantDeactivatedEvent.builder()
            .tenantId(id)
            .previousStatus(previousStatus)
            .reason(reason)
            .updatedBy(updatedBy)
            .updatedAt(lastUpdated)
            .build());
    }
    
    /**
     * Check if tenant is operational
     */
    public boolean isOperational() {
        return status.isOperational();
    }
    
    /**
     * Check if tenant can be accessed
     */
    public boolean isAccessible() {
        return status.isAccessible();
    }
    
    /**
     * Check if tenant requires enhanced security
     */
    public boolean requiresEnhancedSecurity() {
        return configuration.isEnhancedSecurityEnabled() || 
               type.requiresFullCompliance();
    }
    
    /**
     * Check if tenant supports jurisdiction
     */
    public boolean supportsJurisdiction(com.amanahfi.platform.regulatory.domain.Jurisdiction jurisdiction) {
        return configuration.getAllJurisdictions().contains(jurisdiction);
    }
    
    /**
     * Check if tenant supports language
     */
    public boolean supportsLanguage(String language) {
        return configuration.getSupportedLanguages().contains(language);
    }
    
    /**
     * Check if tenant supports currency
     */
    public boolean supportsCurrency(String currency) {
        return configuration.getSupportedCurrencies().contains(currency);
    }
    
    // Validation methods
    
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        
        if (name.length() > 100) {
            throw new IllegalArgumentException("Tenant name cannot exceed 100 characters");
        }
    }
    
    private static void validateContactEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact email cannot be null or empty");
        }
        
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Contact email must be a valid email address");
        }
    }
    
    private static void validateAdministratorUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Administrator user ID cannot be null or empty");
        }
    }
}