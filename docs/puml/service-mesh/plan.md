# Service Mesh Migration Plan

## Objective

Migrate Open Finance services from direct east-west traffic to service-mesh managed communication with strict mTLS, policy-based authorization, controlled egress, and unified observability, while preserving FAPI controls in API/application layers.

## Scope

In scope:
- Open Finance service runtime in Kubernetes.
- Mesh onboarding for service-to-service traffic.
- Security, reliability, and telemetry policy standardization.
- CI/CD policy checks for mesh resources.

Out of scope:
- Rewriting business domain logic unrelated to transport/policy.
- Replacing external API gateway.

## Reference Diagrams

- As-Is: `as-is-open-finance-runtime.puml`
- To-Be: `to-be-open-finance-service-mesh.puml`

## Delivery Phases

| Phase | Duration | Goal | Exit Criteria | Delivery Artifact |
| --- | --- | --- | --- | --- |
| P0 Discovery & ADR | 1-2 weeks | Select mesh stack and migration constraints | Approved ADR, risk register baseline | `docs/service-mesh/adr-service-mesh-selection.md` |
| P1 Platform Foundation | 2 weeks | Install mesh in non-prod with ingress integration | Mesh control plane healthy; sidecar injection ready | `infra/mesh/base/*`, install runbook |
| P2 Security Baseline | 2 weeks | Apply mTLS + workload authz + egress control baseline | Namespaces in `STRICT` mTLS (non-prod); deny-by-default policies tested | `infra/mesh/security/*` |
| P3 Wave 1 Services | 2 weeks | Onboard low-risk services | Canaries pass SLO gates and rollback drills | Wave 1 migration report |
| P4 Wave 2 Services | 3 weeks | Onboard AIS/metadata/CoP services | Contract and perf tests pass under mesh | Wave 2 migration report |
| P5 Wave 3 Critical Flows | 3 weeks | Onboard consent and payment-adjacent services | Security and latency gates pass; compliance signoff | Wave 3 migration report |
| P6 Hardening & Cutover | 1-2 weeks | Global hardening and operational handover | Global policy lock, DR drill pass, go-live signoff | Production readiness checklist |

## Wave Ordering

1. Wave 1: `openfinance-open-products-service`, `openfinance-atm-directory-service`
2. Wave 2: `openfinance-personal-financial-data-service`, `openfinance-business-financial-data-service`, `openfinance-banking-metadata-service`, `openfinance-confirmation-of-payee-service`
3. Wave 3: `openfinance-consent-authorization-service` and payment-adjacent bounded contexts

## Controls and Quality Gates (CMMI-Aligned)

- Requirements Management: each service wave has signed acceptance criteria and non-functional SLO targets.
- Project Planning/Monitoring: weekly milestone review with burn-down and dependency register.
- Risk Management: tracked by probability/impact with mitigation owner and due date.
- Configuration Management: all mesh policies in Git; no manual drift in production.
- Process and Product Quality Assurance: policy lint, security scan, contract tests, performance gates.
- Verification and Validation: pre-prod smoke, integration, E2E, and UAT approval per wave.

## CI/CD Requirements

- Validate mesh resources (`kubectl --dry-run`, policy lint, schema validation).
- Run contract tests and integration tests against sidecar-enabled environments.
- Enforce canary promotion gates on error rate, latency, and saturation.
- Block merge if security policy pack or observability baseline is missing.

## Operational Readiness

- Golden signals dashboard per service: latency, traffic, errors, saturation.
- Trace propagation and structured logs enforced at ingress and sidecars.
- Incident runbooks for policy rollback, mTLS cert issues, and egress failures.

## Definition of Done

- Service receives traffic through mesh ingress and sidecar.
- Strict mTLS enabled for the service namespace.
- Least-privilege AuthorizationPolicy applied and verified.
- Egress allowlist policy enforced.
- SLOs and regression test suite pass for three consecutive deploys.

