# Use Case 03: Payment Initiation Service (PIS)

## 1. High-Level Design (HLD)

### Architecture Overview

PIS is a transactional write-heavy service requiring strong consistency for state transitions and strict idempotency.

* **Pattern:** Saga orchestration with compensating actions (no distributed 2PC across services).
* **Idempotency:** Mandatory for all write operations to prevent duplicate debits.
* **State Machine:** `Pending -> ConsentAuthorised -> RiskChecked -> Submitted -> Settled | Rejected | Reversed`.

### Components

1. **API Gateway + Security Filters:** mTLS, DPoP verification, FAPI headers.
2. **Idempotency Shield (Redis + DB record):** Checks `X-Idempotency-Key` and payload hash.
3. **Payment Orchestrator:** Coordinates validation, risk check, rail submission, compensation.
4. **Risk Engine:** Fraud, sanctions, AML checks (sync/async depending on risk policy).
5. **Core/Rail Connector:** Integration to SEPA/SWIFT/local rails with retry/circuit breaker.
6. **Payment Store (PostgreSQL):** System of record for intents, states, and audit logs.
7. **Kafka Publisher:** Emits payment lifecycle domain events.

---

## 2. Functional Requirements

1. **Single Immediate Payment:** Transfer amount to beneficiary immediately.
2. **Status Polling:** TPP can query payment status and terminal outcome.
3. **Funds Check:** Validate and reserve funds before submission.
4. **Cut-Off Handling:** Route or defer based on banking-hour and rail rules.
5. **Idempotent Retries:** Same key + same payload returns original outcome; same key + different payload returns conflict.

## 3. Service Level Implementation (NFRs)

* **Acknowledgement:** `201 Created`/`202 Accepted` within 500ms.
* **Processing Performance:** Payment-processing path P95 < 200ms for internal decisioning components.
* **Consistency:** ACID local transactions for each state transition.
* **Availability:** 99.99% for payment processing path.
* **Idempotency Window:** Minimum 24 hours key retention.
* **Auditability:** Immutable payment transition log required.

---

## 4. API Signatures

### Create Payment Consent

```http
POST /open-banking/v1/payment-consents
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
Content-Type: application/json
```

**Payload:**

```json
{
  "instructedAmount": { "amount": "100.00", "currency": "USD" },
  "creditorAccount": { "iban": "US123..." }
}
```

### Submit Payment

```http
POST /open-banking/v1/payments
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
X-Idempotency-Key: <UUID>
x-jws-signature: <Detached_JWS>
```

### Get Payment Status

```http
GET /open-banking/v1/payments/{PaymentId}
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

---

## 5. Database Design (Project-Aligned Persistence)

**System of Record:** PostgreSQL  
**Hot Idempotency Cache:** Redis  
**Analytics:** MongoDB silver copy

**Table: `pis.payment_intents`**

* **PK:** `payment_id`
* **Fields:** `psu_id`, `tpp_id`, `consent_id`, `amount`, `currency`, `creditor_ref`, `status`, `created_at`

**Table: `pis.payment_state_transitions`**

* **PK:** `transition_id`
* **FK:** `payment_id`
* **Fields:** `previous_status`, `new_status`, `occurred_at`, `system_actor`, `reason_code`
* **Purpose:** Immutable audit/event trail.

**Table: `pis.idempotency_keys`**

* **PK:** `idempotency_key`
* **Fields:** `payload_hash`, `response_code`, `response_body`, `created_at`, `expires_at`
* **Constraint:** Unique key + conflict on payload mismatch.

**Redis Keys**

* `pis:idempotency:{key}` (short-latency lookup)

---

## 6. Postman Collection Structure

* **Folder:** `1. Setup Payment`
* `POST /payment-consents`

* **Folder:** `2. Execute Payment`
* `POST /payments` (with unique `X-Idempotency-Key`)
* `POST /payments` (retry with same key; assert same response)
* `POST /payments` (same key, changed payload; assert `409`)

* **Folder:** `3. Monitoring`
* `GET /payments/{id}` (poll until terminal status)
