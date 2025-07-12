package com.amanahfi.platform.tenant.application;

import com.amanahfi.platform.shared.events.DomainEventPublisher;
import com.amanahfi.platform.tenant.domain.*;
import com.amanahfi.platform.tenant.port.in.*;
import com.amanahfi.platform.tenant.port.out.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for tenant management
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TenantManagementService implements TenantManagementUseCase {
    
    private final TenantRepository tenantRepository;
    private final DomainEventPublisher eventPublisher;
    private final TenantContextManager tenantContextManager;
    
    @Override
    public TenantId createTenant(CreateTenantCommand command) {
        log.info("Creating tenant: {}", command.getName());
        
        // Validate command
        command.validate();
        
        // Check if tenant with same name already exists
        if (tenantRepository.existsByName(command.getName())) {
            throw new IllegalArgumentException("Tenant with name '" + command.getName() + "' already exists");
        }
        
        // Create tenant
        Tenant tenant = Tenant.create(
            command.getName(),
            command.getDescription(),
            command.getType(),
            command.getConfiguration(),
            command.getContactEmail(),
            command.getContactPhone(),
            command.getAdministratorUserId(),
            command.getCreatedBy()
        );
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant created successfully: {}", tenant.getId());
        return tenant.getId();
    }
    
    @Override
    public void activateTenant(ActivateTenantCommand command) {
        log.info("Activating tenant: {}", command.getTenantId());
        
        // Validate command
        command.validate();
        
        // Get tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        // Activate tenant
        tenant.activate(command.getUpdatedBy());
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant activated successfully: {}", command.getTenantId());
    }
    
    @Override
    public void suspendTenant(SuspendTenantCommand command) {
        log.info("Suspending tenant: {}", command.getTenantId());
        
        // Validate command
        command.validate();
        
        // Get tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        // Suspend tenant
        tenant.suspend(command.getReason(), command.getUpdatedBy());
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant suspended successfully: {}", command.getTenantId());
    }
    
    @Override
    public void updateTenantConfiguration(UpdateTenantConfigurationCommand command) {
        log.info("Updating tenant configuration: {}", command.getTenantId());
        
        // Validate command
        command.validate();
        
        // Get tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        // Update configuration
        tenant.updateConfiguration(command.getConfiguration(), command.getUpdatedBy());
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant configuration updated successfully: {}", command.getTenantId());
    }
    
    @Override
    public void updateTenantContactInfo(UpdateTenantContactInfoCommand command) {
        log.info("Updating tenant contact info: {}", command.getTenantId());
        
        // Validate command
        command.validate();
        
        // Get tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        // Update contact info
        tenant.updateContactInfo(
            command.getContactEmail(),
            command.getContactPhone(),
            command.getUpdatedBy()
        );
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant contact info updated successfully: {}", command.getTenantId());
    }
    
    @Override
    public void deactivateTenant(DeactivateTenantCommand command) {
        log.info("Deactivating tenant: {}", command.getTenantId());
        
        // Validate command
        command.validate();
        
        // Get tenant
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        // Deactivate tenant
        tenant.deactivate(command.getReason(), command.getUpdatedBy());
        
        // Save tenant
        tenantRepository.save(tenant);
        
        // Publish events
        tenant.getUncommittedEvents().forEach(eventPublisher::publish);
        tenant.markEventsAsCommitted();
        
        log.info("Tenant deactivated successfully: {}", command.getTenantId());
    }
    
    @Override
    public Optional<Tenant> getTenant(GetTenantQuery query) {
        log.debug("Getting tenant: {}", query.getTenantId());
        
        // Validate query
        query.validate();
        
        return tenantRepository.findById(query.getTenantId());
    }
    
    @Override
    public List<Tenant> getAllTenants(GetAllTenantsQuery query) {
        log.debug("Getting all tenants");
        
        // Validate query
        query.validate();
        
        return tenantRepository.findAll();
    }
    
    @Override
    public List<Tenant> getTenantsByStatus(GetTenantsByStatusQuery query) {
        log.debug("Getting tenants by status: {}", query.getStatus());
        
        // Validate query
        query.validate();
        
        return tenantRepository.findByStatus(query.getStatus());
    }
    
    @Override
    public List<Tenant> getTenantsByType(GetTenantsByTypeQuery query) {
        log.debug("Getting tenants by type: {}", query.getType());
        
        // Validate query
        query.validate();
        
        return tenantRepository.findByType(query.getType());
    }
    
    @Override
    public List<Tenant> getTenantsByJurisdiction(GetTenantsByJurisdictionQuery query) {
        log.debug("Getting tenants by jurisdiction: {}", query.getJurisdiction());
        
        // Validate query
        query.validate();
        
        return tenantRepository.findByJurisdiction(query.getJurisdiction());
    }
    
    @Override
    public boolean tenantExists(TenantId tenantId) {
        return tenantRepository.existsById(tenantId);
    }
    
    @Override
    public long getTenantCount() {
        return tenantRepository.count();
    }
    
    @Override
    public TenantContext createTenantContext(CreateTenantContextCommand command) {
        log.debug("Creating tenant context for tenant: {}, user: {}", 
            command.getTenantId(), command.getUserId());
        
        // Validate command
        command.validate();
        
        // Verify tenant exists and is accessible
        Tenant tenant = tenantRepository.findById(command.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + command.getTenantId()));
        
        if (!tenant.isAccessible()) {
            throw new IllegalStateException("Tenant is not accessible: " + command.getTenantId());
        }
        
        // Create tenant context
        TenantContext context = TenantContext.builder()
            .tenantId(command.getTenantId())
            .userId(command.getUserId())
            .sessionId(command.getSessionId())
            .correlationId(command.getCorrelationId())
            .requestTime(command.getRequestTime())
            .clientIp(command.getClientIp())
            .userAgent(command.getUserAgent())
            .language(command.getLanguage())
            .currency(command.getCurrency())
            .timezone(command.getTimezone())
            .isAdmin(command.isAdmin())
            .hasElevatedPrivileges(command.isHasElevatedPrivileges())
            .attributes(command.getAttributes())
            .build();
        
        // Validate context
        context.validate();
        
        // Set context in manager
        tenantContextManager.setTenantContext(context);
        
        log.debug("Tenant context created successfully for tenant: {}, user: {}", 
            command.getTenantId(), command.getUserId());
        
        return context;
    }
}