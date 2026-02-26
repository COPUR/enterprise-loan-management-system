# Use Case 11: FX & Remittance Services

## 1. High-Level Design (HLD)

### Architecture Overview

FX services are latency-sensitive and require deterministic booking behavior under concurrent access.

* **Pattern:** Pub/Sub rate streaming + atomic booking transaction.
* **Cohesion:** `FX_Domain` owns quote generation, expiry validation, and booking.
* **Concurrency:** Optimistic concurrency/version checks prevent stale-quote execution.

### Components

1. **Rate Streamer:** Ingests market feeds and updates in-memory/Redis rate cache.
2. **Quote Service:** Creates short-lived actionable quotes with expiry.
3. **Booking Engine:** Atomically validates quote freshness, reserves funds, and books deal.
4. **Remittance Gateway:** Integrates with SWIFT/local corridor providers.
5. **FX Ledger Store (PostgreSQL):** Persistent record of quotes, deals, and settlement state.
6. **Kafka Publisher:** Emits quote/deal/settlement lifecycle events.

---

## 2. Functional Requirements

1. **Live Rates:** Generate actionable quote valid for N seconds (for example, 30s).
2. **Expiry Enforcement:** Reject execution when `now > validUntil`.
3. **Settlement Support:** Immediate or deferred settlement based on corridor.
4. **Idempotent Booking:** Repeated execution calls with same idempotency key must not create duplicate deals.

## 3. Service Level Implementation (NFRs)

* **Quote Latency:** P95 < 100ms.
* **Rate Precision:** Up to 6 decimal places.
* **Availability:** 24/7 with controlled fallback rates outside market hours.
* **Security:** OAuth 2.1 + FAPI 2.0 + DPoP + mTLS.
* **Auditability:** Full quote-to-deal trail with interaction IDs.

---

## 4. API Signatures

### Get FX Quote

```http
POST /open-finance/v1/fx-quotes
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
Content-Type: application/json
```

**Payload:**

```json
{
  "sourceCurrency": "AED",
  "targetCurrency": "USD",
  "sourceAmount": 1000.00
}
```

**Response:**

```json
{
  "quoteId": "Q_999",
  "exchangeRate": 0.272290,
  "validUntil": "2026-02-09T12:00:30Z"
}
```

### Execute FX Deal

```http
POST /open-finance/v1/fx-deals
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <UUID>
X-Idempotency-Key: <UUID>
```

**Payload:**

```json
{ "quoteId": "Q_999" }
```

---

## 5. Database Design (Project-Aligned Persistence)

**System of Record:** PostgreSQL  
**Live Rate/Quote Cache:** Redis  
**Analytics:** MongoDB silver copy

**Table: `fx.fx_rates_snapshot`**

* **PK:** `pair_id`
* **Fields:** `rate`, `timestamp`, `spread`, `source`
* **Purpose:** Auditable persisted snapshots (cache remains primary for low-latency reads).

**Table: `fx.fx_quotes`**

* **PK:** `quote_id`
* **Fields:** `pair_id`, `quoted_rate`, `source_amount`, `target_amount`, `valid_until`, `version`, `status`

**Table: `fx.fx_deals`**

* **PK:** `deal_id`
* **FK:** `quote_id`
* **Fields:** `psu_id`, `booked_rate`, `buy_amount`, `sell_amount`, `settlement_status`, `booked_at`

**Table: `fx.idempotency_keys`**

* **PK:** `idempotency_key`
* **Fields:** `payload_hash`, `response_body`, `expires_at`

---

## 6. Postman Collection Structure

* **Folder:** `Treasury - FX`
* `POST /fx-quotes`
* `POST /fx-deals` (execute within quote validity window)
* `POST /fx-deals` (retry with same key and assert no duplicate booking)
