# Microservices Repo Audit (Phase 1)

## Current Repository Overview
- **Monorepo** with bounded contexts: `open-finance-context`, `payment-context`, `loan-context`, `risk-context`, `customer-context`, etc.
- **Shared libraries** used across contexts: `shared-kernel`, `shared-infrastructure`, `common-domain`, `common-test`.
- **Open Finance implementation** is contained in a single bounded context with use case subpackages (domain/application/infrastructure).

## Catalog of Shared Assets
- **Libraries:** `shared-kernel`, `shared-infrastructure`, `common-domain`, `common-test`.
- **Documentation:** HLDs under `docs/architecture/open-finance/use-cases/hld/`.
- **Testing:** Cross‑use case tests in `open-finance-context` and common test fixtures.

## Dependency Observations
- Open finance submodules are layered (domain -> application -> infrastructure) but still **share the same build and runtime context**, which is a coupling point when extracting services.
- Shared libraries introduce **implicit coupling** across future microservices (domain exceptions, utilities, shared infra).

## Share‑Nothing Debt Candidates
- **Shared DB expectations**: HLDs assume shared analytics and common read models; these need to be explicitly owned per service in extraction.
- **Shared cache usage**: in‑memory cache adapters are global to the current bounded context; per‑service caches must be isolated.
- **Cross‑use case dependencies**: repeated re‑use of common contracts may become brittle without explicit API contracts per service.

## Initial Extraction Risks
- **Consent coupling**: multiple services depend on consent status; extraction requires a clear consent service API and caching strategy.
- **Idempotency**: currently in‑context idempotency patterns must be migrated per service with distinct stores and TTL policies.
- **Observability**: tracing and metrics are not yet standardized across extracted services.

## Recommendations (Immediate)
- Formalize **service‑level data ownership** before extraction.
- Define **API contracts** (OpenAPI) per service to remove implicit coupling.
- Establish **standard observability** baseline (trace id, metrics, logs).

