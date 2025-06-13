# Enterprise Architecture Documentation Index

## Overview

This documentation follows TOGAF Enterprise Architecture principles with BDAT (Business, Data, Application, Technology) categorization and Domain Level structure for the Enterprise Loan Management System.

## Architecture Categories

### Business Architecture
- **Domain Models**: Core business domain definitions and bounded contexts
  - [Domain Model](business-architecture/domain-models/domain-model.puml)
  - [Bounded Contexts](business-architecture/domain-models/bounded-contexts.puml)
- **Use Cases**: Business scenarios and banking workflows
  - [Technology Use Case Mapping](business-architecture/use-cases/TECHNOLOGY_USECASE_MAPPING.md)
  - [Banking Workflow](business-architecture/use-cases/banking-workflow.puml)
- **Scenarios**: Business case demonstrations and showcase scenarios
  - [Technology Showcase Summary](business-architecture/scenarios/TECHNOLOGY_SHOWCASE_SUMMARY.md)
  - [Showcase Scenarios](business-architecture/scenarios/SHOWCASE_SCENARIOS.md)

### Application Architecture
- **Microservices**: Service architecture and component designs
  - [Component Diagram](application-architecture/microservices/component-diagram.puml)
  - [Microservices Architecture](application-architecture/microservices/microservices-architecture-diagram.puml)
  - [Hexagonal Architecture](application-architecture/microservices/hexagonal-architecture.puml)
  - [Simple Architecture](application-architecture/microservices/simple-architecture.puml)
  - [Gradle 9 Microservices Upgrade Report](application-architecture/microservices/GRADLE_9_MICROSERVICES_UPGRADE_REPORT.md)
- **API Specifications**: RESTful and GraphQL API documentation
  - [OpenFinance API Documentation](application-architecture/api-specifications/OPENFINANCE_API_DOCUMENTATION.md)
- **Integration Patterns**: SAGA patterns and workflow orchestration
  - [SAGA Workflow Diagram](application-architecture/integration-patterns/saga-workflow-diagram.puml)
- **Sequence Diagrams**: Process flow visualizations
  - [Loan Creation Sequence](application-architecture/sequence-diagrams/loan-creation-sequence.puml)
  - [Payment Processing Sequence](application-architecture/sequence-diagrams/payment-processing-sequence.puml)

### Data Architecture
- **Data Models**: Database schemas and entity relationships
  - [ER Diagram](data-architecture/data-models/er-diagram.puml)
  - [Database Isolation Diagram](data-architecture/data-models/database-isolation-diagram.puml)

### Technology Architecture
- **Infrastructure Diagrams**: Cloud and infrastructure architecture
  - [AWS EKS Architecture](technology-architecture/infrastructure-diagrams/aws-eks-architecture.puml)
  - [Cache Performance Architecture](technology-architecture/infrastructure-diagrams/cache-performance-architecture.puml)
- **Infrastructure**: Technology stack and modernization reports
  - [Gradle Modernization Report](technology-architecture/infrastructure/GRADLE_MODERNIZATION_REPORT.md)
  - [Redis ElastiCache Documentation](technology-architecture/infrastructure/REDIS_ELASTICACHE_DOCUMENTATION.md)
  - [Cache Performance Tests](technology-architecture/infrastructure/CACHE_PERFORMANCE_TESTS.md)
- **Deployment**: Deployment guides and infrastructure setup
  - [AWS EKS Deployment Complete](technology-architecture/deployment/AWS_EKS_DEPLOYMENT_COMPLETE.md)
  - [Gitpod Deployment](technology-architecture/deployment/GITPOD_DEPLOYMENT.md)
  - [CI/CD Pipeline](technology-architecture/deployment/ci-cd-pipeline.puml)
- **Monitoring**: System monitoring and observability
  - [System Status Report](technology-architecture/monitoring/SYSTEM_STATUS_REPORT.md)
  - [Monitoring Documentation](technology-architecture/monitoring/MONITORING_DOCUMENTATION.md)
  - [Monitoring Observability](technology-architecture/monitoring/monitoring-observability.puml)

### Security Architecture
- **Compliance**: FAPI and regulatory compliance documentation
  - [FAPI MCP LLM Interface Summary](security-architecture/compliance/FAPI_MCP_LLM_INTERFACE_SUMMARY.md)
- **Security Models**: Security architecture and authentication patterns
  - [FAPI Security Architecture](security-architecture/security-models/fapi-security-architecture.puml)
  - [Security Architecture Diagram](security-architecture/security-models/security-architecture-diagram.puml)

### Enterprise Governance
- **Standards**: Development and documentation standards
  - [Competitive Technology Analysis](enterprise-governance/standards/COMPETITIVE_TECHNOLOGY_ANALYSIS.md)
  - [Documentation Standards Complete](enterprise-governance/standards/DOCUMENTATION_STANDARDS_COMPLETE.md)
- **Documentation**: Project documentation and artifact summaries
  - [Repository Summary](enterprise-governance/documentation/REPOSITORY_SUMMARY.md)
  - [Documentation Update Summary](enterprise-governance/documentation/DOCUMENTATION_UPDATE_SUMMARY.md)
  - [Updated Artifacts Summary](enterprise-governance/documentation/UPDATED_ARTIFACTS_SUMMARY.md)
  - [Git Setup](enterprise-governance/documentation/GIT_SETUP.md)
  - [Replit Configuration](enterprise-governance/documentation/replit.md)
- **Quality Assurance**: Testing frameworks and validation reports
  - [Testing Documentation](enterprise-governance/quality-assurance/TESTING.md)
  - [Regression Test Report](enterprise-governance/quality-assurance/REGRESSION_TEST_REPORT.md)
  - [Gitpod Validation Report](enterprise-governance/quality-assurance/GITPOD_VALIDATION_REPORT.md)
  - [TDD Coverage Visualization](enterprise-governance/quality-assurance/tdd-coverage-visualization.puml)

## Generated Diagrams

All PlantUML diagrams are automatically compiled to SVG and PNG formats for technical documentation and presentation purposes.

### Architecture Visualization Categories:
- **Business Domain Models**: Domain boundaries and business contexts
- **Application Sequences**: Process flows and service interactions  
- **Data Relationships**: Database schemas and data flow patterns
- **Technology Infrastructure**: Cloud architecture and deployment models
- **Security Compliance**: Authentication and authorization patterns

## Navigation

- [Business Architecture](business-architecture/)
- [Application Architecture](application-architecture/)
- [Data Architecture](data-architecture/)
- [Technology Architecture](technology-architecture/)
- [Security Architecture](security-architecture/)
- [Enterprise Governance](enterprise-governance/)

## Enterprise Architecture Compliance

This documentation structure follows:
- **TOGAF 9.2** Enterprise Architecture Framework
- **BDAT** categorization (Business, Data, Application, Technology)
- **Banking Industry Standards** for financial services architecture
- **Domain-Driven Design** principles for bounded context separation
- **Formal Banking Language** without emojis as per enterprise standards