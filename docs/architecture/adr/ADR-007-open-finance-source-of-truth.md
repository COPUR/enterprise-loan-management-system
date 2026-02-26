# ADR-007: Open Finance Source of Truth

## Status
Accepted

## Date
2026-02-25

## Context
The repository currently contains two implementation tracks for the same Open Finance capabilities:

1. `open-finance-context/*` modules in root Gradle settings.
2. `services/openfinance-*` standalone deployable services.

Both tracks contain overlapping domain and application behavior. This creates:
- duplicate implementation ownership,
- contract drift risk,
- inconsistent security/observability rollout,
- higher regression and release complexity.

## Decision
Adopt **service-first runtime ownership**:

1. `services/openfinance-*` is the only runtime source of truth for deployable Open Finance APIs.
2. `open-finance-context/*` is repurposed as shared capability contracts and reusable domain abstractions only.
3. Runtime controller/application logic must not be duplicated in both tracks.
4. OpenAPI contracts under `api/openapi/*.yaml` remain canonical and must be enforced by contract tests in service repos/modules.

## Rationale
- Deployable service folders already carry CI/CD, Terraform service stacks, and runtime integration patterns.
- Service-first ownership aligns better with microservice autonomy and independent release.
- Keeping context modules as contract/domain libraries preserves shared language without duplicate runtime code.

## Consequences

### Positive
- Single implementation owner per capability.
- Faster security/control rollout (FAPI, DPoP, observability) through one runtime path.
- Cleaner release process and reduced drift between contracts and deployed behavior.

### Negative
- Requires staged migration and de-duplication effort.
- Some root context modules will lose runtime responsibilities and need refactoring.

## Migration Plan

1. Inventory duplicate packages between `open-finance-context` and `services/openfinance-*`.
2. Classify each duplicate as:
   - `runtime-service` (keep in `services/openfinance-*`),
   - `shared-contract` (move/keep in context/shared module),
   - `delete`.
3. Remove duplicate runtime web/application adapters from `open-finance-context`.
4. Keep or extract shared value objects and contract models into non-runtime modules.
5. Add CI guard that fails when duplicate runtime capability packages exist across both tracks.
6. Re-run regression, contract, and coverage gates after each capability cutover.

## Guardrails
- Contract-first: OpenAPI changes must be backward compatible unless versioned.
- Security-first: protected operations require mTLS + JWT scope checks + DPoP.
- Test-first: no migration step merges without green unit + integration + contract gates.
- Observability baseline required on retained runtime services.

## Acceptance Criteria
- No capability has runtime controller logic in both `open-finance-context` and `services/openfinance-*`.
- All Open Finance service modules pass CI quality/security/coverage gates.
- Architecture scorecard shows duplicate-runtime risk removed for Open Finance modules.

## References
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/CROSS_DOMAIN_ARCHITECTURE_ALIGNMENT_ROADMAP.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/architecture-scorecard-latest.md`
