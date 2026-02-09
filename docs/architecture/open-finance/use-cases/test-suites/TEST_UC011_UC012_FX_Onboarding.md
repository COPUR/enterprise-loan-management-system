# Test Suite: FX Services & Dynamic Onboarding
**Scope:** UC011 (FX), UC012 (Onboarding)
**Actors:** TPP, LFI (Exchange House)

## 1. Prerequisites
* Valid Client Token.
* Encrypted KYC payload for onboarding.

## 2. Test Cases

### Suite A: FX Quotes
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-FX-001** | Get FX Quote | Pair: AED-USD | `200 OK`, `QuoteId`, `ValidUntil` timestamp | Functional |
| **TC-FX-002** | Execute Quote in Time | Valid `QuoteId` (< 30s) | `200 OK` / `201 Created`, Deal Booked | Functional |
| **TC-FX-003** | Execute Expired Quote | `QuoteId` (> 30s old) | `400 Bad Request`, Error: `Quote Expired` | Negative |
| **TC-FX-004** | Market Closed | Request on Sunday | `503 Service Unavailable` or Indicative Rate Only | Edge Case |

### Suite B: Dynamic Onboarding (UC012)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-ONB-001** | Create Account (eKYC) | Encrypted JWE with EID Data | `201 Created`, New `AccountId` returned | Functional |
| **TC-ONB-002** | Data Decryption Fail | Corrupted JWE Payload | `400 Bad Request`, Error: `Decryption Failed` | Security |
| **TC-ONB-003** | Sanctions Hit (Applicant) | Name: "TEST_BLOCKED" | `403 Forbidden`, Onboarding Rejected | Compliance |
