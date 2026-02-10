# Microservices Gap Analysis & Service Roadmaps

## Scope
Assessed Open Finance HLDs and current implementation posture against 10 microservices design principles. This document captures gaps and proposes service‑specific roadmaps to reach a true microservice design.

**Assessed HLDs**
- `docs/architecture/open-finance/use-cases/hld/UC001_Personal_Financial_Management_HLD.md`
- `docs/architecture/open-finance/use-cases/hld/UC003_Confirmation_of_Payee_HLD.md`
- `docs/architecture/open-finance/use-cases/hld/UC006_Payments_HLD.md`
- `docs/architecture/open-finance/use-cases/hld/HEXAGONAL_HLD_COMPLIANCE_CHECKLIST.md`

## Gap Analysis Summary (Against 10 Principles)

Legend: `Strong`, `Partial`, `Missing`

| Principle | UC001 AIS | UC003 CoP | UC006 Payments | Cross‑Cutting Gap Summary |
| --- | --- | --- | --- | --- |
| Independent & Autonomous Services | Partial | Partial | Partial | Data ownership boundaries and blast‑radius isolation not explicit. |
| API Aggregation | Partial | Partial | Partial | API composition/BFF strategy not defined. |
| Flexibility | Missing | Missing | Missing | Feature flags/config‑driven behavior not specified. |
| Scalability | Strong | Partial | Partial | Scaling tactics beyond read path are undefined. |
| Constant Monitoring | Missing | Missing | Missing | Tracing/metrics/alerts not captured in HLDs. |
| Failure Isolation/Resilience | Partial | Partial | Partial | Circuit breakers, bulkheads, retry policies missing. |
| Realtime Load Balancing | Missing | Missing | Missing | Load balancing strategy not defined. |
| Inclusion of DevOps | Missing | Missing | Missing | CI/CD, deployment, rollback not documented. |
| Versioning | Partial | Partial | Partial | Version lifecycle and deprecation policy missing. |
| Availability | Partial | Partial | Partial | Multi‑AZ/DR and redundancy details not specified. |

## Refactor Themes (Cross‑Cutting)

1. **Service Ownership & Boundaries**
- Define DB or schema ownership per service and enforce contract boundaries for upstream/downstream interactions.

2. **Observability Standard**
- Require tracing (`X-FAPI-Interaction-ID` => trace id), metrics (latency/error rate), logs with correlation ids, and SLOs.

3. **Resilience Patterns**
- Add circuit breakers, retries with backoff, timeouts, bulkheads, and clear fallback behavior.

4. **API Versioning**
- Formal version lifecycle, deprecation windows, and backward compatibility policy.

5. **DevOps & Release**
- Standardize CI/CD, canary/blue‑green, rollback conditions, and environment promotion.

6. **Load Balancing & Scaling**
- Define autoscaling triggers, queue partitioning, and service mesh/ingress behavior.

## Mandatory Remediation Pack (Wave 0 + Wave 1)

These remediations are non-optional and must be completed before broad service rollout.

1. **DPoP Must Be Mandatory on Protected APIs**
- Enforce `DPoP` as required in OpenAPI contracts for protected services.
- Enforce runtime DPoP proof validation, not header-presence checks only.

2. **Corporate AIS Contract Drift Closure**
- Align OpenAPI and implementation for accounts, balances, transactions, scheduled-payments, and parties.
- Remove path mismatch between spec and controller mappings.
- Add CI contract tests to fail build on drift.

3. **Runtime Persistence Hardening**
- Replace in-memory seeded runtime repositories with production adapters.
- Keep in-memory adapters test-only or behind explicit non-prod profile.

4. **ETag and Cache Hardening**
- Remove local unbounded controller maps for ETag state.
- Use distributed cache with TTL and bounded cardinality.
- Generate ETag from full response-significant material to avoid stale `304 Not Modified`.

5. **Observability Baseline**
- Add tracing (trace id propagation), metrics, and structured logs.
- Enforce PII masking in logs by default.

6. **Delivery Pipeline and IaC Hardening**
- Replace Jenkins/GitLab placeholder pipeline steps with executable gates.
- Replace output-only Terraform stubs with provider-backed resources.
- Keep bounded-context stub repos only as temporary extraction scaffolding, not delivery endpoints.

## Service Roadmaps

### UC001 AIS (Personal Financial Management)
**Phase 1: Boundary Hardening (Weeks 0–2)**
- Define AIS data ownership (read models + cache) and upstream CBS event contracts.
- Document read model refresh policy and rehydration strategy.
- Enforce mandatory DPoP and JWT scope validation in gateway/security chain.

**Phase 2: Resilience & Observability (Weeks 2–4)**
- Add circuit breakers/timeouts for cache and read‑model access.
- Define SLOs and metric instrumentation for read latency and error rates.

**Phase 3: Scalability & Operations (Weeks 4–6)**
- Formalize autoscaling rules for read traffic and consumer lag thresholds.
- Add backpressure handling for Kafka projections.

**Phase 4: DevOps & Versioning (Weeks 6–8)**
- Define API versioning and deprecation policy.
- Implement CI/CD with staged rollout and rollback conditions.

### UC003 CoP (Confirmation of Payee)
**Phase 1: Boundary Hardening (Weeks 0–2)**
- Establish ownership of directory index and audit store.
- Document fallback response (`UnableToCheck`) under index/cache degradation.

**Phase 2: Resilience & Observability (Weeks 2–4)**
- Circuit breaker for fuzzy search and rate‑limit enforcement.
- Tracing/metrics for P95 latency and match score distribution.

**Phase 3: Scalability & Operations (Weeks 4–6)**
- Introduce autoscaling based on request volume and p95 latency.
- Define load testing thresholds and rate‑limit guardrails.

**Phase 4: DevOps & Versioning (Weeks 6–8)**
- API versioning policy and release strategy with backward compatibility checks.

### UC006 Payments (Single/International)
**Phase 1: Boundary Hardening (Weeks 0–3)**
- Formalize payment state machine and compensation boundaries.
- Define idempotency store ownership and TTL policy.

**Phase 2: Resilience & Observability (Weeks 3–6)**
- Circuit breakers/timeouts for rails and risk engine.
- End‑to‑end tracing for payment lifecycle events.
- Mandatory DPoP/JWS verification coverage for protected payment APIs.

**Phase 3: Scalability & Operations (Weeks 6–9)**
- Partition async settlement queue and scale workers by backlog.
- Define RTO/RPO and active‑active expectations for payment store.

**Phase 4: DevOps & Versioning (Weeks 9–12)**
- Formal release policy with rollback triggers and idempotency guarantees during rollout.

### UC013 Request to Pay
**Phase 1: Boundary Hardening (Weeks 0–2)**
- Formalize ownership of request store and notification adapter contracts.
- Document compensation and retry semantics for notification delivery.

**Phase 2: Resilience & Observability (Weeks 2–4)**
- Add circuit breaker for downstream notification channel.
- Expose SLOs for request acceptance and status queries.

**Phase 3: Scalability & Operations (Weeks 4–6)**
- Define autoscaling for burst traffic and cache hit ratio targets.

### UC014 Open Products Data
**Phase 1: Boundary Hardening (Weeks 0–2)**
- Document catalog ownership and cache revalidation policy.

**Phase 2: Observability & Scaling (Weeks 2–4)**
- Add metrics for cache hit rate, response latency, and invalid filter attempts.
- Formalize public‑cache TTL policy and CDN integration.

### UC015 ATM Open Data
**Phase 1: Boundary Hardening (Weeks 0–2)**
- Define data ownership for ATM directory and update cadence.

**Phase 2: Observability & Scaling (Weeks 2–4)**
- Add latency metrics for location‑filtered queries.
- Document geo‑filter cache invalidation strategy.

## Dependency & Sequencing Notes
- Prioritize platform guardrails first: shared security chain, observability baseline, contract tests, runnable CI/CD, provider-backed Terraform.
- Use Business Financial Data Service as pilot for drift/persistence/ETag hardening before broad AIS rollout.
- Migrate additional read services next (Personal Financial Data, Banking Metadata), then transactional workflow services.

## Assumptions / Scope
- `DPoP` is mandatory for protected service endpoints.
- This roadmap does not include non-critical feature expansion.
- Existing service contracts may require minor version bump if path/header requirements change.
- Runtime target remains Java 23 across extracted services.

## Definition of Done (Microservices Roadmap)
- Service ownership documented (DB/schema, contracts, dependencies).
- Observability SLIs/SLOs defined and instrumented.
- Resilience patterns implemented and tested.
- API versioning and deprecation policy published.
- CI/CD + rollback strategy validated.
