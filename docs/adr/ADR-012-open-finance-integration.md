# ADR-012: Open Finance Integration Architecture

## Status
Proposed

## Context
The Central Bank of UAE (CBUAE) has introduced Open Finance Regulation (C7/2023) requiring financial institutions to expose standardized APIs for secure data sharing. Our Enterprise Loan Management System needs to comply with these regulations while maintaining our existing architectural principles and security standards.

Key requirements:
- FAPI 2.0 security compliance
- Mutual TLS (mTLS) authentication
- OAuth 2.1 with DPoP tokens
- Consent management with audit trail
- Integration with CBUAE Trust Framework
- Participant directory synchronization
- Sandbox testing capability

## Decision

### 1. Create New Bounded Context
We will create a dedicated `open-finance-context` bounded context that acts as an anti-corruption layer between our internal domain models and the Open Finance standards.

### 2. Hexagonal Architecture Pattern
Following our existing patterns:
- **Domain Core**: Pure Open Finance business logic (consent, participant, data sharing)
- **Input Ports**: Use cases for consent management, data sharing, participant registration
- **Output Ports**: Integration with CBUAE, certificate management, event publishing
- **Adapters**: REST controllers, CBUAE clients, persistence adapters

### 3. Security Architecture
Enhance existing security infrastructure:
- Extend Keycloak configuration for FAPI 2.0
- Implement PAR (Pushed Authorization Request) in authorization flow
- Add DPoP token validation layer
- Configure Spring Security for mTLS with dynamic certificate validation

### 4. Event-Driven Integration
Leverage existing Kafka infrastructure:
- Publish consent lifecycle events
- Subscribe to customer and loan events
- Implement sagas for complex flows (consent authorization, participant onboarding)

### 5. Data Mapping Strategy
Create transformation layer:
- Map internal Loan/Customer entities to Open Finance data models
- Implement consent-aware filtering
- Apply data minimization based on scope

### 6. CBUAE Integration Approach
Centralized integration through adapters:
- Single adapter for participant directory sync
- Dedicated sandbox adapter for testing
- Certificate management through Vault integration

## Consequences

### Positive
- **Isolation**: Open Finance complexity isolated from core business logic
- **Reusability**: Other contexts can consume Open Finance capabilities via events
- **Testability**: Clear boundaries enable comprehensive testing
- **Compliance**: Centralized consent and audit management
- **Flexibility**: Can adapt to CBUAE spec changes without affecting core domains

### Negative
- **Complexity**: Additional bounded context increases system complexity
- **Data Duplication**: Some data mapping and transformation overhead
- **Latency**: Additional hop for data transformation
- **Maintenance**: New context requires dedicated team ownership

### Risks and Mitigations
1. **Certificate Management Complexity**
   - Risk: Certificate rotation causing downtime
   - Mitigation: Automated rotation with 30-day advance renewal

2. **API Version Changes**
   - Risk: CBUAE spec changes breaking integrations
   - Mitigation: Version adapters, 6-month backward compatibility

3. **Performance Impact**
   - Risk: Data transformation adding latency
   - Mitigation: Caching layer for participant directory, efficient mappers

4. **Consent Synchronization**
   - Risk: Consent state inconsistency
   - Mitigation: Event sourcing for consent audit trail

## Alternatives Considered

### 1. Direct Integration in Existing Contexts
- Rejected: Would pollute core domains with Open Finance concerns
- Violated separation of concerns

### 2. Shared Kernel Approach
- Rejected: Open Finance models too different from internal models
- Would create tight coupling

### 3. External Gateway Service
- Rejected: Would separate business logic from implementation
- Harder to maintain consistency

## Implementation Plan

### Phase 1: Foundation (4 weeks)
- Bounded context structure
- Domain models and ports
- Basic security configuration

### Phase 2: CBUAE Integration (4 weeks)
- Trust framework adapters
- Sandbox environment setup
- Certificate management

### Phase 3: Consent & APIs (4 weeks)
- Consent management system
- Open Finance API endpoints
- Data transformation layer

### Phase 4: Production Readiness (2 weeks)
- Performance optimization
- Monitoring and alerting
- Compliance validation

## References
- CBUAE Open Finance Regulation (C7/2023)
- FAPI 2.0 Security Profile
- OAuth 2.1 Authorization Framework
- ADR-001: Hexagonal Architecture
- ADR-003: Event-Driven Architecture
- ADR-005: Domain-Driven Design