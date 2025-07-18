package com.bank.infrastructure.context;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Banking Context Propagation for Java 21 Virtual Threads
 * 
 * Provides thread-safe context propagation across virtual threads for:
 * - Security context (authentication, authorization)
 * - Request context (HTTP request attributes)
 * - Banking transaction context (audit trails, correlation IDs)
 * - Tenant context (multi-tenant isolation)
 * 
 * This implementation bridges the gap until Java 21 Scoped Values become stable
 * and provides structured context management for banking operations.
 */
@Component
public class BankingContextPropagation {

    /**
     * Banking context holder for virtual thread propagation
     */
    private static final ThreadLocal<BankingExecutionContext> BANKING_CONTEXT = 
        new ThreadLocal<>();

    /**
     * Execute task with current banking context propagated to virtual thread
     */
    public <T> CompletableFuture<T> executeWithContext(
            Supplier<T> task, 
            Executor virtualThreadExecutor) {
        
        // Capture current context
        BankingExecutionContext currentContext = captureCurrentContext();
        
        return CompletableFuture.supplyAsync(() -> {
            // Propagate context to virtual thread
            try {
                propagateContext(currentContext);
                return task.get();
            } finally {
                // Clean up context
                clearContext();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Execute callable with banking context propagation
     */
    public <T> CompletableFuture<T> executeWithContext(
            Callable<T> task, 
            Executor virtualThreadExecutor) {
        
        BankingExecutionContext currentContext = captureCurrentContext();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                propagateContext(currentContext);
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Task execution failed", e);
            } finally {
                clearContext();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Execute runnable with banking context propagation
     */
    public CompletableFuture<Void> executeWithContext(
            Runnable task, 
            Executor virtualThreadExecutor) {
        
        BankingExecutionContext currentContext = captureCurrentContext();
        
        return CompletableFuture.runAsync(() -> {
            try {
                propagateContext(currentContext);
                task.run();
            } finally {
                clearContext();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Get current banking context
     */
    public Optional<BankingExecutionContext> getCurrentContext() {
        return Optional.ofNullable(BANKING_CONTEXT.get());
    }

    /**
     * Set banking context for current thread
     */
    public void setCurrentContext(BankingExecutionContext context) {
        BANKING_CONTEXT.set(context);
    }

    /**
     * Clear banking context for current thread
     */
    public void clearContext() {
        BANKING_CONTEXT.remove();
    }

    /**
     * Capture current execution context from Spring Security and Request
     */
    private BankingExecutionContext captureCurrentContext() {
        // Capture security context
        SecurityContext securityContext = SecurityContextHolder.getContext();
        
        // Capture request context
        ServletRequestAttributes requestAttributes = null;
        try {
            requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        } catch (IllegalStateException e) {
            // No request context available (e.g., async processing)
        }
        
        // Get correlation ID from request or generate new one
        String correlationId = extractCorrelationId(requestAttributes)
            .orElse(UUID.randomUUID().toString());
        
        // Get tenant ID from request
        String tenantId = extractTenantId(requestAttributes)
            .orElse("default");
        
        // Get user ID from security context
        String userId = extractUserId(securityContext)
            .orElse("anonymous");
        
        return BankingExecutionContext.builder()
            .correlationId(correlationId)
            .tenantId(tenantId)
            .userId(userId)
            .securityContext(securityContext)
            .requestAttributes(requestAttributes)
            .capturedAt(System.currentTimeMillis())
            .build();
    }

    /**
     * Propagate captured context to current virtual thread
     */
    private void propagateContext(BankingExecutionContext context) {
        if (context == null) {
            return;
        }
        
        // Set banking context
        BANKING_CONTEXT.set(context);
        
        // Propagate Spring Security context
        if (context.getSecurityContext() != null) {
            SecurityContextHolder.setContext(context.getSecurityContext());
        }
        
        // Propagate request context if available
        if (context.getRequestAttributes() != null) {
            RequestContextHolder.setRequestAttributes(context.getRequestAttributes());
        }
    }

    /**
     * Extract correlation ID from request headers
     */
    private Optional<String> extractCorrelationId(ServletRequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return Optional.empty();
        }
        
        HttpServletRequest request = requestAttributes.getRequest();
        String correlationId = request.getHeader("X-Correlation-ID");
        
        if (correlationId == null) {
            correlationId = request.getHeader("X-Request-ID");
        }
        
        return Optional.ofNullable(correlationId);
    }

    /**
     * Extract tenant ID from request headers
     */
    private Optional<String> extractTenantId(ServletRequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return Optional.empty();
        }
        
        HttpServletRequest request = requestAttributes.getRequest();
        String tenantId = request.getHeader("X-Tenant-ID");
        
        return Optional.ofNullable(tenantId);
    }

    /**
     * Extract user ID from security context
     */
    private Optional<String> extractUserId(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getAuthentication() == null) {
            return Optional.empty();
        }
        
        return Optional.of(securityContext.getAuthentication().getName());
    }

    /**
     * Banking execution context for virtual thread propagation
     */
    @lombok.Builder
    @lombok.Data
    public static class BankingExecutionContext {
        private String correlationId;
        private String tenantId;
        private String userId;
        private SecurityContext securityContext;
        private ServletRequestAttributes requestAttributes;
        private long capturedAt;
        
        /**
         * Check if context is expired (older than 5 minutes)
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - capturedAt > 300_000; // 5 minutes
        }
        
        /**
         * Get context age in milliseconds
         */
        public long getAge() {
            return System.currentTimeMillis() - capturedAt;
        }
    }

    /**
     * Context-aware task wrapper for banking operations
     */
    @FunctionalInterface
    public interface BankingTask<T> {
        T execute(BankingExecutionContext context) throws Exception;
    }

    /**
     * Execute banking task with full context awareness
     */
    public <T> CompletableFuture<T> executeBankingTask(
            BankingTask<T> task,
            Executor virtualThreadExecutor) {
        
        BankingExecutionContext currentContext = captureCurrentContext();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                propagateContext(currentContext);
                return task.execute(currentContext);
            } catch (Exception e) {
                throw new RuntimeException("Banking task execution failed", e);
            } finally {
                clearContext();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Utility methods for context-aware logging
     */
    public static class ContextAwareLogger {
        
        public static String formatLogMessage(String message) {
            BankingExecutionContext context = BANKING_CONTEXT.get();
            if (context == null) {
                return message;
            }
            
            return String.format("[%s][%s][%s] %s",
                context.getCorrelationId(),
                context.getTenantId(),
                context.getUserId(),
                message);
        }
        
        public static void logWithContext(String message, Object... args) {
            String formattedMessage = formatLogMessage(String.format(message, args));
            System.out.println(formattedMessage); // Replace with actual logger
        }
    }

    /**
     * Context validation for banking operations
     */
    public static class ContextValidator {
        
        public static void validateBankingContext() {
            BankingExecutionContext context = BANKING_CONTEXT.get();
            
            if (context == null) {
                throw new IllegalStateException("Banking context not available");
            }
            
            if (context.isExpired()) {
                throw new IllegalStateException("Banking context expired");
            }
            
            if (context.getUserId() == null || "anonymous".equals(context.getUserId())) {
                throw new SecurityException("Authenticated user required for banking operations");
            }
        }
        
        public static void validateTenantContext() {
            BankingExecutionContext context = BANKING_CONTEXT.get();
            
            if (context == null || context.getTenantId() == null) {
                throw new IllegalStateException("Tenant context required for multi-tenant operations");
            }
        }
    }
}