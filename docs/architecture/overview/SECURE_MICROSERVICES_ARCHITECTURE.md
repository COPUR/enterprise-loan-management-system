# Secure Microservices Architecture Baseline

## Purpose

This document defines the enforceable security architecture baseline for the platform runtime.

- Identity: Keycloak (OAuth2.1/OIDC)
- Trust model: Zero Trust with mTLS service mesh
- API proof model: Bearer token + DPoP for protected endpoints
- Contract source: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/api/openapi`
- API inventory source: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/API_CATALOGUE.md`

## Scope

This baseline applies to:

- Open Finance capability services
- Bounded-context services (`customer-context`, `loan-context`, `payment-context`, `risk-context`, `compliance-context`)
- Shared ingress, security, observability, and secret-management layers

## Architecture Principles

- Contract-first delivery: OpenAPI is authoritative for routes, methods, and auth requirements.
- Defense in depth: gateway, mesh, workload, and data layers each enforce controls.
- Least privilege: scopes, audience, DPoP binding, and policy checks are mandatory for protected APIs.
- Runtime secret resolution: secrets are fetched at runtime and masked at rest.
- Traceability: every request is correlated with trace and interaction IDs.

## Security Control Planes

### 1. Edge and Ingress Plane

- API gateway and ingress enforce TLS 1.2+ and mTLS where required.
- WAF and rate limiting execute before routing.
- Request IDs and FAPI interaction IDs are normalized.

### 2. Identity and Authorization Plane

- Keycloak is the centralized IdP and authorization server.
- LDAP/AD federation is used for enterprise identity sources.
- Token introspection/JWKS validation is centralized and cached.
- OAuth2.1 Authorization Code + PKCE is mandatory for user-consent journeys.
- Client Credentials flow is allowed for machine-to-machine use cases.

### 3. API Proof and Access Plane

- Protected endpoints require:
  - Valid JWT (issuer, audience, expiry, signature)
  - Valid DPoP proof (htu, htm, jti replay prevention, nonce where required)
  - Scope and consent checks
- Public endpoints are restricted to approved open-data contracts and still rate-limited.

### 4. Service-to-Service Plane

- Service mesh enforces strict mTLS between workloads.
- AuthorizationPolicy/OPA checks service identity and operation policy.
- Sidecar telemetry emits request-level metrics and traces.

### 5. Data Protection Plane

- Data in transit: TLS/mTLS everywhere.
- Data at rest: encrypted storage (DB, cache, object storage, backups).
- Field-level protection for sensitive values (PII, account identifiers, keys).
- Sensitive logs are masked before persistence.

### 6. Observability and Audit Plane

- Structured logs include: `trace_id`, `span_id`, `x-fapi-interaction-id`, caller, decision.
- Metrics include security failures by control type.
- Audit events are immutable and searchable by interaction ID.

## Canonical Request Security Chain

1. Client calls API with `Authorization: DPoP <access-token>` and `DPoP` proof header.
2. Gateway validates mTLS and rate limits.
3. Security chain validates JWT signature/claims.
4. Security chain validates DPoP proof and replay window.
5. Scope/consent policy authorizes operation.
6. Service handles request and emits structured telemetry.
7. Data adapters enforce at-rest and field-level controls.
8. Audit event persists with correlation IDs.

## AAA Architecture (Distributed, Keycloak-Centric)

### Authentication

- Keycloak realm authenticates users and clients.
- MFA and risk signals can trigger step-up authentication.
- LDAP federation integrates enterprise identity sources.

### Authorization

- Coarse-grained auth via OAuth scopes and token audiences.
- Fine-grained auth via service policy (OPA/Istio AuthorizationPolicy).
- Consent-bound decisions enforced in domain/application services.

### Accounting

- Immutable audit logs for consent, payment, verification, and admin actions.
- Trace-linked records for dispute, compliance, and incident response.

## Standards Mapping

- FAPI 2.0 Baseline and Advanced profiles (token, signing, transport)
- OAuth2.1 + OIDC
- RFC 9449 (DPoP)
- OWASP ASVS controls for API and session security
- PCI DSS / privacy controls for data handling and logging

## Reference Diagrams

- Security container view: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/architecture/security-architecture.puml`
- Request sequence view: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/architecture/security-request-flow.uml`
- Service-level security view: `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/puml/security/service-level-security.puml`

## Delivery Guardrails

Changes are considered incomplete if any of the following are missing:

- Updated OpenAPI contract in `api/openapi`
- Updated API catalogue in `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/API_CATALOGUE.md`
- Updated security diagrams listed above
- Tests proving authn/authz behavior for changed endpoints
