# Test Suite: Payment Initiation Services (PIS)
**Scope:** Payment Initiation (Single/Intl), Corporate Bulk Payments (Bulk)
**Actors:** TPP (PIS), PSU, ASPSP

## 1. Prerequisites
* PSU has sufficient funds.
* Valid `payment-consent` authorized.
* Signing Certificate for Non-Repudiation (JWS).

## 2. Test Cases

### Suite A: Single Payment Execution
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-PIS-001** | Create Payment Consent | Amount: 100 AED, Payee: Valid IBAN | `201 Created`, `ConsentId` returned, Status: `AwaitingAuthorisation` | Functional |
| **TC-PIS-002** | Initiate Payment (Happy Path) | Valid `ConsentId`, `x-idempotency-key` | `201 Created`, Status: `AcceptedSettlementInProcess` | Functional |
| **TC-PIS-003** | Insufficient Funds | Amount: > Balance | `400 Bad Request` or `422 Unprocessable Entity` (Funds Check Fail) | Negative |
| **TC-PIS-004** | Idempotency Check (Replay) | Same Payload, Same Key | `201 Created` (Returns cached response, NO new debit) | Reliability |
| **TC-PIS-005** | Idempotency Conflict | Different Payload, Same Key | `400 Bad Request` or `409 Conflict` | Reliability |
| **TC-PIS-006** | Tampered Payload | Modified Amount vs Signature | `400 Bad Request`, Error: `Signature Invalid` | Security |

### Suite B: International & Future Dated
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-PIS-007** | Future Dated Payment | `RequestedExecutionDate`: T+5 | `201 Created`, Status: `Pending`, Funds NOT debited immediately | Functional |
| **TC-PIS-008** | International (FX) | GBP to USD | `201 Created`, Exchange Rate included in response or separate call | Functional |
| **TC-PIS-009** | Sanctions Hit (Simulation) | Payee Name: "TEST_SANCTION_LIST" | `201 Created` but later status poll shows `Rejected` | Compliance |

### Suite C: Corporate Bulk (Corporate Bulk Payments)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-BULK-001** | Upload Bulk File (Valid) | CSV with 100 valid lines | `202 Accepted`, `FileId` returned | Functional |
| **TC-BULK-002** | Upload Large File (>10MB) | File size 15MB | `413 Payload Too Large` | NFR |
| **TC-BULK-003** | File Integrity Fail | Checksum hash mismatch | `400 Bad Request`, Error: `Integrity Failure` | Security |
| **TC-BULK-004** | Partial Failure Logic | File with 1 invalid IBAN | File Status: `PartiallyAccepted` (if supported) or Report generated | Functional |

## 3. Automated Script Hints
* **Idempotency Test:**

```javascript
// Run request twice
if(pm.collectionVariables.get("run_count") == 1) {
    pm.test("First Call Success", function () { pm.response.to.have.status(201); });
} else {
    pm.test("Second Call Idempotent", function () {
         pm.response.to.have.status(201);
         pm.expect(pm.response.headers.get("x-idempotency-key")).to.eql(originalKey);
    });
}
```
