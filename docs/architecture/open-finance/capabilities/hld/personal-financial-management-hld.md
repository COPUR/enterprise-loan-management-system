# Use Case HLD: Personal Financial Management - Personal Financial Management (Retail Data)

## 1. High-Level Design (HLD) & Architecture

### Architectural Principles

* **Pattern:** CQRS with event-driven read projections.
* **Decoupling:** `OpenFinance_AIS_Service` is isolated from core banking write workloads via Kafka-fed read models.
* **Cohesion:** Account information logic (accounts, balances, transactions, beneficiaries) remains in the AIS domain.

### System Components

1. **API Gateway:** mTLS, DPoP validation, FAPI header validation, throttling.
2. **Consent Manager:** Validates active `ConsentId` and requested scopes.
3. **AIS Service:** Orchestrates retrieval and policy enforcement.
4. **AIS Read Models (PostgreSQL):** Query-optimized account and transaction views.
5. **Cache (Redis):** Hot account and balance views.
6. **Silver Copy (MongoDB):** Analytics/reporting projections only.

### Distributed Data Flow

1. **Sync:** Core banking updates -> Kafka topics -> AIS projector -> PostgreSQL read models (+ cache refresh).
2. **Read:** TPP request -> API Gateway -> AIS Service -> PostgreSQL/Redis -> response.

---

## 2. Functional Requirements

1. **Consent Verification:** Valid DPoP-bound access token and active consent with required scopes.
2. **Data Scope:**
* *Accounts:* IBAN (masked where required), currency, status.
* *Balances:* InterimAvailable, InterimBooked.
* *Transactions:* BookingDate, ValueDate, amount, merchant details.
3. **Filtering & Pagination:** Date filtering and mandatory pagination for large datasets.
4. **PII Controls:** Data minimization and field-level masking/encryption rules.

---

## 3. Service Level Implementation & NFRs

### Performance Guardrails

* **TTLB:** <= 500ms for standard queries.
* **Internal Processing Budget:** <= 250ms.
* **Availability:** 99.99% for AIS API path.
* **Scalability:** Horizontal scaling to high read TPS.

### Security Guardrails

* **Transport:** mTLS + TLS 1.3.
* **Authorization:** FAPI 2.0 + OAuth 2.1 + DPoP.
* **Request Correlation:** `X-FAPI-Interaction-ID` required.
* **Data Protection:** PII encryption and immutable audit logging.

---

## 4. API Signatures

### GET /accounts

```http
GET /open-finance/v1/accounts
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

### GET /accounts/{AccountId}/transactions

```http
GET /open-finance/v1/accounts/{AccountId}/transactions?fromBookingDateTime=2026-01-01T00:00:00Z
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

**Response Payload (Snippet):**

```json
{
  "Data": {
    "Transaction": [
      {
        "TransactionId": "TXN_123456",
        "Amount": { "Amount": "100.00", "Currency": "AED" },
        "CreditDebitIndicator": "Debit",
        "Status": "Booked",
        "BookingDateTime": "2026-01-02T10:00:00Z"
      }
    ]
  },
  "Links": { "Self": "..." },
  "Meta": { "TotalPages": 5 }
}
```

---

## 5. Database Design (Project-Aligned Persistence)

**Primary Read Models:** PostgreSQL  
**Cache:** Redis  
**Analytics:** MongoDB silver copy

### Table: `ais_read_models.accounts_view`

```sql
(account_id PK, psu_id, iban_masked, currency, account_type, status)
```

### Table: `ais_read_models.transactions_view`

```sql
(transaction_id PK, account_id FK, amount, currency, booking_date, value_date, merchant_name)
```

### MongoDB (Analytics)

```json
{
  "participantId": "BANK-001",
  "date": "ISODate(...)",
  "metrics": {
    "totalTransactions": 100000,
    "activeAccounts": 23000
  }
}
```

---

## 6. Postman Collection Structure

* **Collection:** `LFI_PFM`
* **Folder:** `Auth`
* `POST /oauth2/par`
* `GET /oauth2/authorize`
* `POST /oauth2/token`

* **Folder:** `Resources`
* `GET /accounts` (assert 200, TTLB <= 500ms)
* `GET /accounts/{id}/balances`
* `GET /accounts/{id}/transactions` (validate pagination links)
