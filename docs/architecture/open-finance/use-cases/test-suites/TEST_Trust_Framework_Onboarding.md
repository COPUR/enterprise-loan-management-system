# Test Suite: Trust Framework & Dynamic Registration
**Scope:** Ecosystem Security, mTLS, Certificates
**References:** *Trust Framework User Documentation*
**Actors:** TPP System, Directory Service

## 1. Prerequisites
* QWAC (Qualified Web Authentication Certificate) or equivalent Transport Cert.
* Software Statement Assertion (SSA) from the Directory.

## 2. Test Cases

### Suite A: Dynamic Client Registration (DCR)
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-DCR-001** | Register New TPP | `POST /register` with valid SSA | `201 Created`, returns `client_id` and `client_secret` (if applicable) | Functional |
| **TC-DCR-002** | Register with Invalid SSA | Tampered/Expired SSA | `400 Bad Request`, Error: `invalid_software_statement` | Security |
| **TC-DCR-003** | Update Client Metadata | `PUT /register/{clientId}` | `200 OK`, Redirect URIs updated | Functional |
| **TC-DCR-004** | Delete Client | `DELETE /register/{clientId}` | `204 No Content` | Functional |

### Suite B: Certificate Management
| ID | Test Case Description | Input Data | Expected Result | Type |
|----|-----------------------|------------|-----------------|------|
| **TC-PKI-001** | Connect with Valid mTLS | Request with registered Cert | Connection Established, API response `200` | Security |
| **TC-PKI-002** | Connect with Revoked Cert | Request with Revoked Cert | Connection Rejected (TLS Handshake Failure) | Security |
| **TC-PKI-003** | Rotate Keys (JWKS) | Update JWKS URI in Directory | API Hub fetches new keys within cache TTL (e.g., 15 mins) | Operational |
