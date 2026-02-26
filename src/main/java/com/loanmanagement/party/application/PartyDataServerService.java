package com.loanmanagement.party.application;

import com.loanmanagement.party.domain.*;
import com.loanmanagement.party.infrastructure.PartyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Party Data Server Application Service
 * Provides Identity Provider (IDP) functionality for the banking system
 * Integrates with Keycloak for complete OAuth 2.1 + DPoP authentication
 */
@Service
@Transactional
public class PartyDataServerService {

    private static final Logger logger = LoggerFactory.getLogger(PartyDataServerService.class);
    
    @Autowired
    private PartyRepository partyRepository;
    
    /**
     * Creates a new party in the system
     */
    public Party createParty(
            String externalId,
            String identifier, 
            String displayName,
            String email,
            PartyType partyType,
            String createdBy) {
        
        logger.info("Creating new party: identifier={}, type={}", identifier, partyType);
        
        // Check if party already exists
        Optional<Party> existingParty = partyRepository.findByIdentifier(identifier);
        if (existingParty.isPresent()) {
            throw new IllegalArgumentException("Party with identifier " + identifier + " already exists");
        }
        
        // Create new party
        Party party = Party.create(externalId, identifier, displayName, email, partyType, createdBy);
        
        // Save party
        Party savedParty = partyRepository.save(party);
        
        logger.info("Successfully created party: id={}, identifier={}", 
            savedParty.getId(), savedParty.getIdentifier());
        
        return savedParty;
    }
    
    /**
     * Finds party by identifier (for authentication)
     */
    @Transactional(readOnly = true)
    public Optional<Party> findPartyByIdentifier(String identifier) {
        logger.debug("Finding party by identifier: {}", identifier);
        return partyRepository.findByIdentifier(identifier);
    }
    
    /**
     * Finds party by external ID (for Keycloak integration)
     */
    @Transactional(readOnly = true)
    public Optional<Party> findPartyByExternalId(String externalId) {
        logger.debug("Finding party by external ID: {}", externalId);
        return partyRepository.findByExternalId(externalId);
    }
    
    /**
     * Authenticates party and returns authentication result
     */
    @Transactional(readOnly = true)
    public PartyAuthenticationResult authenticateParty(String identifier, String password) {
        logger.debug("Authenticating party: {}", identifier);
        
        Optional<Party> partyOpt = findPartyByIdentifier(identifier);
        if (partyOpt.isEmpty()) {
            logger.warn("Authentication failed - party not found: {}", identifier);
            return PartyAuthenticationResult.failed("Party not found");
        }
        
        Party party = partyOpt.get();
        
        // Check party status
        if (!party.isActive()) {
            logger.warn("Authentication failed - party not active: {}, status={}", 
                identifier, party.getStatus());
            return PartyAuthenticationResult.failed("Party is not active");
        }
        
        // Check compliance level
        if (!party.isCompliant()) {
            logger.warn("Authentication failed - party not compliant: {}, compliance={}", 
                identifier, party.getComplianceLevel());
            return PartyAuthenticationResult.failed("Party compliance check failed");
        }
        
        logger.info("Authentication successful for party: {}", identifier);
        return PartyAuthenticationResult.success(party);
    }
    
    /**
     * Updates party status
     */
    public Party updatePartyStatus(String identifier, PartyStatus newStatus, String updatedBy) {
        logger.info("Updating party status: identifier={}, newStatus={}", identifier, newStatus);
        
        Party party = findPartyByIdentifier(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Party not found: " + identifier));
        
        party.setStatus(newStatus);
        party.setUpdatedBy(updatedBy);
        
        Party savedParty = partyRepository.save(party);
        
        logger.info("Successfully updated party status: id={}, status={}", 
            savedParty.getId(), savedParty.getStatus());
        
        return savedParty;
    }
    
    /**
     * Assigns role to party
     */
    public Party assignRole(String identifier, String roleName, String roleDescription, 
                           RoleSource source, String assignedBy) {
        logger.info("Assigning role to party: identifier={}, role={}", identifier, roleName);
        
        Party party = findPartyByIdentifier(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Party not found: " + identifier));
        
        // Check if role already assigned
        if (party.hasRole(roleName)) {
            logger.warn("Role already assigned to party: identifier={}, role={}", identifier, roleName);
            return party;
        }
        
        PartyRole role = new PartyRole(roleName, roleDescription, source, assignedBy);
        party.addRole(role);
        
        Party savedParty = partyRepository.save(party);
        
        logger.info("Successfully assigned role to party: id={}, role={}", 
            savedParty.getId(), roleName);
        
        return savedParty;
    }
    
    /**
     * Removes role from party
     */
    public Party removeRole(String identifier, String roleName, String revokedBy) {
        logger.info("Removing role from party: identifier={}, role={}", identifier, roleName);
        
        Party party = findPartyByIdentifier(identifier)
            .orElseThrow(() -> new IllegalArgumentException("Party not found: " + identifier));
        
        // Find and revoke the role
        party.getPartyRoles().stream()
            .filter(role -> role.getRoleName().equals(roleName) && role.isActive())
            .findFirst()
            .ifPresent(role -> role.revoke(revokedBy));
        
        Party savedParty = partyRepository.save(party);
        
        logger.info("Successfully removed role from party: id={}, role={}", 
            savedParty.getId(), roleName);
        
        return savedParty;
    }
    
    /**
     * Gets all parties for administrative purposes
     */
    @Transactional(readOnly = true)
    public List<Party> getAllParties() {
        logger.debug("Getting all parties");
        return partyRepository.findAll();
    }
    
    /**
     * Gets active parties by type
     */
    @Transactional(readOnly = true)
    public List<Party> getActivePartiesByType(PartyType partyType) {
        logger.debug("Getting active parties by type: {}", partyType);
        return partyRepository.findByPartyTypeAndStatus(partyType, PartyStatus.ACTIVE);
    }
    
    /**
     * Validates party for OAuth 2.1 + DPoP authentication
     */
    @Transactional(readOnly = true)
    public PartyValidationResult validateForOAuth(String identifier) {
        logger.debug("Validating party for OAuth: {}", identifier);
        
        Optional<Party> partyOpt = findPartyByIdentifier(identifier);
        if (partyOpt.isEmpty()) {
            return PartyValidationResult.invalid("Party not found");
        }
        
        Party party = partyOpt.get();
        
        // Check party status
        if (!party.isActive()) {
            return PartyValidationResult.invalid("Party is not active");
        }
        
        // Check compliance level for OAuth access
        if (!party.isCompliant()) {
            return PartyValidationResult.invalid("Party does not meet compliance requirements");
        }
        
        // Check if party has necessary roles for banking operations
        boolean hasValidRole = party.getActiveRoles().stream()
            .anyMatch(role -> isValidBankingRole(role.getRoleName()));
        
        if (!hasValidRole) {
            return PartyValidationResult.invalid("Party does not have valid banking roles");
        }
        
        return PartyValidationResult.valid(party);
    }
    
    private boolean isValidBankingRole(String roleName) {
        return roleName.startsWith("BANKING_") || 
               roleName.startsWith("LOAN_") ||
               roleName.startsWith("CUSTOMER_") ||
               roleName.startsWith("COMPLIANCE_");
    }
    
    /**
     * Party authentication result
     */
    public static class PartyAuthenticationResult {
        private final boolean success;
        private final String errorMessage;
        private final Party party;
        
        private PartyAuthenticationResult(boolean success, String errorMessage, Party party) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.party = party;
        }
        
        public static PartyAuthenticationResult success(Party party) {
            return new PartyAuthenticationResult(true, null, party);
        }
        
        public static PartyAuthenticationResult failed(String errorMessage) {
            return new PartyAuthenticationResult(false, errorMessage, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Party getParty() { return party; }
    }
    
    /**
     * Party validation result for OAuth
     */
    public static class PartyValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Party party;
        
        private PartyValidationResult(boolean valid, String errorMessage, Party party) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.party = party;
        }
        
        public static PartyValidationResult valid(Party party) {
            return new PartyValidationResult(true, null, party);
        }
        
        public static PartyValidationResult invalid(String errorMessage) {
            return new PartyValidationResult(false, errorMessage, null);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public Party getParty() { return party; }
    }
}