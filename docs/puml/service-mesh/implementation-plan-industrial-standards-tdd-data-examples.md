# Implementation Plan: Keycloak-Centric AAA + Service Mesh (Industrial Standards, TDD, Sample Data)

## 1. Purpose and Outcomes

This plan defines the implementation path for a production-grade Open Finance security platform with:

- Centralized identity provider (Keycloak preferred)
- LDAP/AD federation
- Distributed authorization (gateway + mesh + ext_authz/OPA)
- DPoP-bound token security with replay protection
- TDD-first engineering workflow and measurable quality gates

Primary outcomes:

1. Strong AAA control plane across all Open Finance services
2. Secure east-west communication with strict mTLS
3. DPoP conformance for protected APIs
4. Deterministic verification with automated tests and quality gates

---

## 2. Scope

In scope:

- Identity and access architecture for Open Finance workloads
- Keycloak realms, clients, scopes, claims, and policy model
- Mesh security policy integration (Istio or equivalent)
- API gateway integration for OAuth/OIDC and DPoP validation
- CI/CD security and test gates

Out of scope:

- Replacing all business-domain logic
- Custom IdP development
- Non-security functional redesign of existing use cases

---

## 3. Industrial Standards Baseline

## 3.1 Security and Identity Standards

| Domain | Standard / Reference | Implementation Use |
| --- | --- | --- |
| OAuth2 core | RFC 6749 | Authorization server and token endpoint behavior |
| OAuth2 threat model | RFC 6819 | Threat controls and mitigation checklist |
| OAuth2 auth code + PKCE | RFC 7636 | Public clients and native/mobile consent flows |
| OAuth2 for native apps | RFC 8252 | Redirect URI and app-to-app security |
| OAuth2 mTLS client auth | RFC 8705 | Confidential client authentication |
| OAuth2 PAR | RFC 9126 | Pushed authorization requests for high-risk flows |
| OAuth2/JWT access token profile | RFC 9068 | JWT claim contract |
| OpenID Connect Core | OIDC 1.0 | Identity layer and user authentication |
| DPoP | RFC 9449 | Sender-constrained tokens and proof validation |
| JWK/JWKS | RFC 7517 | Key distribution and rotation |
| JWT/JWS/JWE | RFC 7519/7515/7516 | Token and signature/encryption handling |
| FAPI | OpenID FAPI 1.0/2.0 profiles | Financial-grade API controls and hardening |
| API security verification | OWASP ASVS + OWASP API Security Top 10 | Secure coding and verification checklist |
| Identity assurance | NIST SP 800-63 | Identity lifecycle and authenticator assurance |
| ISMS | ISO/IEC 27001:2022 | Control governance and auditability |
| Privacy management | ISO/IEC 27701 | PII processing and minimization controls |
| Payment data (if applicable) | PCI DSS 4.0 | Security controls where payment data is in scope |

## 3.2 Platform and Delivery Standards

| Domain | Standard / Practice | Implementation Use |
| --- | --- | --- |
| Container security | CIS Kubernetes Benchmark | Cluster and workload hardening |
| Software supply chain | SLSA / signed artifacts | Build provenance and integrity |
| IaC governance | Terraform + policy-as-code | Repeatable and auditable infra rollout |
| Observability | OpenTelemetry semantic conventions | Consistent metrics/log/trace model |
| Delivery discipline | CMMI Level-3 style process control | Requirements, verification, traceability |

---

## 4. Target Architecture (AAA + Mesh)

1. Keycloak cluster (active-active) is the centralized authentication authority.
2. LDAP/AD federation provides enterprise identity source and group mapping.
3. API gateway performs first-line DPoP validation and route-level coarse authz.
4. Service mesh enforces strict mTLS and workload-to-workload authorization.
5. OPA/ext_authz agents provide fine-grained ABAC/RBAC decisions.
6. Immutable audit sink stores authN/authZ/DPoP evidence with correlation IDs.

Reference docs:

- `to-be-open-finance-service-mesh.puml`
- `keycloak-aaa-blueprint.md`
- `plan.md`
- `refactor.md`

---

## 5. Delivery Phases and Work Packages

## Phase 0: Governance and Readiness (1-2 weeks)

Deliverables:

- Approved ADR for mesh + IdP choices
- Security requirements baseline (FAPI/DPoP/mTLS)
- Traceability matrix from controls to tests

Exit criteria:

- Signed architecture decision
- Risk register created and owned

## Phase 1: Platform Foundation (2 weeks)

Deliverables:

- Mesh control plane in non-prod
- Sidecar injection policy by namespace
- Ingress/egress gateways integrated with existing API gateway

Exit criteria:

- Mesh health green
- Smoke tests pass with sidecar-enabled workloads

## Phase 2: Identity Foundation (2 weeks)

Deliverables:

- Keycloak HA deployment + PostgreSQL HA backend
- LDAP federation configured (users/groups)
- Realm and client baseline defined as code

Exit criteria:

- OIDC discovery/JWKS reachable
- Auth code + token flow passing in non-prod

## Phase 3: DPoP and AuthZ Foundation (2 weeks)

Deliverables:

- Gateway DPoP verifier + replay cache
- ext_authz integration with OPA
- Namespace-level deny-by-default authorization policies

Exit criteria:

- Valid DPoP accepted, replay rejected
- Unauthorized cross-service requests blocked

## Phase 4: Wave 1 Service Migration (2 weeks)

Target services:

- `openfinance-open-products-service`
- `openfinance-atm-directory-service`

Exit criteria:

- Canaries stable
- SLOs within budget

## Phase 5: Wave 2 Service Migration (3 weeks)

Target services:

- `openfinance-personal-financial-data-service`
- `openfinance-business-financial-data-service`
- `openfinance-banking-metadata-service`
- `openfinance-confirmation-of-payee-service`

Exit criteria:

- Contract + integration + performance tests pass in mesh

## Phase 6: Wave 3 Critical Security Flows (3 weeks)

Target services:

- `openfinance-consent-authorization-service`
- Payment-adjacent contexts requiring strict compliance controls

Exit criteria:

- DPoP and consent-bound authorization fully enforced
- Compliance signoff complete

## Phase 7: Hardening and Production Cutover (1-2 weeks)

Deliverables:

- Global strict mTLS
- Drift detection and immutable policy promotion
- DR and incident game-day evidence

Exit criteria:

- Production readiness signoff by engineering, security, compliance

---

## 6. TDD Execution Model (Mandatory)

## 6.1 Test Pyramid and Sequence

1. Unit tests (domain and policy evaluation logic)
2. Contract tests (OpenAPI and token claim contract)
3. Component tests (gateway verifier + authz agent integration)
4. Integration tests (Keycloak + LDAP + replay cache + service)
5. E2E tests (consent/auth/payment journeys)
6. UAT and compliance scenario tests

## 6.2 Red-Green-Refactor by Work Package

For each feature:

1. Write failing tests:
   - Security behavior
   - Negative paths (replay, invalid issuer, wrong audience, stale iat)
2. Implement minimum code/policy to pass.
3. Refactor:
   - Remove duplication
   - Improve readability and policy reuse
4. Re-run full suite and coverage gates.

## 6.3 TDD Quality Gates

| Gate | Threshold |
| --- | --- |
| Unit coverage | >= 85% line coverage |
| Security regression pack | 100% pass |
| DPoP conformance suite | 100% pass |
| Contract drift | 0 unresolved diffs |
| Critical path p95 latency | within agreed SLO (for example < 500ms external APIs) |

## 6.4 Test Categories (Minimum)

- `TC-AUTH-001`: auth code + PKCE happy path
- `TC-AUTH-002`: invalid client assertion
- `TC-DPOP-001`: valid DPoP proof accepted
- `TC-DPOP-002`: replayed `jti` rejected
- `TC-DPOP-003`: `ath` mismatch rejected
- `TC-TOKEN-001`: wrong audience rejected
- `TC-TOKEN-002`: expired token rejected
- `TC-MTLS-001`: no cert rejected at mesh edge
- `TC-AUTHZ-001`: unauthorized service identity blocked
- `TC-AUDIT-001`: all decision fields logged and queryable

---

## 7. Sample Data (Dev/Sandbox)

The following sample data is for non-production testing only.

## 7.1 Realm and Client Sample (Keycloak)

```json
{
  "realm": "open-finance-sandbox",
  "enabled": true,
  "accessTokenLifespan": 300,
  "ssoSessionIdleTimeout": 1800,
  "clients": [
    {
      "clientId": "tpp-mobile-sandbox",
      "publicClient": true,
      "standardFlowEnabled": true,
      "attributes": {
        "pkce.code.challenge.method": "S256",
        "require.pushed.authorization.requests": "true"
      },
      "redirectUris": [
        "com.tpp.app://callback",
        "https://tpp-sandbox.example/callback"
      ],
      "defaultClientScopes": ["openid", "profile", "accounts.read", "balances.read"]
    },
    {
      "clientId": "tpp-backend-sandbox",
      "publicClient": false,
      "clientAuthenticatorType": "client-jwt",
      "serviceAccountsEnabled": true,
      "attributes": {
        "tls.client.certificate.bound.access.tokens": "true"
      },
      "defaultClientScopes": ["payments.write", "consents.manage"]
    }
  ]
}
```

## 7.2 LDAP Federation Sample (LDIF)

```ldif
dn: ou=people,dc=openfinance,dc=example
objectClass: organizationalUnit
ou: people

dn: uid=psu.sandbox.001,ou=people,dc=openfinance,dc=example
objectClass: inetOrgPerson
uid: psu.sandbox.001
cn: PSU Sandbox 001
sn: Sandbox
mail: psu001@openfinance.example
employeeType: retail

dn: cn=grp_tpp_payments,ou=groups,dc=openfinance,dc=example
objectClass: groupOfNames
cn: grp_tpp_payments
member: uid=psu.sandbox.001,ou=people,dc=openfinance,dc=example
```

## 7.3 DPoP Proof Payload Sample

```json
{
  "htu": "https://api.sandbox.openfinance.example/open-finance/v1/payments",
  "htm": "POST",
  "iat": 1771846800,
  "jti": "1ef9d2d0-6ca8-4f31-b0f2-18c4d6f58c00",
  "ath": "hJtXIZ2uSN5kbQfbtTNWbg",
  "nonce": "optional-nonce-issued-by-gateway"
}
```

## 7.4 Consent and Token Session Sample

```json
{
  "consentId": "CONS_SANDBOX_1001",
  "customerId": "CUST_SANDBOX_1001",
  "participantId": "TPP_SANDBOX_01",
  "scopes": ["ReadAccounts", "ReadBalances"],
  "status": "AUTHORIZED",
  "expiresAt": "2026-03-31T23:59:59Z"
}
```

```json
{
  "jti": "ef4b7e9a-17b7-4d8d-a6c0-e6f192cb0031",
  "subject": "psu.sandbox.001",
  "active": true,
  "issuedAt": "2026-02-24T10:00:00Z",
  "expiresAt": "2026-02-24T10:05:00Z"
}
```

## 7.5 Replay Cache Entry Sample

```json
{
  "key": "dpop:1ef9d2d0-6ca8-4f31-b0f2-18c4d6f58c00:thumbprint123:POST:/open-finance/v1/payments",
  "ttlSeconds": 300,
  "status": "USED"
}
```

---

## 8. CI/CD and Release Gates

Required pipeline stages:

1. Static analysis and secret scanning
2. Unit tests and coverage gate
3. Contract tests (OpenAPI, token claims)
4. Integration tests with Keycloak + LDAP + Redis test containers
5. Security tests (DPoP negative matrix, mTLS checks)
6. Performance tests (load and burst)
7. Progressive delivery (canary + auto rollback)

Blocking criteria:

- Coverage < 85%
- Any failed DPoP or authz security test
- Contract drift unresolved
- Critical vulnerabilities unresolved

---

## 9. RACI (Execution Accountability)

| Workstream | Engineering | Platform/SRE | Security | Compliance | Product |
| --- | --- | --- | --- | --- | --- |
| Keycloak and LDAP setup | R | A | C | C | I |
| DPoP gateway enforcement | R | A | A | C | I |
| Mesh authz policy | R | A | C | I | I |
| Test automation (TDD suites) | A | C | C | I | I |
| Audit retention and dispute evidence | C | C | A | A | I |
| Release approval | R | R | A | A | A |

Legend: R = Responsible, A = Accountable, C = Consulted, I = Informed.

---

## 10. Acceptance Criteria (Go-Live)

1. All in-scope APIs reject missing/invalid/replayed DPoP proofs.
2. All east-west calls are mTLS-protected and policy-verified.
3. All services trust approved Keycloak issuer and audience.
4. Audit trail is complete for authN/authZ/DPoP events.
5. TDD gates pass with >= 85% coverage and full security regression green.
6. DR failover and rollback rehearsals completed successfully.

