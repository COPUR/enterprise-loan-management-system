# Architecture Scorecard (Wave 0 Baseline)

## Summary
- Total modules assessed: 46
- Aligned modules: 25
- Modules needing tests: 12
- Modules needing gates: 0
- Stub modules: 6
- Average score: 51.8/100

## Top Risk Modules
- `amanahfi-platform:accounts-context` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:api-gateway` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:compliance-context` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:event-streaming` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:murabaha-context` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:onboarding-context` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:payments-context` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `amanahfi-platform:shared-kernel` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `common:common-domain` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci
- `masrufi-framework` (30/100): missing-tests,no-coverage-gate,no-openapi,no-service-ci

## Detailed Scorecard
| Module | Origin | Score | Class | Main | Test | Cov Gate | Arch Tests | OpenAPI | CI | Terraform | Risks |
|---|---|---:|---|---:|---:|---|---|---|---|---|---|
| `service:openfinance-atm-directory-service` | standalone-service | 90 | aligned | 1 | 1 | Y | N | Y | Y | Y | - |
| `service:openfinance-banking-metadata-service` | standalone-service | 90 | aligned | 63 | 34 | Y | N | Y | Y | Y | - |
| `service:openfinance-business-financial-data-service` | standalone-service | 90 | aligned | 56 | 33 | Y | N | Y | Y | Y | - |
| `service:openfinance-confirmation-of-payee-service` | standalone-service | 90 | aligned | 1 | 1 | Y | N | Y | Y | Y | - |
| `service:openfinance-consent-authorization-service` | standalone-service | 90 | aligned | 57 | 21 | Y | N | Y | Y | Y | - |
| `service:openfinance-open-products-service` | standalone-service | 90 | aligned | 1 | 1 | Y | N | Y | Y | Y | - |
| `service:openfinance-personal-financial-data-service` | standalone-service | 90 | aligned | 55 | 32 | Y | N | Y | Y | Y | - |
| `open-finance-context:open-finance-application` | root-settings | 80 | aligned | 26 | 15 | Y | Y | Y | N | N | no-service-ci |
| `open-finance-context:open-finance-domain` | root-settings | 80 | aligned | 273 | 169 | Y | Y | Y | N | N | no-service-ci |
| `open-finance-context:open-finance-infrastructure` | root-settings | 80 | aligned | 224 | 97 | Y | Y | Y | N | N | no-service-ci |
| `compliance-context:compliance-application` | root-settings | 70 | aligned | 3 | 3 | Y | N | Y | N | N | no-service-ci |
| `compliance-context:compliance-domain` | root-settings | 70 | aligned | 7 | 4 | Y | N | Y | N | N | no-service-ci |
| `compliance-context:compliance-infrastructure` | root-settings | 70 | aligned | 3 | 4 | Y | N | Y | N | N | no-service-ci |
| `customer-context:customer-application` | root-settings | 70 | aligned | 6 | 7 | Y | N | Y | N | N | no-service-ci |
| `customer-context:customer-domain` | root-settings | 70 | aligned | 10 | 5 | Y | N | Y | N | N | no-service-ci |
| `customer-context:customer-infrastructure` | root-settings | 70 | aligned | 4 | 5 | Y | N | Y | N | N | no-service-ci |
| `loan-context:loan-application` | root-settings | 70 | aligned | 7 | 7 | Y | N | Y | N | N | no-service-ci |
| `loan-context:loan-domain` | root-settings | 70 | aligned | 22 | 9 | Y | N | Y | N | N | no-service-ci |
| `loan-context:loan-infrastructure` | root-settings | 70 | aligned | 4 | 3 | Y | N | Y | N | N | no-service-ci |
| `payment-context:payment-application` | root-settings | 70 | aligned | 11 | 5 | Y | N | Y | N | N | no-service-ci |
| `payment-context:payment-domain` | root-settings | 70 | aligned | 16 | 4 | Y | N | Y | N | N | no-service-ci |
| `payment-context:payment-infrastructure` | root-settings | 70 | aligned | 29 | 6 | Y | N | Y | N | N | no-service-ci |
| `risk-context:risk-application` | root-settings | 70 | aligned | 3 | 3 | Y | N | Y | N | N | no-service-ci |
| `risk-context:risk-domain` | root-settings | 70 | aligned | 7 | 5 | Y | N | Y | N | N | no-service-ci |
| `risk-context:risk-infrastructure` | root-settings | 70 | aligned | 3 | 4 | Y | N | Y | N | N | no-service-ci |
| `amanahfi-platform:accounts-context` | root-settings | 30 | needs-tests | 16 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:api-gateway` | root-settings | 30 | needs-tests | 11 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:compliance-context` | root-settings | 30 | needs-tests | 22 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:event-streaming` | root-settings | 30 | needs-tests | 7 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:murabaha-context` | root-settings | 30 | needs-tests | 13 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:onboarding-context` | root-settings | 30 | needs-tests | 15 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:payments-context` | root-settings | 30 | needs-tests | 16 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `amanahfi-platform:shared-kernel` | root-settings | 30 | needs-tests | 1 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `common:common-domain` | root-settings | 30 | needs-tests | 1 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `masrufi-framework` | root-settings | 30 | needs-tests | 35 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `shared-infrastructure` | root-settings | 30 | needs-tests | 84 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `shared-kernel` | root-settings | 30 | needs-tests | 23 | 0 | N | N | N | N | N | missing-tests,no-coverage-gate,no-openapi,no-service-ci |
| `common:common-infrastructure` | root-settings | 15 | empty | 0 | 0 | N | N | N | N | N | - |
| `common:common-test` | root-settings | 15 | empty | 0 | 0 | N | N | N | N | N | - |
| `test-plugin-compatibility` | root-settings | 15 | empty | 0 | 0 | N | N | N | N | N | - |
| `stub:compliance-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |
| `stub:customer-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |
| `stub:loan-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |
| `stub:open-finance-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |
| `stub:payment-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |
| `stub:risk-context` | extraction-stub | 10 | stub | 0 | 0 | N | N | N | Y | N | stub-only |

## Artifact Paths
- CSV: `docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.csv`
- Markdown: `docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.md`
