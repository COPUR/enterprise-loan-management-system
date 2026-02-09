# Open Finance Guardrails and Standards Catalog

## 1. Scope and Source Documents

This catalog consolidates architecture/security/data/NFR guardrails referenced across the project and applies them to Open Finance HLD validation.

Primary sources reviewed:

* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
* `docs/HEXAGONAL_ARCHITECTURE_GUARDRAILS.md`
* `docs/architecture/KEYCLOAK_FAPI_DISTRIBUTED_CONSENT_ARCHITECTURE.md`
* `docs/enterprisearchitecture/compliance-security/FAPI2_DPOP_ANALYSIS.md`
* `docs/OAuth2.1-Architecture-Guide.md`
* `docs/API_REFERENCE_GUIDE.md`
* `docs/adr/ADR-012-open-finance-integration.md`
* `docs/architecture/adr/ADR-006-zero-trust-security.md`
* `docs/architecture/adr/ADR-003-saga-pattern.md`
* `docs/architecture/decisions/ADR-013-non-functional-requirements-architecture.md`
* `docs/architecture/MONGODB_BCNF_DKNF_BASELINE.md`

---

## 2. Guardrails and Standards by Category

### A. Security and Identity Standards

* Enforce OAuth 2.1 + OIDC, FAPI 2.0 profile, and DPoP-bound tokens.
* Enforce mTLS/TLS 1.3 for service and client communication.
* Require PAR + PKCE in authorization flows.
* Require anti-replay controls (`jti`/nonce checks) and strict token validation.
* Require immutable audit trails for sensitive operations.

Key references:

* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
* `docs/architecture/KEYCLOAK_FAPI_DISTRIBUTED_CONSENT_ARCHITECTURE.md`
* `docs/enterprisearchitecture/compliance-security/FAPI2_DPOP_ANALYSIS.md`
* `docs/architecture/adr/ADR-006-zero-trust-security.md`

### B. API and Protocol Standards

* Require FAPI security headers (`X-FAPI-Interaction-ID`, related request context headers).
* Use DPoP token type for protected resource endpoints.
* Enforce idempotency keys for write operations.
* Use consistent error contract and rate-limit signaling.

Key references:

* `docs/API_REFERENCE_GUIDE.md`
* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`

### C. Architecture and Service Boundary Standards

* Enforce bounded context isolation and anti-corruption layers for external standards.
* Enforce hexagonal architecture (ports/adapters), no infrastructure leakage into domain.
* Enforce repository pattern and explicit transaction boundaries.

Key references:

* `docs/HEXAGONAL_ARCHITECTURE_GUARDRAILS.md`
* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
* `docs/adr/ADR-012-open-finance-integration.md`

### D. Distributed Transaction and Consistency Standards

* Prefer saga orchestration with compensating actions for cross-service workflows.
* Avoid distributed 2PC for microservice transactions.
* Require idempotent saga steps and timeout handling.

Key references:

* `docs/architecture/adr/ADR-003-saga-pattern.md`
* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`

### E. Data Architecture Standards

* Use PostgreSQL as transactional/event source of truth for critical operational state.
* Use Redis for low-latency cache/idempotency/replay/rate-limit controls.
* Use MongoDB as analytics silver copy where applicable.
* Enforce BCNF/DKNF constraints for Open Finance analytics collections.
* Enforce PII encryption/tokenization and immutable auditing.

Key references:

* `docs/architecture/KEYCLOAK_FAPI_DISTRIBUTED_CONSENT_ARCHITECTURE.md`
* `docs/architecture/MONGODB_BCNF_DKNF_BASELINE.md`
* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`

### F. NFR / SLO / Resilience Standards

* Performance SLO examples: auth P95 < 100ms, payment processing P95 < 200ms, critical API P95 < 500ms.
* Availability goals: core services >= 99.99%; auth path >= 99.995%.
* Define RTO/RPO and use circuit breaker + retry for external dependencies.

Key references:

* `docs/architecture/decisions/ADR-013-non-functional-requirements-architecture.md`
* `docs/OAuth2.1-Architecture-Guide.md`

### G. Compliance and Governance Standards

* PCI DSS, GDPR, and FAPI requirements are mandatory guardrails.
* Audit/compliance reporting must be operationally supported.
* Architecture guardrail compliance is a merge gate expectation.

Key references:

* `docs/architecture/adr/ADR-006-zero-trust-security.md`
* `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
* `docs/HEXAGONAL_ARCHITECTURE_GUARDRAILS.md`

---

## 3. Latest HLD Compliance Review and Fixes

The latest Open Finance HLD documents were reviewed and updated for alignment:

* `UC01_Consent_Management_System.md`
* `UC02_Account_Information_Service.md`
* `UC03_Payment_Initiation_Service.md`
* `UC05_UC08_Corporate_Treasury_Services.md`
* `UC09_UC10_Insurance_Services.md`
* `UC11_FX_Services.md`
Additional consistency updates were also applied to:
* `UC001_Personal_Financial_Management_HLD.md`
* `UC003_Confirmation_of_Payee_HLD.md`
* `UC006_Payments_HLD.md`
* `UC001_UC015_Open_Finance_Use_Cases_Overview.md`

### Key Corrections Applied

* Upgraded security baseline language from OAuth 2.0/Bearer usage to OAuth 2.1 + FAPI 2.0 + DPoP + mTLS.
* Added FAPI interaction/correlation requirements in API signatures.
* Removed 2PC positioning in payment initiation and aligned to saga orchestration.
* Aligned persistence strategy to PostgreSQL (transactional/event SoR), Redis (hot control/cache), MongoDB (analytics silver copy).
* Added explicit idempotency behavior for write APIs and replay protection language.
* Strengthened NFR targets and audit/immutability requirements in each HLD.

---

## 4. Ongoing Review Checklist for New HLDs

* Security profile is explicitly FAPI 2.0 + OAuth 2.1 + DPoP + mTLS.
* API examples include required FAPI/DPoP/idempotency headers.
* Cross-service transactions use saga + compensation, not distributed 2PC.
* Data store roles are explicit (PostgreSQL SoR, Redis control/cache, Mongo analytics).
* NFR section includes latency, availability, and resilience controls.
* Audit and compliance controls are explicit for all sensitive operations.
