# API Reference Guide - Enterprise Loan Management System

## 📋 Overview

This document provides comprehensive API documentation for the Enterprise Loan Management System with FAPI 2.0 + DPoP security implementation.

**Base URL**: `https://api.banking.example.com`  
**Security Profile**: FAPI 2.0 + DPoP (RFC 9449)  
**Authentication**: OAuth 2.1 with DPoP-bound tokens  
**API Version**: v1  

---

## 🔒 Security Requirements

### Required Headers for All API Calls

```http
Authorization: DPoP <access-token>
DPoP: <dpop-proof-jwt>
X-FAPI-Interaction-ID: <uuid>
X-FAPI-Auth-Date: <rfc7231-date>
X-FAPI-Customer-IP-Address: <client-ip>
Content-Type: application/json
```

### DPoP Proof Structure (RFC 9449)

```json
{
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": {
    "kty": "EC",
    "crv": "P-256",
    "x": "...",
    "y": "..."
  }
}
```

**Claims**:
```json
{
  "jti": "<unique-identifier>",
  "htm": "POST",
  "htu": "https://api.banking.example.com/api/v1/loans",
  "iat": 1640995200,
  "ath": "<access-token-hash>"
}
```

---

## 🏦 Loan Management API

### Create Loan Application

Creates a new loan application with banking regulatory compliance.

**Endpoint**: `POST /api/v1/loans`  
**Authorization**: `LOAN_OFFICER`, `SENIOR_LOAN_OFFICER`  
**Idempotency**: Required (`X-Idempotency-Key` header)

#### Request

```http
POST /api/v1/loans
Authorization: DPoP eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjJXS1FQa2Z5REJBNTJUZGZ1SzJVcDY2WUNhWGJ2OXBGa08tUVQ5U1phRSIsInkiOiJFd1JOaTRtaDE4RElTbkFtTG1BeERIUkdTX2ZtOXMwcFVIXzFzd1hqU0pZIn19.eyJqdGkiOiJTZGZHc2RmZ3NkZmdTZGZHIiwiaHRtIjoiUE9TVCIsImh0dSI6Imh0dHBzOi8vYXBpLmJhbmtpbmcuZXhhbXBsZS5jb20vYXBpL3YxL2xvYW5zIiwiaWF0IjoxNjQwOTk1MjAwLCJhdGgiOiJmTUF1OVhoVTZKeFQ3SjQ2cmJFVlBXa0ZLdGJleFBaU1lQcElleG5wN0JzIn0.BNaOI0DJdYJNn5TgcSEOjJx8Z3P3L7A3J7X0Qrx5tHX1ZQNJ5oP3lYPJZfGsT1J5b3TGdFjH7L5uQ9DfGdJ1
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440000
X-FAPI-Auth-Date: Mon, 06 Jan 2025 12:00:00 GMT
X-FAPI-Customer-IP-Address: 192.168.1.100
X-Idempotency-Key: loan-app-2025-001-abc123
Content-Type: application/json

{
  "customerId": "CUST001",
  "amount": "50000.00",
  "currency": "USD",
  "interestRate": 5.25,
  "installmentCount": 60,
  "loanType": "PERSONAL",
  "purpose": "Home Improvement",
  "requestedStartDate": "2025-01-15",
  "collateral": {
    "type": "REAL_ESTATE",
    "value": "75000.00",
    "description": "Property at 123 Main St"
  },
  "applicantInformation": {
    "employment": {
      "employer": "Tech Corp Inc",
      "position": "Software Engineer",
      "monthlyIncome": "8500.00",
      "employmentDuration": 24
    },
    "creditScore": 750,
    "debtToIncomeRatio": 0.35
  }
}
```

#### Response

```http
HTTP/1.1 201 Created
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440000
X-Idempotency-Key: loan-app-2025-001-abc123
Content-Type: application/json

{
  "loanId": "LOAN-2025-001234",
  "status": "PENDING_REVIEW",
  "applicationDate": "2025-01-06T12:00:00Z",
  "customerId": "CUST001",
  "amount": "50000.00",
  "currency": "USD",
  "interestRate": 5.25,
  "installmentCount": 60,
  "monthlyPayment": "945.23",
  "totalRepayment": "56713.80",
  "loanType": "PERSONAL",
  "purpose": "Home Improvement",
  "nextSteps": [
    "Document verification required",
    "Credit check in progress",
    "Underwriter assignment pending"
  ],
  "estimatedDecisionDate": "2025-01-10T17:00:00Z",
  "complianceInfo": {
    "tilaDisclosureRequired": true,
    "coolingOffPeriod": "3 days",
    "regulatoryNotices": [
      "TILA disclosure will be provided within 3 business days",
      "Right to rescind within 3 days of loan closing"
    ]
  }
}
```

### Get Loan Details

Retrieves detailed information about a specific loan.

**Endpoint**: `GET /api/v1/loans/{loanId}`  
**Authorization**: `CUSTOMER` (own loans), `LOAN_OFFICER`, `SENIOR_LOAN_OFFICER`

#### Request

```http
GET /api/v1/loans/LOAN-2025-001234
Authorization: DPoP eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjJXS1FQa2Z5REJBNTJUZGZ1SzJVcDY2WUNhWGJ2OXBGa08tUVQ5U1phRSIsInkiOiJFd1JOaTRtaDE4RElTbkFtTG1BeERIUkdTX2ZtOXMwcFVIXzFzd1hqU0pZIn19.eyJqdGkiOiJTZGZHc2RmZ3NkZmdTZGZHMSIsImh0bSI6IkdFVCIsImh0dSI6Imh0dHBzOi8vYXBpLmJhbmtpbmcuZXhhbXBsZS5jb20vYXBpL3YxL2xvYW5zL0xPQU4tMjAyNS0wMDEyMzQiLCJpYXQiOjE2NDA5OTUyMDAsImF0aCI6ImZNQXU5WGhVNkp4VDdKNDZyYkVWUFdrRkt0YmV4UFpTWVBwSWV4bnA3QnMifQ.CObJLJeZKIoJOp6TgcSEOjJx8Z3P3L7A3J7X0Qrx5tHX1ZQNJ5oP3lYPJZfGsT1J5b3TGdFjH7L5uQ9DfGdJ2
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440001
X-FAPI-Auth-Date: Mon, 06 Jan 2025 12:05:00 GMT
X-FAPI-Customer-IP-Address: 192.168.1.100
```

#### Response

```http
HTTP/1.1 200 OK
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440001
Content-Type: application/json

{
  "loanId": "LOAN-2025-001234",
  "status": "ACTIVE",
  "applicationDate": "2025-01-06T12:00:00Z",
  "approvalDate": "2025-01-10T15:30:00Z",
  "disbursementDate": "2025-01-15T10:00:00Z",
  "customerId": "CUST001",
  "amount": "50000.00",
  "currency": "USD",
  "interestRate": 5.25,
  "installmentCount": 60,
  "monthlyPayment": "945.23",
  "remainingBalance": "48500.00",
  "nextPaymentDate": "2025-02-15",
  "nextPaymentAmount": "945.23",
  "paymentHistory": {
    "totalPaymentsMade": 2,
    "totalAmountPaid": "1890.46",
    "lastPaymentDate": "2025-01-15T14:30:00Z",
    "lastPaymentAmount": "945.23"
  },
  "paymentBreakdown": {
    "principalPaid": "1500.00",
    "interestPaid": "390.46",
    "feesPaid": "0.00"
  },
  "complianceInfo": {
    "regulationCompliance": {
      "tila": "COMPLIANT",
      "respa": "COMPLIANT",
      "fdcpa": "COMPLIANT"
    },
    "auditTrail": {
      "lastAuditDate": "2025-01-05T09:00:00Z",
      "nextAuditDate": "2025-04-05T09:00:00Z",
      "complianceOfficer": "Jane Smith"
    }
  }
}
```

### Process Payment

Processes a payment against a loan with FDCPA-compliant allocation waterfall.

**Endpoint**: `POST /api/v1/loans/{loanId}/payments`  
**Authorization**: `CUSTOMER` (own loans), `LOAN_OFFICER`  
**Idempotency**: Required (`X-Idempotency-Key` header)

#### Request

```http
POST /api/v1/loans/LOAN-2025-001234/payments
Authorization: DPoP eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjJXS1FQa2Z5REJBNTJUZGZ1SzJVcDY2WUNhWGJ2OXBGa08tUVQ5U1phRSIsInkiOiJFd1JOaTRtaDE4RElTbkFtTG1BeERIUkdTX2ZtOXMwcFVIXzFzd1hqU0pZIn19.eyJqdGkiOiJTZGZHc2RmZ3NkZmdTZGZHMyIsImh0bSI6IlBPU1QiLCJodHUiOiJodHRwczovL2FwaS5iYW5raW5nLmV4YW1wbGUuY29tL2FwaS92MS9sb2Fucy9MT0FOLTIwMjUtMDAxMjM0L3BheW1lbnRzIiwiaWF0IjoxNjQwOTk1MjAwLCJhdGgiOiJmTUF1OVhoVTZKeFQ3SjQ2cmJFVlBXa0ZLdGJleFBaU1lQcElleG5wN0JzIn0.DPeKMOeZKIoJOp6TgcSEOjJx8Z3P3L7A3J7X0Qrx5tHX1ZQNJ5oP3lYPJZfGsT1J5b3TGdFjH7L5uQ9DfGdJ3
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440002
X-FAPI-Auth-Date: Mon, 06 Jan 2025 12:10:00 GMT
X-FAPI-Customer-IP-Address: 192.168.1.100
X-Idempotency-Key: payment-2025-001-xyz789
Content-Type: application/json

{
  "amount": "1000.00",
  "currency": "USD",
  "paymentType": "REGULAR",
  "paymentChannel": "ONLINE",
  "paymentMethod": {
    "type": "BANK_TRANSFER",
    "accountNumber": "****1234",
    "routingNumber": "021000021",
    "bankName": "Example Bank"
  },
  "scheduledDate": "2025-01-15",
  "notes": "Early payment to reduce interest"
}
```

#### Response

```http
HTTP/1.1 201 Created
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440002
X-Idempotency-Key: payment-2025-001-xyz789
Content-Type: application/json

{
  "paymentId": "PAY-2025-005678",
  "status": "PROCESSED",
  "processedDate": "2025-01-15T14:30:00Z",
  "amount": "1000.00",
  "currency": "USD",
  "paymentAllocation": {
    "fees": "0.00",
    "interest": "187.50",
    "principal": "812.50"
  },
  "remainingBalance": "47687.50",
  "nextPaymentDate": "2025-02-15",
  "nextPaymentAmount": "945.23",
  "fdcpaCompliance": {
    "allocationMethod": "WATERFALL",
    "allocationOrder": [
      "Processing Fees",
      "Late Fees", 
      "Interest",
      "Principal"
    ],
    "auditTrail": "Payment allocated per FDCPA requirements"
  },
  "transactionReference": "TXN-2025-112233"
}
```

---

## 🤖 AI Assistant API

### Loan Application Analysis

Performs comprehensive AI analysis of loan applications using ML models.

**Endpoint**: `POST /api/v1/ai/analyze/loan-application`  
**Authorization**: `LOAN_OFFICER`, `UNDERWRITER`

#### Request

```http
POST /api/v1/ai/analyze/loan-application
Authorization: DPoP eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjJXS1FQa2Z5REJBNTJUZGZ1SzJVcDY2WUNhWGJ2OXBGa08tUVQ5U1phRSIsInkiOiJFd1JOaTRtaDE4RElTbkFtTG1BeERIUkdTX2ZtOXMwcFVIXzFzd1hqU0pZIn19.eyJqdGkiOiJTZGZHc2RmZ3NkZmdTZGZHNCIsImh0bSI6IlBPU1QiLCJodHUiOiJodHRwczovL2FwaS5iYW5raW5nLmV4YW1wbGUuY29tL2FwaS92MS9haS9hbmFseXplL2xvYW4tYXBwbGljYXRpb24iLCJpYXQiOjE2NDA5OTUyMDAsImF0aCI6ImZNQXU5WGhVNkp4VDdKNDZyYkVWUFdrRkt0YmV4UFpTWVBwSWV4bnA3QnMifQ.EPfLNPeZKIoJOp6TgcSEOjJx8Z3P3L7A3J7X0Qrx5tHX1ZQNJ5oP3lYPJZfGsT1J5b3TGdFjH7L5uQ9DfGdJ4
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440003
X-FAPI-Auth-Date: Mon, 06 Jan 2025 12:15:00 GMT
X-FAPI-Customer-IP-Address: 192.168.1.100
Content-Type: application/json

{
  "loanApplicationId": "LOAN-2025-001234",
  "analysisType": "COMPREHENSIVE",
  "includeRiskAssessment": true,
  "includeFraudDetection": true,
  "includeComplianceCheck": true,
  "customerData": {
    "creditScore": 750,
    "annualIncome": 102000,
    "debtToIncomeRatio": 0.35,
    "employmentHistory": 24,
    "homeOwnership": "RENT"
  },
  "loanData": {
    "requestedAmount": 50000,
    "loanPurpose": "HOME_IMPROVEMENT",
    "loanTerm": 60,
    "interestRate": 5.25
  }
}
```

#### Response

```http
HTTP/1.1 200 OK
X-FAPI-Interaction-ID: 550e8400-e29b-41d4-a716-446655440003
Content-Type: application/json

{
  "analysisId": "AI-ANALYSIS-2025-001",
  "analysisDate": "2025-01-06T12:15:30Z",
  "overallRecommendation": "APPROVE",
  "confidence": 0.87,
  "riskAssessment": {
    "riskScore": "LOW",
    "riskLevel": 2.3,
    "riskFactors": [
      {
        "factor": "Credit Score",
        "impact": "POSITIVE",
        "weight": 0.25,
        "description": "Credit score of 750 indicates excellent creditworthiness"
      },
      {
        "factor": "Debt-to-Income Ratio",
        "impact": "POSITIVE", 
        "weight": 0.20,
        "description": "DTI of 35% is within acceptable range"
      },
      {
        "factor": "Employment Stability",
        "impact": "POSITIVE",
        "weight": 0.15,
        "description": "24 months employment history shows stability"
      }
    ],
    "probabilityOfDefault": 0.034
  },
  "fraudDetection": {
    "fraudRisk": "LOW",
    "fraudScore": 0.12,
    "alerts": [],
    "verificationRecommendations": [
      "Standard income verification sufficient",
      "No additional documentation required"
    ]
  },
  "complianceCheck": {
    "tilaCompliance": "PASS",
    "respaCompliance": "PASS", 
    "fdcpaCompliance": "PASS",
    "fairLendingCompliance": "PASS",
    "regulatoryNotes": [
      "Application meets all regulatory requirements",
      "No discriminatory patterns detected"
    ]
  },
  "recommendations": {
    "approvalRecommendation": "APPROVE",
    "suggestedTerms": {
      "interestRate": 5.25,
      "loanAmount": 50000,
      "term": 60,
      "monthlyPayment": 945.23
    },
    "conditions": [
      "Standard income verification",
      "Property assessment required for collateral"
    ]
  },
  "mlModelInfo": {
    "primaryModel": "LoanApprovalModel_v2.1",
    "modelVersion": "2024.12.15",
    "trainingDataDate": "2024-12-01",
    "features": 47,
    "accuracy": 0.894
  }
}
```

---

## 🔐 OAuth 2.1 & Security Endpoints

### Pushed Authorization Request (PAR)

Initiates FAPI 2.0 compliant authorization flow.

**Endpoint**: `POST /oauth2/par`  
**Authentication**: Private Key JWT client authentication

#### Request

```http
POST /oauth2/par
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNsaWVudC1qd3Qta2V5LWlkIn0...

client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion=eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNsaWVudC1qd3Qta2V5LWlkIn0.eyJpc3MiOiJlbnRlcnByaXNlLWJhbmtpbmctYXBwIiwic3ViIjoiZW50ZXJwcmlzZS1iYW5raW5nLWFwcCIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5MC9yZWFsbXMvYmFua2luZy1mYXBpMi9wcm90b2NvbC9vcGVuaWRfY29ubmVjdC90b2tlbiIsImp0aSI6InVuaXF1ZS1qd3QtaWQtMTIzIiwiZXhwIjoxNjQwOTk1NTAwLCJpYXQiOjE2NDA5OTUyMDB9.LKj8KsKjNsHjJk1jN...
&response_type=code
&client_id=enterprise-banking-app
&redirect_uri=https://banking.example.com/callback
&scope=openid profile banking-scope banking-loans banking-payments
&state=xyz789abc123
&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
&code_challenge_method=S256
&dpop_jkt={"kty":"EC","crv":"P-256","x":"f2WKQP...","y":"EwRNi4..."}
```

#### Response

```http
HTTP/1.1 201 Created
Cache-Control: no-cache, no-store
Content-Type: application/json

{
  "request_uri": "urn:ietf:params:oauth:request_uri:6esc_11ACC5bwc014ltc14eY22c",
  "expires_in": 300
}
```

### Token Exchange

Exchanges authorization code for DPoP-bound access token.

**Endpoint**: `POST /oauth2/token`  
**Authentication**: Private Key JWT + DPoP proof

#### Request

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjJXS1FQa2Z5REJBNTJUZGZ1SzJVcDY2WUNhWGJ2OXBGa08tUVQ5U1phRSIsInkiOiJFd1JOaTRtaDE4RElTbkFtTG1BeERIUkdTX2ZtOXMwcFVIXzFzd1hqU0pZIn19.eyJqdGkiOiJTZGZHc2RmZ3NkZmdTZGZHNSIsImh0bSI6IlBPU1QiLCJodHUiOiJodHRwczovL2FwaS5iYW5raW5nLmV4YW1wbGUuY29tL29hdXRoMi90b2tlbiIsImlhdCI6MTY0MDk5NTIwMH0.FQgMOPeZKIoJOp6TgcSEOjJx8Z3P3L7A3J7X0Qrx5tHX1ZQNJ5oP3lYPJZfGsT1J5b3TGdFjH7L5uQ9DfGdJ5

grant_type=authorization_code
&code=authorization_code_received_from_par
&redirect_uri=https://banking.example.com/callback
&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion=eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNsaWVudC1qd3Qta2V5LWlkIn0...
&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

#### Response

```http
HTTP/1.1 200 OK
Cache-Control: no-cache, no-store
Content-Type: application/json

{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGSjg2R2NGM2pUYk5PY29oc051NmQ...",
  "token_type": "DPoP",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJmYWtlLXJlZnJlc2gta2V5In0...",
  "refresh_expires_in": 28800,
  "scope": "openid profile banking-scope banking-loans banking-payments",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGSjg2R2NGM2pUYk5PY29oc051NmQ..."
}
```

---

## 📊 Error Handling

### Standard Error Response Format

```json
{
  "error": "invalid_request",
  "error_description": "The request is missing a required parameter",
  "error_uri": "https://docs.banking.example.com/errors#invalid_request",
  "timestamp": "2025-01-06T12:00:00Z",
  "interaction_id": "550e8400-e29b-41d4-a716-446655440000",
  "trace_id": "b7ad6b7169203331"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `invalid_request` | 400 | Malformed request or missing required parameters |
| `invalid_dpop_proof` | 400 | DPoP proof validation failed |
| `invalid_fapi_headers` | 400 | Required FAPI headers missing or invalid |
| `unauthorized` | 401 | Authentication required or invalid credentials |
| `insufficient_scope` | 403 | Access token lacks required scope |
| `forbidden` | 403 | Operation not permitted for current user |
| `not_found` | 404 | Requested resource not found |
| `duplicate_request` | 409 | Idempotency key already used |
| `rate_limit_exceeded` | 429 | Too many requests |
| `internal_server_error` | 500 | Unexpected server error |

### FAPI-Specific Errors

```json
{
  "error": "invalid_fapi_headers",
  "error_description": "Missing required X-FAPI-Interaction-ID header",
  "required_headers": [
    "X-FAPI-Interaction-ID",
    "X-FAPI-Auth-Date", 
    "X-FAPI-Customer-IP-Address"
  ],
  "timestamp": "2025-01-06T12:00:00Z",
  "interaction_id": null
}
```

### DPoP-Specific Errors

```json
{
  "error": "invalid_dpop_proof",
  "error_description": "DPoP proof JTI has already been used",
  "error_details": {
    "validation_failures": [
      {
        "field": "jti",
        "reason": "replay_detected",
        "description": "JTI 'SdfGsdfgsdgfSdfG' was already used within the last 60 seconds"
      }
    ]
  },
  "timestamp": "2025-01-06T12:00:00Z",
  "interaction_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 🔄 Rate Limiting

### Rate Limit Headers

All responses include rate limiting information:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995260
X-RateLimit-Retry-After: 60
```

### Rate Limits by Endpoint Category

| Category | Requests per Minute | Burst Capacity |
|----------|-------------------|----------------|
| Loan Operations | 50 | 10 |
| Payment Processing | 30 | 5 |
| AI Analysis | 20 | 3 |
| OAuth Token | 10 | 2 |
| General API | 100 | 20 |

---

## 📋 Compliance & Audit

### Audit Information

All API operations generate comprehensive audit logs including:

- **FAPI Interaction ID**: Unique request identifier
- **User Authorization Context**: Role and permissions
- **DPoP Token Binding**: Cryptographic proof validation
- **Regulatory Compliance**: FDCPA, TILA, RESPA adherence
- **Security Events**: Authentication, authorization, violations

### Regulatory Compliance

- **FDCPA**: Payment allocation waterfall automatically applied
- **TILA**: Truth in Lending disclosures provided with loan operations
- **RESPA**: Settlement procedure compliance for real estate loans
- **PCI DSS**: Payment card security standards maintained

---

## 📖 Additional Resources

- **FAPI 2.0 Specification**: [OpenID FAPI 2.0 Security Profile](https://openid.net/specs/fapi-2_0-security-profile.html)
- **DPoP RFC 9449**: [Demonstrating Proof-of-Possession](https://datatracker.ietf.org/doc/html/rfc9449)
- **OAuth 2.1**: [OAuth 2.1 Authorization Framework](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1)
- **Banking Regulations**: Internal compliance documentation
- **API Sandbox**: Test environment available at `https://sandbox.banking.example.com`

---

**Document Version**: 1.0  
**Last Updated**: January 2025  
**Security Profile**: FAPI 2.0 + DPoP  
**Compliance Level**: Banking Grade