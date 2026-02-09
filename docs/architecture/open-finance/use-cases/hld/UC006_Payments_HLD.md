# Use Case HLD: UC006 - Single & International Payments

## 1. High-Level Design (HLD) & Architecture

### Architectural Principles

* **Idempotency:** Mandatory to prevent duplicate payment execution.
* **Saga Orchestration:** Distributed workflow with compensations for partial failure.
* **Asynchronous Settlement:** API acknowledges quickly while settlement progresses asynchronously.

### System Components

1. **API Gateway:** Enforces mTLS, DPoP, and FAPI headers.
2. **Idempotency Layer (Redis + DB):** Deduplicates write requests.
3. **Payment Orchestrator:** Funds checks, state transitions, and compensation logic.
4. **Risk/Sanctions Engine:** AML and fraud controls.
5. **Rail Connectors:** Aani/SWIFT/local payment networks.
6. **Payment Store (PostgreSQL):** Source of truth for intents, transitions, and audit.
7. **Kafka Event Bus:** Payment lifecycle events.

### Distributed Data Flow

1. TPP sends `POST /payments`.
2. Idempotency check validates key and payload hash.
3. Payment intent persists as `Pending`.
4. API returns `201/202` quickly.
5. Async workers execute debit/credit and advance state machine.
6. Status APIs return final state.

---

## 2. Functional Requirements

1. **Consent Binding:** Payment payload must match authorized `ConsentId` constraints.
2. **Idempotency Rules:**
* Same key + same payload => return original response.
* Same key + different payload => `409 Conflict`.
3. **Risk Analysis:** Trigger step-up auth/reject on risk policy failures.
4. **Atomic Funds Check:** Check-and-reserve before submission.

---

## 3. Service Level Implementation & NFRs

### Performance Guardrails

* **Acknowledgement:** < 500ms.
* **Internal Processing:** payment decision path P95 < 200ms.
* **Availability:** 99.99% payment path.

### Security Guardrails

* **Security Profile:** OAuth 2.1 + FAPI 2.0 + DPoP + mTLS.
* **Non-Repudiation:** Detached JWS signatures for signed payment instructions.
* **Replay Protection:** DPoP JTI/nonce checks and idempotency enforcement.
* **Audit:** Immutable lifecycle logs for every transition.

---

## 4. API Signatures

### POST /payments

```http
POST /open-finance/v1/payments
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
X-Idempotency-Key: <UUID>
x-jws-signature: <Detached_Signature>
Content-Type: application/json
```

**Request Body:**

```json
{
  "Data": {
    "ConsentId": "CONS_888",
    "Initiation": {
      "InstructionIdentification": "INSTR_001",
      "EndToEndIdentification": "E2E_001",
      "InstructedAmount": { "Amount": "500.00", "Currency": "AED" },
      "CreditorAccount": { "SchemeName": "IBAN", "Identification": "AE..." }
    }
  }
}
```

---

## 5. Database Design (Project-Aligned Persistence)

**System of Record:** PostgreSQL  
**Idempotency Cache:** Redis  
**Analytics:** MongoDB silver copy

### Table: `pis.payment_consents`

```sql
(consent_id PK, status, max_amount, currency, payee_hash, expiry_at)
```

### Table: `pis.idempotency_keys`

```sql
(idempotency_key PK, tpp_id, request_hash, response_code, response_body, created_at, expires_at)
```

### Table: `pis.payment_transactions`

```sql
(payment_id PK, consent_id, debit_account_id, status, created_at, updated_at)
```

### Table: `pis.payment_transition_audit`

```sql
(audit_id PK, payment_id FK, previous_status, new_status, occurred_at, actor)
```

---

## 6. Postman Collection Structure

* **Collection:** `LFI_UC006_Payments`
* **Folder:** `Consent`
* `POST /payment-consents`

* **Folder:** `Execution`
* `POST /payments` (key A)
* `POST /payments` (retry key A, same payload)
* `POST /payments` (retry key A, different payload => 409)
* `GET /payments/{id}`
