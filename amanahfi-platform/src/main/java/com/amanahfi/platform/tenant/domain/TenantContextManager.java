package com.amanahfi.platform.tenant.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Manages tenant context for the current execution thread
 */
@Component
@Slf4j
public class TenantContextManager {
    
    private static final ThreadLocal<TenantContext> TENANT_CONTEXT = new ThreadLocal<>();
    
    /**
     * Set the tenant context for the current thread
     */
    public void setTenantContext(TenantContext context) {
        if (context != null) {
            context.validate();
            TENANT_CONTEXT.set(context);
            log.debug("Tenant context set for tenant: {}, user: {}", 
                context.getTenantId(), context.getUserId());
        } else {
            log.warn("Attempted to set null tenant context");
        }
    }
    
    /**
     * Get the current tenant context
     */
    public Optional<TenantContext> getCurrentTenantContext() {
        return Optional.ofNullable(TENANT_CONTEXT.get());
    }
    
    /**
     * Get the current tenant ID
     */
    public Optional<TenantId> getCurrentTenantId() {
        return getCurrentTenantContext().map(TenantContext::getTenantId);
    }
    
    /**
     * Get the current user ID
     */
    public Optional<String> getCurrentUserId() {
        return getCurrentTenantContext().map(TenantContext::getUserId);
    }
    
    /**
     * Get the current correlation ID
     */
    public Optional<String> getCurrentCorrelationId() {
        return getCurrentTenantContext().map(TenantContext::getCorrelationId);
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isCurrentUserAdmin() {
        return getCurrentTenantContext()
            .map(TenantContext::isAdmin)
            .orElse(false);
    }
    
    /**
     * Check if current user has elevated privileges
     */
    public boolean hasElevatedPrivileges() {
        return getCurrentTenantContext()
            .map(TenantContext::isHasElevatedPrivileges)
            .orElse(false);
    }
    
    /**
     * Get the current language
     */
    public Optional<String> getCurrentLanguage() {
        return getCurrentTenantContext().map(TenantContext::getLanguage);
    }
    
    /**
     * Get the current currency
     */
    public Optional<String> getCurrentCurrency() {
        return getCurrentTenantContext().map(TenantContext::getCurrency);
    }
    
    /**
     * Get the current timezone
     */
    public Optional<String> getCurrentTimezone() {
        return getCurrentTenantContext().map(TenantContext::getTimezone);
    }
    
    /**
     * Clear the tenant context for the current thread
     */
    public void clearTenantContext() {
        TenantContext context = TENANT_CONTEXT.get();
        if (context != null) {
            log.debug("Clearing tenant context for tenant: {}, user: {}", 
                context.getTenantId(), context.getUserId());
        }
        TENANT_CONTEXT.remove();
    }
    
    /**
     * Execute a task with a specific tenant context
     */
    public <T> T executeWithTenantContext(TenantContext context, java.util.function.Supplier<T> task) {
        TenantContext previousContext = TENANT_CONTEXT.get();
        try {
            setTenantContext(context);
            return task.get();
        } finally {
            if (previousContext != null) {
                setTenantContext(previousContext);
            } else {
                clearTenantContext();
            }
        }
    }
    
    /**
     * Execute a task with a specific tenant context (no return value)
     */
    public void executeWithTenantContext(TenantContext context, Runnable task) {
        TenantContext previousContext = TENANT_CONTEXT.get();
        try {
            setTenantContext(context);
            task.run();
        } finally {
            if (previousContext != null) {
                setTenantContext(previousContext);
            } else {
                clearTenantContext();
            }
        }
    }
    
    /**
     * Ensure tenant context is set
     */
    public void ensureTenantContext() {
        if (getCurrentTenantContext().isEmpty()) {
            throw new IllegalStateException("No tenant context set for current thread");
        }
    }
    
    /**
     * Ensure tenant context is set for specific tenant
     */
    public void ensureTenantContext(TenantId expectedTenantId) {
        TenantContext context = getCurrentTenantContext()
            .orElseThrow(() -> new IllegalStateException("No tenant context set for current thread"));
        
        if (!context.getTenantId().equals(expectedTenantId)) {
            throw new IllegalStateException("Tenant context mismatch. Expected: " + expectedTenantId + 
                ", Actual: " + context.getTenantId());
        }
    }
}