# Enterprise Banking Platform API Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Authentication & Security](#authentication--security)
4. [API Design Principles](#api-design-principles)
5. [Core APIs](#core-apis)
6. [Real-time Features](#real-time-features)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)
9. [SDKs & Examples](#sdks--examples)
10. [Testing Guide](#testing-guide)
11. [Compliance & Regulations](#compliance--regulations)

## Introduction

The Enterprise Banking Platform API provides comprehensive banking services including customer management, loan processing, payment operations, and compliance features. Built with financial-grade security (FAPI 2.0) and modern API standards.

### Key Features

- **FAPI 2.0 Compliant**: Meets Financial-grade API security requirements
- **Real-time Updates**: Server-Sent Events and webhooks for live data
- **HATEOAS**: Hypermedia-driven API for better client decoupling
- **Idempotency**: Duplicate request protection for financial operations
- **Comprehensive Observability**: OpenTelemetry tracing and metrics
- **Event-Driven Architecture**: Async processing with domain events

### API Versions

- **Current Version**: v1.0
- **Base URL**: `https://api.banking.local`
- **Documentation**: OpenAPI 3.1+ specification available

## Getting Started

### Prerequisites

1. **Client Registration**: Contact your integration team for client credentials
2. **mTLS Certificate**: Obtain client certificate for mutual TLS authentication
3. **OAuth 2.1 Setup**: Configure OAuth flows with appropriate scopes
4. **Development Environment**: Access to staging APIs for testing

### Quick Start

```bash
# 1. Test connectivity
curl -X GET https://api.banking.local/health \
  --cert client.crt \
  --key client.key

# 2. Obtain access token
curl -X POST https://auth.banking.local/oauth2/token \
  --cert client.crt \
  --key client.key \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=customer:read customer:write"

# 3. Make your first API call
curl -X GET https://api.banking.local/api/v1/customers/CUST-12345678 \
  --cert client.crt \
  --key client.key \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "X-FAPI-Financial-Id: GB-FCA-123456"
```

## Authentication & Security

### OAuth 2.1 with FAPI 2.0

The platform implements OAuth 2.1 with Financial-grade API security profile:

```mermaid
sequenceDiagram
    participant Client
    participant AuthServer as Authorization Server
    participant API as Banking API

    Client->>AuthServer: 1. Client Credentials Grant (mTLS)
    AuthServer-->>Client: 2. Access Token (JWT)
    Client->>API: 3. API Request (mTLS + Bearer Token)
    API-->>Client: 4. Response + HATEOAS Links
```

### Required Headers

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
X-FAPI-Financial-Id: GB-FCA-123456
X-FAPI-Interaction-Id: 12345678-1234-1234-1234-123456789012
Idempotency-Key: loan-2024-001-xyz789  # For POST/PUT operations
```

### Scopes

| Scope | Description |
|-------|-------------|
| `customer:read` | Read customer information |
| `customer:write` | Create and modify customers |
| `loan:read` | Read loan information |
| `loan:write` | Create loan applications |
| `loan:approve` | Approve/reject loans |
| `payment:read` | Read payment information |
| `payment:write` | Process payments |
| `admin` | Administrative access |

## API Design Principles

### 1. RESTful Design

Resources are accessed via HTTP methods:

```http
GET    /api/v1/customers           # List customers
POST   /api/v1/customers           # Create customer
GET    /api/v1/customers/{id}      # Get customer
PUT    /api/v1/customers/{id}      # Update customer
DELETE /api/v1/customers/{id}      # Delete customer
```

### 2. HATEOAS Implementation

All responses include hypermedia links for state transitions:

```json
{
  "customerId": "CUST-12345678",
  "firstName": "John",
  "lastName": "Doe",
  "status": "ACTIVE",
  "_links": {
    "self": {
      "href": "https://api.banking.local/api/v1/customers/CUST-12345678"
    },
    "update-credit-limit": {
      "href": "https://api.banking.local/api/v1/customers/CUST-12345678/credit-limit"
    },
    "reserve-credit": {
      "href": "https://api.banking.local/api/v1/customers/CUST-12345678/credit/reserve"
    },
    "events": {
      "href": "https://api.banking.local/api/v1/customers/CUST-12345678/events"
    }
  }
}
```

### 3. Idempotency

All state-changing operations require an idempotency key:

```http
POST /api/v1/payments
Idempotency-Key: pay-2024-001-uuid123
Content-Type: application/json

{
  "customerId": "CUST-12345678",
  "fromAccountId": "ACC-11111111",
  "toAccountId": "ACC-22222222",
  "amount": 1000.00,
  "currency": "USD",
  "paymentType": "BANK_TRANSFER"
}
```

## Core APIs

### Customer Management

#### Create Customer

```http
POST /api/v1/customers
Idempotency-Key: cust-2024-001-abc123
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "dateOfBirth": "1985-06-15",
  "address": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "12345",
    "country": "US"
  },
  "creditLimit": 50000.00
}
```

#### Response

```json
{
  "customerId": "CUST-12345678",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "status": "ACTIVE",
  "creditLimit": 50000.00,
  "availableCredit": 50000.00,
  "currency": "USD",
  "createdAt": "2024-01-15T10:30:00Z",
  "_links": {
    "self": {"href": "/api/v1/customers/CUST-12345678"},
    "update-credit-limit": {"href": "/api/v1/customers/CUST-12345678/credit-limit"},
    "events": {"href": "/api/v1/customers/CUST-12345678/events"}
  }
}
```

### Loan Processing

#### Submit Loan Application

```http
POST /api/v1/loans
Idempotency-Key: loan-2024-001-xyz789
Content-Type: application/json

{
  "customerId": "CUST-12345678",
  "principalAmount": 25000.00,
  "currency": "USD",
  "annualInterestRate": 8.5,
  "termInMonths": 60
}
```

#### Loan Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING_APPROVAL: Submit Application
    PENDING_APPROVAL --> APPROVED: Approve
    PENDING_APPROVAL --> REJECTED: Reject
    APPROVED --> ACTIVE: Disburse
    ACTIVE --> PAID_OFF: Final Payment
    ACTIVE --> DEFAULTED: Default
```

### Payment Processing

#### Process Payment

```http
POST /api/v1/payments
Idempotency-Key: pay-2024-001-uuid123
Content-Type: application/json

{
  "customerId": "CUST-12345678",
  "fromAccountId": "ACC-11111111",
  "toAccountId": "ACC-22222222",
  "amount": 1000.00,
  "currency": "USD",
  "paymentType": "BANK_TRANSFER",
  "description": "Monthly rent payment"
}
```

#### Payment States

```mermaid
stateDiagram-v2
    [*] --> PENDING: Initiate
    PENDING --> PROCESSING: Validate
    PROCESSING --> COMPLETED: Success
    PROCESSING --> FAILED: Error
    PENDING --> CANCELLED: Cancel
    FAILED --> PENDING: Retry
    COMPLETED --> [*]
```

## Real-time Features

### Server-Sent Events

Subscribe to real-time updates for resources:

```javascript
const eventSource = new EventSource(
  'https://api.banking.local/api/v1/payments/PAY-12345678/events',
  {
    headers: {
      'Authorization': 'Bearer ' + accessToken,
      'X-FAPI-Financial-Id': 'GB-FCA-123456'
    }
  }
);

eventSource.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Payment update:', data);
};

eventSource.addEventListener('PaymentCompleted', function(event) {
  const payment = JSON.parse(event.data);
  console.log('Payment completed:', payment.paymentId);
});
```

### Webhooks

Configure webhooks for external system notifications:

```json
{
  "webhookUrl": "https://your-system.com/webhooks/banking",
  "events": ["PaymentCompleted", "LoanApproved", "FraudDetected"],
  "secret": "your-webhook-secret"
}
```

Webhook payload example:

```json
{
  "webhookId": "webhook-12345",
  "eventType": "PaymentCompleted",
  "timestamp": "2024-01-15T10:31:00Z",
  "data": {
    "paymentId": "PAY-12345678",
    "amount": 1000.00,
    "status": "COMPLETED"
  },
  "signature": "sha256=base64EncodedHMACSignature"
}
```

## Error Handling

### Standard Error Format (RFC 9457)

```json
{
  "error": "validation_failed",
  "error_description": "The request data failed validation",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/customers",
  "details": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

### HTTP Status Codes

| Code | Description | Use Case |
|------|-------------|----------|
| `200` | OK | Successful GET/PUT |
| `201` | Created | Successful POST |
| `400` | Bad Request | Invalid request data |
| `401` | Unauthorized | Missing/invalid authentication |
| `402` | Payment Required | Insufficient funds |
| `403` | Forbidden | Access denied |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Resource already exists |
| `422` | Unprocessable Entity | Business rule violation |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Server error |

### Error Categories

#### Business Logic Errors

```json
{
  "error": "insufficient_credit",
  "error_description": "Customer does not have sufficient credit limit",
  "timestamp": "2024-01-15T10:30:00Z",
  "details": {
    "requested": 10000.00,
    "available": 5000.00,
    "currency": "USD"
  }
}
```

#### Validation Errors

```json
{
  "error": "validation_failed",
  "error_description": "Request validation failed",
  "timestamp": "2024-01-15T10:30:00Z",
  "details": [
    {
      "field": "principalAmount",
      "message": "Amount must be between 1,000 and 10,000,000"
    },
    {
      "field": "annualInterestRate",
      "message": "Interest rate cannot exceed 50%"
    }
  ]
}
```

## Rate Limiting

### Rate Limits by Endpoint Type

| Endpoint Type | Limit | Window |
|---------------|-------|--------|
| Authentication | 5 requests | 1 minute |
| Financial Operations | 10 requests | 1 minute |
| Read Operations | 100 requests | 1 minute |
| Admin Operations | 2 requests | 1 minute |

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642176000
Retry-After: 60
```

### Rate Limit Response

```json
{
  "error": "rate_limit_exceeded",
  "error_description": "Too many requests. Please retry after the specified time.",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## SDKs & Examples

### JavaScript/Node.js

```javascript
const BankingAPI = require('@banking-platform/api-client');

const client = new BankingAPI({
  baseURL: 'https://api.banking.local',
  clientCert: './client.crt',
  clientKey: './client.key',
  accessToken: 'your-access-token',
  financialId: 'GB-FCA-123456'
});

// Create customer
const customer = await client.customers.create({
  firstName: 'John',
  lastName: 'Doe',
  email: 'john.doe@example.com',
  // ... other fields
}, {
  idempotencyKey: 'cust-2024-001-abc123'
});

// Subscribe to events
const eventStream = client.customers.events(customer.customerId);
eventStream.on('CustomerUpdated', (event) => {
  console.log('Customer updated:', event.data);
});
```

### Python

```python
from banking_platform import BankingClient

client = BankingClient(
    base_url='https://api.banking.local',
    client_cert='./client.crt',
    client_key='./client.key',
    access_token='your-access-token',
    financial_id='GB-FCA-123456'
)

# Process payment
payment = client.payments.create(
    customer_id='CUST-12345678',
    from_account_id='ACC-11111111',
    to_account_id='ACC-22222222',
    amount=1000.00,
    currency='USD',
    payment_type='BANK_TRANSFER',
    idempotency_key='pay-2024-001-uuid123'
)

print(f"Payment created: {payment.payment_id}")
```

### Java

```java
BankingClient client = BankingClient.builder()
    .baseUrl("https://api.banking.local")
    .clientCertificate("./client.crt", "./client.key")
    .accessToken("your-access-token")
    .financialId("GB-FCA-123456")
    .build();

// Submit loan application
LoanApplication loan = client.loans().create(
    CreateLoanRequest.builder()
        .customerId("CUST-12345678")
        .principalAmount(new BigDecimal("25000.00"))
        .currency("USD")
        .annualInterestRate(new BigDecimal("8.5"))
        .termInMonths(60)
        .build(),
    "loan-2024-001-xyz789" // idempotency key
);

System.out.println("Loan application: " + loan.getLoanId());
```

## Testing Guide

### Test Environment

- **Base URL**: `https://api-staging.banking.local`
- **Test Data**: Pre-populated test customers and accounts
- **Rate Limits**: Relaxed for testing (10x production limits)

### Test Scenarios

#### 1. Happy Path - Customer Onboarding

```bash
# 1. Create customer
curl -X POST https://api-staging.banking.local/api/v1/customers \
  --cert test-client.crt --key test-client.key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: test-$(uuidgen)" \
  -H "Content-Type: application/json" \
  -d @test-customer.json

# 2. Apply for loan
curl -X POST https://api-staging.banking.local/api/v1/loans \
  --cert test-client.crt --key test-client.key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: test-$(uuidgen)" \
  -H "Content-Type: application/json" \
  -d @test-loan.json

# 3. Process payment
curl -X POST https://api-staging.banking.local/api/v1/payments \
  --cert test-client.crt --key test-client.key \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: test-$(uuidgen)" \
  -H "Content-Type: application/json" \
  -d @test-payment.json
```

#### 2. Error Scenarios

```bash
# Test insufficient funds
curl -X POST https://api-staging.banking.local/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: test-insufficient-funds" \
  -d '{
    "customerId": "CUST-TEST-001",
    "fromAccountId": "ACC-EMPTY-001",
    "toAccountId": "ACC-TEST-002",
    "amount": 999999.99,
    "currency": "USD",
    "paymentType": "BANK_TRANSFER"
  }'

# Expected: 402 Payment Required
```

#### 3. Idempotency Testing

```bash
# Send same request twice with same idempotency key
IDEMPOTENCY_KEY="test-idempotency-$(date +%s)"

# First request
curl -X POST https://api-staging.banking.local/api/v1/customers \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d @test-customer.json

# Second request (should return same response)
curl -X POST https://api-staging.banking.local/api/v1/customers \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d @test-customer.json
```

### Test Data

#### Test Customer

```json
{
  "firstName": "Test",
  "lastName": "Customer",
  "email": "test@example.com",
  "phoneNumber": "+1-555-TEST",
  "dateOfBirth": "1990-01-01",
  "address": {
    "street": "123 Test St",
    "city": "Test City",
    "state": "TS",
    "zipCode": "12345",
    "country": "US"
  },
  "creditLimit": 10000.00
}
```

## Compliance & Regulations

### FAPI 2.0 Compliance

The platform implements Financial-grade API security requirements:

- **Mutual TLS**: Client certificate authentication
- **JWT Secured Authorization**: Strong token security
- **Request Object**: Signed request parameters
- **JARM**: JWT Secured Authorization Response Mode

### PCI DSS Compliance

- **Data Encryption**: All sensitive data encrypted at rest and in transit
- **Access Controls**: Role-based access with least privilege
- **Audit Logging**: Comprehensive audit trails
- **Network Security**: Network segmentation and firewalls

### AML/KYC Compliance

- **Customer Verification**: Automated KYC checks
- **Transaction Monitoring**: Real-time AML screening
- **Sanctions Screening**: OFAC and other sanctions lists
- **Suspicious Activity Reporting**: Automated SAR generation

### GDPR Compliance

- **Data Minimization**: Only necessary data collected
- **Right to Erasure**: Customer data deletion capabilities
- **Data Portability**: Export customer data in standard formats
- **Consent Management**: Granular consent tracking

### Audit and Reporting

All API interactions are logged with:

- **Request/Response Logs**: Full audit trail
- **User Activity**: User action tracking
- **Compliance Events**: Regulatory reporting events
- **Security Events**: Authentication and authorization logs

### Data Retention

| Data Type | Retention Period | Purpose |
|-----------|------------------|---------|
| Transaction Records | 7 years | Regulatory requirement |
| Audit Logs | 7 years | Compliance and security |
| Customer Data | Until account closure + 7 years | Business and regulatory |
| Authentication Logs | 2 years | Security monitoring |

---

## Support and Resources

- **API Documentation**: [OpenAPI Specification](./openapi-specification.yaml)
- **Async API**: [Event Documentation](./asyncapi-specification.yaml)
- **Architecture**: [System Architecture Diagrams](../architecture/api-flow-diagrams.md)
- **Status Page**: https://status.banking.local
- **Support**: api-support@banking.local
- **Developer Portal**: https://developers.banking.local

For additional support or questions, please contact the API integration team.