# Service Mesh Refactor Guide

## Purpose

Define the concrete refactor backlog to move from application-centric transport controls to mesh-governed security, resilience, and observability without breaking business behavior.

## Refactor Principles

1. Keep domain behavior in service code; move transport policy to mesh.
2. Enforce zero-trust east-west communication.
3. Prefer declarative policy over custom per-service middleware.
4. Use progressive delivery with rollback-first execution.
5. Standardize telemetry across all services.

## Architecture Refactor Mapping

### As-Is

- API gateway handles north-south.
- Services communicate directly.
- Security/retry/timeout logic duplicated in service code.
- Uneven logs, traces, and metrics.

### To-Be

- API gateway + mesh ingress for north-south.
- Sidecar proxy for all service pods.
- mTLS and service authorization in mesh policy layer.
- Centralized retries/timeouts/circuit-breaking via traffic policies.
- Unified telemetry via OpenTelemetry + metrics/log pipelines.

## Refactor Workstreams

## WS1: Networking and Security

- Add namespace-level sidecar injection.
- Introduce `PeerAuthentication` in `PERMISSIVE`, then `STRICT`.
- Add `AuthorizationPolicy` deny-by-default + explicit allow rules.
- Add egress policy with gateway and allow-listed external hosts.
- Remove conflicting in-service transport ACL code where mesh replaces it.

## WS2: Reliability and Traffic Policy

- Replace ad hoc retry logic with mesh `VirtualService`/`DestinationRule` policies.
- Set per-route timeout budgets and outlier detection.
- Add canary and blue/green routing definitions.
- Standardize connection pool limits and circuit-breaker thresholds.

## WS3: Observability Baseline

- Enforce trace headers propagation at gateway and sidecars.
- Export metrics/logs/traces using consistent service labels.
- Add standard dashboards and alerts for all onboarded services.
- Remove custom metrics formats not aligned to platform conventions.

## WS4: Service Code Simplification

- Keep business auth checks (FAPI scopes, consent state, DPoP/JWS validation) in app.
- Remove duplicate transport-level token forwarding checks if mesh already enforces mTLS identity.
- Standardize error model for policy-denied and unauthorized responses.
- Externalize all runtime configuration per 12-factor principles.

## WS5: CI/CD and Governance

- Add mesh policy validation jobs in Jenkins/GitLab pipelines.
- Add pre-deploy policy simulation checks.
- Require wave-specific verification checklist in PR templates.
- Add drift detection for runtime mesh objects vs Git.

## Service-Level Refactor Checklist

1. Sidecar injection enabled.
2. Service identity and authz policy applied.
3. mTLS strict mode validated.
4. Retry/timeout policy defined by route class.
5. Egress policy constrained.
6. Contract and integration tests pass under mesh.
7. Load test confirms latency budget.
8. Dashboards and alerts linked.
9. Runbook updated.
10. Rollback tested.

## Risk Controls

- Policy lockout risk: pre-merge simulation and staged rollout.
- Latency regression risk: canary plus automatic rollback gates.
- Observability blind spots: block promotion unless telemetry health checks pass.
- Dependency outage amplification: mesh timeouts and bulkheads with conservative defaults.

## Definition of Refactor Completion

- No direct unencrypted east-west service traffic.
- All in-scope services managed by mesh policies.
- No duplicated transport retries/circuit breakers in service code unless explicitly justified.
- Security and reliability gates are mandatory in CI/CD.
- Operational team accepts runbooks and incident drill outcomes.

