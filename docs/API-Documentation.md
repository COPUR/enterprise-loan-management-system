# API Documentation
## Enterprise Banking System - RESTful APIs with FAPI 2.0 + DPoP Security

### Table of Contents
1. [API Overview](#api-overview)
2. [FAPI 2.0 + DPoP Authentication](#fapi-20--dpop-authentication)
3. [Security Requirements](#security-requirements)
4. [Core Banking APIs](#core-banking-apis)
5. [Party Management APIs](#party-management-apis)
6. [OAuth2.1 Integration APIs](#oauth21-integration-apis)
7. [Security APIs](#security-apis)
8. [Error Handling](#error-handling)
9. [Rate Limiting](#rate-limiting)
10. [Migration Guide](#migration-guide)

---

## API Overview

The Enterprise Banking System provides a comprehensive set of RESTful APIs designed for secure banking operations with **FAPI 2.0 Security Profile** and **DPoP (Demonstrating Proof-of-Possession)** authentication. All APIs follow OpenAPI 3.0 specification and implement financial-grade security controls.

### Base URL
```
Production: https://api.banking.enterprise.com/v1
Staging: https://api-staging.banking.enterprise.com/v1
Development: http://localhost:8080/api/v1
```

### API Design Principles

1. **RESTful Design**: Resource-based URLs with standard HTTP methods
2. **FAPI 2.0 Security**: Financial-grade API security profile implementation
3. **DPoP Authentication**: Demonstrating Proof-of-Possession for enhanced security
4. **PAR Required**: Pushed Authorization Requests mandatory for all flows
5. **PKCE Required**: Proof Key for Code Exchange for all authorization requests
6. **Idempotency**: POST/PUT operations support idempotency keys
7. **Pagination**: Cursor-based pagination for list operations
8. **Versioning**: URL-based versioning (v1, v2, etc.)

### Security Architecture

- **Authorization Code Flow Only**: Hybrid and implicit flows are not supported
- **DPoP-bound Tokens**: All access tokens are bound to DPoP keys
- **Private Key JWT**: Client authentication via private_key_jwt only
- **Back-channel Delivery**: No front-channel token delivery
- **JTI Replay Prevention**: Unique JWT identifiers prevent replay attacks

### Standard Headers

#### Request Headers
```http
Authorization: DPoP {access_token}
DPoP: {dpop_proof_jwt}
Content-Type: application/json
Accept: application/json
X-Idempotency-Key: {uuid} (for POST/PUT operations)
X-Request-ID: {uuid}
X-FAPI-Interaction-ID: {uuid}
X-FAPI-Auth-Date: {rfc7231_date}
X-FAPI-Customer-IP-Address: {client_ip}
```

#### Response Headers
```http
Content-Type: application/json
X-Request-ID: {uuid}
X-FAPI-Interaction-ID: {uuid}
X-RateLimit-Remaining: {count}
X-RateLimit-Reset: {unix_timestamp}
DPoP-Nonce: {nonce} (when required)
WWW-Authenticate: DPoP (on authentication errors)
```

---

## FAPI 2.0 + DPoP Authentication

### Overview

The Enterprise Banking System implements **FAPI 2.0 Security Profile** with **DPoP (Demonstrating Proof-of-Possession)** as mandated by financial regulations for enhanced security.

### Key Changes from OAuth 2.0

| Feature | FAPI 1.0 | FAPI 2.0 + DPoP |
|---------|----------|------------------|
| **Authorization Flows** | Authorization Code + Hybrid | Authorization Code Only |
| **Token Binding** | mTLS Certificate Binding | DPoP Key Binding |
| **Client Authentication** | mTLS + private_key_jwt | private_key_jwt Only |
| **Authorization Requests** | Direct + PAR | PAR Only |
| **Response Modes** | query, fragment, form_post | query Only |
| **PKCE** | Recommended | Required |
| **Token Type** | Bearer | DPoP |

### DPoP Authentication Flow

#### Step 1: Generate DPoP Key Pair

```javascript
// Generate EC P-256 key pair for DPoP
const keyPair = await crypto.subtle.generateKey(
  {
    name: "ECDSA",
    namedCurve: "P-256"
  },
  true,
  ["sign", "verify"]
);

// Calculate JKT (JWK Thumbprint)
const jwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
const jkt = await calculateJktThumbprint(jwk);
```

#### Step 2: Pushed Authorization Request (PAR)

```http
POST /oauth2/par
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {client_assertion_jwt}

client_id=banking-app
&redirect_uri=https://app.banking.com/callback
&response_type=code
&scope=openid banking-loans banking-payments
&code_challenge=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
&code_challenge_method=S256
&dpop_jkt={jkt_thumbprint}
&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion={private_key_jwt}
```

**PAR Response:**
```json
{
  "request_uri": "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
  "expires_in": 300
}
```

#### Step 3: Authorization Request

```http
GET /oauth2/authorize?client_id=banking-app&request_uri=urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c
```

#### Step 4: Token Exchange with DPoP

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
DPoP: {dpop_proof_jwt}

grant_type=authorization_code
&code={authorization_code}
&redirect_uri=https://app.banking.com/callback
&code_verifier={pkce_verifier}
&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion={private_key_jwt}
```

**DPoP Proof Structure:**
```json
{
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": {
    "kty": "EC",
    "crv": "P-256",
    "x": "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
    "y": "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"
  }
}
.
{
  "jti": "e1j3V_bX5C4NTcU1g74sBHxF",
  "htm": "POST",
  "htu": "https://api.banking.com/oauth2/token",
  "iat": 1640995200
}
```

**Token Response:**
```json
{
  "access_token": "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "DPoP",
  "expires_in": 300,
  "refresh_token": "def502002b0f5d...",
  "scope": "openid banking-loans banking-payments"
}
```

#### Step 5: API Calls with DPoP

```http
GET /api/v1/loans
Authorization: DPoP eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIi...
X-FAPI-Interaction-ID: 12345678-1234-1234-1234-123456789012
X-FAPI-Auth-Date: Tue, 15 Jan 2024 10:30:00 GMT
X-FAPI-Customer-IP-Address: 192.168.1.100
```

**DPoP Proof for API Call:**
```json
{
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": { /* same DPoP key */ }
}
.
{
  "jti": "f2j4W_cY6D5OUdV2h85tCIyG",
  "htm": "GET",
  "htu": "https://api.banking.com/api/v1/loans",
  "iat": 1640995260,
  "ath": "fUHyO2r2Z3DZ53EsNrWBb3GGh5O6mGVlN1A4B3E2F7A"
}
```

### Authentication Error Handling

#### Missing DPoP Proof
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: DPoP
Content-Type: application/json

{
  "error": "invalid_dpop_proof",
  "error_description": "DPoP proof is required but missing"
}
```

#### Invalid DPoP Proof
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: DPoP error="invalid_dpop_proof", error_description="DPoP proof signature verification failed"
Content-Type: application/json

{
  "error": "invalid_dpop_proof", 
  "error_description": "DPoP proof signature verification failed"
}
```

#### DPoP Nonce Required
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: DPoP
DPoP-Nonce: 8c5a3b2d-4e6f-1a2b-3c4d-567890abcdef
Content-Type: application/json

{
  "error": "use_dpop_nonce",
  "error_description": "DPoP nonce is required for this client"
}
```

---

## Security Requirements

### Mandatory Security Features

#### 1. FAPI Headers (Required)
All API requests must include these headers:

```http
X-FAPI-Interaction-ID: {uuid_v4}
X-FAPI-Auth-Date: {rfc7231_date}
X-FAPI-Customer-IP-Address: {client_ip_address}
```

#### 2. DPoP Proof Requirements
- **Algorithm**: ES256, ES384, ES512, RS256, RS384, RS512, PS256, PS384, PS512
- **Key Type**: EC (P-256, P-384, P-521) or RSA (2048+ bits)
- **JTI**: Unique identifier preventing replay attacks
- **HTM**: HTTP method (GET, POST, PUT, DELETE)
- **HTU**: HTTP URI (scheme, host, port, path - no query/fragment)
- **ATH**: Access token hash (SHA-256, base64url encoded)

#### 3. Client Authentication
Only `private_key_jwt` is supported:

```json
{
  "iss": "banking-app",
  "sub": "banking-app", 
  "aud": "https://api.banking.com/oauth2/token",
  "jti": "12345678-1234-1234-1234-123456789012",
  "iat": 1640995200,
  "exp": 1640995500
}
```

#### 4. PKCE Requirements
- **Code Challenge Method**: S256 only
- **Code Verifier**: 43-128 characters, base64url-encoded
- **Code Challenge**: SHA256 hash of verifier, base64url-encoded

### Removed/Unsupported Features

#### ❌ Not Supported in FAPI 2.0
- **Hybrid Flows**: `response_type=code id_token`, `code token`, `code id_token token`
- **Implicit Flows**: `response_type=token`, `id_token`
- **Front-channel Delivery**: `response_mode=fragment`, `form_post`
- **mTLS Authentication**: `tls_client_auth`, `self_signed_tls_client_auth`
- **Bearer Tokens**: Only DPoP-bound tokens supported
- **Direct Authorization**: PAR is mandatory

### OAuth2.1 Integration

The API uses OAuth2.1 Authorization Code Flow with PKCE and DPoP for authentication:

#### Token Endpoint
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code={authorization_code}
&redirect_uri={redirect_uri}
&client_id={client_id}
&code_verifier={code_verifier}
```

#### Token Response
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 300,
  "refresh_token": "def502002b0f5d...",
  "scope": "banking:read banking:write"
}
```

### Authorization Scopes

| Scope | Description | Example APIs |
|-------|-------------|--------------|
| `banking:read` | Read banking data | GET /customers, GET /loans |
| `banking:write` | Modify banking data | POST /loans, PUT /customers |
| `banking:admin` | Administrative operations | POST /users, DELETE /roles |
| `audit:read` | Access audit logs | GET /audit-logs |
| `compliance:manage` | Compliance operations | POST /compliance-reports |

### Role-Based Access Control

Authorization is enforced at multiple levels:

1. **OAuth2.1 Scopes**: Coarse-grained permissions
2. **LDAP Groups**: Organizational hierarchy
3. **Party Roles**: Fine-grained banking permissions
4. **Monetary Limits**: Transaction amount restrictions

---

## Core Banking APIs

### Customer Management

#### Create Customer
```http
POST /api/v1/customers
Authorization: Bearer {token}
Content-Type: application/json

{
  "personalInfo": {
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-01-15",
    "email": "john.smith@example.com",
    "phone": "+1-555-0123"
  },
  "address": {
    "street": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "identification": {
    "type": "SSN",
    "number": "123-45-6789",
    "issuingCountry": "US"
  }
}
```

**Response (201 Created):**
```json
{
  "customerId": "cust_12345",
  "personalInfo": {
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-01-15",
    "email": "john.smith@example.com",
    "phone": "+1-555-0123"
  },
  "address": {
    "street": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  },
  "status": "ACTIVE",
  "creditLimit": {
    "amount": 50000.00,
    "currency": "USD",
    "lastUpdated": "2024-01-15T10:30:00Z"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "_links": {
    "self": "/api/v1/customers/cust_12345",
    "loans": "/api/v1/customers/cust_12345/loans",
    "payments": "/api/v1/customers/cust_12345/payments"
  }
}
```

#### Get Customer
```http
GET /api/v1/customers/{customerId}
Authorization: Bearer {token}
```

**Response (200 OK):** Same as create response

#### Update Customer
```http
PUT /api/v1/customers/{customerId}
Authorization: Bearer {token}
Content-Type: application/json
X-Idempotency-Key: {uuid}

{
  "personalInfo": {
    "email": "john.smith.new@example.com",
    "phone": "+1-555-0124"
  }
}
```

#### List Customers
```http
GET /api/v1/customers?limit=50&cursor={next_cursor}&status=ACTIVE
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "customers": [
    {
      "customerId": "cust_12345",
      "personalInfo": { /* ... */ },
      "status": "ACTIVE",
      "_links": {
        "self": "/api/v1/customers/cust_12345"
      }
    }
  ],
  "pagination": {
    "hasNext": true,
    "nextCursor": "cursor_abc123",
    "limit": 50,
    "total": 1250
  },
  "_links": {
    "self": "/api/v1/customers?limit=50",
    "next": "/api/v1/customers?limit=50&cursor=cursor_abc123"
  }
}
```

### Loan Management

#### Create Loan
```http
POST /api/v1/loans
Authorization: Bearer {token}
Content-Type: application/json
X-Idempotency-Key: {uuid}

{
  "customerId": "cust_12345",
  "amount": 100000.00,
  "currency": "USD",
  "purpose": "Home Purchase",
  "installmentCount": 360,
  "interestRate": 3.5,
  "startDate": "2024-02-01",
  "loanType": "MORTGAGE",
  "collateral": {
    "type": "REAL_ESTATE",
    "value": 350000.00,
    "description": "Single family home at 123 Oak Street"
  }
}
```

**Response (201 Created):**
```json
{
  "loanId": "loan_67890",
  "customerId": "cust_12345",
  "amount": 100000.00,
  "currency": "USD",
  "purpose": "Home Purchase",
  "installmentCount": 360,
  "interestRate": 3.5,
  "startDate": "2024-02-01",
  "maturityDate": "2054-01-01",
  "loanType": "MORTGAGE",
  "status": "PENDING_APPROVAL",
  "monthlyPayment": 449.04,
  "totalInterest": 61654.40,
  "totalAmount": 161654.40,
  "collateral": {
    "type": "REAL_ESTATE",
    "value": 350000.00,
    "description": "Single family home at 123 Oak Street"
  },
  "approvalWorkflow": {
    "currentStep": "CREDIT_ASSESSMENT",
    "requiredApprovals": [
      {
        "role": "LOAN_OFFICER",
        "minimumAuthorityLevel": 7,
        "status": "PENDING"
      },
      {
        "role": "LOAN_MANAGER", 
        "minimumAuthorityLevel": 8,
        "status": "PENDING"
      }
    ]
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "_links": {
    "self": "/api/v1/loans/loan_67890",
    "customer": "/api/v1/customers/cust_12345",
    "installments": "/api/v1/loans/loan_67890/installments",
    "approve": "/api/v1/loans/loan_67890/approve",
    "reject": "/api/v1/loans/loan_67890/reject"
  }
}
```

#### Approve Loan
```http
POST /api/v1/loans/{loanId}/approve
Authorization: Bearer {token}
Content-Type: application/json
X-Idempotency-Key: {uuid}

{
  "approvalNotes": "Credit assessment completed. Customer meets all criteria.",
  "conditions": [
    "Provide proof of insurance",
    "Submit final employment verification"
  ]
}
```

**Response (200 OK):**
```json
{
  "loanId": "loan_67890",
  "status": "APPROVED",
  "approvedBy": "john.smith@banking.local",
  "approvedAt": "2024-01-15T14:30:00Z",
  "approvalNotes": "Credit assessment completed. Customer meets all criteria.",
  "conditions": [
    "Provide proof of insurance",
    "Submit final employment verification"
  ],
  "nextSteps": [
    "Customer to fulfill conditions",
    "Schedule loan disbursement"
  ]
}
```

#### Get Loan Installments
```http
GET /api/v1/loans/{loanId}/installments?status=PENDING&limit=12
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "loanId": "loan_67890",
  "installments": [
    {
      "installmentNumber": 1,
      "dueDate": "2024-03-01",
      "principalAmount": 165.71,
      "interestAmount": 283.33,
      "totalAmount": 449.04,
      "status": "PENDING",
      "remainingBalance": 99834.29
    },
    {
      "installmentNumber": 2,
      "dueDate": "2024-04-01",
      "principalAmount": 166.19,
      "interestAmount": 282.85,
      "totalAmount": 449.04,
      "status": "PENDING",
      "remainingBalance": 99668.10
    }
  ],
  "summary": {
    "totalInstallments": 360,
    "paidInstallments": 0,
    "pendingInstallments": 360,
    "nextDueDate": "2024-03-01",
    "monthlyPayment": 449.04
  }
}
```

### Payment Processing

#### Process Payment
```http
POST /api/v1/payments
Authorization: Bearer {token}
Content-Type: application/json
X-Idempotency-Key: {uuid}

{
  "loanId": "loan_67890",
  "amount": 449.04,
  "currency": "USD",
  "paymentMethod": {
    "type": "BANK_TRANSFER",
    "accountNumber": "****1234",
    "routingNumber": "021000021"
  },
  "paymentDate": "2024-03-01",
  "notes": "Regular monthly payment"
}
```

**Response (201 Created):**
```json
{
  "paymentId": "pay_54321",
  "loanId": "loan_67890",
  "amount": 449.04,
  "currency": "USD",
  "paymentDate": "2024-03-01",
  "status": "COMPLETED",
  "transactionId": "txn_987654321",
  "allocation": {
    "principalAmount": 165.71,
    "interestAmount": 283.33,
    "penaltyAmount": 0.00,
    "feeAmount": 0.00
  },
  "remainingBalance": 99834.29,
  "nextDueDate": "2024-04-01",
  "paymentMethod": {
    "type": "BANK_TRANSFER",
    "accountNumber": "****1234"
  },
  "processedAt": "2024-03-01T10:15:00Z",
  "_links": {
    "self": "/api/v1/payments/pay_54321",
    "loan": "/api/v1/loans/loan_67890",
    "receipt": "/api/v1/payments/pay_54321/receipt"
  }
}
```

---

## Party Management APIs

### User Role Management

#### Get User Roles
```http
GET /api/v1/party/users/{userId}/roles
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "userId": "john.smith@banking.local",
  "partyId": "party_12345",
  "roles": [
    {
      "roleId": "role_67890",
      "roleName": "LOAN_OFFICER",
      "roleSource": "PARTY_MANAGEMENT",
      "active": true,
      "effectiveFrom": "2024-01-01T00:00:00Z",
      "effectiveTo": "2024-12-31T23:59:59Z",
      "authorityLevel": 7,
      "monetaryLimit": 500000.00,
      "businessUnit": "Commercial Loans",
      "assignedBy": "admin.user@banking.local",
      "assignedAt": "2024-01-01T09:00:00Z"
    }
  ],
  "groups": [
    {
      "groupName": "loan-officers",
      "groupType": "FUNCTIONAL",
      "groupRole": "MEMBER",
      "active": true,
      "effectiveFrom": "2024-01-01T00:00:00Z"
    }
  ],
  "effectivePermissions": [
    "LOAN_VIEW",
    "LOAN_CREATE",
    "LOAN_APPROVE_500K",
    "CUSTOMER_VIEW",
    "CUSTOMER_CREATE"
  ]
}
```

#### Assign Role
```http
POST /api/v1/party/users/{userId}/roles
Authorization: Bearer {token}
Content-Type: application/json
X-Idempotency-Key: {uuid}

{
  "roleName": "LOAN_MANAGER",
  "effectiveFrom": "2024-02-01T00:00:00Z",
  "effectiveTo": "2024-12-31T23:59:59Z",
  "authorityLevel": 8,
  "monetaryLimit": 2000000.00,
  "businessUnit": "Commercial Loans",
  "justification": "Promotion to senior role"
}
```

**Response (201 Created):**
```json
{
  "roleAssignmentId": "assignment_98765",
  "userId": "john.smith@banking.local",
  "roleName": "LOAN_MANAGER",
  "status": "PENDING_APPROVAL",
  "effectiveFrom": "2024-02-01T00:00:00Z",
  "effectiveTo": "2024-12-31T23:59:59Z",
  "authorityLevel": 8,
  "monetaryLimit": 2000000.00,
  "businessUnit": "Commercial Loans",
  "justification": "Promotion to senior role",
  "approvalWorkflow": {
    "requiredApprovals": [
      {
        "approverRole": "BANKING_ADMIN",
        "status": "PENDING"
      }
    ]
  },
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Role Compliance

#### Get Role Review Status
```http
GET /api/v1/party/compliance/role-reviews?status=DUE&limit=50
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "roleReviews": [
    {
      "reviewId": "review_11111",
      "userId": "john.smith@banking.local",
      "roleName": "LOAN_OFFICER",
      "lastReviewDate": "2023-07-15T00:00:00Z",
      "nextReviewDate": "2024-01-15T00:00:00Z",
      "status": "DUE",
      "riskLevel": "MEDIUM",
      "reviewType": "QUARTERLY",
      "assignedReviewer": "manager.user@banking.local"
    }
  ],
  "summary": {
    "totalReviews": 45,
    "dueReviews": 12,
    "overdueReviews": 3,
    "completedThisMonth": 8
  }
}
```

---

## OAuth2.1 Integration APIs

### Token Management

#### Introspect Token
```http
POST /oauth2/introspect
Authorization: Basic {client_credentials}
Content-Type: application/x-www-form-urlencoded

token={access_token}
```

**Response (200 OK):**
```json
{
  "active": true,
  "sub": "john.smith@banking.local",
  "scope": "banking:read banking:write",
  "client_id": "banking-app",
  "token_type": "Bearer",
  "exp": 1640998800,
  "iat": 1640995200,
  "banking_roles": ["LOAN_OFFICER"],
  "authority_level": 7,
  "monetary_limit": 500000,
  "business_unit": "Commercial Loans"
}
```

#### Revoke Token
```http
POST /oauth2/revoke
Authorization: Basic {client_credentials}
Content-Type: application/x-www-form-urlencoded

token={access_token}
&token_type_hint=access_token
```

**Response (200 OK):** Empty response body

### User Info

#### Get User Information
```http
GET /oauth2/userinfo
Authorization: Bearer {access_token}
```

**Response (200 OK):**
```json
{
  "sub": "john.smith@banking.local",
  "name": "John Smith",
  "given_name": "John",
  "family_name": "Smith",
  "email": "john.smith@banking.local",
  "email_verified": true,
  "banking_roles": ["LOAN_OFFICER"],
  "departments": ["Commercial Loans"],
  "authority_level": 7,
  "monetary_limit": 500000
}
```

---

## Security APIs

### Audit Logging

#### Get Audit Logs
```http
GET /api/v1/audit/logs?user={userId}&action={action}&from={date}&to={date}&limit=100
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "auditLogs": [
    {
      "logId": "audit_log_123456",
      "timestamp": "2024-01-15T10:30:00Z",
      "userId": "john.smith@banking.local",
      "sessionId": "session_abc123",
      "action": "LOAN_APPROVED",
      "resource": "/api/v1/loans/loan_67890/approve",
      "resourceId": "loan_67890",
      "ipAddress": "192.168.1.100",
      "userAgent": "Banking-App/1.0.0",
      "outcome": "SUCCESS",
      "details": {
        "loanAmount": 100000.00,
        "customerId": "cust_12345",
        "approvalLevel": "LOAN_OFFICER"
      },
      "complianceMarkers": [
        "SOX_FINANCIAL_TRANSACTION",
        "PCI_SENSITIVE_DATA"
      ]
    }
  ],
  "pagination": {
    "hasNext": true,
    "nextCursor": "cursor_def456",
    "limit": 100,
    "total": 15420
  }
}
```

#### Create Security Event
```http
POST /api/v1/security/events
Authorization: Bearer {token}
Content-Type: application/json

{
  "eventType": "SUSPICIOUS_ACTIVITY",
  "severity": "HIGH",
  "description": "Multiple failed login attempts from unusual location",
  "userId": "john.smith@banking.local",
  "ipAddress": "192.168.1.100",
  "details": {
    "failedAttempts": 5,
    "timeWindow": "5 minutes",
    "location": "Unknown"
  }
}
```

**Response (201 Created):**
```json
{
  "eventId": "security_event_789012",
  "eventType": "SUSPICIOUS_ACTIVITY",
  "severity": "HIGH",
  "status": "UNDER_INVESTIGATION",
  "assignedTo": "security-team@banking.local",
  "createdAt": "2024-01-15T10:30:00Z",
  "ticketNumber": "SEC-2024-001"
}
```

---

## Error Handling

### Standard Error Response

All APIs return errors in a consistent format:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "One or more validation errors occurred",
    "details": [
      {
        "field": "amount",
        "code": "AMOUNT_EXCEEDS_LIMIT",
        "message": "Loan amount exceeds customer credit limit",
        "rejectedValue": 150000.00,
        "maxValue": 50000.00
      }
    ],
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_12345",
    "path": "/api/v1/loans"
  }
}
```

### Error Codes

#### Authentication & Authorization Errors
| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 401 | `INVALID_TOKEN` | JWT token is invalid or expired |
| 401 | `TOKEN_REQUIRED` | Authorization header missing |
| 403 | `INSUFFICIENT_PRIVILEGES` | User lacks required role or scope |
| 403 | `MONETARY_LIMIT_EXCEEDED` | Operation exceeds user's monetary authority |
| 403 | `BUSINESS_UNIT_MISMATCH` | User cannot access resource from different business unit |

#### Business Logic Errors
| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_FAILED` | Input validation failed |
| 400 | `DUPLICATE_REQUEST` | Idempotency key already used |
| 404 | `RESOURCE_NOT_FOUND` | Requested resource does not exist |
| 409 | `BUSINESS_RULE_VIOLATION` | Operation violates business rules |
| 422 | `INSUFFICIENT_FUNDS` | Customer lacks sufficient credit |

#### System Errors
| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 429 | `RATE_LIMIT_EXCEEDED` | Too many requests |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected system error |
| 503 | `SERVICE_UNAVAILABLE` | System temporarily unavailable |

---

## Rate Limiting

### Rate Limit Rules

| Endpoint Category | Limit | Window | Scope |
|-------------------|-------|---------|-------|
| Authentication | 10 requests | 1 minute | Per IP |
| Customer Operations | 100 requests | 1 minute | Per User |
| Loan Operations | 50 requests | 1 minute | Per User |
| Payment Processing | 20 requests | 1 minute | Per User |
| Admin Operations | 30 requests | 1 minute | Per User |

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 75
X-RateLimit-Reset: 1640995800
X-RateLimit-Window: 60
```

### Rate Limit Exceeded Response

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640995860
Retry-After: 60

{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please try again later.",
    "retryAfter": 60,
    "limit": 100,
    "window": 60
  }
}
```

---

## SDK Examples

### Java SDK Example

```java
// Initialize the Banking API client
BankingApiClient client = BankingApiClient.builder()
    .baseUrl("https://api.banking.enterprise.com/v1")
    .oauthClient(oauthClient)
    .build();

// Create a customer
CreateCustomerRequest request = CreateCustomerRequest.builder()
    .personalInfo(PersonalInfo.builder()
        .firstName("John")
        .lastName("Smith")
        .email("john.smith@example.com")
        .build())
    .build();

Customer customer = client.customers().create(request);

// Create a loan
CreateLoanRequest loanRequest = CreateLoanRequest.builder()
    .customerId(customer.getCustomerId())
    .amount(new BigDecimal("100000.00"))
    .currency("USD")
    .installmentCount(360)
    .interestRate(new BigDecimal("3.5"))
    .purpose("Home Purchase")
    .build();

Loan loan = client.loans().create(loanRequest);

// Approve the loan (requires LOAN_OFFICER role)
ApproveLoanRequest approvalRequest = ApproveLoanRequest.builder()
    .approvalNotes("Credit assessment completed successfully")
    .build();

LoanApproval approval = client.loans().approve(loan.getLoanId(), approvalRequest);
```

### Python SDK Example

```python
from banking_api import BankingApiClient
from banking_api.models import CreateCustomerRequest, CreateLoanRequest

# Initialize client
client = BankingApiClient(
    base_url="https://api.banking.enterprise.com/v1",
    oauth_client=oauth_client
)

# Create customer
customer_request = CreateCustomerRequest(
    personal_info={
        "firstName": "John",
        "lastName": "Smith",
        "email": "john.smith@example.com"
    }
)

customer = client.customers.create(customer_request)

# Create loan
loan_request = CreateLoanRequest(
    customer_id=customer.customer_id,
    amount=100000.00,
    currency="USD",
    installment_count=360,
    interest_rate=3.5,
    purpose="Home Purchase"
)

loan = client.loans.create(loan_request)
```

---

## Testing & Development

### API Testing

#### Postman Collection
A comprehensive Postman collection is available with:
- OAuth2.1 authentication flow
- All API endpoints with examples
- Environment variables for different environments
- Automated tests for response validation

#### Sandbox Environment
```
Base URL: https://api-sandbox.banking.enterprise.com/v1
Features:
- Test data with realistic scenarios
- No rate limiting
- OAuth2.1 test realm
- Mock external services
```

### Webhook Support

#### Loan Status Webhooks
```http
POST {webhook_url}
Content-Type: application/json
X-Banking-Signature: sha256={signature}

{
  "eventType": "LOAN_STATUS_CHANGED",
  "eventId": "event_123456",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "loanId": "loan_67890",
    "previousStatus": "PENDING_APPROVAL",
    "newStatus": "APPROVED",
    "approvedBy": "john.smith@banking.local"
  }
}
```

---

## Conclusion

The Enterprise Banking System APIs provide a comprehensive, secure, and compliant interface for banking operations. Key features include:

### API Capabilities
- **OAuth2.1 Security**: Industry-standard authentication
- **FAPI Compliance**: Financial-grade API security
- **Role-based Authorization**: Fine-grained access control
- **Comprehensive Coverage**: Full banking operation support
- **Developer Experience**: Clear documentation and SDKs

### Security Features
- **Multi-layer Authorization**: OAuth2.1 + LDAP + Party Management
- **Audit Logging**: Comprehensive transaction tracking
- **Rate Limiting**: Protection against abuse
- **Encryption**: All data protected in transit and at rest
- **Compliance**: SOX, PCI DSS, and GDPR compliance

---

## Migration Guide

### Migrating from FAPI 1.0 to FAPI 2.0 + DPoP

#### 1. Client Registration Changes

**Before (FAPI 1.0):**
```json
{
  "client_id": "banking-app",
  "client_secret": "secret123",
  "token_endpoint_auth_method": "client_secret_basic",
  "response_types": ["code", "code id_token"],
  "response_modes": ["query", "fragment"]
}
```

**After (FAPI 2.0 + DPoP):**
```json
{
  "client_id": "banking-app", 
  "token_endpoint_auth_method": "private_key_jwt",
  "response_types": ["code"],
  "response_modes": ["query"],
  "require_pushed_authorization_requests": true,
  "dpop_bound_access_tokens": true,
  "jwks_uri": "https://app.banking.com/.well-known/jwks.json"
}
```

#### 2. Client Code Changes

**Before (Bearer Tokens):**
```javascript
// Old: Bearer token requests
const response = await fetch('/api/v1/loans', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});
```

**After (DPoP Tokens):**
```javascript
// New: DPoP-bound token requests
const dpopProof = await createDPoPProof('GET', '/api/v1/loans', accessToken);

const response = await fetch('/api/v1/loans', {
  headers: {
    'Authorization': `DPoP ${accessToken}`,
    'DPoP': dpopProof,
    'Content-Type': 'application/json',
    'X-FAPI-Interaction-ID': uuidv4(),
    'X-FAPI-Auth-Date': new Date().toUTCString(),
    'X-FAPI-Customer-IP-Address': clientIP
  }
});
```

#### 3. Authorization Flow Changes

**Before (Direct Authorization):**
```javascript
// Old: Direct authorization request
const authUrl = `${authEndpoint}?client_id=${clientId}&response_type=code id_token&redirect_uri=${redirectUri}`;
window.location.href = authUrl;
```

**After (PAR + Authorization Code):**
```javascript
// New: PAR then authorization
const parResponse = await fetch('/oauth2/par', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Authorization': `Bearer ${clientAssertion}`
  },
  body: new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    redirect_uri: redirectUri,
    scope: 'openid banking-loans',
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
    dpop_jkt: jktThumbprint,
    client_assertion_type: 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
    client_assertion: clientAssertion
  })
});

const { request_uri } = await parResponse.json();
const authUrl = `${authEndpoint}?client_id=${clientId}&request_uri=${request_uri}`;
window.location.href = authUrl;
```

#### 4. Error Handling Updates

**New DPoP-specific Error Codes:**

| Error Code | Description | Action Required |
|------------|-------------|-----------------|
| `invalid_dpop_proof` | DPoP proof missing or invalid | Generate new DPoP proof |
| `use_dpop_nonce` | Server requires DPoP nonce | Include nonce in next proof |
| `invalid_request_uri` | PAR request URI invalid/expired | Create new PAR request |
| `unsupported_response_type` | Non-code response type used | Use `response_type=code` only |

#### 5. Testing Migration

Use the provided migration tool:

```bash
# Generate DPoP keys and configuration
java -cp app.jar com.bank.loan.loan.security.migration.DPoPMigrationTool \
  "banking-app" \
  "Banking Application" \
  "EC" \
  "./migration-output"

# Validate FAPI 2.0 compliance
curl -X GET https://api.banking.com/oauth2/status \
  -H "Accept: application/json"
```

#### 6. Rollback Plan

If migration issues occur:

1. **Immediate**: Switch back to `application-enterprise.yml` profile
2. **DNS**: Point API endpoints to FAPI 1.0 servers
3. **Client**: Deploy previous client version with Bearer tokens
4. **Monitoring**: Alert on authentication failure rate increases

#### 7. Support Resources

- **DPoP Client Library**: Use provided `DPoPClientLibrary.java`
- **Migration Tool**: `DPoPMigrationTool.java` for automated setup
- **Configuration**: `application-fapi2-dpop.yml` for reference
- **Documentation**: This API documentation for complete reference

For additional technical details, refer to the [FAPI 2.0 Migration Guide](FAPI-2.0-Migration-Guide.md) and [DPoP Implementation Guide](DPoP-Implementation-Guide.md).