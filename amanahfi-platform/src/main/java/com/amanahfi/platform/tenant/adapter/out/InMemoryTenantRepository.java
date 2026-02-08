package com.amanahfi.platform.tenant.adapter.out;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.tenant.domain.Tenant;
import com.amanahfi.platform.tenant.domain.TenantId;
import com.amanahfi.platform.tenant.domain.TenantStatus;
import com.amanahfi.platform.tenant.domain.TenantType;
import com.amanahfi.platform.tenant.port.out.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of TenantRepository for testing and development
 */
@Repository
@Slf4j
public class InMemoryTenantRepository implements TenantRepository {
    
    private final ConcurrentHashMap<TenantId, Tenant> tenants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TenantId> nameIndex = new ConcurrentHashMap<>();
    
    @Override
    public Tenant save(Tenant tenant) {
        log.debug("Saving tenant: {}", tenant.getId());
        
        // Update main storage
        tenants.put(tenant.getId(), tenant);
        
        // Update name index
        nameIndex.put(tenant.getName(), tenant.getId());
        
        log.debug("Tenant saved: {} - {}", tenant.getId(), tenant.getName());
        return tenant;
    }
    
    @Override
    public Optional<Tenant> findById(TenantId id) {
        log.debug("Finding tenant by ID: {}", id);
        
        Tenant tenant = tenants.get(id);
        if (tenant != null) {
            log.debug("Found tenant: {} - {}", id, tenant.getName());
        } else {
            log.debug("Tenant not found: {}", id);
        }
        
        return Optional.ofNullable(tenant);
    }
    
    @Override
    public Optional<Tenant> findByName(String name) {
        log.debug("Finding tenant by name: {}", name);
        
        TenantId tenantId = nameIndex.get(name);
        if (tenantId != null) {
            Tenant tenant = tenants.get(tenantId);
            if (tenant != null) {
                log.debug("Found tenant by name: {} - {}", name, tenantId);
                return Optional.of(tenant);
            }
        }
        
        log.debug("Tenant not found by name: {}", name);
        return Optional.empty();
    }
    
    @Override
    public List<Tenant> findAll() {
        log.debug("Finding all tenants - Count: {}", tenants.size());
        return List.copyOf(tenants.values());
    }
    
    @Override
    public List<Tenant> findByStatus(TenantStatus status) {
        log.debug("Finding tenants by status: {}", status);
        
        List<Tenant> result = tenants.values().stream()
            .filter(tenant -> tenant.getStatus() == status)
            .toList();
        
        log.debug("Found {} tenants with status: {}", result.size(), status);
        return result;
    }
    
    @Override
    public List<Tenant> findByType(TenantType type) {
        log.debug("Finding tenants by type: {}", type);
        
        List<Tenant> result = tenants.values().stream()
            .filter(tenant -> tenant.getType() == type)
            .toList();
        
        log.debug("Found {} tenants with type: {}", result.size(), type);
        return result;
    }
    
    @Override
    public List<Tenant> findByJurisdiction(Jurisdiction jurisdiction) {
        log.debug("Finding tenants by jurisdiction: {}", jurisdiction);
        
        List<Tenant> result = tenants.values().stream()
            .filter(tenant -> tenant.supportsJurisdiction(jurisdiction))
            .toList();
        
        log.debug("Found {} tenants with jurisdiction: {}", result.size(), jurisdiction);
        return result;
    }
    
    @Override
    public boolean existsById(TenantId id) {
        boolean exists = tenants.containsKey(id);
        log.debug("Tenant exists: {} = {}", id, exists);
        return exists;
    }
    
    @Override
    public boolean existsByName(String name) {
        boolean exists = nameIndex.containsKey(name);
        log.debug("Tenant exists by name: {} = {}", name, exists);
        return exists;
    }
    
    @Override
    public void deleteById(TenantId id) {
        log.debug("Deleting tenant by ID: {}", id);
        
        Tenant tenant = tenants.remove(id);
        if (tenant != null) {
            nameIndex.remove(tenant.getName());
            log.debug("Tenant deleted: {} - {}", id, tenant.getName());
        } else {
            log.debug("Tenant not found for deletion: {}", id);
        }
    }
    
    @Override
    public void delete(Tenant tenant) {
        log.debug("Deleting tenant: {} - {}", tenant.getId(), tenant.getName());
        
        tenants.remove(tenant.getId());
        nameIndex.remove(tenant.getName());
        
        log.debug("Tenant deleted: {} - {}", tenant.getId(), tenant.getName());
    }
    
    @Override
    public long count() {
        long count = tenants.size();
        log.debug("Total tenant count: {}", count);
        return count;
    }
    
    @Override
    public long countByStatus(TenantStatus status) {
        long count = tenants.values().stream()
            .filter(tenant -> tenant.getStatus() == status)
            .count();
        
        log.debug("Tenant count by status {}: {}", status, count);
        return count;
    }
    
    @Override
    public long countByType(TenantType type) {
        long count = tenants.values().stream()
            .filter(tenant -> tenant.getType() == type)
            .count();
        
        log.debug("Tenant count by type {}: {}", type, count);
        return count;
    }
    
    /**
     * Clear all tenants (for testing)
     */
    public void deleteAll() {
        log.warn("Deleting all tenants - Count: {}", tenants.size());
        tenants.clear();
        nameIndex.clear();
        log.warn("All tenants deleted");
    }
}