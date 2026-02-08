package com.amanahfi.platform.tenant.port.out;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.tenant.domain.Tenant;
import com.amanahfi.platform.tenant.domain.TenantId;
import com.amanahfi.platform.tenant.domain.TenantStatus;
import com.amanahfi.platform.tenant.domain.TenantType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for tenant persistence
 */
public interface TenantRepository {
    
    /**
     * Save a tenant
     */
    Tenant save(Tenant tenant);
    
    /**
     * Find tenant by ID
     */
    Optional<Tenant> findById(TenantId id);
    
    /**
     * Find tenant by name
     */
    Optional<Tenant> findByName(String name);
    
    /**
     * Find all tenants
     */
    List<Tenant> findAll();
    
    /**
     * Find tenants by status
     */
    List<Tenant> findByStatus(TenantStatus status);
    
    /**
     * Find tenants by type
     */
    List<Tenant> findByType(TenantType type);
    
    /**
     * Find tenants by jurisdiction
     */
    List<Tenant> findByJurisdiction(Jurisdiction jurisdiction);
    
    /**
     * Check if tenant exists by ID
     */
    boolean existsById(TenantId id);
    
    /**
     * Check if tenant exists by name
     */
    boolean existsByName(String name);
    
    /**
     * Delete tenant by ID
     */
    void deleteById(TenantId id);
    
    /**
     * Delete tenant
     */
    void delete(Tenant tenant);
    
    /**
     * Count all tenants
     */
    long count();
    
    /**
     * Count tenants by status
     */
    long countByStatus(TenantStatus status);
    
    /**
     * Count tenants by type
     */
    long countByType(TenantType type);
}