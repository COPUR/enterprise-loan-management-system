# Enterprise Loan Management System - Documentation Index

## Purpose
This index maps the current, active documentation set and reflects the latest repository changes.

## Recent Updates (February 2026)
- Build/runtime baseline updated to **Gradle 9.3.1** + **OpenJDK 23.0.2**.
- Mongo analytics design governance added:
  - `architecture/MONGODB_BCNF_DKNF_BASELINE.md`
  - `../scripts/mongodb/migrate-open-finance-analytics.sh`
  - `../scripts/validation/validate-mongodb-analytics-design.sh`
- PMD architecture governance review added:
  - `architecture/PMD_HEXAGONAL_BIAN_REVIEW.md`
- Documentation and README links synchronized with existing file paths.

## Primary Entry Points
- `README.md` - Docs hub
- `../DOCUMENTATION_INDEX.md` - Root/global index
- `COMPREHENSIVE_SYSTEM_ANALYSIS.md` - Full system analysis
- `DEPLOYMENT_READY_STATUS.md` - Current delivery status
- `KNOWLEDGE_TRANSFER.md` - Operational handoff content

## Architecture
- `architecture/overview/ARCHITECTURE_CATALOGUE.md`
- `architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md`
- `architecture/README.md`
- `architecture/DIAGRAM_REFERENCE_INDEX.md`
- `architecture/adr/`
- `architecture/decisions/`
- `architecture/MONGODB_BCNF_DKNF_BASELINE.md`
- `architecture/PMD_HEXAGONAL_BIAN_REVIEW.md`
- `architecture/open-finance/use-cases/hld/`
- `architecture/open-finance/use-cases/test-suites/`

## Enterprise Architecture (TOGAF)
- `enterprisearchitecture/README.md`
- `enterprisearchitecture/architecture-vision/vision-document.md`
- `enterprisearchitecture/business-architecture/capability-model.md`
- `enterprisearchitecture/data-architecture/overview.md`
- `enterprisearchitecture/application-architecture/integration-catalog.md`
- `enterprisearchitecture/technology-radar/current-stack.md`
- `enterprisearchitecture/implementation-development/README-TESTING.md`
- `enterprisearchitecture/implementation-development/MASTER_IMPLEMENTATION_CHECKLIST.md`
- `enterprisearchitecture/implementation-development/MICROSERVICES_TRANSFORMATION_TASK_LIST.md`
- `enterprisearchitecture/implementation-development/MICROSERVICE_SERVICE_NOMENCLATURE.md`
- `enterprisearchitecture/implementation-development/MICROSERVICES_TRANSFORMATION_PLAN.md`
- `enterprisearchitecture/implementation-development/SERVICE_DATA_OWNERSHIP_MATRIX.md`
- `enterprisearchitecture/implementation-development/SERVICE_API_CONTRACTS_INDEX.md`
- `enterprisearchitecture/implementation-development/OBSERVABILITY_BASELINE.md`
- `enterprisearchitecture/implementation-development/transformation/PHASE_1_ANALYSIS_AND_GOVERNANCE.md`
- `enterprisearchitecture/implementation-development/transformation/PHASE_2_TEMPLATE_AND_REPO_SETUP.md`
- `enterprisearchitecture/implementation-development/transformation/PHASE_3_CICD_AUTOMATION.md`
- `enterprisearchitecture/implementation-development/transformation/PHASE_4_TERRAFORM_AND_DEPLOYMENT.md`
- `enterprisearchitecture/implementation-development/transformation/MICROSERVICES_REPO_AUDIT.md`

## Security & Compliance
- `OAuth2.1-Architecture-Guide.md`
- `architecture/overview/SECURE_MICROSERVICES_ARCHITECTURE.md`
- `enterprisearchitecture/compliance-security/FAPI2_DPOP_ANALYSIS.md`
- `enterprisearchitecture/compliance-security/PCI_DSS_V4_COMPLIANCE_MAPPING.md`
- `enterprisearchitecture/compliance-security/SENSITIVE_DATA_MANAGEMENT.md`
- `architecture/adr/ADR-004-oauth21-authentication.md`
- `architecture/adr/ADR-005-istio-service-mesh.md`
- `architecture/adr/ADR-006-zero-trust-security.md`

## API, Testing, and Operations
- `API_DOCUMENTATION_GUIDE.md`
- `API_REFERENCE_GUIDE.md`
- `POSTMAN_TESTING_GUIDE.md`
- `SIT_INTEGRATION_TEST_REPORT.md`
- `LOAD_TESTING_MANUAL.md`
- `DEPLOYMENT_GUIDE_COMPREHENSIVE.md`
- `deployment/local-development.md`
- `DEPLOYMENT.md`

## Diagrams
- Rendered: `images/security/`, `images/cross-platform/`
- PlantUML: `puml/`
- CI/CD: `ci-cd.puml`, `ci-cd.svg`

## Validation and Maintenance
- `../scripts/validation/validate-documentation.sh`
- `../scripts/run-validation.sh`
- `../scripts/validation/validate-mongodb-analytics-design.sh`
- Update this index whenever new canonical docs are added or paths change.

---
Last Updated: February 2026  
Status: Active and synchronized with current repository structure
