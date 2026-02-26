# Cross-Domain Architecture Alignment Roadmap

## Objective
Align all active domains and modules to one engineering model:
- DDD bounded contexts
- Hexagonal architecture (domain, application, infrastructure)
- TDD-first delivery with enforceable coverage gates
- Contract-first APIs (OpenAPI)
- FAPI-grade security controls where applicable
- Standard observability baseline
- 12-factor runtime conventions

This roadmap expands beyond open-finance and covers core banking contexts, Islamic platform modules, shared modules, and extraction stubs.

## Baseline Discovery (As-Is)

### Active root modules (settings.gradle)
- 33 active modules.
- Mature module family: `open-finance-context` (high test volume, coverage gates, architecture/property tests).
- Partially mature families:
  - `customer-context`, `loan-context`, `payment-context` have code and layered structure but no committed test sources.
  - `shared-kernel`, `shared-infrastructure`, `common/*` partially populated.
- Stub families:
  - `risk-context:*` and `compliance-context:*` are placeholders (`.gitkeep`), no `build.gradle` in submodules.

### Service-oriented modules under `services/`
- Active service codebases: `services/openfinance-*` (7 modules).
- Stronger maturity: `openfinance-consent-authorization-service`, `openfinance-personal-financial-data-service`, `openfinance-business-financial-data-service`, `openfinance-banking-metadata-service`.
- Minimal implementations: `openfinance-confirmation-of-payee-service`, `openfinance-open-products-service`, `openfinance-atm-directory-service` (low source/test counts).
- `services/bounded-contexts/*` are extraction stubs only (README/manifest/OpenAPI/CI stub), no runtime implementation.

### AmanahFi and MasruFi families
- `amanahfi-platform` has active modules with code but no committed test suites across contexts.
- Root build includes `amanahfi-platform:event-streaming`, but `amanahfi-platform/settings.gradle` does not include event-streaming; alignment drift exists.
- `masrufi-framework` is a single library module with selective source exclusions and no committed test sources.

### Governance and delivery
- GitHub Actions root CI exists and runs `./gradlew check`.
- Open-finance services have runnable Jenkins/GitLab templates with quality/security/image gates.
- Bounded-context stubs have contract-only CI templates.
- Terraform exists and is provider-backed for open-finance service stacks; other domain stacks are not yet represented at the same level.

## Key Gaps

1. Architectural inconsistency across domains:
- Open-finance has enforced test/coverage/architecture gates; core contexts do not.

2. Incomplete bounded contexts:
- Risk and compliance root contexts are placeholders, blocking full domain orchestration.

3. Parallel runtime tracks:
- Open-finance exists both as context modules and service modules; ownership boundaries and source of truth are not finalized.

4. Test maturity imbalance:
- Multiple domains compile but have effectively zero committed automated tests.

5. Module graph drift:
- Inclusion differences between root and nested platform settings (AmanahFi).

6. Extraction not completed:
- `services/bounded-contexts/*` are repo stubs without implementation migration.

## Target Architecture (To-Be)

For every bounded context (customer, loan, payment, risk, compliance, open-finance capabilities, Islamic contexts):
- `domain`: pure business model, invariants, domain events, ports
- `application`: use case orchestration, command/query handlers
- `infrastructure`: adapters (db, cache, messaging, HTTP), controllers
- test hierarchy:
  - unit tests on domain/application first
  - integration tests for adapters
  - contract tests on APIs/events
  - e2e/uat per capability

Cross-cutting standards:
- 85%+ line coverage minimum per runtime module (target 90% for critical financial paths).
- Failing quality gates in CI for:
  - coverage
  - architectural boundary violations
  - contract drift
  - security scanning
- Standard runtime baseline:
  - trace id propagation
  - metrics (latency/error/throughput/saturation)
  - structured logs with PII masking
- Standard deployment baseline:
  - Docker image build/sign/publish
  - Terraform stack per deployable service
  - environment-specific overlays (dev/stage/prod)

## Refactor Workstreams

### Workstream 1: Architecture and Build Governance
- Unify conventions plugins usage across all contexts.
- Enforce architecture rules (domain must not depend on infrastructure).
- Enforce coverage and test tasks in every deployable module.
- Remove module include drift and dead modules.

### Workstream 2: Domain Completion
- Implement `risk-context` and `compliance-context` root modules (domain/application/infrastructure).
- Add domain events and policies used by customer/loan/payment.

### Workstream 3: Core Banking Context Hardening
- Add TDD suites for `customer-context`, `loan-context`, `payment-context`.
- Introduce integration/contract tests and CI coverage gates.
- Extract framework concerns from domain model where needed.

### Workstream 4: Open Finance Consolidation
- Select one source-of-truth strategy:
  - Option A: keep `services/openfinance-*` as runtime source and slim `open-finance-context` to shared contracts/core
  - Option B: keep `open-finance-context` as source and generate runtime services from it
- Remove duplicated business logic after decision.
- Keep OpenAPI contracts canonical and versioned.

### Workstream 5: AmanahFi and MasruFi Alignment
- Bring AmanahFi contexts to same TDD and coverage baseline.
- Resolve `event-streaming` inclusion drift and define risk-context role.
- Decompose `masrufi-framework` into bounded contexts or declare as shared kernel extension explicitly.

### Workstream 6: Service Extraction and Repo Strategy
- Promote `services/bounded-contexts/*` from stubs to actual extracted repos.
- Keep mono-repo as orchestrator until parity gates pass.
- Define cutover criteria and deprecate duplicated module paths.

## Wave Plan (Execution Roadmap)

## Wave 0 (Week 1): Control Plane Stabilization
- Finalize module inventory and ownership map.
- Freeze new architecture variants.
- Add architecture scorecard automation (per module).

Exit criteria:
- Every active module classified as `runtime`, `library`, `stub`, or `deprecated`.

## Wave 1 (Weeks 2-3): Core Context Quality Uplift
- `customer-context`, `loan-context`, `payment-context`:
  - add unit tests and coverage gates
  - add architecture tests and contract checks
  - normalize build conventions

Exit criteria:
- Each core context has runnable tests and minimum 85% coverage on critical domain/application classes.

## Wave 2 (Weeks 4-5): Implement Risk and Compliance Contexts
- Create real implementations for:
  - `risk-context:risk-domain|application|infrastructure`
  - `compliance-context:compliance-domain|application|infrastructure`
- Integrate with payment and loan flows.

Exit criteria:
- Risk/compliance contexts no longer placeholder modules.

## Wave 3 (Weeks 6-7): Open Finance Consolidation Decision and Refactor
- Decide source-of-truth model (Option A or B).
- Remove duplicate logic between `open-finance-context` and `services/openfinance-*`.
- Keep only one implementation owner per capability.

Exit criteria:
- No duplicate runtime behavior implemented in two module families.

## Wave 4 (Weeks 8-9): AmanahFi Alignment
- Add TDD and coverage to AmanahFi context modules.
- Resolve settings/include drift.
- Align observability and security baselines.

Exit criteria:
- AmanahFi modules pass same quality/security gates as core contexts.

## Wave 5 (Weeks 10-11): MasruFi Decomposition or Canonicalization
- Either:
  - decompose MasruFi into bounded contexts, or
  - position it as shared extension module with strict boundaries.
- Add missing tests and remove excluded dead code paths.

Exit criteria:
- MasruFi has explicit architecture role and enforceable quality gates.

## Wave 6 (Weeks 12-13): Extraction and Cutover
- Move bounded-context stubs into real extracted repos.
- Wire CI/CD and IaC per extracted service.
- Run full regression + e2e certification.

Exit criteria:
- Extracted repositories production-ready with consistent standards.

## Delivery Artifacts Per Wave

- Wave 0:
  - Module ownership matrix
  - Architecture scorecard automation report
- Wave 1:
  - Test/coverage reports for customer/loan/payment contexts
  - Updated build conventions adoption report
- Wave 2:
  - Risk/compliance service contracts + implementation docs
- Wave 3:
  - Open-finance consolidation ADR
  - De-duplication migration changelog
- Wave 4:
  - AmanahFi parity report vs core standards
- Wave 5:
  - MasruFi architecture decision record and refactor report
- Wave 6:
  - Extraction completion report
  - Release readiness checklist

## Prioritized Backlog Seed (Cross-Domain)

1. Create architecture scorecard job that outputs per-module maturity (tests, coverage, conventions, contract presence).
2. Add missing `build.gradle` and scaffolding to `risk-context` and `compliance-context` submodules.
3. Add unit test suites to `customer-context` domain/application modules.
4. Add unit test suites to `loan-context` domain/application modules.
5. Add unit test suites to `payment-context` domain/application modules.
6. Enforce per-module coverage gate (85% min) where runtime code exists.
7. Resolve AmanahFi include drift (`event-streaming` and risk-context ownership).
8. Decide open-finance source-of-truth strategy and publish ADR.
9. Eliminate duplicate implementation paths after ADR approval.
10. Promote bounded-context repo stubs into executable extracted repos.

## Immediate Next 2-Week Plan

Week 1:
- Ship Wave 0 artifacts.
- Start Wave 1 on `customer-context`.

Week 2:
- Complete Wave 1 for `loan-context` and `payment-context`.
- Open Wave 2 scaffolding for `risk-context`.

## Wave Status Update (2026-02-25)

### Completed
- Wave 0 control-plane stabilization artifacts are in place.
- Wave 1 quality uplift completed for `customer-context`, `loan-context`, and `payment-context` with active tests and coverage gates.
- Wave 2 implementation completed for `risk-context` and `compliance-context`:
  - domain/application/infrastructure layers created
  - OpenAPI contracts published
  - contract tests added
  - coverage gates passing above the 85% minimum

### In Progress
- Wave 3 open-finance source-of-truth consolidation (de-duplication execution).

### Next (Execution Order)
1. Execute de-duplication based on ADR-007 (`docs/architecture/adr/ADR-007-open-finance-source-of-truth.md`) and keep one runtime owner per capability.
2. Add CI duplicate-runtime detector across `open-finance-context` and `services/openfinance-*`.
3. Start Wave 4 parity uplift for `amanahfi-platform` modules (tests, coverage, OpenAPI, CI parity).
