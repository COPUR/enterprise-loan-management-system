# Use Case 02: Account Information Service (AIS)

## 1. High-Level Design (HLD)

### Architecture Overview

AIS is a read-heavy service and follows CQRS with explicit separation between transactional source systems and read models.

* **Pattern:** CQRS + Event-Driven Projection.
* **Data Flow:** Core Banking changes are published to Kafka; AIS projectors update PostgreSQL read models and cache hot data in Redis.
* **Decoupling:** AIS never performs synchronous table-level joins into core banking stores during API requests.

### Components

1. **API Gateway + FAPI Security Layer:** mTLS, DPoP, FAPI header enforcement.
2. **Consent/Scope Validator:** Validates `ConsentId`, participant, and allowed scopes.
3. **AIS Read API:** Serves accounts, balances, and transactions.
4. **AIS Projector:** Consumes `AccountUpdated` and `TransactionBooked` events from Kafka.
5. **Read Model Store (PostgreSQL):** Query-optimized account/balance/transaction views.
6. **Redis Cache:** Short-lived account summary and balance cache.
7. **MongoDB Silver Copy (Optional):** Analytics/reporting projections only.

---

## 2. Functional Requirements

1. **Filtering:** Filter transactions by booking/value date ranges.
2. **Pagination:** Mandatory cursor/page-based pagination for large result sets.
3. **Scope Enforcement:** `ReadBalances` consent cannot access transactions (`403`).
4. **Multi-Currency:** Return booked/interim balances per account currency.
5. **Data Minimization:** Mask IBAN or sensitive PII fields unless explicitly consented.

## 3. Service Level Implementation (NFRs)

* **Response Time:** Balance inquiry P95 < 150ms; transactions endpoint P95 < 500ms.
* **Throughput:** Sustain high read throughput (targeting >1,000 TPS on AIS endpoints).
* **Freshness:** Read model lag target < 5 seconds from event publication.
* **Availability:** 99.99% for AIS service path.
* **Security:** OAuth 2.1 + FAPI 2.0 + DPoP + mTLS.

---

## 4. API Signatures

### Get Account List

```http
GET /open-banking/v1/accounts
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

### Get Balances

```http
GET /open-banking/v1/accounts/{AccountId}/balances
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

### Get Transactions

```http
GET /open-banking/v1/accounts/{AccountId}/transactions?fromBookingDateTime=...&page=1
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
```

---

## 5. Database Design (Project-Aligned Persistence)

**Primary Read Models:** PostgreSQL  
**Cache:** Redis  
**Analytics:** MongoDB silver copy

**Table: `ais_read_models.accounts_view`**

* **PK:** `account_id`
* **Fields:** `psu_id`, `iban_masked`, `currency`, `product_type`, `status`

**Table: `ais_read_models.balances_view`**

* **PK:** `(account_id, balance_type)`
* **Fields:** `amount`, `currency`, `as_of`

**Table: `ais_read_models.transactions_view`**

* **PK:** `transaction_id`
* **FK:** `account_id`
* **Fields:** `amount`, `currency`, `booking_date`, `merchant_name`, `remittance_info`
* **Indexes:** `(account_id, booking_date DESC)`, `(account_id, value_date DESC)`

**Redis Keys**

* `ais:account:{accountId}:summary`
* `ais:account:{accountId}:balances`

**MongoDB (Optional Analytics)**

* Aggregated behavioral analytics only; no transactional source-of-truth writes.

---

## 6. Postman Collection Structure

* **Folder:** `Resources - Accounts`
* `GET /accounts`
* `GET /accounts/{id}`

* **Folder:** `Resources - Balances`
* `GET /accounts/{id}/balances`

* **Folder:** `Resources - Transactions`
* `GET /accounts/{id}/transactions` (Validate pagination and date filters)

* **Folder:** `Security Assertions`
* Assert required `X-FAPI-Interaction-ID` echo
* Assert `403` on insufficient scope
