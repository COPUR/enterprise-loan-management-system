package com.amanahfi.platform.security.domain;

import com.amanahfi.platform.tenant.domain.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityPrincipal")
class SecurityPrincipalTest {
    
    private SecurityPrincipal validPrincipal;
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
        tenantId = TenantId.generate();
        
        validPrincipal = SecurityPrincipal.builder()
            .userId("user-123")
            .username("john.doe@bank.ae")
            .fullName("John Doe")
            .email("john.doe@bank.ae")
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
    }
    
    @Test
    @DisplayName("Should create valid security principal")
    void shouldCreateValidSecurityPrincipal() {
        // When & Then
        assertThat(validPrincipal.getUserId()).isEqualTo("user-123");
        assertThat(validPrincipal.getUsername()).isEqualTo("john.doe@bank.ae");
        assertThat(validPrincipal.getTenantId()).isEqualTo(tenantId);
        assertThat(validPrincipal.isAuthenticated()).isTrue();
        assertThat(validPrincipal.isValid()).isTrue();
        assertThat(validPrincipal.isExpired()).isFalse();
    }
    
    @Test
    @DisplayName("Should validate security principal successfully")
    void shouldValidateSecurityPrincipalSuccessfully() {
        // When & Then
        assertThatNoException().isThrownBy(() -> validPrincipal.validate());
    }
    
    @Test
    @DisplayName("Should check role membership correctly")
    void shouldCheckRoleMembershipCorrectly() {
        // When & Then
        assertThat(validPrincipal.hasRole("USER")).isTrue();
        assertThat(validPrincipal.hasRole("LOAN_OFFICER")).isTrue();
        assertThat(validPrincipal.hasRole("ADMIN")).isFalse();
        assertThat(validPrincipal.hasRole("SYSTEM_ADMIN")).isFalse();
    }
    
    @Test
    @DisplayName("Should check multiple role membership correctly")
    void shouldCheckMultipleRoleMembershipCorrectly() {
        // Given
        Set<String> requiredRoles = Set.of("USER", "ADMIN");
        Set<String> availableRoles = Set.of("USER", "LOAN_OFFICER");
        Set<String> unavailableRoles = Set.of("ADMIN", "SYSTEM_ADMIN");
        
        // When & Then
        assertThat(validPrincipal.hasAnyRole(requiredRoles)).isTrue();
        assertThat(validPrincipal.hasAnyRole(availableRoles)).isTrue();
        assertThat(validPrincipal.hasAnyRole(unavailableRoles)).isFalse();
    }
    
    @Test
    @DisplayName("Should check permission correctly")
    void shouldCheckPermissionCorrectly() {
        // When & Then
        assertThat(validPrincipal.hasPermission("READ_LOANS")).isTrue();
        assertThat(validPrincipal.hasPermission("CREATE_LOANS")).isTrue();
        assertThat(validPrincipal.hasPermission("DELETE_LOANS")).isFalse();
        assertThat(validPrincipal.hasPermission("ADMIN_ACCESS")).isFalse();
    }
    
    @Test
    @DisplayName("Should check multiple permission correctly")
    void shouldCheckMultiplePermissionCorrectly() {
        // Given
        Set<String> requiredPermissions = Set.of("READ_LOANS", "ADMIN_ACCESS");
        Set<String> availablePermissions = Set.of("READ_LOANS", "CREATE_LOANS");
        Set<String> unavailablePermissions = Set.of("DELETE_LOANS", "ADMIN_ACCESS");
        
        // When & Then
        assertThat(validPrincipal.hasAnyPermission(requiredPermissions)).isTrue();
        assertThat(validPrincipal.hasAnyPermission(availablePermissions)).isTrue();
        assertThat(validPrincipal.hasAnyPermission(unavailablePermissions)).isFalse();
    }
    
    @Test
    @DisplayName("Should check admin status correctly")
    void shouldCheckAdminStatusCorrectly() {
        // Given - regular user
        assertThat(validPrincipal.isAdmin()).isFalse();
        assertThat(validPrincipal.isSystemAdmin()).isFalse();
        
        // Given - admin user
        SecurityPrincipal adminPrincipal = validPrincipal.toBuilder()
            .roles(Set.of("USER", "ADMIN"))
            .build();
        
        // When & Then
        assertThat(adminPrincipal.isAdmin()).isTrue();
        assertThat(adminPrincipal.isSystemAdmin()).isFalse();
        
        // Given - system admin user
        SecurityPrincipal systemAdminPrincipal = validPrincipal.toBuilder()
            .roles(Set.of("USER", "SYSTEM_ADMIN"))
            .build();
        
        // When & Then
        assertThat(systemAdminPrincipal.isAdmin()).isFalse();
        assertThat(systemAdminPrincipal.isSystemAdmin()).isTrue();
        
        // Given - tenant admin user
        SecurityPrincipal tenantAdminPrincipal = validPrincipal.toBuilder()
            .roles(Set.of("USER", "TENANT_ADMIN"))
            .build();
        
        // When & Then
        assertThat(tenantAdminPrincipal.isAdmin()).isTrue();
        assertThat(tenantAdminPrincipal.isSystemAdmin()).isFalse();
    }
    
    @Test
    @DisplayName("Should check expiration correctly")
    void shouldCheckExpirationCorrectly() {
        // Given - valid principal
        assertThat(validPrincipal.isExpired()).isFalse();
        assertThat(validPrincipal.isValid()).isTrue();
        
        // Given - expired principal
        SecurityPrincipal expiredPrincipal = validPrincipal.toBuilder()
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();
        
        // When & Then
        assertThat(expiredPrincipal.isExpired()).isTrue();
        assertThat(expiredPrincipal.isValid()).isFalse();
        
        // Given - unauthenticated principal
        SecurityPrincipal unauthenticatedPrincipal = validPrincipal.toBuilder()
            .authenticated(false)
            .build();
        
        // When & Then
        assertThat(unauthenticatedPrincipal.isValid()).isFalse();
    }
    
    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // When & Then - null user ID
        SecurityPrincipal invalidPrincipal = validPrincipal.toBuilder()
            .userId(null)
            .build();
        
        assertThatThrownBy(() -> invalidPrincipal.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID cannot be null or empty");
        
        // When & Then - empty username
        invalidPrincipal = validPrincipal.toBuilder()
            .username("")
            .build();
        
        assertThatThrownBy(() -> invalidPrincipal.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username cannot be null or empty");
        
        // When & Then - null tenant ID
        invalidPrincipal = validPrincipal.toBuilder()
            .tenantId(null)
            .build();
        
        assertThatThrownBy(() -> invalidPrincipal.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant ID cannot be null");
        
        // When & Then - null authentication timestamp
        invalidPrincipal = validPrincipal.toBuilder()
            .authenticatedAt(null)
            .build();
        
        assertThatThrownBy(() -> invalidPrincipal.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Authentication timestamp cannot be null");
        
        // When & Then - null session ID
        invalidPrincipal = validPrincipal.toBuilder()
            .sessionId(null)
            .build();
        
        assertThatThrownBy(() -> invalidPrincipal.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Session ID cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should handle null roles and permissions")
    void shouldHandleNullRolesAndPermissions() {
        // Given
        SecurityPrincipal principalWithNulls = validPrincipal.toBuilder()
            .roles(null)
            .permissions(null)
            .build();
        
        // When & Then
        assertThat(principalWithNulls.hasRole("USER")).isFalse();
        assertThat(principalWithNulls.hasPermission("READ_LOANS")).isFalse();
        assertThat(principalWithNulls.hasAnyRole(Set.of("USER"))).isFalse();
        assertThat(principalWithNulls.hasAnyPermission(Set.of("READ_LOANS"))).isFalse();
        assertThat(principalWithNulls.isAdmin()).isFalse();
        assertThat(principalWithNulls.isSystemAdmin()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle empty roles and permissions")
    void shouldHandleEmptyRolesAndPermissions() {
        // Given
        SecurityPrincipal principalWithEmpty = validPrincipal.toBuilder()
            .roles(Set.of())
            .permissions(Set.of())
            .build();
        
        // When & Then
        assertThat(principalWithEmpty.hasRole("USER")).isFalse();
        assertThat(principalWithEmpty.hasPermission("READ_LOANS")).isFalse();
        assertThat(principalWithEmpty.hasAnyRole(Set.of("USER"))).isFalse();
        assertThat(principalWithEmpty.hasAnyPermission(Set.of("READ_LOANS"))).isFalse();
        assertThat(principalWithEmpty.isAdmin()).isFalse();
        assertThat(principalWithEmpty.isSystemAdmin()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle strong authentication methods")
    void shouldHandleStrongAuthenticationMethods() {
        // Given - MFA authentication
        SecurityPrincipal mfaPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.MFA)
            .build();
        
        assertThat(mfaPrincipal.getAuthenticationMethod().isStrong()).isTrue();
        
        // Given - Certificate authentication
        SecurityPrincipal certPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.CLIENT_CERTIFICATE)
            .build();
        
        assertThat(certPrincipal.getAuthenticationMethod().isStrong()).isTrue();
        
        // Given - Password authentication
        SecurityPrincipal passwordPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.PASSWORD)
            .build();
        
        assertThat(passwordPrincipal.getAuthenticationMethod().isStrong()).isFalse();
    }
}