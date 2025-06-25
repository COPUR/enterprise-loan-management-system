# Party Data Domain Analysis and Integration

## Executive Summary

The Enterprise Banking System contains a sophisticated **Party Data Domain** implementation that demonstrates excellent domain modeling practices but requires integration completion. This analysis covers the current state, architectural patterns, security considerations, and integration requirements with Keycloak OAuth 2.1.

## Current Party Data Domain Architecture

### 1. Domain Model Overview

```
Party Domain Structure:
┌─────────────────────────────────────────────────────────────┐
│                    Party Aggregate Root                     │
├─────────────────────────────────────────────────────────────┤
│ - PartyId (UUID-based identifier)                          │
│ - ExternalId (external system identifier)                  │
│ - Identifier (business identifier)                         │
│ - DisplayName (human-readable name)                        │
│ - Email (contact email)                                     │
│ - PartyType (INDIVIDUAL, ORGANIZATION, etc.)               │
│ - PartyStatus (lifecycle management)                       │
│ - ComplianceLevel (KYC/AML status)                         │
│ - CreatedAt, UpdatedAt (temporal tracking)                 │
│ - Version (optimistic locking)                             │
└─────────────────────────────────────────────────────────────┘
               │                              │
               ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│      PartyRole          │    │      PartyGroup         │
├─────────────────────────┤    ├─────────────────────────┤
│ - RoleId                │    │ - GroupId               │
│ - RoleName              │    │ - GroupName             │
│ - AuthorityLevel        │    │ - GroupType             │
│ - MonetaryLimit         │    │ - GroupRole             │
│ - EffectiveFrom/To      │    │ - EffectiveFrom/To      │
│ - RoleSource            │    │ - BusinessUnit          │
│ - RequiresReview        │    │ - GeographicScope       │
└─────────────────────────┘    └─────────────────────────┘
```

### 2. Party Types and Business Rules

#### PartyType Enumeration
```java
public enum PartyType {
    INDIVIDUAL,        // Natural persons/customers
    ORGANIZATION,      // Corporate entities
    SERVICE_ACCOUNT,   // System/service identities
    SYSTEM_USER,       // Internal system users
    API_CLIENT         // External API consumers
}
```

#### PartyStatus Lifecycle
```java
public enum PartyStatus {
    ACTIVE,           // Fully operational
    INACTIVE,         // Temporarily disabled
    PENDING,          // Awaiting approval/activation
    SUSPENDED,        // Temporarily blocked
    BLOCKED,          // Security/compliance block
    CLOSED,           // Permanently deactivated
    UNDER_REVIEW      // Compliance/security review
}
```

#### Compliance Levels with Transaction Limits
```java
public enum ComplianceLevel {
    LOW(1000.00),           // Basic verification
    STANDARD(10000.00),     // Standard KYC
    HIGH(100000.00),        // Enhanced due diligence
    RESTRICTED(0.00),       // No transactions allowed
    SUSPENDED(0.00)         // Compliance suspension
}
```

### 3. Role-Based Access Control (RBAC) Integration

#### Role Sources and Authority Levels
```java
public enum RoleSource {
    DATABASE,        // Internal role assignment
    LDAP,           // LDAP directory
    KEYCLOAK,       // OAuth 2.1 identity provider
    EXTERNAL,       // External identity federation
    AUTOMATED,      // System-generated roles
    LEGACY_IMPORT   // Migrated from legacy systems
}
```

#### Group Types for Organizational Structure
```java
public enum GroupType {
    DEPARTMENT,         // Organizational departments
    TEAM,              // Working teams
    FUNCTIONAL,        // Functional roles
    SECURITY,          // Security groups
    PROJECT,           // Project-based groups
    GEOGRAPHIC,        // Location-based groups
    COMPLIANCE,        // Compliance/audit groups
    AUDIT,             // Audit trail groups
    BUSINESS_LINE,     // Business line groups
    RISK_MANAGEMENT    // Risk management groups
}
```

## Party Domain Integration with Keycloak

### 1. Identity Federation Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Keycloak      │    │   Party Domain   │    │  Customer       │
│   Identity      │    │   (Identity)     │    │  Domain         │
│   Provider      │    │                  │    │  (Business)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. OAuth 2.1          │                       │
         │    Authentication     │                       │
         │──────────────────────▶│                       │
         │                       │                       │
         │ 2. JWT Claims         │ 3. Party Lookup      │
         │    Validation         │    by ExternalId     │
         │◀──────────────────────│                       │
         │                       │                       │
         │                       │ 4. Business Context  │
         │                       │    Enrichment        │
         │                       │──────────────────────▶│
         │                       │                       │
         │                       │ 5. Unified Identity  │
         │                       │◀──────────────────────│
```

### 2. Keycloak User Attributes Mapping

#### User Attribute Mapping Configuration
```json
{
  "keycloak_to_party_mapping": {
    "sub": "externalId",
    "preferred_username": "identifier",
    "name": "displayName",
    "email": "email",
    "custom_attributes": {
      "customer_id": "businessIdentifier",
      "branch_code": "organizationalUnit",
      "employee_id": "staffIdentifier",
      "compliance_level": "complianceLevel",
      "party_type": "partyType"
    }
  },
  "role_mapping": {
    "keycloak_roles": "banking_roles",
    "groups": "party_groups",
    "authorities": "authority_level"
  }
}
```

#### Party Creation from Keycloak Claims
```java
@Component
public class KeycloakPartyMapper {
    
    public Party createPartyFromJwt(JwtAuthenticationToken jwt) {
        Map<String, Object> claims = jwt.getTokenAttributes();
        
        return Party.builder()
            .externalId((String) claims.get("sub"))
            .identifier((String) claims.get("preferred_username"))
            .displayName((String) claims.get("name"))
            .email((String) claims.get("email"))
            .partyType(mapPartyType(claims.get("party_type")))
            .partyStatus(PartyStatus.ACTIVE)
            .complianceLevel(mapComplianceLevel(claims.get("compliance_level")))
            .roles(mapRoles(claims.get("banking_roles")))
            .groups(mapGroups(claims.get("groups")))
            .build();
    }
    
    private List<PartyRole> mapRoles(Object rolesObj) {
        if (rolesObj instanceof List<?> roles) {
            return roles.stream()
                .map(role -> PartyRole.builder()
                    .roleName((String) role)
                    .roleSource(RoleSource.KEYCLOAK)
                    .effectiveFrom(Instant.now())
                    .authorityLevel(determineAuthorityLevel(role))
                    .monetaryLimit(determineMonetaryLimit(role))
                    .build())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
```

### 3. FAPI Security Integration with Party Domain

#### Enhanced Party Security Attributes
```java
@Entity
public class PartySecurityProfile {
    
    @Id
    private PartyId partyId;
    
    @Enumerated(EnumType.STRING)
    private MfaStatus mfaStatus;
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    private Instant lastAuthenticationTime;
    private String lastAuthenticationMethod;
    private Integer consecutiveFailedAttempts;
    private Instant accountLockedUntil;
    
    // FAPI-specific attributes
    private boolean requiresPushedAuthorizationRequest;
    private boolean requiresJwtSecuredAuthorizationRequest;
    private boolean tlsClientCertificateBound;
    private String authorizedCertificateThumbprint;
    
    // Risk-based authentication
    private Set<String> trustedDeviceFingerprints;
    private Set<String> trustedIpAddresses;
    private GeographicLocation lastKnownLocation;
}

public enum MfaStatus {
    NOT_ENROLLED,
    ENROLLED_SMS,
    ENROLLED_TOTP,
    ENROLLED_WEBAUTHN,
    ENROLLED_PUSH,
    TEMPORARY_BYPASS,
    SUSPENDED
}

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

### 4. Party Domain Service Implementation

#### Complete Hexagonal Architecture Implementation
```java
// Application Service
@Service
@Transactional
public class PartyManagementService {
    
    private final PartyRepository partyRepository;
    private final DomainEventPublisher eventPublisher;
    private final KeycloakPartyMapper partyMapper;
    
    public Party authenticateParty(JwtAuthenticationToken jwt) {
        String externalId = jwt.getTokenAttributes().get("sub").toString();
        
        return partyRepository.findByExternalId(externalId)
            .map(party -> updatePartyFromToken(party, jwt))
            .orElseGet(() -> createPartyFromToken(jwt));
    }
    
    public Party createPartyFromToken(JwtAuthenticationToken jwt) {
        Party party = partyMapper.createPartyFromJwt(jwt);
        Party savedParty = partyRepository.save(party);
        
        eventPublisher.publish(new PartyCreatedEvent(savedParty));
        return savedParty;
    }
    
    public void assignRole(PartyId partyId, String roleName, 
                          AuthorityLevel authorityLevel, 
                          MonetaryAmount monetaryLimit) {
        Party party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException(partyId));
            
        PartyRole role = PartyRole.builder()
            .roleName(roleName)
            .authorityLevel(authorityLevel)
            .monetaryLimit(monetaryLimit)
            .effectiveFrom(Instant.now())
            .roleSource(RoleSource.DATABASE)
            .build();
            
        party.assignRole(role);
        partyRepository.save(party);
        
        eventPublisher.publish(new PartyRoleAssignedEvent(partyId, role));
    }
}

// Domain Repository Port
public interface PartyRepository {
    Optional<Party> findById(PartyId partyId);
    Optional<Party> findByExternalId(String externalId);
    Optional<Party> findByIdentifier(String identifier);
    Optional<Party> findByEmail(String email);
    List<Party> findByPartyType(PartyType partyType);
    List<Party> findByComplianceLevel(ComplianceLevel level);
    Party save(Party party);
    void delete(PartyId partyId);
}

// Infrastructure Adapter
@Repository
public class JpaPartyRepository implements PartyRepository {
    
    private final PartyJpaRepository jpaRepository;
    private final PartyMapper partyMapper;
    
    @Override
    public Optional<Party> findByExternalId(String externalId) {
        return jpaRepository.findByExternalId(externalId)
            .map(partyMapper::toDomain);
    }
    
    @Override
    public Party save(Party party) {
        PartyJpaEntity entity = partyMapper.toEntity(party);
        PartyJpaEntity saved = jpaRepository.save(entity);
        return partyMapper.toDomain(saved);
    }
}
```

### 5. Party API Controllers with FAPI Compliance

#### REST Controller Implementation
```java
@RestController
@RequestMapping("/api/v1/parties")
@PreAuthorize("hasRole('banking-admin') or hasRole('banking-manager')")
@Validated
public class PartyController {
    
    private final PartyManagementService partyService;
    
    @GetMapping("/{partyId}")
    @PreAuthorize("hasPermission(#partyId, 'Party', 'READ')")
    public ResponseEntity<PartyDto> getParty(
            @PathVariable @Valid PartyId partyId,
            @RequestHeader("X-FAPI-Interaction-ID") String interactionId) {
        
        Party party = partyService.findById(partyId);
        return ResponseEntity.ok()
            .header("X-FAPI-Interaction-ID", interactionId)
            .body(PartyDto.from(party));
    }
    
    @PostMapping("/{partyId}/roles")
    @PreAuthorize("hasRole('banking-admin')")
    public ResponseEntity<Void> assignRole(
            @PathVariable PartyId partyId,
            @RequestBody @Valid AssignRoleRequest request,
            @RequestHeader("X-FAPI-Interaction-ID") String interactionId) {
        
        partyService.assignRole(partyId, request.getRoleName(), 
            request.getAuthorityLevel(), request.getMonetaryLimit());
            
        return ResponseEntity.ok()
            .header("X-FAPI-Interaction-ID", interactionId)
            .build();
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('banking-admin') or hasRole('banking-manager')")
    public ResponseEntity<Page<PartyDto>> searchParties(
            @RequestParam(required = false) PartyType partyType,
            @RequestParam(required = false) ComplianceLevel complianceLevel,
            @RequestParam(required = false) String identifier,
            Pageable pageable,
            @RequestHeader("X-FAPI-Interaction-ID") String interactionId) {
        
        Page<Party> parties = partyService.searchParties(
            partyType, complianceLevel, identifier, pageable);
            
        return ResponseEntity.ok()
            .header("X-FAPI-Interaction-ID", interactionId)
            .body(parties.map(PartyDto::from));
    }
}
```

### 6. Event-Driven Integration

#### Domain Events
```java
@DomainEvent
public class PartyCreatedEvent {
    private final PartyId partyId;
    private final String externalId;
    private final PartyType partyType;
    private final Instant createdAt;
}

@DomainEvent
public class PartyRoleAssignedEvent {
    private final PartyId partyId;
    private final String roleName;
    private final AuthorityLevel authorityLevel;
    private final Instant assignedAt;
}

@DomainEvent
public class PartyComplianceLevelChangedEvent {
    private final PartyId partyId;
    private final ComplianceLevel oldLevel;
    private final ComplianceLevel newLevel;
    private final String reason;
    private final Instant changedAt;
}
```

#### Event Handlers
```java
@Component
public class PartyEventHandler {
    
    private final CustomerManagementService customerService;
    private final AuditService auditService;
    
    @EventHandler
    public void on(PartyCreatedEvent event) {
        // Sync with Customer domain
        if (isCustomerParty(event.getPartyType())) {
            customerService.createCustomerFromParty(event.getPartyId());
        }
        
        // Create audit trail
        auditService.recordPartyCreation(event);
    }
    
    @EventHandler
    public void on(PartyRoleAssignedEvent event) {
        // Update Keycloak roles if needed
        if (shouldSyncToKeycloak(event.getRoleName())) {
            keycloakSyncService.syncRoleAssignment(event);
        }
        
        // Record authorization change
        auditService.recordRoleAssignment(event);
    }
}
```

### 7. Database Schema and Migration

#### SQL Migration for Party Tables
```sql
-- V1__Create_party_tables.sql
CREATE SCHEMA IF NOT EXISTS party_domain;

CREATE TABLE party_domain.parties (
    party_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) UNIQUE NOT NULL,
    identifier VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(500) NOT NULL,
    email VARCHAR(320) NOT NULL,
    party_type VARCHAR(50) NOT NULL CHECK (party_type IN ('INDIVIDUAL', 'ORGANIZATION', 'SERVICE_ACCOUNT', 'SYSTEM_USER', 'API_CLIENT')),
    party_status VARCHAR(50) NOT NULL CHECK (party_status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'BLOCKED', 'CLOSED', 'UNDER_REVIEW')),
    compliance_level VARCHAR(50) NOT NULL CHECK (compliance_level IN ('LOW', 'STANDARD', 'HIGH', 'RESTRICTED', 'SUSPENDED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE party_domain.party_roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES party_domain.parties(party_id),
    role_name VARCHAR(255) NOT NULL,
    authority_level INTEGER NOT NULL,
    monetary_limit DECIMAL(15,2),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    role_source VARCHAR(50) NOT NULL CHECK (role_source IN ('DATABASE', 'LDAP', 'KEYCLOAK', 'EXTERNAL', 'AUTOMATED', 'LEGACY_IMPORT')),
    requires_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL
);

CREATE TABLE party_domain.party_groups (
    group_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES party_domain.parties(party_id),
    group_name VARCHAR(255) NOT NULL,
    group_type VARCHAR(50) NOT NULL CHECK (group_type IN ('DEPARTMENT', 'TEAM', 'FUNCTIONAL', 'SECURITY', 'PROJECT', 'GEOGRAPHIC', 'COMPLIANCE', 'AUDIT', 'BUSINESS_LINE', 'RISK_MANAGEMENT')),
    group_role VARCHAR(50) NOT NULL CHECK (group_role IN ('MEMBER', 'LEADER', 'ADMINISTRATOR', 'DEPUTY', 'SENIOR_MEMBER', 'OBSERVER', 'GUEST')),
    business_unit VARCHAR(255),
    geographic_scope VARCHAR(255),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_parties_external_id ON party_domain.parties(external_id);
CREATE INDEX idx_parties_identifier ON party_domain.parties(identifier);
CREATE INDEX idx_parties_email ON party_domain.parties(email);
CREATE INDEX idx_parties_type_status ON party_domain.parties(party_type, party_status);
CREATE INDEX idx_party_roles_party_id ON party_domain.party_roles(party_id);
CREATE INDEX idx_party_roles_effective ON party_domain.party_roles(effective_from, effective_to);
CREATE INDEX idx_party_groups_party_id ON party_domain.party_groups(party_id);
CREATE INDEX idx_party_groups_effective ON party_domain.party_groups(effective_from, effective_to);
```

## Integration Recommendations

### 1. Complete Hexagonal Architecture
- Implement missing application services and use cases
- Create repository adapters with proper mapping
- Add comprehensive domain event handling

### 2. Keycloak Integration
- Implement JWT claims-to-Party mapping
- Create party synchronization services
- Add role federation between systems

### 3. Customer Domain Integration
- Establish clear relationship between Party and Customer
- Implement shared value objects
- Create unified identity management

### 4. Security Enhancements
- Add FAPI-compliant security attributes
- Implement risk-based authentication
- Create comprehensive audit trails

### 5. API Development
- Create REST and GraphQL endpoints
- Implement proper FAPI security headers
- Add comprehensive validation and error handling

This party data domain provides an excellent foundation for enterprise identity management with proper separation of concerns, comprehensive business rules, and security-first design principles.