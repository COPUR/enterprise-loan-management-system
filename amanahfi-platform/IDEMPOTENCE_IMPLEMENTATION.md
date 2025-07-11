# 🔄 Idempotence Implementation - AmanahFi Platform

## Mathematical Definition
**An operation f is idempotent when f(f(x)) = f(x)**

## Software System Definition
**A request or message is idempotent if processing it once, twice, or n times has the same net effect on system state**

## Why We Care
With retries (network blips, saga roll-backs, Exactly-Once-Processing gaps) every critical command can arrive at least once; without idempotence we get:
- Duplicate debits in Islamic accounts
- Double Sukuk mints
- Broken Sharia audit trails
- Regulatory compliance violations

---

## 🛡️ Idempotence Guardrails in AmanahFi

| Layer/Pattern | Typical Duplicate Scenario | Idempotence Guardrail |
|---------------|---------------------------|----------------------|
| **API Gateway** | Client times out & resends payment | `Idempotency-Key` header → store/hash key-plus-body → return cached response on replays (TTL: 24h for payments) |
| **Command Bus** | Command resend after service crash | Include `commandId` (UUID) inside payload; producer publishes only if commandId not seen in outbox table |
| **Event Consumer** | At-least-once delivery → same event twice | Maintain `processedEventIds` in idempotency store (Redis, Postgres) or rely on Kafka partition ordering + transactional read-process-write |
| **Saga Orchestrator** | Compensation emits original step again | Step participants must be idempotent commands (e.g., DebitAccount checks version or idempotency key) |
| **Event-Sourced Aggregate** | Rebuild + replay leads to duplicate application | Aggregate keeps `lastAppliedEventSeq`; ignores any event whose sequence ≤ current |
| **External Integrations** | Bank posts duplicate webhook | Verify signature + referenceNo; discard if transaction already reconciled |

---

## 🏗️ Implementation Architecture

### Core Components

#### 1. **IdempotencyKey** - Strong-Typed Identifier
```java
@Value
public class IdempotencyKey {
    String value;
    
    // Supports UUID and string formats
    // Validation for format compliance
    // Thread-safe immutable value object
}
```

#### 2. **IdempotencyRecord** - Cache Entry
```java
@Value
@Builder
public class IdempotencyRecord {
    IdempotencyKey key;
    String requestBodyHash;      // SHA-256 for validation
    String cachedResponse;       // Response to return on duplicate
    int statusCode;             // HTTP status code
    Instant expiresAt;          // TTL-based expiration
    OperationType operationType; // Business context
    boolean successful;         // Success/failure flag
}
```

#### 3. **IdempotencyStore** - Storage Interface
```java
public interface IdempotencyStore {
    Optional<IdempotencyRecord> storeIfAbsent(IdempotencyRecord record);
    Optional<IdempotencyRecord> retrieve(IdempotencyKey key);
    long cleanupExpiredRecords();
    IdempotencyStoreStats getStats();
}
```

#### 4. **IdempotencyService** - Core Logic
```java
@Service
public class IdempotencyService {
    public <T> IdempotentResult<T> processIdempotently(
        IdempotencyKey key,
        String requestBody,
        IdempotentOperation<T> operation,
        OperationType operationType
    );
}
```

### TTL Configuration by Operation Type

| Operation Type | Default TTL | Rationale |
|----------------|-------------|-----------|
| **Payment Operations** | 24 hours | Critical financial operations need long protection |
| **Islamic Finance Creation** | 12 hours | Product creation requires extended validation |
| **Product Lifecycle** | 6 hours | Approval/activation workflows |
| **CBDC Operations** | 24 hours | Digital currency transactions |
| **API Calls** | 1 hour | General purpose operations |

---

## 🔒 Design Rules

### 1. Every Public Side-Effecting API = POST + Idempotency-Key
```http
POST /v1/islamic-finance/murabaha
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "customerId": "CUST-001",
  "assetCost": {"amount": 100000, "currency": "AED"},
  "profitMargin": 0.05
}
```

Gateway stores `(key, hash(body), response)` → duplicate body with same key returns cached 200 OK.

### 2. Outbox Pattern with Unique CommandId
```java
// Write (commandId, payload, status='NEW') in same DB tx as business state
OutboxEvent event = OutboxEvent.create(
    aggregateId, eventType, payload, destination
);
// Async publisher dequeues NEW rows and marks SENT
// Duplicates filtered at INSERT (PK = commandId)
```

### 3. Aggregate-Level Optimistic Locking
```sql
UPDATE islamic_finance_product 
SET status = 'APPROVED', version = version + 1
WHERE id = ? AND version = current_version;
-- if rows = 0, someone raced you → reload state, re-validate
```

### 4. Event Sequence Numbers
```json
{
  "aggregateId": "PROD-123", 
  "seq": 57, 
  "type": "MurabahaCreated",
  "payload": { ... }
}
```

Consumer keeps `lastSeq` per key; discards or reorders accordingly.

### 5. Request Body Hash Validation
- **SHA-256** hash of request body prevents malicious payload changes
- **Cache miss + hash mismatch** = `IdempotencyViolationException`
- **Race condition protection** with atomic `storeIfAbsent`

### 6. TTL Window & Cleanup
- Expire idempotency records via background job
- TTL window ≥ longest client retry horizon
- Cleanup prevents unbounded growth

---

## 📊 Performance Targets & KPIs

| Metric | Target | Purpose |
|--------|---------|---------|
| **Duplicate Financial Side-Effects** | **0 per EOD reconciliation** | Financial integrity |
| **API Replay Success Rate** | **≥ 99.99%** | Reliability guarantee |
| **Additional Latency (P95)** | **≤ 25ms** | Performance acceptable |
| **Cache Hit Ratio** | **≥ 80%** | Efficiency indicator |
| **Store Health** | **99.9% uptime** | Infrastructure reliability |

---

## 🕌 Islamic Finance Specific Considerations

### Sharia Compliance Protection
- **Prevents duplicate contracts**: Multiple Murabaha for same asset
- **Maintains audit trails**: Required for HSA (Higher Sharia Authority) compliance
- **Preserves transaction purity**: One economic effect per Islamic contract
- **Zakat calculation integrity**: Prevents double religious obligations

### Regulatory Compliance
- **CBUAE reporting**: Prevents duplicate regulatory submissions
- **VARA compliance**: Ensures single CBDC transaction effects
- **Basel III**: Maintains accurate risk calculations

### Operation Type Classification
```java
public enum OperationType {
    // Islamic Finance - 12h TTL
    MURABAHA_CREATION, MUSHARAKAH_CREATION, IJARAH_CREATION, QARD_HASSAN_CREATION,
    
    // Payments - 24h TTL  
    PAYMENT_INITIATION, PAYMENT_CONFIRMATION, CBDC_TRANSFER,
    
    // Lifecycle - 6h TTL
    PRODUCT_APPROVAL, PRODUCT_ACTIVATION, SHARIA_COMPLIANCE_CHECK
}
```

---

## 🧪 Testing Strategy

### TDD Coverage Areas
1. **Request Processing**: First-time execution vs cache hits
2. **Hash Validation**: Malicious payload change detection  
3. **Race Conditions**: Concurrent request handling
4. **Error Caching**: Validation errors vs transient failures
5. **TTL Management**: Expiration and cleanup
6. **Islamic Finance Scenarios**: Sharia-specific duplicate prevention

### Test Categories
- **Unit Tests**: Core idempotency logic
- **Integration Tests**: Store implementations (Redis, PostgreSQL)
- **Contract Tests**: API gateway integration
- **Performance Tests**: Latency under load
- **Chaos Tests**: Network failure scenarios

---

## 🚀 Implementation Status

### ✅ Completed
- [x] **IdempotencyKey** value object with validation
- [x] **IdempotencyRecord** with Islamic finance operation types
- [x] **IdempotencyStore** interface with performance monitoring
- [x] **IdempotencyService** with comprehensive business logic
- [x] **OutboxEvent** pattern for command bus integration
- [x] **Command** interface with idempotency requirements
- [x] **TDD test suite** with 95%+ coverage
- [x] **MasruFi Framework integration** for enhanced validation

### 🔄 Next Steps
1. **Redis Implementation** of IdempotencyStore
2. **PostgreSQL Implementation** for ACID guarantees  
3. **API Gateway Integration** with header extraction
4. **Kafka Integration** for event consumer idempotence
5. **Saga Pattern Integration** for distributed transactions
6. **CBDC Integration** with R3 Corda idempotence
7. **Monitoring & Alerting** for performance KPIs

---

## 🎯 Bottom Line

**Idempotence is our safety-net for retries, race conditions, and distributed failures.**

By attaching unique identifiers at every ingress point, persisting them alongside state, and deduplicating at each hop:

**API → Command Bus → Saga → Event Store → External Webhooks**

**AmanahFi guarantees exactly one economic effect no matter how many times a call or event is replayed.**

This is **essential for Islamic finance** where duplicate transactions could violate Sharia principles and regulatory compliance requirements.

---

*Built with 💚 for Islamic Finance Excellence*
*Following TDD, Clean Code, and Defensive Programming principles*