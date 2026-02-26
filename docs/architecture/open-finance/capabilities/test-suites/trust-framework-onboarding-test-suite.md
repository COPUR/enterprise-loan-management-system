# Test Suite: Trust Framework & Dynamic Registration
**Scope:** Ecosystem Security, mTLS, Certificates, Dynamic Client Registration (DCR)
**References:** *Trust Framework User Documentation*
**Actors:** TPP System, Directory Service, Certificate Authority (CA)

## 1. Prerequisites
* Valid Software Statement Assertion (SSA) generated from the Directory.
* Transport Certificate (QWAC or equivalent) and Signing Certificate.

## 2. Test Cases

### Suite A: Dynamic Client Registration (DCR)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-DCR-001** | Register New TPP (Happy Path) | `POST /register` with valid JWT (SSA) signed by Directory | `201 Created`, returns `client_id`, `client_secret` (if applicable), and `client_secret_expires_at` | Functional |
| **TC-DCR-002** | Register with Tampered SSA | SSA signature modified | `400 Bad Request`, Error: `invalid_software_statement` | Security |
| **TC-DCR-003** | Register with Expired SSA | SSA `exp` claim is in the past | `400 Bad Request`, Error: `software_statement_expired` | Security |
| **TC-DCR-004** | Update Client Metadata | `PUT /register/{clientId}` with new `redirect_uris` | `200 OK`, response reflects new URIs. | Functional |
| **TC-DCR-005** | Rotate Client Secret | `POST /register/{clientId}/rotate_secret` | `200 OK`, New Secret returned. Old secret invalid immediately. | Security |

### Suite B: Certificate & Key Management
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-PKI-001** | mTLS Enforcement | Call API without Cert | `403 Forbidden` or Connection Reset (Handshake Failure) | Security |
| **TC-PKI-002** | Revoked Certificate Check | Call API with Revoked Cert (CRL/OCSP) | Connection Rejected (TLS Handshake Failure) | Security |
| **TC-PKI-003** | JWKS Rotation | Update `jwks_uri` in Directory | API Hub fetches new keys within cache TTL (e.g., 15 mins). Signatures with old key fail. | Operational |
| **TC-PKI-004** | CA Validation | Use Cert signed by unknown CA | `403 Forbidden` | Security |
