# Test Suite: Confirmation of Payee (CoP)
**Scope:** Confirmation of Payee
**Actors:** TPP, ASPSP

## 1. Prerequisites
* Client Credentials Token (No User Interaction required).
* Test Data with known Account Names.

## 2. Test Cases

### Suite A: Matching Logic
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-COP-001** | Exact Match | IBAN: Valid, Name: "Al Tareq LLC" | `200 OK`, Result: `Match` | Functional |
| **TC-COP-002** | Close Match (Typos) | IBAN: Valid, Name: "Al Tariq LLC" | `200 OK`, Result: `CloseMatch`, NameReturned: "Al Tareq LLC" | Functional |
| **TC-COP-003** | No Match | IBAN: Valid, Name: "Random Corp" | `200 OK`, Result: `NoMatch`, NameReturned: `null` | Functional |
| **TC-COP-004** | Account Closed/Deceased | IBAN: Closed Account | `200 OK` (or 422), AccountStatus: `Deceased` or `Closed` | Functional |

### Suite B: Performance & Security
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-COP-005** | Latency Check | Valid Request | Response Time < 300ms | NFR |
| **TC-COP-006** | Account Enumeration Protection | Burst 50 requests for sequential IBANs | `429 Too Many Requests` (Rate Limit Triggered) | Security |
| **TC-COP-007** | Invalid IBAN Checksum | IBAN: Invalid Check Digits | `400 Bad Request`, Error: `Invalid IBAN` (Fail fast before DB lookup) | Functional |
