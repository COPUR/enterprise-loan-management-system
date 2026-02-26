# Keycloak AAA Blueprint (Distributed, DPoP-Ready)

## Purpose

Define a production-grade AAA architecture for Open Finance using Keycloak as the centralized IdP, LDAP/AD as enterprise identity source, and distributed policy enforcement through gateway, mesh, and authz agents.

## Design Goals

- Centralize authentication and token issuance.
- Keep authorization decisions close to services while governed centrally.
- Enforce DPoP token binding and replay protection for protected APIs.
- Support FAPI-aligned flows for TPP and internal clients.
- Preserve service autonomy with clear trust contracts and bounded ownership.

## Target Topology

1. Centralized Keycloak cluster (HA) for OAuth2/OIDC.
2. LDAP/AD federation for workforce/internal users and groups.
3. API gateway as first DPoP enforcement point.
4. Service mesh sidecars + ext_authz agents for distributed authorization.
5. Immutable audit pipeline for authN/authZ/DPoP evidence.

## Realm Strategy

| Realm | Purpose | Identity Source | Token Audience Pattern |
| --- | --- | --- | --- |
| `open-finance-prod` | External TPP and PSU journeys | Local + regulated client onboarding | `api://open-finance/*` |
| `open-finance-internal` | Service-to-service and ops tooling | LDAP/AD federation | `api://internal/*` |
| `open-finance-sandbox` | Partner sandbox and certification | Local + test LDAP | `api://sandbox/*` |

### Realm Guardrails

- Separate signing keys per realm.
- Realm-specific client policies and token lifetimes.
- No cross-realm token trust unless explicitly configured and audited.

## Client Taxonomy

| Client Type | Examples | Auth Method | Required Controls |
| --- | --- | --- | --- |
| Public TPP app | Mobile/web TPP apps | PKCE + PAR | DPoP, short access token TTL, refresh rotation |
| Confidential TPP backend | ERP/TMS integrations | `private_key_jwt` + mTLS | DPoP, PAR, strict redirect URI and JWK policies |
| Gateway/Relying party | API gateway verifier/introspection client | `client_secret_jwt` or mTLS | Rate-limited introspection/JWKS cache |
| Internal service account | Mesh workloads | mTLS + service account JWT exchange | Audience-scoped tokens, least privilege roles |

## Protocol and Token Policy

### OAuth/OIDC Features

- Authorization Code + PKCE for user-facing consent flows.
- Client Credentials for internal service flows where appropriate.
- PAR mandatory for high-risk endpoints.
- JWK rotation with overlapping keys and `kid` continuity.

### Token Baseline

- Access token TTL: 5-15 minutes.
- Refresh token TTL: risk-based, rotate on use.
- Include claims: `iss`, `sub`, `aud`, `exp`, `iat`, `jti`, `scope`, `azp`, `client_id`.
- Include confirmation claim for DPoP-bound tokens: `cnf.jkt`.

## DPoP Architecture

### Enforcement Points

1. API gateway validates DPoP proof:
   - JWS signature and public key.
   - `htu`, `htm`, `iat`, `jti`.
   - `ath` against presented access token.
   - token binding with `cnf.jkt`.
2. Replay cache blocks duplicate proofs within skew window.
3. Mesh/ext_authz performs secondary policy checks for critical routes.

### Replay Cache Model

- Key format: `dpop:{jti}:{jkt}:{htu}:{htm}`.
- Store: Redis cluster (HA).
- TTL: `max(clock_skew_window, proof_validity_window)`; default 5 minutes.
- Reject policy: replay -> `401` with audit event.

## Practical Note

If runtime Keycloak version does not fully enforce DPoP semantics at token endpoint, keep issuance in Keycloak and perform canonical DPoP verification at gateway + authz agent until platform support is complete.

## LDAP/AD Federation Model

### Federation Settings

- Read-only LDAP bind for user lookup and group sync.
- Periodic full sync and incremental sync for near-real-time changes.
- Group-to-role mapper per realm.
- User attribute mapper for ABAC fields (segment, legal entity, region, entitlement tier).

### Identity Hygiene

- Enforce normalized usernames and immutable subject identifiers.
- Avoid direct PII claims in access token where not required.
- Use token exchange or user-info endpoints for expanded profile retrieval.

## Distributed Authorization (OPA/ext_authz)

### Decision Layers

1. Gateway layer: coarse access and DPoP presence.
2. Mesh/workload layer: service identity, namespace, and route allowlist.
3. OPA/ext_authz layer: fine-grained ABAC/RBAC with business claims.

### Policy Inputs

- JWT claims (`sub`, `scope`, `aud`, `azp`, `cnf.jkt`, custom entitlement claims).
- HTTP context (method, path, headers).
- Service identity (SPIFFE ID or mTLS principal).
- Domain context (consent state, tenant ownership, corporate division).

## Accounting and Compliance

### Mandatory Audit Fields

- Timestamp, request ID, interaction ID.
- Client ID, subject ID, token `jti`.
- DPoP `jti`, `jkt`, result (`pass/fail/replay`).
- Authz decision, policy ID/version, reason.
- Resource path, method, response code.

### Retention and Integrity

- Write to immutable sink (WORM-enabled store).
- Retention by policy (for example 13+ months for dispute support).
- Hash-chain or signed log batches for tamper evidence.

## Keycloak Configuration as Code

### Repository Structure

- `infra/identity/keycloak/realms/*.json`
- `infra/identity/keycloak/clients/*.json`
- `infra/identity/keycloak/client-scopes/*.json`
- `infra/identity/keycloak/policies/*.md`

### Pipeline Gates

1. Realm schema validation.
2. Drift detection between Git and running Keycloak.
3. Non-prod import + smoke tests.
4. Token contract tests (claims, audience, TTL, key id).
5. DPoP conformance tests (valid, invalid, replay, wrong `ath`).

## Reference Claim Contract

```json
{
  "iss": "https://idp.openfinance.example/realms/open-finance-prod",
  "sub": "2f0a4b8e-...-d1",
  "aud": ["api://open-finance/consent"],
  "azp": "tpp-mobile-app",
  "scope": "accounts.read balances.read",
  "jti": "ef4b7e9a-...-31",
  "cnf": {
    "jkt": "n0f8...thumbprint..."
  },
  "exp": 1771847400,
  "iat": 1771846800
}
```

## Rollout Sequence

1. Stand up Keycloak HA in non-prod.
2. Integrate LDAP federation and role mappers.
3. Configure clients/scopes and token policies.
4. Enable gateway DPoP verification and replay cache.
5. Enable ext_authz policies for one low-risk service wave.
6. Expand by wave with canary and rollback gates.
7. Enforce strict production policies after proving stability.

## Minimum Acceptance Criteria

- Central Keycloak issuer trusted by all in-scope services.
- LDAP sync and role mapping verified for all required groups.
- DPoP replay tests pass with deterministic rejection.
- ext_authz policies active for protected routes.
- Audit events complete and queryable for compliance workflows.
