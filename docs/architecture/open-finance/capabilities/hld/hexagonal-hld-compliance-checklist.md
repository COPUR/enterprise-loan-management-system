# Hexagonal HLD Compliance Checklist (Open Finance Capability Set)

## Purpose
This checklist validates Open Finance HLDs against project hexagonal/DDD/FAPI guardrails before and during implementation.

References:
- `docs/HEXAGONAL_ARCHITECTURE_GUARDRAILS.md`
- `docs/architecture/open-finance/capabilities/hld/open-finance-guardrails-standards-catalog.md`
- `docs/architecture/ARCHITECTURE_GUARDRAILS.md`

## Checklist

### 1. Inside/Outside Boundary
- [ ] Domain model contains business rules only (no framework annotations/HTTP/DB concerns).
- [ ] Capability orchestration is in application layer and depends only on domain ports.
- [ ] Infrastructure adapters implement domain output ports.

### 2. Ports and Adapters
- [ ] Each use case has explicit input port(s) aligned to business capabilities.
- [ ] External dependencies are represented as output ports (repo/cache/events/security/external providers).
- [ ] Controllers and transport DTOs are adapter concerns only.

### 3. DDD Tactical Design
- [ ] Aggregates/entities/value objects enforce invariants in constructors/factories/methods.
- [ ] Domain exceptions are explicit and mapped at adapter edge.
- [ ] Ubiquitous language is reflected in package/class names.

### 4. FAPI and API Contract
- [ ] Protected endpoints enforce `Authorization` token type (`DPoP`/`Bearer`) and `DPoP` proof header.
- [ ] `X-FAPI-Interaction-ID` is required and echoed for traceability.
- [ ] Write endpoints enforce idempotency keys and deterministic replay/conflict behavior.

### 5. Cache and Consistency
- [ ] Read-path cache semantics are explicit (`X-OF-Cache`, `ETag`, `If-None-Match`).
- [ ] Write replay state/idempotency TTL is explicit.
- [ ] `Cache-Control: no-store` is enforced for sensitive responses.

### 6. Testing and Quality Gates
- [ ] TDD sequence documented and followed (tests-first, implementation, refactor).
- [ ] Test pyramid implemented (unit, integration, e2e/functional, UAT).
- [ ] Use-case package coverage is >= 85% in domain/application/infrastructure.

### 7. 12-Factor Alignment
- [ ] Runtime config externalized via properties/env (no hard-coded operational values).
- [ ] Stateless services with state delegated to ports/adapters.
- [ ] Logs/events are stream-friendly and correlation-ID compatible.

## HLD Review Snapshot (2026-02-10)

| HLD | Hexagonal Boundary | FAPI/API | Idempotency/Cache | Testability | Status |
| --- | --- | --- | --- | --- | --- |
| `consent-management-system-hld.md` | Clear | Strong | Strong | High | Compliant |
| `account-information-service-hld.md` | Clear | Strong | Read cache defined | High | Compliant |
| `confirmation-of-payee-hld.md` | Clear | Strong | Read cache defined | High | Compliant |
| `payments-initiation-hld.md` | Clear | Strong | Strong write idempotency | High | Compliant |
| `corporate-treasury-and-bulk-payments-hld.md` | Clear | Strong | Batch idempotency explicit | High | Compliant |
| `insurance-data-and-quotes-hld.md` | Clear | Strong | Quote/cache semantics explicit | High | Compliant |
| `fx-remittance-services-hld.md` | Clear | Strong | Quote/deal idempotency explicit | High | Compliant |
| `open-finance-capability-overview.md` | Conceptual (cross-capability) | Strong | Baseline stated | Medium | Compliant |

## Implementation Follow-up (Current)
- Dynamic Onboarding implementation completed using this checklist:
  - Ports: `OnboardingUseCase`, KYC/Sanctions/Account/Idempotency/Cache/Event output ports.
  - FAPI controls + idempotency + ETag/cache headers implemented.
  - Test pyramid complete with Dynamic Onboarding coverage > 85% across all three modules.
- Open Products Data implementation completed using this checklist:
  - Ports: `ProductDataUseCase`, catalog/cache output ports.
  - Open-data API controls + cache optimization implemented (`X-FAPI-Interaction-ID`, optional token-type validation, `X-OF-Cache`, `ETag`, `If-None-Match`, `Cache-Control: public`).
  - Test pyramid complete (unit + integration + e2e/UAT) with Open Products Data coverage > 85% across domain/application/infrastructure.
