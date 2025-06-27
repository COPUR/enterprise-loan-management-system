# Enterprise Banking System - Diagram Reference Index

## Overview

This document provides a comprehensive index of all architectural diagrams, PlantUML sources, and generated SVG files in the Enterprise Loan Management System.

## Architecture Diagrams

### Core System Architecture

| Diagram | Description | SVG Location | PlantUML Source | ADR Reference |
|---------|-------------|--------------|-----------------|---------------|
| **Enhanced Enterprise Banking Security Architecture** | Zero-trust security model with OAuth 2.1 | [SVG](../images/Enhanced%20Enterprise%20Banking%20Security%20Architecture.svg) | N/A | ADR-006, ADR-012 |
| **Enhanced Enterprise Banking - Hexagonal Architecture** | Domain-driven design with hexagonal architecture | [SVG](../images/Enhanced%20Enterprise%20Banking%20-%20Hexagonal%20Architecture.svg) | N/A | ADR-002 |
| **Enhanced Enterprise Banking - Service Mesh Architecture** | Istio service mesh implementation | [SVG](../images/Enhanced%20Enterprise%20Banking%20-%20Service%20Mesh%20Architecture.svg) | N/A | ADR-005, ADR-008 |

### Security & Authentication

| Diagram | Description | SVG Location | PlantUML Source | ADR Reference |
|---------|-------------|--------------|-----------------|---------------|
| **OAuth 2.1 Authorization Code Flow with PKCE** | Complete OAuth 2.1 authentication flow | [SVG](../images/OAuth%202.1%20Authorization%20Code%20Flow%20with%20PKCE.svg) | [PlantUML](../puml/oauth2-sequence-flow.puml) | ADR-004 |
| **OAuth 2.1 Keycloak Authentication Architecture** | Keycloak identity provider integration | [SVG](../images/OAuth%202.1%20Keycloak%20Authentication%20Architecture.svg) | [PlantUML](../puml/oauth2-keycloak-architecture.puml) | ADR-004 |
| **Enterprise Banking Security Architecture** | Legacy security architecture reference | [SVG](../images/Enterprise%20Banking%20Security%20Architecture.svg) | N/A | ADR-006 |

### Infrastructure & Deployment

| Diagram | Description | SVG Location | PlantUML Source | ADR Reference |
|---------|-------------|--------------|-----------------|---------------|
| **Istio Service Mesh Zero-Trust Architecture** | Service mesh with zero-trust networking | [SVG](../images/Istio%20Service%20Mesh%20Zero-Trust%20Architecture.svg) | [PlantUML](../puml/istio-service-mesh-architecture.puml) | ADR-005, ADR-008 |
| **Secure Microservices Architecture Overview** | Complete microservices deployment | [SVG](../images/Secure%20Microservices%20Architecture%20Overview.svg) | [PlantUML](../puml/secure-microservices-overview.puml) | ADR-008, ADR-009 |

### Domain Architecture

| Diagram | Description | SVG Location | PlantUML Source | ADR Reference |
|---------|-------------|--------------|-----------------|---------------|
| **Enterprise Loan Management System - Component Diagram** | System component relationships | [SVG](../images/Enterprise%20Loan%20Management%20System%20-%20Component%20Diagram.svg) | N/A | ADR-001, ADR-002 |
| **Enterprise Loan Management System - Hexagonal Architecture** | Clean architecture implementation | [SVG](../images/Enterprise%20Loan%20Management%20System%20-%20Hexagonal%20Architecture.svg) | N/A | ADR-002 |

## PlantUML Source Files

### Authentication & Security
- **oauth2-sequence-flow.puml** - OAuth 2.1 authorization flow with PKCE
  - Generated SVG: `OAuth 2.1 Authorization Code Flow with PKCE.svg`
  - Exception handling scenarios included
  - FAPI compliance validation flow

- **oauth2-keycloak-architecture.puml** - Keycloak integration architecture  
  - Generated SVG: `OAuth 2.1 Keycloak Authentication Architecture.svg`
  - Multi-region deployment patterns
  - Fallback and resilience mechanisms

### Infrastructure
- **istio-service-mesh-architecture.puml** - Istio service mesh design
  - Generated SVG: `Istio Service Mesh Zero-Trust Architecture.svg`
  - Circuit breaker patterns
  - Security policies and mTLS

- **secure-microservices-overview.puml** - Complete microservices architecture
  - Generated SVG: `Secure Microservices Architecture Overview.svg`
  - Container orchestration
  - Data layer integration

## Diagram Generation Process

### PlantUML to SVG Generation

```bash
# Generate all SVG files from PlantUML sources
cd docs/puml
find . -name "*.puml" -exec plantuml -tsvg -o ../images {} \;
```

### Recent Updates

All PlantUML diagrams have been enhanced with:
- Exception handling scenarios
- Banking-specific compliance requirements  
- Multi-region deployment patterns
- Circuit breaker and resilience patterns
- Security incident response flows

Last Updated: December 2024

## Verification Commands

```bash
# Verify all SVG files exist
ls -la docs/images/*.svg

# Check PlantUML syntax
plantuml -checkonly docs/puml/*.puml

# Validate file sizes (should be > 1KB for generated diagrams)
find docs/images -name "*.svg" -size +1k
```

## Usage Guidelines

### In Documentation
- Use relative paths from the document location
- Reference both SVG and source PlantUML when available
- Include ADR references for architectural decisions

### Examples

```markdown
# From root README.md
![Architecture Overview](docs/images/Enhanced%20Enterprise%20Banking%20Security%20Architecture.svg)

# From docs/architecture/overview/
![Security Architecture](../../images/Enhanced%20Enterprise%20Banking%20Security%20Architecture.svg)

# From ADR documents
![OAuth 2.1 Flow](../../images/OAuth%202.1%20Authorization%20Code%20Flow%20with%20PKCE.svg)
[PlantUML Source](../../puml/oauth2-sequence-flow.puml)
```

## Maintenance

### Adding New Diagrams
1. Create PlantUML source in `docs/puml/`
2. Generate SVG using PlantUML
3. Update this index
4. Reference in relevant documentation

### Updating Existing Diagrams
1. Modify PlantUML source
2. Regenerate SVG
3. Verify all references still work
4. Update version notes in this index

---

*This index is maintained as part of the Enterprise Banking System architecture documentation. For questions or updates, contact the Architecture Team.*