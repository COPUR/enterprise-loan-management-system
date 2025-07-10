# Diagram Reference Index

This document provides a comprehensive index of all architecture diagrams in the Enterprise Loan Management System documentation.

## Architecture Diagrams

### System Context
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - System Context (Java 21).svg`
- **Source**: `docs/architecture/system-context.puml`
- **Description**: High-level system context showing external systems and user interactions
- **Last Updated**: January 9, 2025

### Hexagonal Architecture
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Hexagonal Architecture (Java 21).svg`
- **Source**: `docs/architecture/hexagonal-architecture.puml`
- **Description**: Detailed hexagonal architecture with ports, adapters, and domain model
- **Last Updated**: January 9, 2025

### Component Diagram
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Component Diagram.svg`
- **Source**: `docs/architecture/component-diagram.puml`
- **Description**: System components and their relationships

### Deployment Diagram
- **File**: `docs/architecture/generated-diagrams/Enterprise Loan Management System - Deployment Diagram.svg`
- **Source**: `docs/architecture/deployment-diagram.puml`
- **Description**: System deployment architecture

## Application Architecture Diagrams

### Microservices Architecture
- **File**: `docs/application-architecture/microservices/generated-diagrams/Enterprise Loan Management - Microservices Architecture.svg`
- **Source**: `docs/application-architecture/microservices/microservices-architecture-diagram.puml`
- **Description**: Java 21 microservices with Istio service mesh
- **Last Updated**: January 9, 2025

### Sequence Diagrams

#### Loan Creation Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/Loan Creation Sequence (Java 21 + FAPI 2.0).svg`
- **Source**: `docs/application-architecture/sequence-diagrams/loan-creation-sequence.puml`
- **Description**: Loan creation process with FAPI 2.0 and Zero Trust security
- **Last Updated**: January 9, 2025

#### OAuth2 Authentication Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/OAuth2.1 Authentication & Authorization Sequence.svg`
- **Source**: `docs/application-architecture/sequence-diagrams/oauth2-authentication-sequence.puml`
- **Description**: OAuth 2.1 authentication flow with FAPI 2.0 compliance

#### Payment Processing Sequence
- **File**: `docs/application-architecture/sequence-diagrams/generated-diagrams/Payment Processing Sequence.svg`
- **Source**: `docs/application-architecture/sequence-diagrams/payment-processing-sequence.puml`
- **Description**: Payment processing workflow with fraud detection

## Security Architecture Diagrams

### FAPI 2.0 Security Architecture
- **File**: `docs/security-architecture/security-models/generated-diagrams/FAPI 2.0 Security Architecture (Java 21).svg`
- **Source**: `docs/security-architecture/security-models/fapi-security-architecture.puml`
- **Description**: FAPI 2.0 security framework with Zero Trust architecture
- **Last Updated**: January 9, 2025

### OWASP Security Architecture
- **File**: `docs/security-architecture/security-models/generated-diagrams/OWASP Top 10 Security Architecture.svg`
- **Source**: `docs/security-architecture/security-models/security-architecture-diagram.puml`
- **Description**: OWASP Top 10 security implementation

## Business Architecture Diagrams

### Domain Model
- **File**: `docs/business-architecture/domain-models/generated-diagrams/Domain Model.svg`
- **Source**: `docs/business-architecture/domain-models/domain-model.puml`
- **Description**: Core business domain model with entities and relationships

### Bounded Contexts
- **File**: `docs/business-architecture/domain-models/generated-diagrams/Bounded Contexts.svg`
- **Source**: `docs/business-architecture/domain-models/bounded-contexts.puml`
- **Description**: Domain-driven design bounded contexts

### Banking Workflow
- **File**: `docs/business-architecture/use-cases/generated-diagrams/Banking Workflow.svg`
- **Source**: `docs/business-architecture/use-cases/banking-workflow.puml`
- **Description**: Core banking process workflows

## Technology Architecture Diagrams

### AWS EKS Architecture
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/AWS EKS Enterprise Loan Management System Architecture (Java 21).svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/aws-eks-architecture.puml`
- **Description**: AWS EKS deployment architecture with Java 21 optimization
- **Last Updated**: January 9, 2025

### Cache Performance Architecture
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/Multi-Level Cache Architecture - Enterprise Loan Management System.svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/cache-performance-architecture.puml`
- **Description**: Multi-level caching architecture for performance optimization

### OAuth2.1 Infrastructure
- **File**: `docs/technology-architecture/infrastructure-diagrams/generated-diagrams/OAuth2.1 Infrastructure Architecture - Banking System.svg`
- **Source**: `docs/technology-architecture/infrastructure-diagrams/oauth2-infrastructure-architecture.puml`
- **Description**: OAuth 2.1 infrastructure setup with Keycloak

### Monitoring & Observability
- **File**: `docs/technology-architecture/monitoring/generated-diagrams/Monitoring & Observability - Enterprise Loan Management System.svg`
- **Source**: `docs/technology-architecture/monitoring/monitoring-observability.puml`
- **Description**: Comprehensive monitoring and observability architecture

## Data Architecture Diagrams

### Entity Relationship Diagram
- **File**: `docs/data-architecture/data-models/generated-diagrams/Entity Relationship Diagram.svg`
- **Source**: `docs/data-architecture/data-models/er-diagram.puml`
- **Description**: Database entity relationships and schema design

### Database Isolation Architecture
- **File**: `docs/data-architecture/data-models/generated-diagrams/Database Isolation Architecture.svg`
- **Source**: `docs/data-architecture/data-models/database-isolation-diagram.puml`
- **Description**: Database isolation and multi-tenancy architecture

## Usage Notes

### Viewing Diagrams
- All SVG files can be viewed directly in web browsers
- GitHub automatically renders SVG files in markdown
- For best quality, use the latest versioned files

### Updating Diagrams
1. Edit the source PlantUML (.puml) file
2. Generate new SVG using: `plantuml -tsvg -o target_directory source_file.puml`
3. Update references in markdown files if needed
4. Commit both PlantUML and SVG files

### Diagram Conventions
- All diagrams use consistent color schemes
- Titles include technology versions (Java 21, Spring Boot 3.4.3)
- Security diagrams emphasize FAPI 2.0 and Zero Trust
- Architecture diagrams show current implementation

---

**Last Updated**: January 9, 2025  
**Status**: Current with Java 21 implementation  
**Maintenance**: Update when architecture changes
