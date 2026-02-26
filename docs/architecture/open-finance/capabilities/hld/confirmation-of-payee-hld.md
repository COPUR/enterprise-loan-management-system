# Use Case HLD: Confirmation of Payee - Confirmation of Payee (CoP)

## 1. High-Level Design (HLD) & Architecture

### Architectural Principles

* **Low Latency:** CoP is a blocking call in payment UX and must optimize read path latency.
* **Stateless API Layer:** Runtime processing is stateless; audit evidence is persisted separately.
* **Decoupled Matching Engine:** Fuzzy matching index is isolated from transactional systems.

### System Components

1. **CoP Service:** Lightweight verification API.
2. **Search Engine:** Fuzzy/phonetic name matching.
3. **Account Status Cache (Redis):** Active/closed/deceased account state.
4. **Audit Store (PostgreSQL):** Immutable request/decision evidence.

### Distributed Data Flow

1. TPP calls `POST /confirmation` with account identifier and name.
2. API validates FAPI/DPoP/mTLS security controls.
3. Service checks account status from Redis/read model.
4. Service computes fuzzy match score via search index.
5. Decision is returned and audit event persisted.

---

## 2. Functional Requirements

1. **Name Matching:** Support Arabic/English transliteration and fuzzy variants.
2. **Privacy:** Never return registered name on `NoMatch`; return partial/controlled name only on `CloseMatch` per policy.
3. **Account Status Check:** Verify beneficiary account can receive funds.
4. **Secondary Identifier:** Support mobile/proxy identifier checks where applicable.

---

## 3. Service Level Implementation & NFRs

### Performance Guardrails

* **TTLB:** <= 300ms.
* **Burst Handling:** Support payroll-period traffic spikes with autoscaling.
* **Availability:** 99.99% for CoP verification endpoint.

### Security Guardrails

* **Authentication:** OAuth 2.1 + FAPI 2.0 + DPoP.
* **Transport:** mTLS + TLS 1.3.
* **Abuse Protection:** Per-TPP rate limits and anti-enumeration controls.
* **Audit Logging:** Immutable request/decision records with interaction IDs.

---

## 4. API Signatures

### POST /confirmation

```http
POST /open-finance/v1/confirmation-of-payee/confirmation
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
Content-Type: application/json
```

**Request Body:**

```json
{
  "Data": {
    "Identification": "AE29000000123456789",
    "SchemeName": "IBAN",
    "Name": "Al Tareq Trading LLC"
  }
}
```

**Response Body:**

```json
{
  "Data": {
    "AccountStatus": "Active",
    "NameMatched": "CloseMatch",
    "MatchedName": "Al Tareq Trading"
  }
}
```

---

## 5. Database Design (Project-Aligned Persistence)

**Operational Read Data:** Search index + Redis  
**Audit System of Record:** PostgreSQL

### Index: `cop_directory`

* Key fields: `iban`, `searchable_name`, `account_status`, `legal_name`.
* Optimized for phonetic/fuzzy matching.

### Table: `cop_audit_log`

```sql
(audit_id PK, tpp_id, request_identifier, request_name, response_decision, match_score, interaction_id, occurred_at)
```

---

## 6. Postman Collection Structure

* **Collection:** `LFI_COP`
* **Folder:** `Verification`
* `POST /confirmation (Exact Match)`
* `POST /confirmation (Close Match)`
* `POST /confirmation (No Match)`
* `POST /confirmation (Closed Account)`
