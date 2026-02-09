# Test Suite: Audit, Liability & Dispute Resolution
**Scope:** Cross-Cutting (Limitation of Liability Model)
**Context:** This suite ensures that the LFI stores sufficient evidence to resolve disputes as per the *Limitation of Liability Model* document.
**Actors:** Compliance Officer, Auditor, System Admin

## 1. Prerequisites
* Access to the `audit_logs` collection (Read-Only).
* Ability to trigger "Dispute Scenarios" via TPP.

## 2. Test Cases

### Suite A: Non-Repudiation Evidence
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AUD-001** | Verify Payment Signature Storage | Trigger Payment (UC006) | DB entry in `payment_audit` contains `x-jws-signature` and the raw payload | Compliance |
| **TC-AUD-002** | Verify Consent Authorization Log | User Authorizes Consent | Log contains `PSU_ID`, `Consent_ID`, `Timestamp`, and `IP_Address` | Compliance |
| **TC-AUD-003** | Verify TPP Identity Logging | Any API Call | Log captures `Client_ID` (TPP) and the specific `Certificate_Thumbprint` used for mTLS | Security |

### Suite B: Liability Scenarios (Simulation)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AUD-004** | TPP Claims "System Error" (False Claim) | TPP alleges 500 Error | Query Audit Log by `RequestID`. Result shows `400 Bad Request` sent. Proof LFI is NOT liable. | Dispute |
| **TC-AUD-005** | Late Execution Dispute | Payment Executed > 500ms | Log records `IngressTime` and `EgressTime`. If diff > 500ms, LFI admits liability for SLA breach. | Dispute |
| **TC-AUD-006** | Unauthorized Transaction | PSU claims they didn't pay | Retrieve `Consent_Snapshot`. Verify `Permissions` matched the payment. If mismatch, LFI liable. | Dispute |

### Suite C: Data Retention
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-AUD-007** | Archive Retrieval | Query log from 13 months ago | Logs must still be retrievable (Retention Policy check) | Compliance |
