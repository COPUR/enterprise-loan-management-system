# Repository Clean Coding Review and Reorganization Plan

## Context

This review evaluates repository structure and coding-governance posture to improve maintainability, architectural clarity, and delivery reliability for a multi-context banking platform.

The review is grounded in:

- Clean Code principles
- SOLID and DDD boundaries
- Hexagonal architecture contracts
- 12-factor operational standards
- Security-by-default (FAPI/DPoP/mTLS aligned)

## Current-State Findings (High-Level)

1. Strong architecture depth exists, but structure is fragmented across multiple historical roots.
2. Naming and placement are inconsistent (`readme.md` vs `README.md`, duplicate-style folders, legacy/archive trees).
3. There are parallel context implementations (top-level contexts + services/bounded-contexts) that need clear ownership contracts.
4. Documentation is extensive but discoverability can be improved with a tighter top-level index and canonical paths.
5. Temporary and migration folders should be governed by explicit retention and archival rules.

## Root-Level Inventory Review

| Item | Type | Assessment | Recommendation |
| --- | --- | --- | --- |
| `.claude` | Dir | Tool-specific metadata | Keep, document purpose in tooling section |
| `.env.template` | File | Useful runtime template | Keep, ensure secrets never committed |
| `.github` | Dir | Governance and CI controls in place | Keep as canonical repo governance root |
| `.gitattributes` | File | Standard | Keep |
| `.gitignore` | File | Functional but broad patterns may hide intended docs | Keep, review wildcard exclusions quarterly |
| `.gradle` | Dir | Local build artifacts | Keep ignored; never commit local cache |
| `.idea` | Dir | IDE metadata | Keep ignored; avoid project-specific noise |
| `.vscode` | Dir | IDE metadata | Keep minimal settings only |
| `CODE_OF_CONDUCT.md` | File | Present and adequate | Keep |
| `CONTRIBUTING.md` | File | Present and improving | Keep, add architecture workflow references |
| `DOCUMENTATION_INDEX.md` | File | Rich but broad | Keep, link new canonical docs |
| `LICENSE` | File | Standard | Keep |
| `amanahfi-platform` | Dir | Bounded platform subtree | Keep, enforce consistent module conventions |
| `api` | Dir | API assets exist | Keep, standardize OpenAPI placement policy |
| `archive` | Dir | Historical snapshots | Keep as read-only; add retention policy |
| `bank-wide-services` | Dir | Legacy naming variant | Consolidate naming with `bankwide` or archive |
| `bankwide` | Dir | Legacy naming variant | Consolidate naming with `bank-wide-services` or archive |
| `build.gradle` | File | Root build orchestration | Keep as canonical root build file |
| `buildSrc` | Dir | Shared build logic | Keep, enforce backward-compatible plugin contracts |
| `ci` | Dir | CI templates/assets | Keep, align with `.github` and pipeline ownership |
| `common` | Dir | Shared modules | Keep with strict "no domain leakage" rule |
| `compliance-context` | Dir | Domain context | Keep, verify boundary contracts |
| `config` | Dir | Static analysis and build config | Keep, expand lint/security profiles |
| `customer-context` | Dir | Domain context | Keep |
| `data` | Dir | Samples/fixtures/outputs mixed | Split into `test-fixtures` and governed sample-data store |
| `docs` | Dir | Documentation hub | Keep as canonical documentation root |
| `gradle` | Dir | Wrapper and build internals | Keep |
| `gradle.properties` | File | Build properties | Keep; avoid env-specific hardcoding |
| `gradlew` | File | Build wrapper | Keep |
| `gradlew.bat` | File | Build wrapper | Keep |
| `infra` | Dir | IaC and infra resources | Keep, align to env-specific modules |
| `loan-context` | Dir | Domain context | Keep |
| `loan-service` | Dir | Potential legacy service root | Evaluate merge/archive with context model |
| `masrufi-framework` | Dir | Framework package | Keep; ensure dependency boundaries explicit |
| `open-finance-context` | Dir | Core context | Keep; high governance priority |
| `payment-context` | Dir | Domain context | Keep |
| `payment-service` | Dir | Potential legacy service root | Evaluate merge/archive with context model |
| `postman` | Dir | API test artifacts | Keep; align with contract tests |
| `readme.md` | File | Non-canonical casing | Keep for compatibility; add canonical `README.md` alias in next wave |
| `risk-context` | Dir | Domain context | Keep |
| `services` | Dir | Microservice runtime roots | Keep; define authoritative ownership mapping |
| `settings.gradle` | File | Root module graph | Keep; enforce module hygiene checks |
| `shared-infrastructure` | Dir | Shared technical components | Keep, limit to pure infra abstractions |
| `shared-kernel` | Dir | DDD shared kernel | Keep; tightly govern change control |
| `simple-test` | Dir | Likely experimental | Archive or convert to documented test harness |
| `src` | Dir | Generic root source tree | Clarify scope; avoid overlap with context/service trees |
| `temp-src` | Dir | Temporary code area | Archive/remove after migration cutoff |
| `templates` | Dir | Service templates | Keep and align to current architecture baseline |
| `test-plugin-compatibility` | Dir | Build compatibility checks | Keep if actively used; otherwise archive |
| `tools` | Dir | Utility scripts and support tooling | Keep; add script quality/linting standards |

## Clean Coding and Structure Standards (Target)

## 1. Naming and Structure

1. Standardize canonical root readme as `README.md` (retain compatibility redirect if needed).
2. Use consistent kebab-case for module/service folders.
3. Keep one authoritative location for each concern:
   - Domain contexts
   - Deployable services
   - Shared kernels/infrastructure
   - Templates

## 2. Architectural Boundaries

1. Enforce hexagonal layering:
   - `domain` has no framework imports.
   - `application` orchestrates use cases.
   - `infrastructure` only through ports/adapters.
2. Prevent cross-context direct DB access.
3. Enforce API contracts for inter-service communication.

## 3. Code Quality and Security

1. Coverage gates >= 85% on changed modules.
2. Static analysis (Checkstyle/PMD/SpotBugs/SAST) as merge blockers.
3. Secret scanning and dependency vulnerability gates.
4. FAPI/DPoP/mTLS regression suite mandatory for security-sensitive modules.

## 4. Documentation Standards

1. Every bounded context has:
   - Purpose
   - Ownership
   - Public contracts
   - Runbook
2. Every major architecture change has ADR + diagram + migration notes.
3. Backlog and roadmap remain linked to capability map and outcomes.

## Reorganization Roadmap (Non-Disruptive)

## Wave A (Immediate: 1-2 sprints)

- Finalize root documentation navigation.
- Freeze archive/temp areas with explicit no-new-content rules.
- Add module ownership map and coding standard gates.

## Wave B (Short term: 2-4 sprints)

- Resolve duplicate/legacy service roots (`loan-service`, `payment-service`, etc.).
- Rationalize naming (`bankwide` vs `bank-wide-services`).
- Standardize API contract folders and test artifact locations.

## Wave C (Mid term: 4-8 sprints)

- Complete convergence on context-to-service ownership mapping.
- Remove or archive deprecated roots (`temp-src`, `simple-test` if inactive).
- Automate architecture conformance checks in CI.

## Mandatory Governance Controls

1. Repository structure change checklist in PR template.
2. Architecture-owner approval for shared-kernel and shared-infrastructure changes.
3. Quarterly repository health audit (structure, docs, stale modules, rule compliance).

