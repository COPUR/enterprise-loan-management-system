package com.amanahfi.platform.security.application;

import com.amanahfi.platform.security.domain.AuthenticationMethod;
import com.amanahfi.platform.security.domain.SecurityContext;
import com.amanahfi.platform.security.domain.SecurityLevel;
import com.amanahfi.platform.security.domain.SecurityPrincipal;
import com.amanahfi.platform.tenant.domain.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for SecurityContextManager with thread-safety validation
 */
@DisplayName("SecurityContextManager Thread-Safe Operations")
class SecurityContextManagerTest {
    
    private SecurityContextManager securityContextManager;
    private SecurityContext validSecurityContext;
    private SecurityPrincipal validPrincipal;
    private TenantId tenantId;
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        securityContextManager = new SecurityContextManager();
        executorService = Executors.newFixedThreadPool(10);
        
        tenantId = TenantId.generate();
        
        validPrincipal = SecurityPrincipal.builder()
            .userId("user-123")
            .username("john.doe@amanahfi.ae")
            .fullName("John Doe")
            .email("john.doe@amanahfi.ae")
            .tenantId(tenantId)
            .roles(Set.of("USER", "LOAN_OFFICER"))
            .permissions(Set.of("READ_LOANS", "CREATE_LOANS"))
            .authenticationMethod(AuthenticationMethod.MFA)
            .authenticated(true)
            .elevated(false)
            .authenticatedAt(Instant.now().minusSeconds(300))
            .expiresAt(Instant.now().plusSeconds(3600))
            .sessionId("session-456")
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .build();
        
        validSecurityContext = SecurityContext.builder()
            .principal(validPrincipal)
            .securityLevel(SecurityLevel.HIGH)
            .elevated(false)
            .contextId("context-789")
            .createdAt(Instant.now().minusSeconds(60))
            .lastAccessedAt(Instant.now())
            .accessCount(5L)
            .sourceIp("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .build();
    }
    
    @AfterEach
    void tearDown() {
        securityContextManager.clearContext();
        executorService.shutdown();
    }
    
    @Test
    @DisplayName("Should set and get security context correctly")
    void shouldSetAndGetSecurityContextCorrectly() {
        // Given - no context initially
        assertThat(securityContextManager.getContext()).isEmpty();
        
        // When
        securityContextManager.setContext(validSecurityContext);
        
        // Then
        Optional<SecurityContext> retrievedContext = securityContextManager.getContext();
        assertThat(retrievedContext).isPresent();
        assertThat(retrievedContext.get()).isEqualTo(validSecurityContext);
    }
    
    @Test
    @DisplayName("Should clear security context correctly")
    void shouldClearSecurityContextCorrectly() {
        // Given
        securityContextManager.setContext(validSecurityContext);
        assertThat(securityContextManager.getContext()).isPresent();
        
        // When
        securityContextManager.clearContext();
        
        // Then
        assertThat(securityContextManager.getContext()).isEmpty();
    }
    
    @Test
    @DisplayName("Should check authentication status correctly")
    void shouldCheckAuthenticationStatusCorrectly() {
        // Given - no context
        assertThat(securityContextManager.isAuthenticated()).isFalse();
        
        // Given - authenticated context
        securityContextManager.setContext(validSecurityContext);
        assertThat(securityContextManager.isAuthenticated()).isTrue();
        
        // Given - unauthenticated context
        SecurityPrincipal unauthenticatedPrincipal = validPrincipal.toBuilder()
            .authenticated(false)
            .build();
        
        SecurityContext unauthenticatedContext = validSecurityContext.toBuilder()
            .principal(unauthenticatedPrincipal)
            .build();
        
        securityContextManager.setContext(unauthenticatedContext);
        assertThat(securityContextManager.isAuthenticated()).isFalse();
    }
    
    @Test
    @DisplayName("Should delegate authorization checks to security context")
    void shouldDelegateAuthorizationChecksToSecurityContext() {
        // Given
        securityContextManager.setContext(validSecurityContext);
        
        // When & Then - role checks
        assertThat(securityContextManager.hasRole("USER")).isTrue();
        assertThat(securityContextManager.hasRole("LOAN_OFFICER")).isTrue();
        assertThat(securityContextManager.hasRole("ADMIN")).isFalse();
        
        // When & Then - permission checks
        assertThat(securityContextManager.hasPermission("READ_LOANS")).isTrue();
        assertThat(securityContextManager.hasPermission("CREATE_LOANS")).isTrue();
        assertThat(securityContextManager.hasPermission("DELETE_LOANS")).isFalse();
        
        // When & Then - multiple checks
        assertThat(securityContextManager.hasAnyRole(Set.of("USER", "ADMIN"))).isTrue();
        assertThat(securityContextManager.hasAnyPermission(Set.of("READ_LOANS", "DELETE_LOANS"))).isTrue();
    }
    
    @Test
    @DisplayName("Should handle authorization checks without context gracefully")
    void shouldHandleAuthorizationChecksWithoutContextGracefully() {
        // Given - no context
        assertThat(securityContextManager.getContext()).isEmpty();
        
        // When & Then - should return false for all checks
        assertThat(securityContextManager.hasRole("USER")).isFalse();
        assertThat(securityContextManager.hasPermission("READ_LOANS")).isFalse();
        assertThat(securityContextManager.hasAnyRole(Set.of("USER", "ADMIN"))).isFalse();
        assertThat(securityContextManager.hasAnyPermission(Set.of("READ_LOANS"))).isFalse();
        assertThat(securityContextManager.isAdmin()).isFalse();
        assertThat(securityContextManager.isSystemAdmin()).isFalse();
    }
    
    @Test
    @DisplayName("Should get user information correctly")
    void shouldGetUserInformationCorrectly() {
        // Given
        securityContextManager.setContext(validSecurityContext);
        
        // When & Then
        assertThat(securityContextManager.getCurrentUserId()).contains("user-123");
        assertThat(securityContextManager.getCurrentUsername()).contains("john.doe@amanahfi.ae");
        assertThat(securityContextManager.getCurrentTenantId()).contains(tenantId);
        
        // Given - no context
        securityContextManager.clearContext();
        
        // When & Then
        assertThat(securityContextManager.getCurrentUserId()).isEmpty();
        assertThat(securityContextManager.getCurrentUsername()).isEmpty();
        assertThat(securityContextManager.getCurrentTenantId()).isEmpty();
    }
    
    @Test
    @DisplayName("Should check security level requirements correctly")
    void shouldCheckSecurityLevelRequirementsCorrectly() {
        // Given
        securityContextManager.setContext(validSecurityContext);
        
        // When & Then
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.LOW)).isTrue();
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.MEDIUM)).isTrue();
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.HIGH)).isTrue();
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.CRITICAL)).isFalse();
        
        // Given - no context
        securityContextManager.clearContext();
        
        // When & Then - should return false without context
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.LOW)).isFalse();
    }
    
    @Test
    @DisplayName("Should require authentication correctly")
    void shouldRequireAuthenticationCorrectly() {
        // Given - no context
        assertThatThrownBy(() -> securityContextManager.requireAuthentication())
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Authentication required");
        
        // Given - unauthenticated context
        SecurityPrincipal unauthenticatedPrincipal = validPrincipal.toBuilder()
            .authenticated(false)
            .build();
        
        SecurityContext unauthenticatedContext = validSecurityContext.toBuilder()
            .principal(unauthenticatedPrincipal)
            .build();
        
        securityContextManager.setContext(unauthenticatedContext);
        
        assertThatThrownBy(() -> securityContextManager.requireAuthentication())
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Authentication required");
        
        // Given - authenticated context
        securityContextManager.setContext(validSecurityContext);
        
        // When & Then - should not throw
        assertThatNoException().isThrownBy(() -> securityContextManager.requireAuthentication());
    }
    
    @Test
    @DisplayName("Should require role correctly")
    void shouldRequireRoleCorrectly() {
        // Given - no context
        assertThatThrownBy(() -> securityContextManager.requireRole("USER"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Role 'USER' required");
        
        // Given - context without required role
        securityContextManager.setContext(validSecurityContext);
        
        assertThatThrownBy(() -> securityContextManager.requireRole("ADMIN"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Role 'ADMIN' required");
        
        // Given - context with required role
        assertThatNoException().isThrownBy(() -> securityContextManager.requireRole("USER"));
        assertThatNoException().isThrownBy(() -> securityContextManager.requireRole("LOAN_OFFICER"));
    }
    
    @Test
    @DisplayName("Should require permission correctly")
    void shouldRequirePermissionCorrectly() {
        // Given - no context
        assertThatThrownBy(() -> securityContextManager.requirePermission("READ_LOANS"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Permission 'READ_LOANS' required");
        
        // Given - context without required permission
        securityContextManager.setContext(validSecurityContext);
        
        assertThatThrownBy(() -> securityContextManager.requirePermission("DELETE_LOANS"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Permission 'DELETE_LOANS' required");
        
        // Given - context with required permission
        assertThatNoException().isThrownBy(() -> securityContextManager.requirePermission("READ_LOANS"));
        assertThatNoException().isThrownBy(() -> securityContextManager.requirePermission("CREATE_LOANS"));
    }
    
    @Test
    @DisplayName("Should require security level correctly")
    void shouldRequireSecurityLevelCorrectly() {
        // Given - no context
        assertThatThrownBy(() -> securityContextManager.requireSecurityLevel(SecurityLevel.HIGH))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Security level 'HIGH' required");
        
        // Given - context with insufficient security level
        securityContextManager.setContext(validSecurityContext);
        
        assertThatThrownBy(() -> securityContextManager.requireSecurityLevel(SecurityLevel.CRITICAL))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Security level 'CRITICAL' required");
        
        // Given - context with sufficient security level
        assertThatNoException().isThrownBy(() -> securityContextManager.requireSecurityLevel(SecurityLevel.HIGH));
        assertThatNoException().isThrownBy(() -> securityContextManager.requireSecurityLevel(SecurityLevel.MEDIUM));
        assertThatNoException().isThrownBy(() -> securityContextManager.requireSecurityLevel(SecurityLevel.LOW));
    }
    
    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void shouldBeThreadSafeForConcurrentAccess() {
        // Given - multiple threads setting different contexts
        int threadCount = 10;
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                // Create thread-specific context
                SecurityPrincipal threadPrincipal = validPrincipal.toBuilder()
                    .userId("user-" + threadId)
                    .username("user" + threadId + "@amanahfi.ae")
                    .sessionId("session-" + threadId)
                    .build();
                
                SecurityContext threadContext = validSecurityContext.toBuilder()
                    .principal(threadPrincipal)
                    .contextId("context-" + threadId)
                    .build();
                
                // Set context in this thread
                securityContextManager.setContext(threadContext);
                
                // Verify context isolation
                Optional<SecurityContext> retrievedContext = securityContextManager.getContext();
                assertThat(retrievedContext).isPresent();
                assertThat(retrievedContext.get().getPrincipal().getUserId()).isEqualTo("user-" + threadId);
                assertThat(retrievedContext.get().getContextId()).isEqualTo("context-" + threadId);
                
                // Perform multiple operations
                assertThat(securityContextManager.isAuthenticated()).isTrue();
                assertThat(securityContextManager.hasRole("USER")).isTrue();
                assertThat(securityContextManager.getCurrentUserId()).contains("user-" + threadId);
                
                // Clear context
                securityContextManager.clearContext();
                assertThat(securityContextManager.getContext()).isEmpty();
            }, executorService);
        }
        
        // When & Then - wait for all threads to complete
        assertThatNoException().isThrownBy(() -> 
            CompletableFuture.allOf(futures).join());
    }
    
    @Test
    @DisplayName("Should maintain context isolation between threads")
    void shouldMaintainContextIsolationBetweenThreads() {
        // Given - set context in main thread
        securityContextManager.setContext(validSecurityContext);
        assertThat(securityContextManager.getCurrentUserId()).contains("user-123");
        
        // When - different context set in separate thread
        CompletableFuture<Void> threadTask = CompletableFuture.runAsync(() -> {
            SecurityPrincipal otherPrincipal = validPrincipal.toBuilder()
                .userId("other-user-456")
                .username("other.user@amanahfi.ae")
                .build();
            
            SecurityContext otherContext = validSecurityContext.toBuilder()
                .principal(otherPrincipal)
                .contextId("other-context-789")
                .build();
            
            securityContextManager.setContext(otherContext);
            
            // Verify different context in this thread
            assertThat(securityContextManager.getCurrentUserId()).contains("other-user-456");
            assertThat(securityContextManager.getContext().get().getContextId()).isEqualTo("other-context-789");
            
        }, executorService);
        
        threadTask.join();
        
        // Then - main thread context should be unchanged
        assertThat(securityContextManager.getCurrentUserId()).contains("user-123");
        assertThat(securityContextManager.getContext().get().getContextId()).isEqualTo("context-789");
    }
    
    @Test
    @DisplayName("Should handle context expiration correctly")
    void shouldHandleContextExpirationCorrectly() {
        // Given - expired context
        SecurityPrincipal expiredPrincipal = validPrincipal.toBuilder()
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();
        
        SecurityContext expiredContext = validSecurityContext.toBuilder()
            .principal(expiredPrincipal)
            .build();
        
        securityContextManager.setContext(expiredContext);
        
        // When & Then - expired context should not be authenticated
        assertThat(securityContextManager.isAuthenticated()).isFalse();
        
        // Security checks should fail
        assertThatThrownBy(() -> securityContextManager.requireAuthentication())
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Authentication required");
    }
    
    @Test
    @DisplayName("Should validate tenant isolation")
    void shouldValidateTenantIsolation() {
        // Given - different tenant contexts
        TenantId tenant1 = TenantId.generate();
        TenantId tenant2 = TenantId.generate();
        
        SecurityPrincipal tenant1Principal = validPrincipal.toBuilder()
            .tenantId(tenant1)
            .userId("user-tenant1")
            .build();
        
        SecurityContext tenant1Context = validSecurityContext.toBuilder()
            .principal(tenant1Principal)
            .build();
        
        // When
        securityContextManager.setContext(tenant1Context);
        
        // Then
        assertThat(securityContextManager.getCurrentTenantId()).contains(tenant1);
        assertThat(securityContextManager.getCurrentTenantId()).doesNotContain(tenant2);
        
        // Verify tenant-specific operations
        assertThat(securityContextManager.belongsToTenant(tenant1)).isTrue();
        assertThat(securityContextManager.belongsToTenant(tenant2)).isFalse();
    }
    
    @Test
    @DisplayName("Should handle Islamic finance authorization correctly")
    void shouldHandleIslamicFinanceAuthorizationCorrectly() {
        // Given - Islamic finance permissions
        SecurityPrincipal islamicFinancePrincipal = validPrincipal.toBuilder()
            .permissions(Set.of("READ_LOANS", "CREATE_LOANS", "ISLAMIC_FINANCE_OPERATIONS"))
            .build();
        
        SecurityContext islamicFinanceContext = validSecurityContext.toBuilder()
            .principal(islamicFinancePrincipal)
            .build();
        
        securityContextManager.setContext(islamicFinanceContext);
        
        // When & Then
        assertThat(securityContextManager.hasPermission("ISLAMIC_FINANCE_OPERATIONS")).isTrue();
        assertThat(securityContextManager.isAuthorizedForIslamicFinance()).isTrue();
        
        assertThatNoException().isThrownBy(() -> 
            securityContextManager.requirePermission("ISLAMIC_FINANCE_OPERATIONS"));
    }
    
    @Test
    @DisplayName("Should handle CBDC authorization correctly")
    void shouldHandleCBDCAuthorizationCorrectly() {
        // Given - CBDC permissions and critical security level
        SecurityPrincipal cbdcPrincipal = validPrincipal.toBuilder()
            .permissions(Set.of("READ_LOANS", "CBDC_OPERATIONS", "DIGITAL_DIRHAM_ACCESS"))
            .roles(Set.of("USER", "CBDC_OPERATOR"))
            .build();
        
        SecurityContext cbdcContext = validSecurityContext.toBuilder()
            .principal(cbdcPrincipal)
            .securityLevel(SecurityLevel.CRITICAL)
            .build();
        
        securityContextManager.setContext(cbdcContext);
        
        // When & Then
        assertThat(securityContextManager.hasPermission("CBDC_OPERATIONS")).isTrue();
        assertThat(securityContextManager.hasRole("CBDC_OPERATOR")).isTrue();
        assertThat(securityContextManager.meetsSecurityLevel(SecurityLevel.CRITICAL)).isTrue();
        assertThat(securityContextManager.isAuthorizedForCBDC()).isTrue();
        
        assertThatNoException().isThrownBy(() -> {
            securityContextManager.requirePermission("CBDC_OPERATIONS");
            securityContextManager.requireRole("CBDC_OPERATOR");
            securityContextManager.requireSecurityLevel(SecurityLevel.CRITICAL);
        });
    }
}