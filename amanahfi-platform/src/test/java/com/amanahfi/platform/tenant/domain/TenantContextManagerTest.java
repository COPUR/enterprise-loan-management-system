package com.amanahfi.platform.tenant.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TenantContextManager")
class TenantContextManagerTest {
    
    private TenantContextManager tenantContextManager;
    private TenantContext validContext;
    private TenantId tenantId;
    
    @BeforeEach
    void setUp() {
        tenantContextManager = new TenantContextManager();
        tenantId = TenantId.generate();
        
        validContext = TenantContext.builder()
            .tenantId(tenantId)
            .userId("user-123")
            .sessionId("session-456")
            .correlationId("correlation-789")
            .requestTime(Instant.now())
            .clientIp("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .language("en")
            .currency("AED")
            .timezone("Asia/Dubai")
            .isAdmin(false)
            .hasElevatedPrivileges(false)
            .attributes(Map.of("region", "MENAT"))
            .build();
    }
    
    @AfterEach
    void tearDown() {
        tenantContextManager.clearTenantContext();
    }
    
    @Test
    @DisplayName("Should set and get tenant context")
    void shouldSetAndGetTenantContext() {
        // When
        tenantContextManager.setTenantContext(validContext);
        
        // Then
        Optional<TenantContext> retrievedContext = tenantContextManager.getCurrentTenantContext();
        assertThat(retrievedContext).isPresent();
        assertThat(retrievedContext.get()).isEqualTo(validContext);
    }
    
    @Test
    @DisplayName("Should get current tenant ID")
    void shouldGetCurrentTenantId() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<TenantId> currentTenantId = tenantContextManager.getCurrentTenantId();
        
        // Then
        assertThat(currentTenantId).isPresent();
        assertThat(currentTenantId.get()).isEqualTo(tenantId);
    }
    
    @Test
    @DisplayName("Should get current user ID")
    void shouldGetCurrentUserId() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<String> currentUserId = tenantContextManager.getCurrentUserId();
        
        // Then
        assertThat(currentUserId).isPresent();
        assertThat(currentUserId.get()).isEqualTo("user-123");
    }
    
    @Test
    @DisplayName("Should get current correlation ID")
    void shouldGetCurrentCorrelationId() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<String> currentCorrelationId = tenantContextManager.getCurrentCorrelationId();
        
        // Then
        assertThat(currentCorrelationId).isPresent();
        assertThat(currentCorrelationId.get()).isEqualTo("correlation-789");
    }
    
    @Test
    @DisplayName("Should check if current user is admin")
    void shouldCheckIfCurrentUserIsAdmin() {
        // Given
        TenantContext adminContext = TenantContext.builder()
            .tenantId(tenantId)
            .userId("admin-123")
            .sessionId("session-456")
            .correlationId("correlation-789")
            .requestTime(Instant.now())
            .clientIp("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .language("en")
            .currency("AED")
            .timezone("Asia/Dubai")
            .isAdmin(true)
            .hasElevatedPrivileges(true)
            .attributes(Map.of("region", "MENAT"))
            .build();
        
        tenantContextManager.setTenantContext(adminContext);
        
        // When & Then
        assertThat(tenantContextManager.isCurrentUserAdmin()).isTrue();
        assertThat(tenantContextManager.hasElevatedPrivileges()).isTrue();
    }
    
    @Test
    @DisplayName("Should check if current user is not admin")
    void shouldCheckIfCurrentUserIsNotAdmin() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When & Then
        assertThat(tenantContextManager.isCurrentUserAdmin()).isFalse();
        assertThat(tenantContextManager.hasElevatedPrivileges()).isFalse();
    }
    
    @Test
    @DisplayName("Should get current language")
    void shouldGetCurrentLanguage() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<String> currentLanguage = tenantContextManager.getCurrentLanguage();
        
        // Then
        assertThat(currentLanguage).isPresent();
        assertThat(currentLanguage.get()).isEqualTo("en");
    }
    
    @Test
    @DisplayName("Should get current currency")
    void shouldGetCurrentCurrency() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<String> currentCurrency = tenantContextManager.getCurrentCurrency();
        
        // Then
        assertThat(currentCurrency).isPresent();
        assertThat(currentCurrency.get()).isEqualTo("AED");
    }
    
    @Test
    @DisplayName("Should get current timezone")
    void shouldGetCurrentTimezone() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When
        Optional<String> currentTimezone = tenantContextManager.getCurrentTimezone();
        
        // Then
        assertThat(currentTimezone).isPresent();
        assertThat(currentTimezone.get()).isEqualTo("Asia/Dubai");
    }
    
    @Test
    @DisplayName("Should clear tenant context")
    void shouldClearTenantContext() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        assertThat(tenantContextManager.getCurrentTenantContext()).isPresent();
        
        // When
        tenantContextManager.clearTenantContext();
        
        // Then
        assertThat(tenantContextManager.getCurrentTenantContext()).isEmpty();
    }
    
    @Test
    @DisplayName("Should execute with tenant context")
    void shouldExecuteWithTenantContext() {
        // Given
        String result = "test-result";
        
        // When
        String actualResult = tenantContextManager.executeWithTenantContext(validContext, () -> {
            // Verify context is set during execution
            assertThat(tenantContextManager.getCurrentTenantContext()).isPresent();
            assertThat(tenantContextManager.getCurrentTenantId()).contains(tenantId);
            return result;
        });
        
        // Then
        assertThat(actualResult).isEqualTo(result);
        assertThat(tenantContextManager.getCurrentTenantContext()).isEmpty();
    }
    
    @Test
    @DisplayName("Should execute with tenant context and restore previous")
    void shouldExecuteWithTenantContextAndRestorePrevious() {
        // Given
        TenantContext originalContext = validContext;
        TenantContext temporaryContext = TenantContext.builder()
            .tenantId(TenantId.generate())
            .userId("temp-user")
            .sessionId("temp-session")
            .correlationId("temp-correlation")
            .requestTime(Instant.now())
            .clientIp("192.168.1.2")
            .userAgent("Mozilla/5.0")
            .language("ar")
            .currency("SAR")
            .timezone("Asia/Riyadh")
            .isAdmin(true)
            .hasElevatedPrivileges(true)
            .attributes(Map.of("region", "KSA"))
            .build();
        
        tenantContextManager.setTenantContext(originalContext);
        
        // When
        tenantContextManager.executeWithTenantContext(temporaryContext, () -> {
            // Verify temporary context is set
            assertThat(tenantContextManager.getCurrentTenantContext()).isPresent();
            assertThat(tenantContextManager.getCurrentTenantContext().get()).isEqualTo(temporaryContext);
        });
        
        // Then
        assertThat(tenantContextManager.getCurrentTenantContext()).isPresent();
        assertThat(tenantContextManager.getCurrentTenantContext().get()).isEqualTo(originalContext);
    }
    
    @Test
    @DisplayName("Should execute with tenant context (no return value)")
    void shouldExecuteWithTenantContextNoReturn() {
        // Given
        final boolean[] executed = {false};
        
        // When
        tenantContextManager.executeWithTenantContext(validContext, () -> {
            // Verify context is set during execution
            assertThat(tenantContextManager.getCurrentTenantContext()).isPresent();
            assertThat(tenantContextManager.getCurrentTenantId()).contains(tenantId);
            executed[0] = true;
        });
        
        // Then
        assertThat(executed[0]).isTrue();
        assertThat(tenantContextManager.getCurrentTenantContext()).isEmpty();
    }
    
    @Test
    @DisplayName("Should ensure tenant context exists")
    void shouldEnsureTenantContextExists() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When & Then
        assertThatNoException().isThrownBy(() -> tenantContextManager.ensureTenantContext());
    }
    
    @Test
    @DisplayName("Should throw exception when ensuring tenant context without context")
    void shouldThrowExceptionWhenEnsuringTenantContextWithoutContext() {
        // When & Then
        assertThatThrownBy(() -> tenantContextManager.ensureTenantContext())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No tenant context set for current thread");
    }
    
    @Test
    @DisplayName("Should ensure tenant context for specific tenant")
    void shouldEnsureTenantContextForSpecificTenant() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        
        // When & Then
        assertThatNoException().isThrownBy(() -> tenantContextManager.ensureTenantContext(tenantId));
    }
    
    @Test
    @DisplayName("Should throw exception when ensuring tenant context for wrong tenant")
    void shouldThrowExceptionWhenEnsuringTenantContextForWrongTenant() {
        // Given
        tenantContextManager.setTenantContext(validContext);
        TenantId wrongTenantId = TenantId.generate();
        
        // When & Then
        assertThatThrownBy(() -> tenantContextManager.ensureTenantContext(wrongTenantId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant context mismatch");
    }
    
    @Test
    @DisplayName("Should handle null tenant context gracefully")
    void shouldHandleNullTenantContextGracefully() {
        // When
        tenantContextManager.setTenantContext(null);
        
        // Then
        assertThat(tenantContextManager.getCurrentTenantContext()).isEmpty();
    }
    
    @Test
    @DisplayName("Should return empty optionals when no context is set")
    void shouldReturnEmptyOptionalsWhenNoContextIsSet() {
        // When & Then
        assertThat(tenantContextManager.getCurrentTenantContext()).isEmpty();
        assertThat(tenantContextManager.getCurrentTenantId()).isEmpty();
        assertThat(tenantContextManager.getCurrentUserId()).isEmpty();
        assertThat(tenantContextManager.getCurrentCorrelationId()).isEmpty();
        assertThat(tenantContextManager.getCurrentLanguage()).isEmpty();
        assertThat(tenantContextManager.getCurrentCurrency()).isEmpty();
        assertThat(tenantContextManager.getCurrentTimezone()).isEmpty();
        assertThat(tenantContextManager.isCurrentUserAdmin()).isFalse();
        assertThat(tenantContextManager.hasElevatedPrivileges()).isFalse();
    }
}