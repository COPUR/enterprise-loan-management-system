# Documentation Hub

## 📋 Overview
This project contains comprehensive documentation for the Enterprise Loan Management System, organized according to industry best practices and TOGAF Enterprise Architecture standards.

## 🔄 Recent Updates (February 2026)
- Upgraded build/runtime baseline documentation to **Gradle 9.3.1** and **OpenJDK 23.0.2**.
- Added MongoDB analytics normalization guidance:
  - `docs/architecture/MONGODB_BCNF_DKNF_BASELINE.md`
- Added MongoDB migration and validation tooling documentation:
  - `scripts/mongodb/migrate-open-finance-analytics.sh`
  - `scripts/validation/validate-mongodb-analytics-design.sh`
- Added PMD governance review for hexagonal architecture and BIAN-like nomenclature:
  - `docs/architecture/PMD_HEXAGONAL_BIAN_REVIEW.md`
- Added service-mesh and organizational transformation big-picture documentation:
  - `docs/architecture/ORGANIZATIONAL_BIG_PICTURE_AS_IS_TO_BE.md`
  - `docs/architecture/ENTERPRISE_CAPABILITY_MAP.md`
  - `docs/architecture/REPOSITORY_CLEAN_CODING_REVIEW.md`
  - `docs/architecture/REPOSITORY_STRUCTURE_POLICY.md`
  - `docs/architecture/MODULE_OWNERSHIP_MAP.md`
  - `docs/GENERAL_BACKLOG.md`
- Added service-mesh transformation diagrams:
  - `docs/puml/service-mesh/organizational-as-is-big-picture.puml`
  - `docs/puml/service-mesh/organizational-to-be-big-picture.puml`
  - `docs/puml/service-mesh/enterprise-capability-map.puml`
- Updated root README and documentation references to current paths.

## 🏗️ Documentation Structure

### 📚 Enterprise Architecture (TOGAF ADM)
Complete TOGAF-compliant enterprise architecture documentation following the Architecture Development Method (ADM).

**📁 [docs/enterprisearchitecture/](docs/enterprisearchitecture/)**
- **Preliminary Phase**: EA principles, governance framework, stakeholder mapping
- **Architecture Vision**: Strategic vision, capability assessment, business scenarios
- **Business Architecture**: Capability model, value streams, process models
- **Data Architecture**: Data models, governance, analytics architecture
- **Application Architecture**: Integration catalog, component design, API specifications
- **Technology Architecture**: Technology radar, infrastructure blueprint, security standards
- **Implementation Planning**: Migration strategy, roadmaps, governance

## 🗂️ Organized Documentation by Category

### Architecture & Design
Documents organized under **docs/enterprisearchitecture/architecture-design/**:
- `PROJECT_STRUCTURE.md` - System structure documentation
- `OPEN_FINANCE_CROSS_PLATFORM_ALIGNMENT.md` - Cross-platform architecture alignment
- `COMPREHENSIVE_SECURITY_POSTURE.md` - Security architecture and design
- `DATABASE_OPTIMIZATION_ANALYSIS.md` - Database architecture analysis

Open Finance use-case HLDs under **docs/architecture/open-finance/use-cases/hld/**:
- `OPEN_FINANCE_GUARDRAILS_STANDARDS_CATALOG.md` - Categorized project guardrails/standards with HLD compliance review
- `UC001_UC015_Open_Finance_Use_Cases_Overview.md` - Consolidated architecture, APIs, and Postman structure for UC001-UC015
- `UC001_Personal_Financial_Management_HLD.md` - Detailed HLD for retail AIS data retrieval
- `UC003_Confirmation_of_Payee_HLD.md` - Detailed HLD for real-time payee verification
- `UC006_Payments_HLD.md` - Detailed HLD for payment initiation and idempotent processing
- `UC01_Consent_Management_System.md` - Foundational consent architecture, token flow, and revocation model
- `UC02_Account_Information_Service.md` - Foundational AIS architecture for read-optimized account data access
- `UC03_Payment_Initiation_Service.md` - Foundational PIS architecture for idempotent transactional payments
- `UC05_UC08_Corporate_Treasury_Services.md` - Corporate treasury and bulk payment architecture for batch/event-driven processing
- `UC09_UC10_Insurance_Services.md` - Insurance data and quote architecture using adapter/ACL integration
- `UC11_FX_Services.md` - Real-time FX and remittance architecture with streaming rates and atomic booking

Transformation and operating model documents under **docs/architecture/**:
- `ORGANIZATIONAL_BIG_PICTURE_AS_IS_TO_BE.md` - Organizational operating model transformation from current state to target state
- `ENTERPRISE_CAPABILITY_MAP.md` - Capability domains, maturity targets, and ownership model
- `REPOSITORY_CLEAN_CODING_REVIEW.md` - Repository-level structure and coding-governance review with phased cleanup roadmap
- `REPOSITORY_STRUCTURE_POLICY.md` - Mandatory structure and change-control rules for repository governance
- `MODULE_OWNERSHIP_MAP.md` - Ownership matrix for bounded contexts, shared foundations, and governance paths
- `WAVE_B_LEGACY_ROOT_RATIONALIZATION_PLAN.md` - Deprecation and migration plan for duplicate/legacy service roots
- `GENERAL_BACKLOG.md` - Cross-domain backlog for security, platform, architecture, and delivery changes

Open Finance test suites under **docs/architecture/open-finance/use-cases/test-suites/**:
- `TEST_UC001_UC002_Account_Information.md` - Functional, negative, security, and NFR tests for AIS retail/corporate data retrieval
- `TEST_UC003_Confirmation_of_Payee.md` - Matching, performance, and anti-enumeration test cases for CoP
- `TEST_UC004_Banking_Metadata.md` - Metadata enrichment and account-party/product metadata validation scenarios
- `TEST_UC005_Corporate_Treasury_Data.md` - Corporate treasury virtual account, sweeping, freshness, and entitlement test cases
- `TEST_UC006_UC008_Payments.md` - Single, international, and bulk payment test coverage with idempotency and integrity checks
- `TEST_UC008_Corporate_Bulk_Payments.md` - Corporate bulk upload, validation, processing, and reporting scenarios
- `TEST_UC007_Recurring_Payments.md` - VRP mandate, limit, revocation, and concurrency test scenarios
- `TEST_UC013_Request_to_Pay.md` - Request-to-pay creation, notification, acceptance/rejection, and duplicate handling tests
- `TEST_UC014_UC015_Open_Data.md` - Public ATM/product data functional, quality, and caching NFR tests
- `TEST_UC009_UC010_Insurance.md` - Insurance data and quote/bind test cases including schema and tamper checks
- `TEST_UC011_UC012_FX_Onboarding.md` - FX quote/booking and dynamic onboarding security/compliance tests
- `TEST_Common_Security_NFR.md` - Cross-cutting security, TLS/mTLS, rate-limiting, and performance resilience guardrail tests
- `TEST_Audit_Liability_Compliance.md` - Audit evidence, dispute simulation, and retention validation aligned to liability model controls
- `TEST_Performance_Scripts_K6.md` - K6 load/stress script templates for UC001 and UC006 NFR validation
- `TEST_Traceability_Matrix.md` - RTM mapping UC requirements to HLD components, APIs, and test-case coverage
- `TEST_Postman_Automation_Strategy.md` - Collection design, reusable scripts, environment variables, and Newman CI execution model
- `TEST_CAAP_Authentication_Redirection.md` - Mobile app deep-link redirection, PAR, and SCA/biometric validation for CAAP integration
- `TEST_Trust_Framework_Onboarding.md` - Dynamic registration, SSA validation, and mTLS/JWKS trust-framework onboarding tests
- `TEST_E2E_Certification_Checklist.md` - Production go-live functional, security, operational readiness, and sign-off checklist

### Implementation & Development
Documents organized under **docs/enterprisearchitecture/implementation-development/**:
- `TDD_IMPLEMENTATION_GUIDE.md` - Test-driven development guide
- `COMPREHENSIVE_TESTING_STRATEGY.md` - Testing implementation strategy
- `MASRUFI_INTEGRATION_EXAMPLE.md` - Integration implementation example
- `OPEN_FINANCE_IMPLEMENTATION_PLAN.md` - Implementation planning
- `OPEN_FINANCE_TASK_BREAKDOWN.md` - Development task breakdown
- `README-TESTING.md` - Testing implementation documentation

### Compliance & Security
Documents organized under **docs/enterprisearchitecture/compliance-security/**:
- `PCI_DSS_V4_COMPLIANCE_MAPPING.md` - PCI DSS compliance mapping
- `FAPI2_DPOP_ANALYSIS.md` - FAPI2 security analysis
- `SECURITY_FIXES.md` - Security implementation fixes
- `SENSITIVE_DATA_MANAGEMENT.md` - Data security management

### Migration & Upgrade
Documents organized under **docs/enterprisearchitecture/migration-upgrade/**:
- `GRADLE_8.14.13_UPGRADE_ANALYSIS.md` - Gradle upgrade analysis
- `JAVA_MIGRATION_ANALYSIS.md` - Consolidated Java migration analysis (17→21→24)
- `MIGRATION_BACKUP_CHECKLIST.md` - Migration checklist
- **Current baseline**: Gradle 9.3.1 + OpenJDK 23.0.2

### Project Management
Documents organized under **docs/enterprisearchitecture/project-management/**:
- `PROJECT_STATUS.md` - Consolidated project status and history
- `TODO_ANALYSIS.md` - Task analysis and tracking
- `MIGRATION_TASK_LIST.md` - Migration task tracking

## 🎯 Quick Navigation

### 🏢 For Business Stakeholders
- [Architecture Vision](docs/enterprisearchitecture/architecture-vision/vision-document.md) - Strategic overview
- [Business Capability Model](docs/enterprisearchitecture/business-architecture/capability-model.md) - Business capabilities
- [Project Status](docs/enterprisearchitecture/project-management/PROJECT_STATUS.md) - Current status

### 👨‍💻 For Developers
- [Integration Catalog](docs/enterprisearchitecture/application-architecture/integration-catalog.md) - API and service catalog
- [Loan Context README](loan-context/README.md) - Hexagonal architecture with PCI-DSS compliance
- [Payment Context README](payment-context/README.md) - Real-time payment processing with fraud detection
- [Shared Kernel README](shared-kernel/README.md) - Domain foundation with security patterns
- [Testing Strategy](docs/enterprisearchitecture/implementation-development/COMPREHENSIVE_TESTING_STRATEGY.md) - Testing approach
- [TDD Guide](docs/enterprisearchitecture/implementation-development/TDD_IMPLEMENTATION_GUIDE.md) - Test-driven development

### 🔧 For DevOps/SRE
- [Technology Radar](docs/enterprisearchitecture/technology-radar/current-stack.md) - Technology stack
- [Deployment Guide](docs/DEPLOYMENT_GUIDE_COMPREHENSIVE.md) - Infrastructure deployment
- [Knowledge Transfer](docs/KNOWLEDGE_TRANSFER.md) - System overview and operations

### 🛡️ For Security/Compliance
- [PCI-DSS v4.0 Architecture](docs/images/security/pci-dss-v4-compliance-architecture.svg) - Multi-layer compliance framework
- [Service-Level Security](docs/images/security/service-level-security.svg) - Zero-trust with FAPI 2.0
- [Data Protection Layers](docs/images/security/data-protection-layers.svg) - Comprehensive data protection
- [Implementation Security](docs/images/security/implementation-security-controls.svg) - Code-to-runtime security
- [Security Architecture](docs/enterprisearchitecture/architecture-design/COMPREHENSIVE_SECURITY_POSTURE.md) - Security design
- [Compliance Mapping](docs/enterprisearchitecture/compliance-security/PCI_DSS_V4_COMPLIANCE_MAPPING.md) - Regulatory compliance
- [Data Management](docs/enterprisearchitecture/compliance-security/SENSITIVE_DATA_MANAGEMENT.md) - Data protection

### 🏛️ For Enterprise Architects
- [EA Principles](docs/enterprisearchitecture/preliminary/ea-principles.md) - Architecture principles
- [Data Architecture](docs/enterprisearchitecture/data-architecture/overview.md) - Data architecture overview
- [TOGAF Overview](docs/enterprisearchitecture/README.md) - Complete TOGAF documentation

## 📊 Architecture Diagrams & PlantUML Sources

### System Overview Diagrams
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [System Architecture](docs/images/security/system-architecture-overview.svg) | Complete system with all 11 bounded contexts | [Source](docs/puml/system-overview/system-architecture-overview.puml) |
| [Technology Stack](docs/images/security/technology-stack-diagram.svg) | Complete technology stack from Java 23 to Kubernetes | [Source](docs/puml/system-overview/technology-stack-diagram.puml) |
| [Bounded Context Map](docs/images/security/bounded-context-map.svg) | Domain-driven design context relationships | [Source](docs/puml/system-overview/bounded-context-map.puml) |
| [Deployment Architecture](docs/images/security/deployment-architecture.svg) | Multi-environment deployment topology | [Source](docs/puml/system-overview/deployment-architecture.puml) |

### Security Architecture Diagrams
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [PCI-DSS v4.0 Compliance](docs/images/security/pci-dss-v4-compliance-architecture.svg) | Multi-layer PCI-DSS v4.0 compliance framework | [Source](docs/puml/security/pci-dss-v4-compliance-architecture.puml) |
| [Service-Level Security](docs/images/security/service-level-security.svg) | Zero-trust service architecture with FAPI 2.0 | [Source](docs/puml/security/service-level-security.puml) |
| [Data Protection Layers](docs/images/security/data-protection-layers.svg) | Comprehensive data protection (PCI-DSS + GDPR) | [Source](docs/puml/security/data-protection-layers.puml) |
| [Implementation Security](docs/images/security/implementation-security-controls.svg) | Code-to-runtime security implementation | [Source](docs/puml/security/implementation-security-controls.puml) |

### Bounded Context Architecture
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [Loan Context Architecture](docs/images/loan-context-architecture.svg) | Hexagonal architecture for loan domain | [Source](docs/puml/bounded-contexts/loan-context-architecture.puml) |
| [Payment Context Architecture](docs/images/payment-context-architecture.svg) | Real-time payment processing with fraud detection | [Source](docs/puml/bounded-contexts/payment-context-architecture.puml) |

### Platform Architecture Diagrams  
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [Islamic Banking (AmanahFi)](docs/images/security/islamic-banking-architecture.svg) | Complete Islamic banking platform architecture | [Source](docs/puml/amanahfi-platform/islamic-banking-architecture.puml) |
| [CBDC Payment Flow](docs/images/security/cbdc-payment-flow.svg) | UAE Digital Dirham payment processing | [Source](docs/puml/amanahfi-platform/cbdc-payment-flow.puml) |
| [Sharia Compliance Flow](docs/images/security/sharia-compliance-flow.svg) | End-to-end Sharia compliance validation | [Source](docs/puml/amanahfi-platform/sharia-compliance-flow.puml) |
| [MasruFi Framework](docs/images/security/framework-integration-architecture.svg) | Framework integration architecture | [Source](docs/puml/masrufi-framework/framework-integration-architecture.puml) |
| [Islamic Finance Products](docs/images/security/islamic-finance-product-flow.svg) | Islamic finance product lifecycle | [Source](docs/puml/masrufi-framework/islamic-finance-product-flow.puml) |

### Service Mesh Transformation Diagrams
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [Open Finance Runtime As-Is](docs/puml/service-mesh/as-is-open-finance-runtime.svg) | Current runtime trust boundaries and service topology | [Source](docs/puml/service-mesh/as-is-open-finance-runtime.puml) |
| [Open Finance Runtime To-Be](docs/puml/service-mesh/to-be-open-finance-service-mesh.svg) | Target mesh architecture with centralized AAA and distributed policy enforcement | [Source](docs/puml/service-mesh/to-be-open-finance-service-mesh.puml) |
| [Organizational Big Picture As-Is](docs/puml/service-mesh/organizational-as-is-big-picture.svg) | Existing organization, governance, and delivery model | [Source](docs/puml/service-mesh/organizational-as-is-big-picture.puml) |
| [Organizational Big Picture To-Be](docs/puml/service-mesh/organizational-to-be-big-picture.svg) | Target operating model with domain streams and platform teams | [Source](docs/puml/service-mesh/organizational-to-be-big-picture.puml) |
| [Enterprise Capability Map](docs/puml/service-mesh/enterprise-capability-map.svg) | Current-to-target capability landscape and maturity trajectory | [Source](docs/puml/service-mesh/enterprise-capability-map.puml) |

### Cross-Platform Integration Diagrams
| Diagram | Description | PlantUML Source |
|---------|-------------|-----------------|
| [Open Finance Ecosystem](docs/images/cross-platform/open-finance-ecosystem.svg) | Complete cross-platform Open Finance architecture | [Source](docs/puml/cross-platform/open-finance-ecosystem.puml) |
| [Multi-Platform Consent Flow](docs/images/cross-platform/multi-platform-consent-simple.svg) | Consent management across all 3 platforms | [Source](docs/puml/cross-platform/multi-platform-consent-simple.puml) |
| [Cross-Platform Data Sharing](docs/images/cross-platform/data-sharing-simple.svg) | Data sharing and aggregation flows | [Source](docs/puml/cross-platform/data-sharing-simple.puml) |

## 📈 Key Achievements

### Project Completion Status
✅ **All 21/21 tasks completed** from the original Open Finance implementation plan
✅ **Production-ready system** with comprehensive testing and security
✅ **TOGAF Enterprise Architecture** documentation complete
✅ **Multi-platform integration** (Enterprise Loans, AmanahFi, Masrufi)
✅ **Regulatory compliance** (UAE CBUAE C7/2023, PCI-DSS v4, FAPI 2.0)

### Technical Highlights
- **170+ tests passing** with TDD approach
- **100% security compliance** with banking-grade security
- **Microservices architecture** with clean architecture principles
- **Event-driven design** with Kafka and CQRS patterns
- **Cloud-native deployment** with Docker, Kubernetes, and Terraform
- **Complete documentation** including knowledge transfer materials

## 🚀 System Capabilities

### Core Banking Features
- **Loan Management**: Complete lifecycle from application to closure
- **Payment Processing**: Real-time payment processing with fraud detection
- **Customer Management**: Digital onboarding with KYC/AML compliance
- **Risk Assessment**: AI-powered credit scoring and risk analytics
- **Islamic Banking**: Full Sharia-compliant banking operations

### Open Finance & Integration
- **FAPI 2.0 Compliance**: Financial-grade API security
- **Open Banking APIs**: UAE CBUAE regulation C7/2023 compliant
- **Cross-platform Integration**: Unified experience across platforms
- **Real-time Data Sharing**: Consent-based data sharing
- **Analytics Platform**: MongoDB-based silver copy analytics

### Technology Stack
- **Java 23**: Modern language with virtual threads
- **Spring Boot 3.3.6**: Latest framework with native compilation
- **PostgreSQL 16.9**: Primary database with ACID compliance
- **Apache Kafka**: Event streaming and messaging
- **Redis**: Distributed caching and session management
- **Kubernetes**: Container orchestration with auto-scaling

## 📊 System Architecture

### Microservices Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                          │
│                 (FAPI 2.0 + OAuth2.1)                  │
└─────────────────────┬───────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    │                 │                 │
┌───▼────┐    ┌──────▼─────┐    ┌──────▼─────┐
│Customer│    │    Loan    │    │  Payment   │
│Service │    │  Service   │    │  Service   │
└────────┘    └────────────┘    └────────────┘
    │                 │                 │
    └─────────────────┼─────────────────┘
                      │
              ┌───────▼────────┐
              │ Event Streaming │
              │    (Kafka)     │
              └────────────────┘
```

### Data Architecture
- **PostgreSQL**: Transactional data with event sourcing
- **MongoDB**: Analytics and reporting (silver copy)
- **Redis**: Distributed caching and sessions
- **Event Store**: Complete audit trail with CQRS

### Security Architecture
- **Zero Trust**: Never trust, always verify
- **Multi-layered Security**: Defense in depth
- **Encryption**: AES-256-GCM at rest, TLS 1.3 in transit
- **Authentication**: OAuth 2.1 + DPoP + mTLS

## 🔄 Continuous Improvement

The system is designed for continuous evolution with:
- **Automated CI/CD**: GitHub Actions pipelines with comprehensive testing
- **Performance Monitoring**: Prometheus + Grafana observability
- **Security Scanning**: Automated vulnerability assessments
- **Compliance Monitoring**: Real-time regulatory compliance checks
- **Architecture Governance**: Regular architecture reviews and updates

---

**Project Status**: ✅ **PRODUCTION READY**  
**Documentation Status**: ✅ **COMPLETE**  
**Last Updated**: February 2026  
**Next Review**: April 2026

For detailed information, start with the [TOGAF Enterprise Architecture README](docs/enterprisearchitecture/README.md).
