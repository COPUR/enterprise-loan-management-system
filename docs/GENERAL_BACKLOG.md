# General Backlog (Architecture, Security, Platform, and Delivery)

## Backlog Purpose

This backlog consolidates enterprise-level work required to move from current-state architecture to target-state platform operations, aligned with:

- Centralized AAA (Keycloak + LDAP + DPoP)
- Service mesh security runtime
- Clean coding and repository hygiene
- Capability maturity uplift

## Prioritization Model

- `P0` Critical security/compliance or production stability blockers
- `P1` High-value platform and delivery enablers
- `P2` Optimization and developer-experience improvements
- `P3` Deferred or exploratory enhancements

## Epic Backlog

| ID | Priority | Epic | Description | Estimate (SP) | Owner |
| --- | --- | --- | --- | --- | --- |
| EP-001 | P0 | Central AAA Platform | Keycloak HA, LDAP federation, token policies, operational runbooks | 34 | Identity Platform Team |
| EP-002 | P0 | DPoP Enforcement | Gateway DPoP verifier, replay cache, conformance tests | 21 | API Security Team |
| EP-003 | P0 | Mesh Zero-Trust Baseline | Strict mTLS, authz policies, egress controls | 34 | Mesh Platform Team |
| EP-004 | P1 | Distributed AuthZ Agents | OPA/ext_authz policy lifecycle and integration | 21 | Security Platform Team |
| EP-005 | P1 | Observability Baseline | OTel, SIEM integration, SLO dashboards, alert standards | 21 | SRE/Observability Team |
| EP-006 | P1 | CI/CD Security Gates | SAST/SCA/coverage/policy gates and drift detection | 21 | DevSecOps Team |
| EP-007 | P1 | Repository Hygiene | Folder rationalization, naming alignment, deprecation cleanup | 21 | Architecture + Platform |
| EP-008 | P2 | Contract and Test Modernization | OpenAPI/contract tests, integration test stabilization | 13 | Domain Teams |
| EP-009 | P2 | Capability Maturity Program | Quarterly capability scoring and roadmap governance | 8 | Architecture Board |
| EP-010 | P3 | Advanced Resilience | Multi-region failover and chaos automation expansion | 13 | SRE Team |

## Story-Level Backlog (Initial Cut)

| ID | Epic | Priority | Story | Estimate (SP) | Dependency |
| --- | --- | --- | --- | --- | --- |
| ST-001 | EP-001 | P0 | Deploy Keycloak HA in non-prod with PostgreSQL HA | 8 | None |
| ST-002 | EP-001 | P0 | Configure LDAP federation and group-to-role mappers | 5 | ST-001 |
| ST-003 | EP-001 | P0 | Implement realm/client config-as-code with drift checks | 8 | ST-001 |
| ST-004 | EP-002 | P0 | Implement DPoP proof verifier at gateway | 8 | ST-003 |
| ST-005 | EP-002 | P0 | Implement replay cache and replay rejection semantics | 5 | ST-004 |
| ST-006 | EP-002 | P0 | Build DPoP conformance test suite in CI | 8 | ST-004 |
| ST-007 | EP-003 | P0 | Enable sidecar injection and permissive mTLS in staging | 5 | ST-001 |
| ST-008 | EP-003 | P0 | Move selected namespaces to strict mTLS | 8 | ST-007 |
| ST-009 | EP-003 | P0 | Add deny-by-default AuthorizationPolicy baseline | 8 | ST-008 |
| ST-010 | EP-004 | P1 | Integrate ext_authz service with gateway/mesh | 8 | ST-009 |
| ST-011 | EP-004 | P1 | Publish initial ABAC/RBAC policy packs per domain | 8 | ST-010 |
| ST-012 | EP-005 | P1 | Standardize OpenTelemetry and trace propagation | 5 | ST-007 |
| ST-013 | EP-005 | P1 | Build security/latency/error SLO dashboards | 5 | ST-012 |
| ST-014 | EP-006 | P1 | Add policy lint and IaC validation gates | 5 | ST-009 |
| ST-015 | EP-006 | P1 | Enforce coverage >=85% and security blockers | 5 | None |
| ST-016 | EP-007 | P1 | Publish repo structure policy and naming standards | 3 | None |
| ST-017 | EP-007 | P1 | Resolve duplicate/legacy folder ownership mapping | 5 | ST-016 |
| ST-018 | EP-008 | P2 | Add contract tests for high-risk APIs (consent/payment) | 8 | ST-015 |
| ST-019 | EP-009 | P2 | Establish quarterly capability review cadence | 3 | None |
| ST-020 | EP-010 | P3 | Run mesh + identity chaos drills and automate recovery checks | 8 | ST-008 |

## Sprint Proposal (First 3 Sprints)

## Sprint 1

- ST-001, ST-002, ST-003, ST-015, ST-016

## Sprint 2

- ST-004, ST-005, ST-007, ST-012, ST-014

## Sprint 3

- ST-006, ST-008, ST-009, ST-010, ST-013

## Definition of Ready

1. Clear acceptance criteria and security impact.
2. Test strategy defined (unit/integration/security).
3. Dependencies identified and owned.
4. Rollback strategy documented.

## Definition of Done

1. Code + policy + docs updated.
2. Required tests and quality gates pass.
3. Observability and runbook updates included.
4. Backlog status and architecture traceability updated.

## Wave A Execution Status (Kickoff)

| Item | Status | Evidence |
| --- | --- | --- |
| Repository structure policy published | Done | `docs/architecture/REPOSITORY_STRUCTURE_POLICY.md` |
| Module ownership map published | Done | `docs/architecture/MODULE_OWNERSHIP_MAP.md` |
| PR governance checklist expanded | Done | `.github/pull_request_template.md` |
| CODEOWNERS hardened for governance and shared foundation paths | Done | `.github/CODEOWNERS` |
| CI governance gate added | Done | `.github/workflows/ci.yml`, `tools/validation/validate-repo-governance.sh` |
| Legacy path freeze enforcement | Done | CI blocking in place and policy markers added: `archive/README.md`, `temp-src/README.md`, `simple-test/README.md` |

## Wave B Execution Status (Kickoff)

| Item | Status | Evidence |
| --- | --- | --- |
| Duplicate legacy roots marked deprecated | Done | `bankwide/README.md`, `bank-wide-services/README.md`, `loan-service/README.md`, `payment-service/README.md` |
| Deprecated-root CI enforcement added | Done | `tools/validation/validate-repo-governance.sh` |
| Governance validator implemented with TDD and coverage gate | Done | `tools/validation/repo_governance_validator.py`, `tools/validation/tests/test_repo_governance_validator.py`, CI fail-under 90% |
| Rationalization plan documented | Done | `docs/architecture/WAVE_B_LEGACY_ROOT_RATIONALIZATION_PLAN.md` |
| Dependency/reference inventory for deprecated roots | Done | Build include dependency check enforced; references categorized (runtime service naming vs folder dependency) |
| Residual tracked file cleanup in deprecated roots | Done | `bankwide/build.gradle` removed and deletion path allowed by governance validator |
| Full relocation/archive of deprecated roots | Planned | Archive/remove deprecated roots after two release cycles and migration sign-off |
