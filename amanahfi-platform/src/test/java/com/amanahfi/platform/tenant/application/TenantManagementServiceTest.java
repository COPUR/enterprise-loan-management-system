package com.amanahfi.platform.tenant.application;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.shared.events.DomainEventPublisher;
import com.amanahfi.platform.tenant.domain.*;
import com.amanahfi.platform.tenant.domain.events.TenantCreatedEvent;
import com.amanahfi.platform.tenant.port.in.*;
import com.amanahfi.platform.tenant.port.out.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantManagementService")
class TenantManagementServiceTest {
    
    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @Mock
    private TenantContextManager tenantContextManager;
    
    @InjectMocks
    private TenantManagementService tenantManagementService;
    
    private TenantConfiguration validConfiguration;
    private CreateTenantCommand createTenantCommand;
    private TenantId tenantId;
    private Tenant tenant;
    
    @BeforeEach
    void setUp() {
        tenantId = TenantId.generate();
        
        validConfiguration = TenantConfiguration.builder()
            .primaryJurisdiction(Jurisdiction.UAE)
            .additionalJurisdictions(Set.of(Jurisdiction.SAUDI_ARABIA))
            .supportedLanguages(Set.of("en", "ar"))
            .defaultLanguage("en")
            .timezone("Asia/Dubai")
            .supportedCurrencies(Set.of("AED", "USD"))
            .defaultCurrency("AED")
            .maxUsers(1000)
            .maxStorageGB(100)
            .maxApiCallsPerMinute(1000)
            .dataRetentionDays(365)
            .advancedFeaturesEnabled(true)
            .enhancedSecurityEnabled(true)
            .whiteLabelEnabled(false)
            .customDomain("testbank.ae")
            .build();
        
        createTenantCommand = CreateTenantCommand.builder()
            .name("Test Bank UAE")
            .description("Test Islamic Bank in UAE")
            .type(TenantType.ISLAMIC_BANK)
            .configuration(validConfiguration)
            .contactEmail("admin@testbank.ae")
            .contactPhone("+971501234567")
            .administratorUserId("admin-123")
            .createdBy("system")
            .build();
        
        tenant = Tenant.create(
            createTenantCommand.getName(),
            createTenantCommand.getDescription(),
            createTenantCommand.getType(),
            createTenantCommand.getConfiguration(),
            createTenantCommand.getContactEmail(),
            createTenantCommand.getContactPhone(),
            createTenantCommand.getAdministratorUserId(),
            createTenantCommand.getCreatedBy()
        );
    }
    
    @Test
    @DisplayName("Should create tenant successfully")
    void shouldCreateTenantSuccessfully() {
        // Given
        when(tenantRepository.existsByName(createTenantCommand.getName())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TenantId result = tenantManagementService.createTenant(createTenantCommand);
        
        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        
        Tenant savedTenant = tenantCaptor.getValue();
        assertThat(savedTenant.getName()).isEqualTo(createTenantCommand.getName());
        assertThat(savedTenant.getType()).isEqualTo(createTenantCommand.getType());
        assertThat(savedTenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
        
        verify(eventPublisher).publish(any(TenantCreatedEvent.class));
    }
    
    @Test
    @DisplayName("Should not create tenant with duplicate name")
    void shouldNotCreateTenantWithDuplicateName() {
        // Given
        when(tenantRepository.existsByName(createTenantCommand.getName())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> tenantManagementService.createTenant(createTenantCommand))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant with name");
        
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(eventPublisher, never()).publish(any());
    }
    
    @Test
    @DisplayName("Should activate tenant successfully")
    void shouldActivateTenantSuccessfully() {
        // Given
        ActivateTenantCommand command = ActivateTenantCommand.builder()
            .tenantId(tenantId)
            .updatedBy("activator-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        tenantManagementService.activateTenant(command);
        
        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
    }
    
    @Test
    @DisplayName("Should not activate non-existent tenant")
    void shouldNotActivateNonExistentTenant() {
        // Given
        ActivateTenantCommand command = ActivateTenantCommand.builder()
            .tenantId(tenantId)
            .updatedBy("activator-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> tenantManagementService.activateTenant(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant not found");
        
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(eventPublisher, never()).publish(any());
    }
    
    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() {
        // Given
        tenant.activate("activator-123");
        tenant.markEventsAsCommitted();
        
        SuspendTenantCommand command = SuspendTenantCommand.builder()
            .tenantId(tenantId)
            .reason("Compliance violation")
            .updatedBy("suspender-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        tenantManagementService.suspendTenant(command);
        
        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
    }
    
    @Test
    @DisplayName("Should update tenant configuration successfully")
    void shouldUpdateTenantConfigurationSuccessfully() {
        // Given
        TenantConfiguration newConfiguration = TenantConfiguration.builder()
            .primaryJurisdiction(Jurisdiction.UAE)
            .additionalJurisdictions(Set.of(Jurisdiction.SAUDI_ARABIA, Jurisdiction.BAHRAIN))
            .supportedLanguages(Set.of("en", "ar", "ur"))
            .defaultLanguage("en")
            .timezone("Asia/Dubai")
            .supportedCurrencies(Set.of("AED", "USD", "SAR"))
            .defaultCurrency("AED")
            .maxUsers(2000)
            .maxStorageGB(200)
            .maxApiCallsPerMinute(2000)
            .dataRetentionDays(730)
            .advancedFeaturesEnabled(true)
            .enhancedSecurityEnabled(true)
            .whiteLabelEnabled(true)
            .customDomain("testbank.ae")
            .build();
        
        UpdateTenantConfigurationCommand command = UpdateTenantConfigurationCommand.builder()
            .tenantId(tenantId)
            .configuration(newConfiguration)
            .updatedBy("updater-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        tenantManagementService.updateTenantConfiguration(command);
        
        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
    }
    
    @Test
    @DisplayName("Should update tenant contact info successfully")
    void shouldUpdateTenantContactInfoSuccessfully() {
        // Given
        UpdateTenantContactInfoCommand command = UpdateTenantContactInfoCommand.builder()
            .tenantId(tenantId)
            .contactEmail("newadmin@testbank.ae")
            .contactPhone("+971507654321")
            .updatedBy("updater-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        tenantManagementService.updateTenantContactInfo(command);
        
        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
    }
    
    @Test
    @DisplayName("Should deactivate tenant successfully")
    void shouldDeactivateTenantSuccessfully() {
        // Given
        tenant.activate("activator-123");
        tenant.markEventsAsCommitted();
        
        DeactivateTenantCommand command = DeactivateTenantCommand.builder()
            .tenantId(tenantId)
            .reason("Business closure")
            .updatedBy("deactivator-123")
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        tenantManagementService.deactivateTenant(command);
        
        // Then
        verify(tenantRepository).save(any(Tenant.class));
        verify(eventPublisher).publish(any());
    }
    
    @Test
    @DisplayName("Should get tenant by ID")
    void shouldGetTenantById() {
        // Given
        GetTenantQuery query = GetTenantQuery.builder()
            .tenantId(tenantId)
            .build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        
        // When
        Optional<Tenant> result = tenantManagementService.getTenant(query);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(tenant);
    }
    
    @Test
    @DisplayName("Should get all tenants")
    void shouldGetAllTenants() {
        // Given
        GetAllTenantsQuery query = GetAllTenantsQuery.builder().build();
        List<Tenant> tenants = List.of(tenant);
        
        when(tenantRepository.findAll()).thenReturn(tenants);
        
        // When
        List<Tenant> result = tenantManagementService.getAllTenants(query);
        
        // Then
        assertThat(result).isEqualTo(tenants);
    }
    
    @Test
    @DisplayName("Should get tenants by status")
    void shouldGetTenantsByStatus() {
        // Given
        GetTenantsByStatusQuery query = GetTenantsByStatusQuery.builder()
            .status(TenantStatus.ACTIVE)
            .build();
        List<Tenant> tenants = List.of(tenant);
        
        when(tenantRepository.findByStatus(TenantStatus.ACTIVE)).thenReturn(tenants);
        
        // When
        List<Tenant> result = tenantManagementService.getTenantsByStatus(query);
        
        // Then
        assertThat(result).isEqualTo(tenants);
    }
    
    @Test
    @DisplayName("Should get tenants by type")
    void shouldGetTenantsByType() {
        // Given
        GetTenantsByTypeQuery query = GetTenantsByTypeQuery.builder()
            .type(TenantType.ISLAMIC_BANK)
            .build();
        List<Tenant> tenants = List.of(tenant);
        
        when(tenantRepository.findByType(TenantType.ISLAMIC_BANK)).thenReturn(tenants);
        
        // When
        List<Tenant> result = tenantManagementService.getTenantsByType(query);
        
        // Then
        assertThat(result).isEqualTo(tenants);
    }
    
    @Test
    @DisplayName("Should get tenants by jurisdiction")
    void shouldGetTenantsByJurisdiction() {
        // Given
        GetTenantsByJurisdictionQuery query = GetTenantsByJurisdictionQuery.builder()
            .jurisdiction(Jurisdiction.UAE)
            .build();
        List<Tenant> tenants = List.of(tenant);
        
        when(tenantRepository.findByJurisdiction(Jurisdiction.UAE)).thenReturn(tenants);
        
        // When
        List<Tenant> result = tenantManagementService.getTenantsByJurisdiction(query);
        
        // Then
        assertThat(result).isEqualTo(tenants);
    }
    
    @Test
    @DisplayName("Should check if tenant exists")
    void shouldCheckIfTenantExists() {
        // Given
        when(tenantRepository.existsById(tenantId)).thenReturn(true);
        
        // When
        boolean exists = tenantManagementService.tenantExists(tenantId);
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("Should get tenant count")
    void shouldGetTenantCount() {
        // Given
        when(tenantRepository.count()).thenReturn(5L);
        
        // When
        long count = tenantManagementService.getTenantCount();
        
        // Then
        assertThat(count).isEqualTo(5L);
    }
    
    @Test
    @DisplayName("Should create tenant context successfully")
    void shouldCreateTenantContextSuccessfully() {
        // Given
        tenant.activate("activator-123");
        
        CreateTenantContextCommand command = CreateTenantContextCommand.builder()
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
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        
        // When
        TenantContext result = tenantManagementService.createTenantContext(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getUserId()).isEqualTo("user-123");
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getCurrency()).isEqualTo("AED");
        
        verify(tenantContextManager).setTenantContext(any(TenantContext.class));
    }
    
    @Test
    @DisplayName("Should not create tenant context for non-existent tenant")
    void shouldNotCreateTenantContextForNonExistentTenant() {
        // Given
        CreateTenantContextCommand command = CreateTenantContextCommand.builder()
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
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> tenantManagementService.createTenantContext(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tenant not found");
        
        verify(tenantContextManager, never()).setTenantContext(any(TenantContext.class));
    }
    
    @Test
    @DisplayName("Should not create tenant context for inaccessible tenant")
    void shouldNotCreateTenantContextForInaccessibleTenant() {
        // Given
        tenant.activate("activator-123");
        tenant.suspend("Compliance violation", "suspender-123");
        
        CreateTenantContextCommand command = CreateTenantContextCommand.builder()
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
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        
        // When & Then
        assertThatThrownBy(() -> tenantManagementService.createTenantContext(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant is not accessible");
        
        verify(tenantContextManager, never()).setTenantContext(any(TenantContext.class));
    }
}