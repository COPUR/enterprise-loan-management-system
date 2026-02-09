# Use Case 01: Consent Management System (CMS)

## 1. High-Level Design (HLD)

### Architecture Overview

The Consent Management System is the gatekeeper for Open Finance authorization and must align with the platform security baseline.

* **Pattern:** Centralized Authorization Server with distributed consent domain (OAuth 2.1 + OIDC, FAPI 2.0 Security Profile).
* **Decoupling:** Resource servers (AIS/PIS) validate signed tokens and introspect cryptographic proof (DPoP/mTLS context), not direct CMS database tables.
* **Distributed Principle:** Strong consistency for consent state transitions and token issuance; eventual consistency for analytics/reporting projections.

### Components

1. **API Gateway:** Enforces mTLS, rate limits, and required FAPI headers.
2. **Keycloak FAPI Realm (IdP):** OAuth 2.1, PKCE, PAR, DPoP-bound token issuance, private_key_jwt client auth.
3. **Consent Application Service:** Consent lifecycle orchestration and policy checks.
4. **Consent Event Store (PostgreSQL):** System of record for consent state and audit.
5. **Consent Cache (Redis):** Active consent lookup, participant status, DPoP nonce/JTI replay protection.
6. **Notification Publisher (Kafka):** Publishes `ConsentAuthorized`, `ConsentRevoked`, and `ConsentExpired` events.
7. **Silver-Copy Analytics (MongoDB):** Compliance and metrics projections only.

---

## 2. Functional Requirements

1. **Granting:** PSU can choose specific accounts and scopes to authorize for a TPP.
2. **Validation:** Participant/TPP must be active and regulator-certified before consent activation.
3. **Revocation:** PSU can revoke at any time; downstream access is blocked immediately (cache invalidation + token checks).
4. **Isolation:** No data access without valid `ConsentId`, valid DPoP-bound token, and scope match.
5. **Replay Protection:** Reject reused DPoP `jti` and nonce violations.

## 3. Service Level Implementation (NFRs)

* **Availability:** 99.995% for authentication/authorization service path.
* **Latency:** Token issuance P95 < 100ms; consent validation P95 < 150ms.
* **Security:** FAPI 2.0 + OAuth 2.1 (PAR, PKCE, DPoP, mTLS).
* **Auditability:** Immutable consent event/audit trail for all lifecycle transitions.
* **Resilience:** Circuit breaker + retry for external participant directory checks.

---

## 4. API Signatures

### Create Consent Intent

```http
POST /open-banking/v1/consents
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
Content-Type: application/json
```

**Payload:**

```json
{
  "permissions": ["ReadAccounts", "ReadBalances"],
  "expirationDateTime": "2026-12-31T00:00:00Z",
  "transactionFromDateTime": "2025-01-01T00:00:00Z"
}
```

### Pushed Authorization Request (PAR)

```http
POST /oauth2/par
Content-Type: application/x-www-form-urlencoded
```

### Authorize (Browser Flow)

```http
GET /oauth2/authorize?client_id=...&request_uri=...&response_type=code
```

### Token Exchange

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
DPoP: <dpop-proof-jwt>
```

---

## 5. Database Design (Project-Aligned Persistence)

**System of Record:** PostgreSQL (event store + read models)  
**Cache:** Redis  
**Analytics Silver Copy:** MongoDB

**Table: `consent_event_store.events`**

* **PK:** `(aggregate_id, sequence_number)`
* **Fields:** `event_type`, `event_data`, `metadata`, `occurred_at`, `correlation_id`

**Table: `consent_read_models.consent_view`**

* **PK:** `id`
* **Fields:** `customer_id`, `participant_id`, `status`, `scopes`, `created_at`, `expires_at`, `revoked_at`

**Table: `consent_read_models.audit_trail`**

* **PK:** `id`
* **Fields:** `aggregate_id`, `event_type`, `actor_id`, `occurred_at`, `compliance_tags`

**Redis Keys**

* `consent:valid:{consentId}` (TTL)
* `dpop:nonce:{jti}` (anti-replay TTL)

**MongoDB Collections (Analytics Only)**

* `consent_metrics_summary`
* `compliance_reports`

---

## 6. Postman Collection Structure

* **Folder:** `1. Client Onboarding`
* `POST /register` (Simulate TPP registration)

* **Folder:** `2. Consent Setup`
* `POST /consents` (Create consent intent)
* `POST /oauth2/par` (Create request URI)
* `GET /oauth2/authorize` (User authorization)

* **Folder:** `3. Token Management`
* `POST /oauth2/token` (Code exchange with DPoP)
* `POST /oauth2/token` (Refresh flow where enabled)

* **Folder:** `4. Consent Lifecycle`
* `PATCH /consents/{ConsentId}/revoke`
* `GET /consents/{ConsentId}`
