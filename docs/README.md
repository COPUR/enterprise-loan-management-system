# Enterprise Loan Management System Documentation

## Overview
This is the top-level documentation hub for architecture, operations, security, testing, and implementation guidance.

## Recent Changes (February 2026)
- Runtime/build baseline aligned to **OpenJDK 25.0.2** and **Gradle 9.3.1**.
- Added MongoDB analytics normalization and migration guidance:
  - `architecture/MONGODB_BCNF_DKNF_BASELINE.md`
  - `../scripts/mongodb/migrate-open-finance-analytics.sh`
  - `../scripts/validation/validate-mongodb-analytics-design.sh`
- Updated core indexes and root README links to current file locations.

## Core References
- `../DOCUMENTATION_INDEX.md` - Root documentation index (cross-repo view)
- `DOCUMENTATION_INDEX.md` - Docs-local index (category view)
- `COMPREHENSIVE_SYSTEM_ANALYSIS.md` - System-wide analysis
- `DEPLOYMENT_READY_STATUS.md` - Deployment readiness status
- `KNOWLEDGE_TRANSFER.md` - Operations and onboarding transfer

## Architecture
- `architecture/overview/ARCHITECTURE_CATALOGUE.md`
- `architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md`
- `architecture/README.md`
- `architecture/DIAGRAM_REFERENCE_INDEX.md`
- `architecture/MONGODB_BCNF_DKNF_BASELINE.md`
- `architecture/adr/` - Core ADRs
- `architecture/decisions/` - Extended ADRs

## Implementation & Enterprise Architecture
- `enterprisearchitecture/README.md`
- `enterprisearchitecture/implementation-development/README-TESTING.md`
- `enterprisearchitecture/implementation-development/COMPREHENSIVE_TESTING_STRATEGY.md`
- `enterprisearchitecture/implementation-development/TDD_IMPLEMENTATION_GUIDE.md`
- `enterprisearchitecture/application-architecture/integration-catalog.md`
- `enterprisearchitecture/data-architecture/overview.md`
- `enterprisearchitecture/technology-radar/current-stack.md`

## API, Deployment, and Operations
- `API_DOCUMENTATION_GUIDE.md`
- `API_REFERENCE_GUIDE.md`
- `DEPLOYMENT_GUIDE_COMPREHENSIVE.md`
- `deployment/local-development.md`
- `DEPLOYMENT.md`
- `SIT_INTEGRATION_TEST_REPORT.md`
- `POSTMAN_TESTING_GUIDE.md`
- `LOAD_TESTING_MANUAL.md`

## Security & Compliance
- `OAuth2.1-Architecture-Guide.md`
- `architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md`
- `enterprisearchitecture/compliance-security/PCI_DSS_V4_COMPLIANCE_MAPPING.md`
- `enterprisearchitecture/compliance-security/FAPI2_DPOP_ANALYSIS.md`
- `enterprisearchitecture/compliance-security/SENSITIVE_DATA_MANAGEMENT.md`

## Visual Architecture Assets
- `images/security/` - Rendered architecture/security diagrams
- `puml/` - PlantUML sources
- `ci-cd.puml`
- `ci-cd.svg`

## Validation Scripts
- `../scripts/validation/validate-documentation.sh`
- `../scripts/validation/validate-mongodb-analytics-design.sh`
- `../scripts/run-validation.sh`

## Baseline
- Java: `25.0.2`
- Gradle: `9.3.1`
- Spring Boot: `3.3.6`

---
Last Updated: February 2026
