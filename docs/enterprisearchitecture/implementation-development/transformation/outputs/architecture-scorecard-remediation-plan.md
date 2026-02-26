# Architecture Scorecard Remediation Plan (Wave 0 -> Wave 3)

Generated from:
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.csv`

## Objective
Raise module alignment from current baseline to enforceable engineering gates while preserving existing delivery flow.

## Baseline Snapshot
- Total modules: 46
- Aligned: 10
- Needs tests: 21
- Stub: 12
- Average score: 38.2/100

## Wave 0 (Immediate Gate Hardening)
Target timeline: 1 sprint

1. Enable mandatory coverage gate in CI for all active root modules:
   - `customer-context:*`
   - `loan-context:*`
   - `payment-context:*`
   - `shared-kernel`
   - `shared-infrastructure`
   - `masrufi-framework`
2. Add per-module test tasks and enforce:
   - `./gradlew test jacocoTestReport jacocoTestCoverageVerification`
3. Introduce architecture dependency rules (domain does not depend on infrastructure) for:
   - `customer-context`
   - `loan-context`
   - `payment-context`
4. Gate merge on:
   - Coverage >= 85%
   - Architecture tests green
   - OpenAPI lint for API-bearing modules

Delivery artifacts:
- CI gate policy update
- Failing module list in pipeline output
- First pass architecture tests committed

## Wave 1 (Core Domain Stabilization)
Target timeline: 1-2 sprints

1. Customer context
   - Add unit tests for domain and application modules
   - Add integration tests for infrastructure adapters
   - Add OpenAPI contract coverage tests
2. Loan context
   - Add missing tests for pricing, eligibility, and lifecycle rules
   - Add persistence integration tests
3. Payment context
   - Add tests for idempotency, state transitions, and consent checks
   - Add contract tests for payment endpoints

Exit criteria:
- Each context reaches score >= 75
- No context remains in `needs-tests`

Delivery artifacts:
- Coverage reports per module
- Context-level test plan and executed results
- Contract test suite logs

## Wave 2 (Platform Risk Cluster Remediation)
Target timeline: 2 sprints

Modules from top-risk cluster:
- `amanahfi-platform:accounts-context`
- `amanahfi-platform:api-gateway`
- `amanahfi-platform:compliance-context`
- `amanahfi-platform:event-streaming`
- `amanahfi-platform:murabaha-context`
- `amanahfi-platform:onboarding-context`
- `amanahfi-platform:payments-context`
- `amanahfi-platform:shared-kernel`
- `common:common-domain`

Actions:
1. Add minimal viable test suites for each module.
2. Add module-specific OpenAPI contracts where externally exposed APIs exist.
3. Add CI pipeline definitions and quality gates.
4. Add architecture tests for layering and dependency boundaries.

Exit criteria:
- All listed modules move from score 30 -> >= 70
- CI + quality gates active for each module

Delivery artifacts:
- Module readiness matrix
- Updated architecture scorecard and trend delta
- Signed technical debt burn-down report

## Wave 3 (Stub Extraction Completion)
Target timeline: 2-3 sprints

Extraction stubs:
- `stub:compliance-context`
- `stub:customer-context`
- `stub:loan-context`
- `stub:open-finance-context`
- `stub:payment-context`
- `stub:risk-context`

Actions:
1. Replace each stub with runnable service skeleton:
   - Build + tests + CI + OpenAPI + Terraform baseline
2. Add README with service ownership and runtime contract.
3. Remove stub-only markers from scorecard classification logic.

Exit criteria:
- No modules remain classified as `stub`
- All bounded contexts have executable baseline

Delivery artifacts:
- New runnable service repos/modules
- Ownership and SLO documentation
- Scorecard class `stub` count = 0

## Governance and Reporting Cadence
1. Regenerate scorecard on every main-branch merge:
   - `./tools/validation/generate_architecture_scorecard.py --root .`
2. Publish:
   - Latest markdown report
   - CSV snapshot for trend charts
3. Track KPIs:
   - Average score
   - Aligned module count
   - Needs-tests count
   - Stub count

## Definition of Done (Global)
1. Coverage and architecture gates enforced, not informational.
2. OpenAPI drift prevented by contract tests in CI.
3. Core contexts have integration tests with real adapters.
4. Scorecard average >= 75 with no critical risk cluster at score <= 30.
