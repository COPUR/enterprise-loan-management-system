package com.amanahfi.platform.tenant.domain;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.tenant.domain.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tenant Domain")
class TenantTest {
    
    private TenantConfiguration validConfiguration;
    private final String tenantName = "Test Bank UAE";
    private final String contactEmail = "admin@testbank.ae";
    private final String administratorUserId = "admin-123";
    private final String createdBy = "system";
    
    @BeforeEach
    void setUp() {
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
    }
    
    @Test
    @DisplayName("Should create tenant successfully")
    void shouldCreateTenantSuccessfully() {
        // When
        Tenant tenant = Tenant.create(
            tenantName,
            "Test Islamic Bank in UAE",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        // Then
        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getName()).isEqualTo(tenantName);
        assertThat(tenant.getType()).isEqualTo(TenantType.ISLAMIC_BANK);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
        assertThat(tenant.getConfiguration()).isEqualTo(validConfiguration);
        assertThat(tenant.getContactEmail()).isEqualTo(contactEmail);
        assertThat(tenant.getAdministratorUserId()).isEqualTo(administratorUserId);
        assertThat(tenant.getCreatedBy()).isEqualTo(createdBy);
        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getVersion()).isEqualTo(0L);
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantCreatedEvent.class);
        
        TenantCreatedEvent event = (TenantCreatedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getName()).isEqualTo(tenantName);
        assertThat(event.getType()).isEqualTo(TenantType.ISLAMIC_BANK);
        assertThat(event.getPrimaryJurisdiction()).isEqualTo(Jurisdiction.UAE);
    }
    
    @Test
    @DisplayName("Should not create tenant with invalid name")
    void shouldNotCreateTenantWithInvalidName() {
        // When & Then
        assertThatThrownBy(() -> Tenant.create(
            null,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tenant name cannot be null or empty");
        
        assertThatThrownBy(() -> Tenant.create(
            "",
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tenant name cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should not create tenant with invalid email")
    void shouldNotCreateTenantWithInvalidEmail() {
        // When & Then
        assertThatThrownBy(() -> Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            "invalid-email",
            "+971501234567",
            administratorUserId,
            createdBy
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Contact email must be a valid email address");
    }
    
    @Test
    @DisplayName("Should activate tenant successfully")
    void shouldActivateTenantSuccessfully() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.markEventsAsCommitted();
        
        // When
        tenant.activate("activator-123");
        
        // Then
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getVersion()).isEqualTo(1L);
        assertThat(tenant.getLastUpdatedBy()).isEqualTo("activator-123");
        assertThat(tenant.isOperational()).isTrue();
        assertThat(tenant.isAccessible()).isTrue();
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantActivatedEvent.class);
        
        TenantActivatedEvent event = (TenantActivatedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getPreviousStatus()).isEqualTo(TenantStatus.PROVISIONING);
        assertThat(event.getNewStatus()).isEqualTo(TenantStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("Should not activate already active tenant")
    void shouldNotActivateAlreadyActiveTenant() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.activate("activator-123");
        
        // When & Then
        assertThatThrownBy(() -> tenant.activate("activator-123"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant is already active");
    }
    
    @Test
    @DisplayName("Should suspend tenant successfully")
    void shouldSuspendTenantSuccessfully() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.activate("activator-123");
        tenant.markEventsAsCommitted();
        
        // When
        tenant.suspend("Compliance violation", "suspender-123");
        
        // Then
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(tenant.getVersion()).isEqualTo(2L);
        assertThat(tenant.getLastUpdatedBy()).isEqualTo("suspender-123");
        assertThat(tenant.isOperational()).isFalse();
        assertThat(tenant.isAccessible()).isFalse();
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantSuspendedEvent.class);
        
        TenantSuspendedEvent event = (TenantSuspendedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getPreviousStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(event.getReason()).isEqualTo("Compliance violation");
    }
    
    @Test
    @DisplayName("Should update tenant configuration successfully")
    void shouldUpdateTenantConfigurationSuccessfully() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.markEventsAsCommitted();
        
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
        
        // When
        tenant.updateConfiguration(newConfiguration, "updater-123");
        
        // Then
        assertThat(tenant.getConfiguration()).isEqualTo(newConfiguration);
        assertThat(tenant.getVersion()).isEqualTo(1L);
        assertThat(tenant.getLastUpdatedBy()).isEqualTo("updater-123");
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantConfigurationUpdatedEvent.class);
        
        TenantConfigurationUpdatedEvent event = (TenantConfigurationUpdatedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getNewConfiguration()).isEqualTo(newConfiguration);
    }
    
    @Test
    @DisplayName("Should update tenant contact info successfully")
    void shouldUpdateTenantContactInfoSuccessfully() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.markEventsAsCommitted();
        
        String newEmail = "newadmin@testbank.ae";
        String newPhone = "+971507654321";
        
        // When
        tenant.updateContactInfo(newEmail, newPhone, "updater-123");
        
        // Then
        assertThat(tenant.getContactEmail()).isEqualTo(newEmail);
        assertThat(tenant.getContactPhone()).isEqualTo(newPhone);
        assertThat(tenant.getVersion()).isEqualTo(1L);
        assertThat(tenant.getLastUpdatedBy()).isEqualTo("updater-123");
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantContactInfoUpdatedEvent.class);
        
        TenantContactInfoUpdatedEvent event = (TenantContactInfoUpdatedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getNewEmail()).isEqualTo(newEmail);
        assertThat(event.getNewPhone()).isEqualTo(newPhone);
    }
    
    @Test
    @DisplayName("Should deactivate tenant successfully")
    void shouldDeactivateTenantSuccessfully() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        tenant.activate("activator-123");
        tenant.markEventsAsCommitted();
        
        // When
        tenant.deactivate("Business closure", "deactivator-123");
        
        // Then
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.DEACTIVATED);
        assertThat(tenant.getVersion()).isEqualTo(2L);
        assertThat(tenant.getLastUpdatedBy()).isEqualTo("deactivator-123");
        assertThat(tenant.isOperational()).isFalse();
        assertThat(tenant.isAccessible()).isFalse();
        
        // Verify event was raised
        assertThat(tenant.getUncommittedEvents()).hasSize(1);
        assertThat(tenant.getUncommittedEvents().get(0)).isInstanceOf(TenantDeactivatedEvent.class);
        
        TenantDeactivatedEvent event = (TenantDeactivatedEvent) tenant.getUncommittedEvents().get(0);
        assertThat(event.getTenantId()).isEqualTo(tenant.getId());
        assertThat(event.getPreviousStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(event.getReason()).isEqualTo("Business closure");
    }
    
    @Test
    @DisplayName("Should check jurisdiction support correctly")
    void shouldCheckJurisdictionSupportCorrectly() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        // When & Then
        assertThat(tenant.supportsJurisdiction(Jurisdiction.UAE)).isTrue();
        assertThat(tenant.supportsJurisdiction(Jurisdiction.SAUDI_ARABIA)).isTrue();
        assertThat(tenant.supportsJurisdiction(Jurisdiction.BAHRAIN)).isFalse();
        assertThat(tenant.supportsJurisdiction(Jurisdiction.TURKEY)).isFalse();
    }
    
    @Test
    @DisplayName("Should check language support correctly")
    void shouldCheckLanguageSupportCorrectly() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        // When & Then
        assertThat(tenant.supportsLanguage("en")).isTrue();
        assertThat(tenant.supportsLanguage("ar")).isTrue();
        assertThat(tenant.supportsLanguage("ur")).isFalse();
        assertThat(tenant.supportsLanguage("tr")).isFalse();
    }
    
    @Test
    @DisplayName("Should check currency support correctly")
    void shouldCheckCurrencySupportCorrectly() {
        // Given
        Tenant tenant = Tenant.create(
            tenantName,
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        // When & Then
        assertThat(tenant.supportsCurrency("AED")).isTrue();
        assertThat(tenant.supportsCurrency("USD")).isTrue();
        assertThat(tenant.supportsCurrency("SAR")).isFalse();
        assertThat(tenant.supportsCurrency("EUR")).isFalse();
    }
    
    @Test
    @DisplayName("Should require enhanced security for financial institutions")
    void shouldRequireEnhancedSecurityForFinancialInstitutions() {
        // Given
        Tenant islamicBank = Tenant.create(
            "Islamic Bank",
            "Description",
            TenantType.ISLAMIC_BANK,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        Tenant fintech = Tenant.create(
            "Fintech",
            "Description",
            TenantType.FINTECH,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        Tenant partner = Tenant.create(
            "Partner",
            "Description",
            TenantType.PARTNER,
            validConfiguration,
            contactEmail,
            "+971501234567",
            administratorUserId,
            createdBy
        );
        
        // When & Then
        assertThat(islamicBank.requiresEnhancedSecurity()).isTrue();
        assertThat(fintech.requiresEnhancedSecurity()).isTrue();
        assertThat(partner.requiresEnhancedSecurity()).isTrue(); // Due to configuration
    }
}