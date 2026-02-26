# Test Suite: CAAP Integration & Redirection
**Scope:** AlTareq Centralized Authentication & Authorization Provider (CAAP)
**References:** * *OF-AlTareq Consent Mobile App User Guide*
* *OF-AlTareq Centralised Authentication and Authorization Integration Guide*
**Actors:** TPP App, AlTareq App (CAAP), User, API Hub

## 1. Prerequisites
* TPP App installed on a test device (iOS/Android).
* AlTareq App (Sandbox/Production build) installed.
* Valid `client_id` and `redirect_uri` registered in the Trust Framework.
* Biometrics (FaceID/TouchID) enrolled on the device.

## 2. Test Cases

### Suite A: Pushed Authorization Requests (PAR)
*The CAAP flow **requires** the TPP to register the consent intent via PAR before redirection.*

| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-CAAP-001** | Create PAR URI (Happy Path) | `POST /par` with valid `payment-consent` or `account-access-consent` claims | `201 Created`, returns `request_uri` (e.g., `urn:ietf:params:oauth:request_uri:...`) and `expires_in` | Functional |
| **TC-CAAP-002** | Create PAR with Invalid Client | `client_id` does not match Certificate | `401 Unauthorized` | Security |
| **TC-CAAP-003** | Create PAR with Invalid Scopes | Scope: `invalid_scope` | `400 Bad Request`, Error: `invalid_scope` | Functional |
| **TC-CAAP-004** | PAR Expiry Check | Wait > 60s (or configured TTL) after receiving `request_uri` | Subsequent use of URI fails with `invalid_request_uri` | Security |

### Suite B: App-to-App Redirection (Deep Linking)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-CAAP-005** | Universal Link Trigger (App Installed) | TPP opens `https://altareq.app/oauth2/auth?request_uri=...` | OS intercepts and opens **AlTareq App** directly without browser interstitial | Integration |
| **TC-CAAP-006** | Fallback Redirect (App Not Installed) | TPP opens Auth URL on device without AlTareq | Browser opens -> Displays Interstitial Page -> Redirects to App Store | UX |
| **TC-CAAP-007** | Consent Authorization (Biometric) | User clicks "Approve" in AlTareq | App prompts for FaceID/TouchID. On success, redirects to `tpp_redirect_uri` with `code` | Functional |
| **TC-CAAP-008** | User Cancellation | User clicks "Cancel" in AlTareq | Redirects to TPP with `error=access_denied` | Functional |

### Suite C: Security & SCA
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-CAAP-009** | Step-Up for Payments | Initiate High-Value Payment | CAAP prompts for **Step-Up** (e.g., PIN + Biometric) if risk engine triggers | Security |
| **TC-CAAP-010** | PKCE Validation | TPP sends `code_challenge` in PAR | Token exchange fails if `code_verifier` is missing or invalid | Security |
| **TC-CAAP-011** | PII Decryption (EFR) | Onboarding Flow | LFI Backend successfully decrypts the PII payload sent by CAAP using LFI Private Key | Integration |
