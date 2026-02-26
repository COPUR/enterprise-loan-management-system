# Use Case 09 & 10: Insurance Data & Quotes

## 1. High-Level Design (HLD)

### Architecture Overview

Insurance integration must shield Open Finance APIs from legacy system complexity while preserving consistent customer outcomes.

* **Pattern:** Adapter / Anti-Corruption Layer (ACL).
* **Decoupling:** `OpenInsurance_Adapter` maps modern REST/JSON contracts to legacy SOAP/XML or mainframe protocols.
* **State Management:** Quotes are short-lived and cached; policy binding is strongly consistent in system-of-record storage.

### Components

1. **Insurance API Gateway:** FAPI 2.0 controls, DPoP validation, throttling.
2. **Quote Engine:** Real-time pricing and underwriting rule evaluation.
3. **Policy ACL:** Contract translation between Open Insurance model and carrier-specific model.
4. **Legacy Integration Connectors:** SOAP/mainframe adapters.
5. **Document Generator:** Async generation of policy schedules/certificates.
6. **Event Publisher (Kafka):** `QuoteCreated`, `QuoteAccepted`, `PolicyIssued` lifecycle events.

---

## 2. Functional Requirements

1. **Dynamic Pricing:** Real-time quote generation using risk inputs.
2. **Bind Flow:** Convert `QuoteId` to `PolicyId` without data re-entry.
3. **Data Sharing:** Retrieve active policies and coverage details per consent.
4. **Read-Your-Writes:** Issued policy must be retrievable immediately after successful bind.
5. **Data Minimization:** Return only consented policy data; sensitive PII masked where required.

## 3. Service Level Implementation (NFRs)

* **Data Retrieval:** P95 < 1000ms (legacy dependency aware).
* **Quote Generation:** P95 < 2000ms.
* **Availability:** 99.99% for quote/bind APIs.
* **Security:** OAuth 2.1 + FAPI 2.0 + DPoP + mTLS.
* **Auditability:** Full lifecycle audit for quote and bind operations.

---

## 4. API Signatures

### Get Motor Policies

```http
GET /open-insurance/v1/motor-insurance-policies
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

### Request Quote

```http
POST /open-insurance/v1/motor-insurance-quotes
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
Content-Type: application/json
```

**Payload:**

```json
{
  "vehicleDetails": { "make": "Toyota", "model": "Camry", "year": 2023 },
  "driverDetails": { "age": 35, "licenseDuration": 10 }
}
```

### Accept Quote (Bind)

```http
PATCH /open-insurance/v1/motor-insurance-quotes/{QuoteId}
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
X-Idempotency-Key: <UUID>
```

**Payload:**

```json
{ "action": "ACCEPT", "paymentReference": "PAY_123" }
```

---

## 5. Database Design (Hybrid Persistence by Concern)

**Transactional System of Record:** PostgreSQL  
**Hot Quote Cache:** Redis  
**Document-Shaped Read/Analytics Models:** MongoDB

**Table: `insurance.quotes`**

* **PK:** `quote_id`
* **Fields:** `psu_id`, `product_type`, `premium_amount`, `valid_until`, `status`, `risk_hash`, `created_at`

**Table: `insurance.policies`**

* **PK:** `policy_id`
* **Fields:** `psu_id`, `policy_number`, `type`, `start_date`, `end_date`, `status`

**Table: `insurance.policy_audit`**

* **PK:** `audit_id`
* **FK:** `policy_id`
* **Fields:** `event_type`, `occurred_at`, `actor`, `interaction_id`

**Redis Keys**

* `insurance:quote:{quoteId}` (TTL)

**MongoDB Collections**

* `policy_documents` (hierarchical coverage view)
* `policy_claims` (read projection; not the policy SoR)

---

## 6. Postman Collection Structure

* **Folder:** `Insurance - Motor`
* `GET /motor-insurance-policies`
* `POST /motor-insurance-quotes` (capture `QuoteId`)
* `PATCH /motor-insurance-quotes/{QuoteId}` (bind policy)

* **Folder:** `Insurance - Assertions`
* Validate read-your-writes after bind
* Validate `X-FAPI-Interaction-ID` correlation in responses
