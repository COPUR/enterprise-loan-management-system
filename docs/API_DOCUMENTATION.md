# API Documentation - Enterprise Loan Management System

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Customer API](#customer-api)
4. [Loan API](#loan-api)
5. [Payment API](#payment-api)
6. [AI Assistant API](#ai-assistant-api)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)
9. [Webhooks](#webhooks)

## Overview

The Enterprise Loan Management System provides a comprehensive REST API for managing customers, loans, and payments. All APIs follow RESTful principles and use JSON for request and response payloads.

### Base URL
```
Production: https://api.loanmanagement.com/v1
Staging: https://api-staging.loanmanagement.com/v1
Development: http://localhost:8080/api/v1
GraphQL: http://localhost:8080/graphql
WebSocket: ws://localhost:8080/ws
```

### API Versioning
The API uses URL versioning. The current version is `v1`. When breaking changes are introduced, a new version will be created.

### Request Headers
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {access_token}
DPoP: {dpop_proof_token}
X-Request-ID: {unique_request_id}
X-Client-ID: {client_application_id}
X-FAPI-Interaction-ID: {fapi_interaction_id}
```

## Authentication

The API uses **OAuth 2.1** with **JWT tokens** and **DPoP (Demonstrating Proof of Possession)** for authentication. **FAPI 2.0 compliance** ensures financial-grade security with **Zero Trust Architecture**.

### Token Endpoint (OAuth 2.1)
```http
POST /auth/realms/banking/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
DPoP: {dpop_proof_token}

grant_type=client_credentials&
client_id={client_id}&
client_secret={client_secret}&
scope=customers:read customers:write loans:read loans:write payments:read payments:write
```

#### Response
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "customers:read customers:write loans:read loans:write"
}
```

### DPoP (Demonstrating Proof of Possession)
For enhanced security, the API supports DPoP tokens:

```http
POST /auth/token
Content-Type: application/x-www-form-urlencoded
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6Ik...

grant_type=client_credentials&
client_id={client_id}&
client_secret={client_secret}
```

## Customer API

### Create Customer
Creates a new customer in the system.

```http
POST /customers
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "dateOfBirth": "1990-01-15",
  "monthlyIncome": 5000.00,
  "address": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "12345",
    "country": "USA"
  }
}
```

#### Response
```json
{
  "id": 12345,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "dateOfBirth": "1990-01-15",
  "monthlyIncome": 5000.00,
  "status": "PENDING",
  "creditScore": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "_links": {
    "self": {"href": "/customers/12345"},
    "loans": {"href": "/customers/12345/loans"},
    "kyc": {"href": "/customers/12345/kyc"}
  }
}
```

### Get Customer
Retrieves customer details by ID.

```http
GET /customers/{customerId}
Authorization: Bearer {access_token}
```

#### Path Parameters
- `customerId` (required): The unique identifier of the customer

#### Response
```json
{
  "id": 12345,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "dateOfBirth": "1990-01-15",
  "monthlyIncome": 5000.00,
  "status": "ACTIVE",
  "creditScore": 750,
  "address": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "12345",
    "country": "USA"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T14:20:00Z"
}
```

### Update Customer
Updates customer information.

```http
PUT /customers/{customerId}
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "phone": "+1-555-987-6543",
  "monthlyIncome": 6000.00,
  "address": {
    "street": "456 Oak Ave",
    "city": "Newtown",
    "state": "CA",
    "zipCode": "54321",
    "country": "USA"
  }
}
```

### List Customers
Retrieves a paginated list of customers.

```http
GET /customers?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {access_token}
```

#### Query Parameters
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sort` (optional): Sort criteria (format: field,direction)
- `status` (optional): Filter by status (PENDING, ACTIVE, SUSPENDED, CLOSED)

#### Response
```json
{
  "content": [
    {
      "id": 12345,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "descending": true
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

## Loan API

### Create Loan Application
Submits a new loan application.

```http
POST /loans
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "customerId": 12345,
  "principalAmount": 50000.00,
  "currency": "USD",
  "termMonths": 60,
  "purpose": "HOME_IMPROVEMENT",
  "requestedInterestRate": 5.5
}
```

#### Response
```json
{
  "id": 98765,
  "customerId": 12345,
  "principalAmount": 50000.00,
  "currency": "USD",
  "termMonths": 60,
  "interestRate": 5.75,
  "monthlyPayment": 958.42,
  "status": "PENDING_APPROVAL",
  "purpose": "HOME_IMPROVEMENT",
  "applicationDate": "2024-01-15T11:00:00Z",
  "riskAssessment": {
    "score": 85,
    "level": "LOW",
    "factors": [
      "Good credit history",
      "Stable income",
      "Low debt-to-income ratio"
    ]
  },
  "_links": {
    "self": {"href": "/loans/98765"},
    "customer": {"href": "/customers/12345"},
    "payments": {"href": "/loans/98765/payments"},
    "documents": {"href": "/loans/98765/documents"}
  }
}
```

### Get Loan Details
Retrieves detailed information about a specific loan.

```http
GET /loans/{loanId}
Authorization: Bearer {access_token}
```

#### Response
```json
{
  "id": 98765,
  "customerId": 12345,
  "principalAmount": 50000.00,
  "currency": "USD",
  "termMonths": 60,
  "interestRate": 5.75,
  "monthlyPayment": 958.42,
  "status": "ACTIVE",
  "purpose": "HOME_IMPROVEMENT",
  "applicationDate": "2024-01-15T11:00:00Z",
  "approvalDate": "2024-01-16T09:30:00Z",
  "disbursementDate": "2024-01-17T14:00:00Z",
  "maturityDate": "2029-01-17T00:00:00Z",
  "outstandingBalance": 48500.00,
  "nextPaymentDue": {
    "date": "2024-02-17",
    "amount": 958.42,
    "principalComponent": 725.00,
    "interestComponent": 233.42
  },
  "paymentSchedule": {
    "totalPayments": 60,
    "completedPayments": 2,
    "remainingPayments": 58
  }
}
```

### Approve Loan
Approves a pending loan application.

```http
POST /loans/{loanId}/approve
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "approvedBy": "loan_officer_123",
  "approvalNotes": "All criteria met, customer verified",
  "conditions": [
    "Insurance required before disbursement",
    "Co-signer documentation needed"
  ]
}
```

### Get Loan Payment Schedule
Retrieves the complete payment schedule for a loan.

```http
GET /loans/{loanId}/payment-schedule
Authorization: Bearer {access_token}
```

#### Response
```json
{
  "loanId": 98765,
  "schedule": [
    {
      "installmentNumber": 1,
      "dueDate": "2024-02-17",
      "principalAmount": 725.00,
      "interestAmount": 233.42,
      "totalAmount": 958.42,
      "status": "PAID",
      "paidDate": "2024-02-15",
      "remainingBalance": 49275.00
    },
    {
      "installmentNumber": 2,
      "dueDate": "2024-03-17",
      "principalAmount": 728.37,
      "interestAmount": 230.05,
      "totalAmount": 958.42,
      "status": "PENDING",
      "remainingBalance": 48546.63
    }
  ]
}
```

## Payment API

### Process Payment
Processes a payment for a loan.

```http
POST /payments
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "loanId": 98765,
  "amount": 958.42,
  "paymentDate": "2024-02-15",
  "paymentMethod": "BANK_TRANSFER",
  "reference": "TXN123456789",
  "type": "REGULAR"
}
```

#### Response
```json
{
  "id": 456789,
  "loanId": 98765,
  "amount": 958.42,
  "paymentDate": "2024-02-15",
  "processedDate": "2024-02-15T10:30:00Z",
  "paymentMethod": "BANK_TRANSFER",
  "reference": "TXN123456789",
  "type": "REGULAR",
  "status": "COMPLETED",
  "allocation": {
    "principal": 725.00,
    "interest": 233.42,
    "fees": 0.00,
    "penalties": 0.00
  },
  "remainingLoanBalance": 49275.00,
  "_links": {
    "self": {"href": "/payments/456789"},
    "loan": {"href": "/loans/98765"},
    "receipt": {"href": "/payments/456789/receipt"}
  }
}
```

### Get Payment History
Retrieves payment history for a loan.

```http
GET /loans/{loanId}/payments?page=0&size=10
Authorization: Bearer {access_token}
```

#### Response
```json
{
  "content": [
    {
      "id": 456789,
      "amount": 958.42,
      "paymentDate": "2024-02-15",
      "type": "REGULAR",
      "status": "COMPLETED",
      "allocation": {
        "principal": 725.00,
        "interest": 233.42
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 2,
  "totalPages": 1
}
```

### Reverse Payment
Reverses a completed payment.

```http
POST /payments/{paymentId}/reverse
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "reason": "CUSTOMER_REQUEST",
  "reversalNotes": "Payment made in error",
  "reversedBy": "agent_456"
}
```

## AI Assistant API

### Natural Language Query
Processes natural language queries about loans and customers.

```http
POST /ai/query
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "query": "What loans are eligible for early payment discount?",
  "context": {
    "customerId": 12345,
    "includeHistory": true
  }
}
```

#### Response
```json
{
  "queryId": "ai_query_789",
  "interpretation": {
    "intent": "LOAN_INQUIRY",
    "entities": {
      "topic": "early_payment_discount",
      "scope": "customer_specific"
    }
  },
  "response": {
    "message": "Based on your loan portfolio, you have 2 loans eligible for early payment discounts:",
    "data": [
      {
        "loanId": 98765,
        "currentBalance": 48500.00,
        "discountAmount": 485.00,
        "discountPercentage": 1.0,
        "validUntil": "2024-03-31"
      }
    ],
    "suggestions": [
      "Make a payment of $48,015 to save $485",
      "Set up automatic payments to qualify for additional discounts"
    ]
  },
  "confidence": 0.95
}
```

### Loan Recommendation
Gets AI-powered loan recommendations.

```http
POST /ai/recommendations
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "customerId": 12345,
  "purpose": "DEBT_CONSOLIDATION",
  "requestedAmount": 30000.00,
  "preferences": {
    "maxMonthlyPayment": 600.00,
    "preferredTerm": 60
  }
}
```

## Error Handling

The API uses standard HTTP status codes and returns detailed error information.

### Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format",
        "rejectedValue": "invalid-email"
      }
    ],
    "timestamp": "2024-01-15T10:30:00Z",
    "path": "/api/v1/customers",
    "requestId": "req_123456789"
  }
}
```

### Common Error Codes
| Status Code | Error Code | Description |
|-------------|------------|-------------|
| 400 | VALIDATION_ERROR | Request validation failed |
| 401 | UNAUTHORIZED | Missing or invalid authentication |
| 403 | FORBIDDEN | Insufficient permissions |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | Resource already exists |
| 422 | BUSINESS_RULE_VIOLATION | Business rule validation failed |
| 429 | RATE_LIMIT_EXCEEDED | Too many requests |
| 500 | INTERNAL_ERROR | Internal server error |
| 503 | SERVICE_UNAVAILABLE | Service temporarily unavailable |

## Rate Limiting

The API implements rate limiting to ensure fair usage and system stability.

### Rate Limit Headers
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642255200
```

### Rate Limits by Tier
| Tier | Requests/Hour | Burst |
|------|---------------|-------|
| Basic | 1,000 | 50/min |
| Standard | 10,000 | 200/min |
| Premium | 100,000 | 1000/min |

### Rate Limit Exceeded Response
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "API rate limit exceeded",
    "retryAfter": 3600,
    "limit": 1000,
    "reset": "2024-01-15T11:00:00Z"
  }
}
```

## Webhooks

The system can send webhooks for important events.

### Webhook Events
- `customer.created`
- `customer.updated`
- `customer.status_changed`
- `loan.created`
- `loan.approved`
- `loan.disbursed`
- `loan.status_changed`
- `payment.processed`
- `payment.failed`
- `payment.reversed`

### Webhook Payload Format
```json
{
  "id": "webhook_123456",
  "event": "loan.approved",
  "created": "2024-01-15T10:30:00Z",
  "data": {
    "loanId": 98765,
    "customerId": 12345,
    "amount": 50000.00,
    "status": "APPROVED"
  },
  "signature": "sha256=abcdef123456..."
}
```

### Webhook Security
Webhooks are signed using HMAC-SHA256. Verify the signature:

```python
import hmac
import hashlib

def verify_webhook(payload, signature, secret):
    expected = hmac.new(
        secret.encode('utf-8'),
        payload.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return hmac.compare_digest(
        f"sha256={expected}",
        signature
    )
```

## API SDKs

Official SDKs are available for:
- Java
- Python
- JavaScript/TypeScript
- Go
- .NET

### Java Example
```java
LoanManagementClient client = LoanManagementClient.builder()
    .clientId("your_client_id")
    .clientSecret("your_client_secret")
    .environment(Environment.PRODUCTION)
    .build();

Customer customer = client.customers()
    .create(CreateCustomerRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .build());
```

## API Changelog

### Version 1.2.0 (2024-01-15)
- Added AI Assistant endpoints
- Enhanced security with DPoP support
- Improved error responses

### Version 1.1.0 (2023-12-01)
- Added webhook support
- Enhanced payment API
- Added batch operations

### Version 1.0.0 (2023-10-01)
- Initial release
- Customer management
- Loan origination
- Payment processing