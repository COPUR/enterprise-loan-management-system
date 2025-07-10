# API Documentation
## Enterprise Banking System - RESTful APIs with OAuth2.1 Integration

### Table of Contents
1. [API Overview](#api-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Core Banking APIs](#core-banking-apis)
4. [Party Management APIs](#party-management-apis)
5. [OAuth2.1 Integration APIs](#oauth21-integration-apis)
6. [Security APIs](#security-apis)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)

---

## API Overview

The Enterprise Banking System provides a comprehensive set of RESTful APIs designed for secure banking operations with OAuth2.1 authentication and authorization. All APIs follow OpenAPI 3.0 specification and implement banking-grade security controls.

### Base URL
```
Production: https://api.banking.enterprise.com/v1
Staging: https://api-staging.banking.enterprise.com/v1
Development: http://localhost:8080/api/v1
```

### API Design Principles

1. **RESTful Design**: Resource-based URLs with standard HTTP methods
2. **OAuth2.1 Security**: Bearer token authentication for all endpoints
3. **FAPI Compliance**: Financial-grade API security headers
4. **Idempotency**: POST/PUT operations support idempotency keys
5. **Pagination**: Cursor-based pagination for list operations
6. **Versioning**: URL-based versioning (v1, v2, etc.)

### Standard Headers

#### Request Headers
```http
Authorization: Bearer {jwt_token}
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
```

---

## Authentication & Authorization

### OAuth2.1 Integration

The API uses OAuth2.1 Authorization Code Flow with PKCE for authentication:

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

For additional technical details, refer to the [OAuth2.1 Architecture Guide](OAuth2.1-Architecture-Guide.md) and [Security Architecture Overview](security-architecture/Security-Architecture-Overview.md).