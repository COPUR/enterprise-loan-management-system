package com.amanahfi.platform.security.domain;

import com.amanahfi.platform.tenant.domain.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for SecurityContext
 */
@DisplayName("SecurityContext")
class SecurityContextTest {
    
    private SecurityContext validSecurityContext;
    private SecurityPrincipal validPrincipal;
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
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
    
    @Test
    @DisplayName("Should create valid security context")
    void shouldCreateValidSecurityContext() {
        // When & Then
        assertThat(validSecurityContext.getPrincipal()).isEqualTo(validPrincipal);
        assertThat(validSecurityContext.getSecurityLevel()).isEqualTo(SecurityLevel.HIGH);
        assertThat(validSecurityContext.isElevated()).isFalse();
        assertThat(validSecurityContext.getContextId()).isEqualTo("context-789");
        assertThat(validSecurityContext.getAccessCount()).isEqualTo(5L);
        assertThat(validSecurityContext.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should validate security context successfully")
    void shouldValidateSecurityContextSuccessfully() {
        // When & Then
        assertThatNoException().isThrownBy(() -> validSecurityContext.validate());
    }
    
    @Test
    @DisplayName("Should check authentication status correctly")
    void shouldCheckAuthenticationStatusCorrectly() {
        // When & Then
        assertThat(validSecurityContext.isAuthenticated()).isTrue();
        assertThat(validSecurityContext.isValid()).isTrue();
        
        // Given - unauthenticated principal
        SecurityPrincipal unauthenticatedPrincipal = validPrincipal.toBuilder()
            .authenticated(false)
            .build();
        
        SecurityContext unauthenticatedContext = validSecurityContext.toBuilder()
            .principal(unauthenticatedPrincipal)
            .build();
        
        // When & Then
        assertThat(unauthenticatedContext.isAuthenticated()).isFalse();
        assertThat(unauthenticatedContext.isValid()).isFalse();
    }
    
    @Test
    @DisplayName("Should check expiration correctly")
    void shouldCheckExpirationCorrectly() {
        // Given - valid context
        assertThat(validSecurityContext.isExpired()).isFalse();
        assertThat(validSecurityContext.isValid()).isTrue();
        
        // Given - expired principal
        SecurityPrincipal expiredPrincipal = validPrincipal.toBuilder()
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();
        
        SecurityContext expiredContext = validSecurityContext.toBuilder()
            .principal(expiredPrincipal)
            .build();
        
        // When & Then
        assertThat(expiredContext.isExpired()).isTrue();
        assertThat(expiredContext.isValid()).isFalse();
    }
    
    @Test
    @DisplayName("Should check elevated privileges correctly")
    void shouldCheckElevatedPrivilegesCorrectly() {
        // Given - regular context
        assertThat(validSecurityContext.isElevated()).isFalse();
        assertThat(validSecurityContext.hasElevatedPrivileges()).isFalse();
        
        // Given - elevated context
        SecurityContext elevatedContext = validSecurityContext.toBuilder()
            .elevated(true)
            .build();
        
        // When & Then
        assertThat(elevatedContext.isElevated()).isTrue();
        assertThat(elevatedContext.hasElevatedPrivileges()).isTrue();
        
        // Given - elevated principal
        SecurityPrincipal elevatedPrincipal = validPrincipal.toBuilder()
            .elevated(true)
            .build();
        
        SecurityContext contextWithElevatedPrincipal = validSecurityContext.toBuilder()
            .principal(elevatedPrincipal)
            .build();
        
        // When & Then
        assertThat(contextWithElevatedPrincipal.hasElevatedPrivileges()).isTrue();
    }
    
    @Test
    @DisplayName("Should check security level requirements correctly")
    void shouldCheckSecurityLevelRequirementsCorrectly() {
        // When & Then
        assertThat(validSecurityContext.meetsSecurityLevel(SecurityLevel.LOW)).isTrue();
        assertThat(validSecurityContext.meetsSecurityLevel(SecurityLevel.MEDIUM)).isTrue();
        assertThat(validSecurityContext.meetsSecurityLevel(SecurityLevel.HIGH)).isTrue();
        assertThat(validSecurityContext.meetsSecurityLevel(SecurityLevel.CRITICAL)).isFalse();
        
        // Given - critical security level context
        SecurityContext criticalContext = validSecurityContext.toBuilder()
            .securityLevel(SecurityLevel.CRITICAL)
            .build();
        
        // When & Then
        assertThat(criticalContext.meetsSecurityLevel(SecurityLevel.LOW)).isTrue();
        assertThat(criticalContext.meetsSecurityLevel(SecurityLevel.MEDIUM)).isTrue();
        assertThat(criticalContext.meetsSecurityLevel(SecurityLevel.HIGH)).isTrue();
        assertThat(criticalContext.meetsSecurityLevel(SecurityLevel.CRITICAL)).isTrue();
    }
    
    @Test
    @DisplayName("Should delegate authorization checks to principal")
    void shouldDelegateAuthorizationChecksToPrincipal() {
        // When & Then - role checks
        assertThat(validSecurityContext.hasRole("USER")).isTrue();
        assertThat(validSecurityContext.hasRole("LOAN_OFFICER")).isTrue();
        assertThat(validSecurityContext.hasRole("ADMIN")).isFalse();
        
        // When & Then - permission checks
        assertThat(validSecurityContext.hasPermission("READ_LOANS")).isTrue();
        assertThat(validSecurityContext.hasPermission("CREATE_LOANS")).isTrue();
        assertThat(validSecurityContext.hasPermission("DELETE_LOANS")).isFalse();
        
        // When & Then - multiple role checks
        assertThat(validSecurityContext.hasAnyRole(Set.of("USER", "ADMIN"))).isTrue();
        assertThat(validSecurityContext.hasAnyRole(Set.of("ADMIN", "SYSTEM_ADMIN"))).isFalse();
        
        // When & Then - multiple permission checks
        assertThat(validSecurityContext.hasAnyPermission(Set.of("READ_LOANS", "DELETE_LOANS"))).isTrue();
        assertThat(validSecurityContext.hasAnyPermission(Set.of("DELETE_LOANS", "ADMIN_ACCESS"))).isFalse();
    }
    
    @Test
    @DisplayName("Should track access correctly")
    void shouldTrackAccessCorrectly() {
        // Given
        Instant beforeAccess = validSecurityContext.getLastAccessedAt();
        Long beforeCount = validSecurityContext.getAccessCount();
        
        // When
        SecurityContext accessedContext = validSecurityContext.recordAccess();
        
        // Then
        assertThat(accessedContext.getLastAccessedAt()).isAfter(beforeAccess);
        assertThat(accessedContext.getAccessCount()).isEqualTo(beforeCount + 1);
        assertThat(accessedContext.getContextId()).isEqualTo(validSecurityContext.getContextId());
    }
    
    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // When & Then - null principal
        SecurityContext invalidContext = validSecurityContext.toBuilder()
            .principal(null)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Principal cannot be null");
        
        // When & Then - null security level
        invalidContext = validSecurityContext.toBuilder()
            .securityLevel(null)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Security level cannot be null");
        
        // When & Then - null context ID
        invalidContext = validSecurityContext.toBuilder()
            .contextId(null)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Context ID cannot be null or empty");
        
        // When & Then - empty context ID
        invalidContext = validSecurityContext.toBuilder()
            .contextId("")
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Context ID cannot be null or empty");
        
        // When & Then - null created at
        invalidContext = validSecurityContext.toBuilder()
            .createdAt(null)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Created at timestamp cannot be null");
        
        // When & Then - null last accessed at
        invalidContext = validSecurityContext.toBuilder()
            .lastAccessedAt(null)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Last accessed at timestamp cannot be null");
    }
    
    @Test
    @DisplayName("Should validate timestamp consistency")
    void shouldValidateTimestampConsistency() {
        // When & Then - last accessed before created
        Instant now = Instant.now();
        
        SecurityContext invalidContext = validSecurityContext.toBuilder()
            .createdAt(now)
            .lastAccessedAt(now.minusSeconds(60))
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Last accessed at cannot be before created at");
    }
    
    @Test
    @DisplayName("Should validate access count")
    void shouldValidateAccessCount() {
        // When & Then - negative access count
        SecurityContext invalidContext = validSecurityContext.toBuilder()
            .accessCount(-1L)
            .build();
        
        assertThatThrownBy(() -> invalidContext.validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access count cannot be negative");
    }
    
    @Test
    @DisplayName("Should check admin status correctly")
    void shouldCheckAdminStatusCorrectly() {
        // Given - regular user
        assertThat(validSecurityContext.isAdmin()).isFalse();
        assertThat(validSecurityContext.isSystemAdmin()).isFalse();
        
        // Given - admin user
        SecurityPrincipal adminPrincipal = validPrincipal.toBuilder()
            .roles(Set.of("USER", "ADMIN"))
            .build();
        
        SecurityContext adminContext = validSecurityContext.toBuilder()
            .principal(adminPrincipal)
            .build();
        
        // When & Then
        assertThat(adminContext.isAdmin()).isTrue();
        assertThat(adminContext.isSystemAdmin()).isFalse();
        
        // Given - system admin user
        SecurityPrincipal systemAdminPrincipal = validPrincipal.toBuilder()
            .roles(Set.of("USER", "SYSTEM_ADMIN"))
            .build();
        
        SecurityContext systemAdminContext = validSecurityContext.toBuilder()
            .principal(systemAdminPrincipal)
            .build();
        
        // When & Then
        assertThat(systemAdminContext.isAdmin()).isFalse();
        assertThat(systemAdminContext.isSystemAdmin()).isTrue();
    }
    
    @Test
    @DisplayName("Should get tenant information correctly")
    void shouldGetTenantInformationCorrectly() {
        // When & Then
        assertThat(validSecurityContext.getTenantId()).isEqualTo(tenantId);
        assertThat(validSecurityContext.getUserId()).isEqualTo("user-123");
        assertThat(validSecurityContext.getUsername()).isEqualTo("john.doe@amanahfi.ae");
    }
    
    @Test
    @DisplayName("Should handle strong authentication methods correctly")
    void shouldHandleStrongAuthenticationMethodsCorrectly() {
        // Given - MFA authentication
        SecurityPrincipal mfaPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.MFA)
            .build();
        
        SecurityContext mfaContext = validSecurityContext.toBuilder()
            .principal(mfaPrincipal)
            .build();
        
        assertThat(mfaContext.hasStrongAuthentication()).isTrue();
        
        // Given - Certificate authentication
        SecurityPrincipal certPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.CLIENT_CERTIFICATE)
            .build();
        
        SecurityContext certContext = validSecurityContext.toBuilder()
            .principal(certPrincipal)
            .build();
        
        assertThat(certContext.hasStrongAuthentication()).isTrue();
        
        // Given - Password authentication
        SecurityPrincipal passwordPrincipal = validPrincipal.toBuilder()
            .authenticationMethod(AuthenticationMethod.PASSWORD)
            .build();
        
        SecurityContext passwordContext = validSecurityContext.toBuilder()
            .principal(passwordPrincipal)
            .build();
        
        assertThat(passwordContext.hasStrongAuthentication()).isFalse();
    }
    
    @Test
    @DisplayName("Should check Islamic finance operation authorization")
    void shouldCheckIslamicFinanceOperationAuthorization() {
        // Given - Islamic finance permissions
        SecurityPrincipal islamicFinancePrincipal = validPrincipal.toBuilder()
            .permissions(Set.of("READ_LOANS", "CREATE_LOANS", "ISLAMIC_FINANCE_OPERATIONS"))
            .build();
        
        SecurityContext islamicFinanceContext = validSecurityContext.toBuilder()
            .principal(islamicFinancePrincipal)
            .build();
        
        // When & Then
        assertThat(islamicFinanceContext.hasPermission("ISLAMIC_FINANCE_OPERATIONS")).isTrue();
        assertThat(islamicFinanceContext.isAuthorizedForIslamicFinance()).isTrue();
        
        // Given - context without Islamic finance permissions
        assertThat(validSecurityContext.hasPermission("ISLAMIC_FINANCE_OPERATIONS")).isFalse();
        assertThat(validSecurityContext.isAuthorizedForIslamicFinance()).isFalse();
    }
    
    @Test
    @DisplayName("Should check CBDC operation authorization")
    void shouldCheckCBDCOperationAuthorization() {
        // Given - CBDC permissions
        SecurityPrincipal cbdcPrincipal = validPrincipal.toBuilder()
            .permissions(Set.of("READ_LOANS", "CREATE_LOANS", "CBDC_OPERATIONS", "DIGITAL_DIRHAM_ACCESS"))
            .roles(Set.of("USER", "LOAN_OFFICER", "CBDC_OPERATOR"))
            .build();
        
        SecurityContext cbdcContext = validSecurityContext.toBuilder()
            .principal(cbdcPrincipal)
            .securityLevel(SecurityLevel.CRITICAL)
            .build();
        
        // When & Then
        assertThat(cbdcContext.hasPermission("CBDC_OPERATIONS")).isTrue();
        assertThat(cbdcContext.hasPermission("DIGITAL_DIRHAM_ACCESS")).isTrue();
        assertThat(cbdcContext.hasRole("CBDC_OPERATOR")).isTrue();
        assertThat(cbdcContext.isAuthorizedForCBDC()).isTrue();
        
        // Given - context without CBDC permissions
        assertThat(validSecurityContext.isAuthorizedForCBDC()).isFalse();
    }
}