package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.tenant.domain.Tenant;
import com.amanahfi.platform.tenant.domain.TenantContext;
import com.amanahfi.platform.tenant.domain.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * Use case interface for tenant management
 */
public interface TenantManagementUseCase {
    
    /**
     * Create a new tenant
     */
    TenantId createTenant(CreateTenantCommand command);
    
    /**
     * Activate a tenant
     */
    void activateTenant(ActivateTenantCommand command);
    
    /**
     * Suspend a tenant
     */
    void suspendTenant(SuspendTenantCommand command);
    
    /**
     * Update tenant configuration
     */
    void updateTenantConfiguration(UpdateTenantConfigurationCommand command);
    
    /**
     * Update tenant contact information
     */
    void updateTenantContactInfo(UpdateTenantContactInfoCommand command);
    
    /**
     * Deactivate a tenant
     */
    void deactivateTenant(DeactivateTenantCommand command);
    
    /**
     * Get a tenant by ID
     */
    Optional<Tenant> getTenant(GetTenantQuery query);
    
    /**
     * Get all tenants
     */
    List<Tenant> getAllTenants(GetAllTenantsQuery query);
    
    /**
     * Get tenants by status
     */
    List<Tenant> getTenantsByStatus(GetTenantsByStatusQuery query);
    
    /**
     * Get tenants by type
     */
    List<Tenant> getTenantsByType(GetTenantsByTypeQuery query);
    
    /**
     * Get tenants by jurisdiction
     */
    List<Tenant> getTenantsByJurisdiction(GetTenantsByJurisdictionQuery query);
    
    /**
     * Check if tenant exists
     */
    boolean tenantExists(TenantId tenantId);
    
    /**
     * Get tenant count
     */
    long getTenantCount();
    
    /**
     * Create tenant context
     */
    TenantContext createTenantContext(CreateTenantContextCommand command);
}