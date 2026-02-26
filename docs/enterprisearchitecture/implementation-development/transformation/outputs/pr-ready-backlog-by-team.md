# PR-Ready Backlog Split by Team (First Execution Batch)

Reference baseline:
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-remediation-plan.md`

## Scope
This first batch targets Wave 0 gate hardening and Wave 1 core-context stabilization.

## Team Allocation
| Team | Primary Scope | Capacity (PRs / sprint) |
| --- | --- | --- |
| DevSecOps Platform Team | CI gates, quality policies, coverage enforcement | 3 |
| Architecture Enablement Team | Arch tests, module boundary rules, scorecard governance | 2 |
| Customer Domain Team | `customer-context:*` test and contract hardening | 2 |
| Loan Domain Team | `loan-context:*` test and contract hardening | 2 |
| Payment Domain Team | `payment-context:*` test and contract hardening | 2 |
| API Standards Team | OpenAPI linting and contract parity checks | 2 |
| SRE Observability Team | Test telemetry baselines for CI visibility | 1 |

## PR Backlog (Ready to Execute)
| PR ID | Team | Title | Modules | Dependencies | Estimate (SP) | Acceptance Criteria | Validation Command | Status |
| --- | --- | --- | --- | --- | ---: | --- | --- | --- |
| PR-001 | DevSecOps Platform Team | Enforce mandatory coverage gates in root pipeline | root CI, `customer-context`, `loan-context`, `payment-context`, `shared-kernel`, `shared-infrastructure`, `masrufi-framework` | None | 5 | CI fails when coverage <85% on listed modules; gate is blocking on PRs | `./gradlew test jacocoTestReport jacocoTestCoverageVerification` | Implemented |
| PR-002 | DevSecOps Platform Team | Add module-targeted test matrix jobs | root CI templates | PR-001 | 3 | Parallel jobs run by module and publish per-module coverage artifacts | CI run artifacts show module-level reports | Implemented |
| PR-003 | Architecture Enablement Team | Add architecture boundary tests for customer context | `customer-context:*` | PR-001 | 3 | Domain modules cannot depend on infrastructure/application packages | `./gradlew :customer-context:customer-domain:test` | Implemented |
| PR-004 | Architecture Enablement Team | Add architecture boundary tests for loan and payment contexts | `loan-context:*`, `payment-context:*` | PR-001 | 5 | Equivalent layer-boundary rules enforced and failing on violations | `./gradlew :loan-context:loan-domain:test :payment-context:payment-domain:test` | Implemented |
| PR-005 | Customer Domain Team | Add domain/application unit tests to reach >=85% | `customer-context:customer-domain`, `customer-context:customer-application` | PR-003 | 5 | Combined line coverage >=85%; no flaky tests | `./gradlew :customer-context:customer-domain:test :customer-context:customer-application:test jacocoTestCoverageVerification` | Implemented |
| PR-006 | Customer Domain Team | Add infrastructure integration tests with real adapters | `customer-context:customer-infrastructure` | PR-005 | 5 | Integration tests run against real DB/cache test containers or equivalents | `./gradlew :customer-context:customer-infrastructure:test` | Implemented |
| PR-007 | Loan Domain Team | Add domain/application unit tests to reach >=85% | `loan-context:loan-domain`, `loan-context:loan-application` | PR-004 | 5 | Loan context unit suite passes and coverage gate is green | `./gradlew :loan-context:loan-domain:test :loan-context:loan-application:test jacocoTestCoverageVerification` | Implemented |
| PR-008 | Loan Domain Team | Add infrastructure integration tests with real adapters | `loan-context:loan-infrastructure` | PR-007 | 5 | Infrastructure adapter tests validate persistence and mapping boundaries | `./gradlew :loan-context:loan-infrastructure:test` | Implemented |
| PR-009 | Payment Domain Team | Add domain/application unit tests for state/idempotency rules | `payment-context:payment-domain`, `payment-context:payment-application` | PR-004 | 5 | Payment state transitions and idempotency rules covered >=85% | `./gradlew :payment-context:payment-domain:test :payment-context:payment-application:test jacocoTestCoverageVerification` | Implemented |
| PR-010 | Payment Domain Team | Add infrastructure integration tests for persistence and locking | `payment-context:payment-infrastructure` | PR-009 | 5 | Integration tests validate adapter behavior, locks, and repository semantics | `./gradlew :payment-context:payment-infrastructure:test` | Implemented |
| PR-011 | API Standards Team | Add OpenAPI lint + contract parity gate for API-bearing modules | root `api/openapi/*`, API modules in customer/loan/payment | PR-001 | 3 | Build fails on OpenAPI drift, missing required headers, or path mismatches | `python3 tools/validation/repo_governance_validator.py` | Implemented |
| PR-012 | API Standards Team | Add contract tests mapping OpenAPI to controllers | `customer-context`, `loan-context`, `payment-context` APIs | PR-011 | 5 | Contract tests fail when controller routes/headers deviate from spec | `./gradlew test --tests '*OpenApiContractTest'` | Implemented |
| PR-013 | DevSecOps Platform Team | Add scorecard regeneration and publish on merge | `tools/validation/*`, docs outputs | PR-001 | 2 | Scorecard files regenerate on merge and are published as build artifacts | `./tools/validation/generate_architecture_scorecard.py --root .` | Implemented |
| PR-014 | SRE Observability Team | Add CI test telemetry summary (duration, flake, failure hot spots) | CI reporting layer | PR-002 | 3 | PR checks show test trend summary and top failing modules | `python3 tools/validation/generate_test_telemetry_summary.py --root . --top-failures 10` | Implemented |

## Sequencing (Execution Order)
1. PR-001
2. PR-002, PR-003
3. PR-004, PR-011
4. PR-005, PR-007, PR-009
5. PR-006, PR-008, PR-010, PR-012
6. PR-013, PR-014

## Sprint Commitments (First 2 Sprints)
| Sprint | Planned PRs |
| --- | --- |
| Sprint 1 | PR-001, PR-002, PR-003, PR-004, PR-011 |
| Sprint 2 | PR-005, PR-006, PR-007, PR-008, PR-009, PR-010, PR-012, PR-013, PR-014 |

## Exit Criteria for First Batch
1. Coverage gates are blocking for all Wave 0 target modules.
2. Customer, loan, and payment contexts no longer classified as `needs-tests`.
3. OpenAPI contract drift is blocked in CI.
4. Scorecard average increases by at least 15 points from baseline.

## GitHub Execution Artifacts
- Milestone map:
  `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/github-milestone-map.md`
- Issue drafts (one per PR ID):
  `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/github-issue-drafts/`
- Reusable issue form:
  `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/.github/ISSUE_TEMPLATE/transformation_work_item.yml`
