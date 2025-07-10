# API Reference Guide
## Enterprise Loan Management Platform

### Version 1.0 | January 2025

---

## Overview

This document provides the complete API specification for the Enterprise Loan Management Platform. All APIs follow RESTful principles and implement financial-grade security standards.

**Production Environment**: `https://api.loanplatform.com`  
**Staging Environment**: `https://api-staging.loanplatform.com`  
**API Version**: v1  
**Security Standard**: FAPI 2.0 with DPoP  

## Security Requirements

### Authentication

All API requests require OAuth 2.1 authentication with DPoP (Demonstrating Proof of Possession) token binding.

**Required Headers**
```http
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-Request-ID: <uuid>
X-Client-ID: <client-identifier>
Content-Type: application/json
```

### DPoP Implementation

DPoP tokens must include:
- Unique JWT identifier (jti)
- HTTP method (htm)
- Target URI (htu)
- Timestamp (iat)
- Access token hash (ath)

Example DPoP proof:
```json
{
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": {
    "kty": "EC",
    "crv": "P-256",
    "x": "base64url-encoded-x-coordinate",
    "y": "base64url-encoded-y-coordinate"
  }
}
```

## Customer Management APIs

### Create Customer

Creates a new customer profile with KYC verification.

**Endpoint**: `POST /api/v1/customers`

**Request Body**:
```json
{
  "personalInfo": {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1985-03-15",
    "ssn": "XXX-XX-1234",
    "email": "john.doe@example.com",
    "phone": "+1-555-123-4567"
  },
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "zipCode": "62701",
    "country": "US"
  },
  "employment": {
    "employer": "Acme Corp",
    "position": "Senior Engineer",
    "annualIncome": 85000,
    "yearsEmployed": 3
  }
}
```

**Response**: `201 Created`
```json
{
  "customerId": "cust_2024_abc123",
  "status": "PENDING_VERIFICATION",
  "createdAt": "2025-01-09T10:30:00Z",
  "verificationRequired": {
    "identity": true,
    "income": true,
    "address": false
  }
}
```

### Get Customer Profile

Retrieves complete customer information.

**Endpoint**: `GET /api/v1/customers/{customerId}`

**Response**: `200 OK`
```json
{
  "customerId": "cust_2024_abc123",
  "personalInfo": {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1985-03-15",
    "email": "john.doe@example.com"
  },
  "creditProfile": {
    "score": 750,
    "rating": "EXCELLENT",
    "lastUpdated": "2025-01-01T00:00:00Z"
  },
  "status": "ACTIVE",
  "memberSince": "2024-01-15T00:00:00Z"
}
```

## Loan Origination APIs

### Submit Loan Application

Initiates a new loan application.

**Endpoint**: `POST /api/v1/loans/applications`

**Request Body**:
```json
{
  "customerId": "cust_2024_abc123",
  "loanDetails": {
    "type": "PERSONAL",
    "purpose": "DEBT_CONSOLIDATION",
    "requestedAmount": 25000,
    "termMonths": 60
  },
  "collateral": null,
  "coborrowers": []
}
```

**Response**: `201 Created`
```json
{
  "applicationId": "app_2025_xyz789",
  "status": "UNDER_REVIEW",
  "submittedAt": "2025-01-09T11:00:00Z",
  "nextSteps": [
    "CREDIT_CHECK",
    "INCOME_VERIFICATION",
    "RISK_ASSESSMENT"
  ],
  "estimatedDecisionTime": "2025-01-09T11:15:00Z"
}
```

### Get Loan Decision

Retrieves the credit decision for an application.

**Endpoint**: `GET /api/v1/loans/applications/{applicationId}/decision`

**Response**: `200 OK`
```json
{
  "applicationId": "app_2025_xyz789",
  "decision": "APPROVED",
  "approvedAmount": 25000,
  "terms": {
    "apr": 8.99,
    "termMonths": 60,
    "monthlyPayment": 518.96,
    "originationFee": 0
  },
  "conditions": [],
  "offerExpiresAt": "2025-01-16T11:00:00Z"
}
```

### Accept Loan Offer

Accepts the approved loan terms.

**Endpoint**: `POST /api/v1/loans/applications/{applicationId}/accept`

**Request Body**:
```json
{
  "acceptedTerms": true,
  "disbursementAccount": {
    "accountNumber": "****5678",
    "routingNumber": "****4321",
    "accountType": "CHECKING"
  },
  "agreedToTermsAt": "2025-01-09T11:30:00Z"
}
```

**Response**: `200 OK`
```json
{
  "loanId": "loan_2025_def456",
  "status": "PENDING_DISBURSEMENT",
  "disbursementDate": "2025-01-10T00:00:00Z",
  "firstPaymentDate": "2025-02-15T00:00:00Z"
}
```

## Payment Processing APIs

### Make Payment

Processes a loan payment.

**Endpoint**: `POST /api/v1/payments`

**Request Body**:
```json
{
  "loanId": "loan_2025_def456",
  "amount": 518.96,
  "paymentMethod": {
    "type": "ACH",
    "accountNumber": "****5678",
    "routingNumber": "****4321"
  },
  "scheduledDate": "2025-02-15"
}
```

**Response**: `201 Created`
```json
{
  "paymentId": "pay_2025_ghi789",
  "status": "SCHEDULED",
  "amount": 518.96,
  "scheduledDate": "2025-02-15",
  "allocation": {
    "principal": 418.96,
    "interest": 100.00,
    "fees": 0
  }
}
```

### Get Payment History

Retrieves payment history for a loan.

**Endpoint**: `GET /api/v1/loans/{loanId}/payments`

**Query Parameters**:
- `startDate` - Filter by start date (ISO 8601)
- `endDate` - Filter by end date (ISO 8601)
- `limit` - Number of records (default: 20, max: 100)
- `offset` - Pagination offset

**Response**: `200 OK`
```json
{
  "loanId": "loan_2025_def456",
  "payments": [
    {
      "paymentId": "pay_2025_ghi789",
      "date": "2025-02-15",
      "amount": 518.96,
      "status": "COMPLETED",
      "allocation": {
        "principal": 418.96,
        "interest": 100.00
      }
    }
  ],
  "summary": {
    "totalPaid": 1037.92,
    "principalPaid": 837.92,
    "interestPaid": 200.00,
    "remainingBalance": 24162.08
  },
  "pagination": {
    "total": 2,
    "limit": 20,
    "offset": 0
  }
}
```

## Loan Servicing APIs

### Get Loan Details

Retrieves comprehensive loan information.

**Endpoint**: `GET /api/v1/loans/{loanId}`

**Response**: `200 OK`
```json
{
  "loanId": "loan_2025_def456",
  "customerId": "cust_2024_abc123",
  "originalAmount": 25000,
  "currentBalance": 24162.08,
  "terms": {
    "apr": 8.99,
    "termMonths": 60,
    "monthlyPayment": 518.96
  },
  "status": "CURRENT",
  "disbursedDate": "2025-01-10",
  "maturityDate": "2030-01-10",
  "nextPaymentDue": {
    "date": "2025-03-15",
    "amount": 518.96
  }
}
```

### Request Payoff Quote

Generates a payoff quote for early loan closure.

**Endpoint**: `POST /api/v1/loans/{loanId}/payoff-quote`

**Request Body**:
```json
{
  "payoffDate": "2025-03-01"
}
```

**Response**: `200 OK`
```json
{
  "quoteId": "quote_2025_jkl012",
  "loanId": "loan_2025_def456",
  "payoffAmount": 24087.54,
  "breakdown": {
    "principal": 24162.08,
    "accruedInterest": 75.46,
    "prepaymentPenalty": 0,
    "credits": -150.00
  },
  "validThrough": "2025-03-01T23:59:59Z",
  "instructions": {
    "paymentMethods": ["WIRE", "ACH", "CHECK"],
    "processingTime": "1-2 business days"
  }
}
```

## Risk Management APIs

### Credit Check

Performs real-time credit assessment.

**Endpoint**: `POST /api/v1/risk/credit-check`

**Request Body**:
```json
{
  "customerId": "cust_2024_abc123",
  "consentProvided": true,
  "purpose": "LOAN_APPLICATION"
}
```

**Response**: `200 OK`
```json
{
  "checkId": "chk_2025_mno345",
  "creditScore": 750,
  "rating": "EXCELLENT",
  "factors": [
    {
      "code": "01",
      "description": "Long credit history"
    },
    {
      "code": "08",
      "description": "Low credit utilization"
    }
  ],
  "recommendation": "APPROVE",
  "maxQualifiedAmount": 50000
}
```

## Error Handling

### Error Response Format

All errors follow a consistent format:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": [
      {
        "field": "loanDetails.requestedAmount",
        "message": "Amount must be between 1000 and 100000"
      }
    ],
    "timestamp": "2025-01-09T12:00:00Z",
    "requestId": "req_abc123"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| UNAUTHORIZED | 401 | Invalid or missing authentication |
| FORBIDDEN | 403 | Insufficient permissions |
| NOT_FOUND | 404 | Resource not found |
| VALIDATION_ERROR | 400 | Request validation failed |
| BUSINESS_ERROR | 422 | Business rule violation |
| RATE_LIMITED | 429 | Too many requests |
| SERVER_ERROR | 500 | Internal server error |

## Rate Limiting

API rate limits are enforced per client:

- **Standard tier**: 1,000 requests per hour
- **Premium tier**: 10,000 requests per hour
- **Enterprise tier**: Custom limits

Rate limit headers:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640998800
```

## Webhooks

### Event Notifications

Configure webhooks to receive real-time event notifications:

**Supported Events**:
- `loan.approved`
- `loan.disbursed`
- `payment.completed`
- `payment.failed`
- `customer.verified`

**Webhook Payload**:
```json
{
  "eventType": "loan.approved",
  "eventId": "evt_2025_pqr678",
  "timestamp": "2025-01-09T11:15:00Z",
  "data": {
    "loanId": "loan_2025_def456",
    "customerId": "cust_2024_abc123",
    "approvedAmount": 25000
  }
}
```

## Testing

### Sandbox Environment

Test environment with mock data:
- **URL**: `https://api-sandbox.loanplatform.com`
- **Test credentials**: Available in developer portal
- **Mock data**: Predefined test scenarios

### Test Credit Card Numbers

For payment testing:
- `4111 1111 1111 1111` - Successful payment
- `4000 0000 0000 0002` - Declined payment
- `4000 0000 0000 0019` - Invalid card

---

**Support**: api-support@loanplatform.com  
**Status Page**: status.loanplatform.com  
**Last Updated**: January 2025