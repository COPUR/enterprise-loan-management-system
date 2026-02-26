# Test Suite: Common Security & NFRs
**Scope:** All Use Cases
**Actors:** TPP, Security Team, Load Test Scripts

## 1. Prerequisites
* Specialized Security Testing Tools (OWASP ZAP, Burp Suite).
* Load Testing Tool (JMeter/K6).

## 2. Test Cases

### Suite A: Security Guardrails
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-SEC-001** | Invalid Bearer Token | Random String | `401 Unauthorized` | Security |
| **TC-SEC-002** | SQL Injection | `?id=1' OR '1'='1` | `400 Bad Request` (Input Validation) - NO DB Error leaked | Security |
| **TC-SEC-003** | TLS Version Check | Force TLS 1.0/1.1 | Connection Refused (Only TLS 1.2+ allowed) | Security |
| **TC-SEC-004** | Missing mTLS Cert | Request without Cert | `403 Forbidden` (Mutual Auth Fail) | Security |

### Suite B: Performance & Resilience
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-NFR-001** | Load Test (Normal) | 50 TPS for 10 mins | 95th Percentile Latency < 500ms | Performance |
| **TC-NFR-002** | Stress Test (Peak) | 200 TPS (Burst) | System handles load without crashing (Latency may increase) | Resilience |
| **TC-NFR-003** | Rate Limiting | Exceed TPP Quota | `429 Too Many Requests` | Reliability |
