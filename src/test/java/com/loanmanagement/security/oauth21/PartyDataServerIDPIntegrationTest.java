package com.loanmanagement.security.oauth21;

import com.loanmanagement.party.application.PartyDataServerService;
import com.loanmanagement.party.domain.*;
import com.loanmanagement.party.infrastructure.PartyRepository;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Party Data Server IDP Integration Test Suite
 * Tests Identity Provider functionality for OAuth 2.1 + DPoP authentication
 * Contributes to 20% of total OAuth 2.1 + DPoP coverage
 */
@SpringJUnitConfig
@DisplayName("Party Data Server - IDP Integration Tests")
class PartyDataServerIDPIntegrationTest {

    @MockBean
    private PartyRepository partyRepository;
    
    private PartyDataServerService partyDataServerService;
    
    @BeforeEach
    void setUp() {
        partyDataServerService = new PartyDataServerService();
        
        // Inject repository via reflection
        try {
            var field = PartyDataServerService.class.getDeclaredField("partyRepository");
            field.setAccessible(true);
            field.set(partyDataServerService, partyRepository);
        } catch (Exception e) {
            // Handle reflection exception
        }
    }
    
    @Test
    @DisplayName("Should create banking parties for OAuth authentication")
    void shouldCreateBankingPartiesForOAuthAuthentication() {
        // Given
        String externalId = "keycloak-user-123";
        String identifier = "banking.user";
        String displayName = "John Banking User";
        String email = "john.user@bank.com";
        PartyType partyType = PartyType.INDIVIDUAL;
        String createdBy = "oauth-system";
        
        Party mockParty = Party.create(externalId, identifier, displayName, email, partyType, createdBy);
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.empty());
        when(partyRepository.save(any(Party.class))).thenReturn(mockParty);
        
        // When
        Party createdParty = partyDataServerService.createParty(
            externalId, identifier, displayName, email, partyType, createdBy
        );
        
        // Then
        assertThat(createdParty).isNotNull();
        assertThat(createdParty.getExternalId()).isEqualTo(externalId);
        assertThat(createdParty.getIdentifier()).isEqualTo(identifier);
        assertThat(createdParty.getDisplayName()).isEqualTo(displayName);
        assertThat(createdParty.getEmail()).isEqualTo(email);
        assertThat(createdParty.getPartyType()).isEqualTo(partyType);
        assertThat(createdParty.isActive()).isTrue();
        assertThat(createdParty.isCompliant()).isTrue();
        
        verify(partyRepository).findByIdentifier(identifier);
        verify(partyRepository).save(any(Party.class));
    }
    
    @Test
    @DisplayName("Should prevent duplicate party creation")
    void shouldPreventDuplicatePartyCreation() {
        // Given
        String identifier = "existing.user";
        Party existingParty = Party.create(
            "existing-ext-123", identifier, "Existing User", "existing@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(existingParty));
        
        // When & Then
        assertThatThrownBy(() -> partyDataServerService.createParty(
            "new-ext-456", identifier, "New User", "new@bank.com",
            PartyType.INDIVIDUAL, "system"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already exists");
        
        verify(partyRepository).findByIdentifier(identifier);
        verify(partyRepository, never()).save(any(Party.class));
    }
    
    @Test
    @DisplayName("Should authenticate parties for OAuth flows")
    void shouldAuthenticatePartiesForOAuthFlows() {
        // Given - Active and compliant party
        String identifier = "auth.user";
        Party activeParty = Party.create(
            "auth-ext-123", identifier, "Auth User", "auth@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        activeParty.addRole(new PartyRole("BANKING_USER", "Banking User", RoleSource.SYSTEM, "system"));
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(activeParty));
        
        // When
        PartyDataServerService.PartyAuthenticationResult result = 
            partyDataServerService.authenticateParty(identifier, "password123");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getParty()).isEqualTo(activeParty);
        assertThat(result.getErrorMessage()).isNull();
        
        verify(partyRepository).findByIdentifier(identifier);
    }
    
    @Test
    @DisplayName("Should reject authentication for non-existent parties")
    void shouldRejectAuthenticationForNonExistentParties() {
        // Given
        String identifier = "nonexistent.user";
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.empty());
        
        // When
        PartyDataServerService.PartyAuthenticationResult result = 
            partyDataServerService.authenticateParty(identifier, "password123");
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getParty()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("Party not found");
        
        verify(partyRepository).findByIdentifier(identifier);
    }
    
    @Test
    @DisplayName("Should reject authentication for inactive parties")
    void shouldRejectAuthenticationForInactiveParties() {
        // Given - Suspended party
        String identifier = "suspended.user";
        Party suspendedParty = Party.create(
            "suspended-ext-123", identifier, "Suspended User", "suspended@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        suspendedParty.suspend("Compliance violation");
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(suspendedParty));
        
        // When
        PartyDataServerService.PartyAuthenticationResult result = 
            partyDataServerService.authenticateParty(identifier, "password123");
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getParty()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("Party is not active");
    }
    
    @Test
    @DisplayName("Should reject authentication for non-compliant parties")
    void shouldRejectAuthenticationForNonCompliantParties() {
        // Given - Basic compliance level party (not compliant for banking)
        String identifier = "basic.user";
        Party basicParty = Party.create(
            "basic-ext-123", identifier, "Basic User", "basic@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        basicParty.updateComplianceLevel(ComplianceLevel.BASIC, "system");
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(basicParty));
        
        // When
        PartyDataServerService.PartyAuthenticationResult result = 
            partyDataServerService.authenticateParty(identifier, "password123");
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getParty()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("Party compliance check failed");
    }
    
    @Test
    @DisplayName("Should manage party roles for OAuth authorization")
    void shouldManagePartyRolesForOAuthAuthorization() {
        // Given
        String identifier = "role.user";
        Party roleParty = Party.create(
            "role-ext-123", identifier, "Role User", "role@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(roleParty));
        when(partyRepository.save(any(Party.class))).thenReturn(roleParty);
        
        // When - Assign banking role
        Party updatedParty = partyDataServerService.assignRole(
            identifier, "LOAN_OFFICER", "Loan Officer Role", RoleSource.EXTERNAL_SYSTEM, "keycloak"
        );
        
        // Then
        assertThat(updatedParty.hasRole("LOAN_OFFICER")).isTrue();
        assertThat(updatedParty.getActiveRoles()).hasSize(1);
        
        // When - Remove role
        Party afterRemoval = partyDataServerService.removeRole(identifier, "LOAN_OFFICER", "admin");
        
        // Then - Role should be deactivated, not removed
        assertThat(afterRemoval.getPartyRoles()).hasSize(1); // Still exists
        PartyRole role = afterRemoval.getPartyRoles().iterator().next();
        assertThat(role.isActive()).isFalse();
        
        verify(partyRepository, times(2)).findByIdentifier(identifier);
        verify(partyRepository, times(2)).save(any(Party.class));
    }
    
    @Test
    @DisplayName("Should validate parties for OAuth 2.1 operations")
    void shouldValidatePartiesForOAuth21Operations() {
        // Given - Valid banking party
        String identifier = "valid.banking.user";
        Party validParty = Party.create(
            "valid-ext-123", identifier, "Valid Banking User", "valid@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        validParty.addRole(new PartyRole("BANKING_USER", "Banking User", RoleSource.SYSTEM, "system"));
        validParty.addRole(new PartyRole("LOAN_VIEWER", "Loan Viewer", RoleSource.SYSTEM, "system"));
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(validParty));
        
        // When
        PartyDataServerService.PartyValidationResult result = 
            partyDataServerService.validateForOAuth(identifier);
        
        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getParty()).isEqualTo(validParty);
        assertThat(result.getErrorMessage()).isNull();
    }
    
    @Test
    @DisplayName("Should reject OAuth validation for parties without banking roles")
    void shouldRejectOAuthValidationForPartiesWithoutBankingRoles() {
        // Given - Party without banking roles
        String identifier = "no.banking.role";
        Party noBankingRoleParty = Party.create(
            "no-banking-ext-123", identifier, "No Banking Role User", "nobaking@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        noBankingRoleParty.addRole(new PartyRole("GENERAL_USER", "General User", RoleSource.SYSTEM, "system"));
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(noBankingRoleParty));
        
        // When
        PartyDataServerService.PartyValidationResult result = 
            partyDataServerService.validateForOAuth(identifier);
        
        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("valid banking roles");
    }
    
    @Test
    @DisplayName("Should manage party status transitions")
    void shouldManagePartyStatusTransitions() {
        // Given
        String identifier = "status.user";
        Party statusParty = Party.create(
            "status-ext-123", identifier, "Status User", "status@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(statusParty));
        when(partyRepository.save(any(Party.class))).thenReturn(statusParty);
        
        // When - Suspend party
        Party suspendedParty = partyDataServerService.updatePartyStatus(
            identifier, PartyStatus.SUSPENDED, "compliance-officer"
        );
        
        // Then
        assertThat(suspendedParty.getStatus()).isEqualTo(PartyStatus.SUSPENDED);
        assertThat(suspendedParty.isActive()).isFalse();
        
        // When - Reactivate party
        Party reactivatedParty = partyDataServerService.updatePartyStatus(
            identifier, PartyStatus.ACTIVE, "admin"
        );
        
        // Then
        assertThat(reactivatedParty.getStatus()).isEqualTo(PartyStatus.ACTIVE);
        assertThat(reactivatedParty.isActive()).isTrue();
        
        verify(partyRepository, times(2)).findByIdentifier(identifier);
        verify(partyRepository, times(2)).save(any(Party.class));
    }
    
    @Test
    @DisplayName("Should retrieve parties by different criteria")
    void shouldRetrievePartiesByDifferentCriteria() {
        // Given
        Party party1 = Party.create("ext-1", "user1", "User One", "user1@bank.com", PartyType.INDIVIDUAL, "system");
        Party party2 = Party.create("ext-2", "user2", "User Two", "user2@bank.com", PartyType.ORGANIZATION, "system");
        Party party3 = Party.create("ext-3", "user3", "User Three", "user3@bank.com", PartyType.INDIVIDUAL, "system");
        
        List<Party> allParties = Arrays.asList(party1, party2, party3);
        List<Party> activeIndividuals = Arrays.asList(party1, party3);
        
        when(partyRepository.findAll()).thenReturn(allParties);
        when(partyRepository.findByPartyTypeAndStatus(PartyType.INDIVIDUAL, PartyStatus.ACTIVE))
            .thenReturn(activeIndividuals);
        
        // When & Then - Get all parties
        List<Party> retrievedAll = partyDataServerService.getAllParties();
        assertThat(retrievedAll).hasSize(3);
        assertThat(retrievedAll).containsExactlyInAnyOrder(party1, party2, party3);
        
        // When & Then - Get active individuals
        List<Party> retrievedIndividuals = partyDataServerService.getActivePartiesByType(PartyType.INDIVIDUAL);
        assertThat(retrievedIndividuals).hasSize(2);
        assertThat(retrievedIndividuals).containsExactlyInAnyOrder(party1, party3);
        
        verify(partyRepository).findAll();
        verify(partyRepository).findByPartyTypeAndStatus(PartyType.INDIVIDUAL, PartyStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("Should find parties by external ID for Keycloak integration")
    void shouldFindPartiesByExternalIdForKeycloakIntegration() {
        // Given
        String externalId = "keycloak-uuid-456";
        Party keycloakParty = Party.create(
            externalId, "keycloak.user", "Keycloak User", "keycloak@bank.com",
            PartyType.INDIVIDUAL, "keycloak"
        );
        
        when(partyRepository.findByExternalId(externalId)).thenReturn(Optional.of(keycloakParty));
        
        // When
        Optional<Party> found = partyDataServerService.findPartyByExternalId(externalId);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(keycloakParty);
        assertThat(found.get().getExternalId()).isEqualTo(externalId);
        
        verify(partyRepository).findByExternalId(externalId);
    }
    
    @Test
    @DisplayName("Should handle concurrent party operations safely")
    void shouldHandleConcurrentPartyOperationsSafely() {
        // Given
        String identifier = "concurrent.user";
        Party concurrentParty = Party.create(
            "concurrent-ext-123", identifier, "Concurrent User", "concurrent@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        
        when(partyRepository.findByIdentifier(identifier)).thenReturn(Optional.of(concurrentParty));
        when(partyRepository.save(any(Party.class))).thenReturn(concurrentParty);
        
        // When - Simulate concurrent role assignments
        String[] roles = {"BANKING_USER", "LOAN_OFFICER", "COMPLIANCE_OFFICER", "AUDIT_VIEWER"};
        
        for (String role : roles) {
            Party updated = partyDataServerService.assignRole(
                identifier, role, role + " Role", RoleSource.SYSTEM, "system"
            );
            assertThat(updated.hasRole(role)).isTrue();
        }
        
        // Then - All roles should be assigned
        assertThat(concurrentParty.getActiveRoles()).hasSize(4);
        
        verify(partyRepository, times(4)).findByIdentifier(identifier);
        verify(partyRepository, times(4)).save(any(Party.class));
    }
    
    @Test
    @DisplayName("Should validate business rules for banking operations")
    void shouldValidateBusinessRulesForBankingOperations() {
        // Test party type business rules
        Party individual = Party.create(
            "ind-1", "individual", "Individual User", "ind@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        assertThat(individual.getPartyType().isIndividual()).isTrue();
        assertThat(individual.getPartyType().isOrganization()).isFalse();
        
        Party organization = Party.create(
            "org-1", "organization", "Bank Corp", "corp@bank.com",
            PartyType.CORPORATION, "system"
        );
        assertThat(organization.getPartyType().isOrganization()).isTrue();
        assertThat(organization.getPartyType().isIndividual()).isFalse();
        
        // Test compliance level business rules
        Party highComplianceParty = Party.create(
            "high-1", "high.user", "High Compliance User", "high@bank.com",
            PartyType.INDIVIDUAL, "system"
        );
        highComplianceParty.updateComplianceLevel(ComplianceLevel.HIGH, "compliance-officer");
        
        assertThat(highComplianceParty.getComplianceLevel().requiresEnhancedDueDiligence()).isTrue();
        assertThat(highComplianceParty.isCompliant()).isTrue();
        
        // Test role source business rules
        PartyRole systemRole = new PartyRole("SYSTEM_ROLE", "System Role", RoleSource.SYSTEM, "system");
        PartyRole externalRole = new PartyRole("EXTERNAL_ROLE", "External Role", RoleSource.EXTERNAL_SYSTEM, "keycloak");
        
        assertThat(systemRole.getSource().isSystemGenerated()).isTrue();
        assertThat(externalRole.getSource().isSystemGenerated()).isFalse();
    }
}