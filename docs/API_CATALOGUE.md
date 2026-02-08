# Enterprise Banking Platform - API Catalogue

## Overview

This document provides comprehensive API documentation for the Enterprise Banking Platform, featuring both Traditional Banking and AmanahFi Islamic Finance endpoints. The platform implements DDD, CQRS, and Event Sourcing patterns with FAPI 2.0 security compliance.

## Security Framework

### Authentication & Authorization
- **FAPI 2.0 Compliance**: Financial-grade API security
- **DPoP (Demonstration of Proof of Possession)**: Token binding
- **OAuth 2.0 with PKCE**: Authorization code flow
- **JWT Bearer Tokens**: Stateless authentication
- **Sharia Compliance**: Islamic finance regulatory requirements

### Security Levels
- **Level 1**: Public endpoints (rate-limited)
- **Level 2**: Authenticated user endpoints
- **Level 3**: Privileged operations (MFA required)
- **Level 4**: Administrative operations (super-admin only)

---

## Traditional Banking APIs

### 1. Customer Management API

#### Create Customer
```http
POST /api/v1/customers
Content-Type: application/json
Authorization: Bearer {jwt_token}
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+971-50-123-4567",
  "annualIncome": 120000.00,
  "currency": "AED",
  "nationalId": "784-1234-5678901-2",
  "address": {
    "street": "Sheikh Zayed Road",
    "city": "Dubai",
    "emirate": "Dubai",
    "postalCode": "12345",
    "country": "UAE"
  }
}
```

**Response (201 Created):**
```json
{
  "customerId": "CUST-12345678",
  "status": "ACTIVE",
  "creditScore": 750,
  "approvedCreditLimit": 500000.00,
  "currency": "AED",
  "createdAt": "2024-01-15T10:30:00Z",
  "links": {
    "self": "/api/v1/customers/CUST-12345678",
    "creditProfile": "/api/v1/customers/CUST-12345678/credit-profile",
    "loans": "/api/v1/customers/CUST-12345678/loans"
  }
}
```

#### Get Customer Credit Profile
```http
GET /api/v1/customers/{customerId}/credit-profile
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "customerId": "CUST-12345678",
  "creditScore": 750,
  "creditLimit": 500000.00,
  "availableCredit": 350000.00,
  "utilizationRatio": 0.30,
  "paymentHistory": "EXCELLENT",
  "debtToIncomeRatio": 0.25,
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```

### 2. Loan Management API

#### Create Loan Application
```http
POST /api/v1/loans
Content-Type: application/json
Authorization: Bearer {jwt_token}
```

**Request Body:**
```json
{
  "customerId": "CUST-12345678",
  "loanType": "PERSONAL",
  "principalAmount": 100000.00,
  "currency": "AED",
  "interestRate": 5.25,
  "termMonths": 36,
  "purpose": "HOME_IMPROVEMENT",
  "collateral": {
    "type": "PROPERTY",
    "value": 150000.00,
    "description": "Residential property in Dubai Marina"
  }
}
```

**Response (201 Created):**
```json
{
  "loanId": "LOAN-87654321",
  "loanReference": "REF-2024-001234",
  "status": "PENDING_APPROVAL",
  "principalAmount": 100000.00,
  "totalAmount": 116273.44,
  "monthlyPayment": 3230.37,
  "interestRate": 5.25,
  "termMonths": 36,
  "currency": "AED",
  "createdAt": "2024-01-15T10:30:00Z",
  "expectedDecisionDate": "2024-01-17T10:30:00Z",
  "links": {
    "self": "/api/v1/loans/LOAN-87654321",
    "payments": "/api/v1/loans/LOAN-87654321/payments",
    "schedule": "/api/v1/loans/LOAN-87654321/schedule"
  }
}
```

#### Get Loan Details
```http
GET /api/v1/loans/{loanId}
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "loanId": "LOAN-87654321",
  "customerId": "CUST-12345678",
  "status": "ACTIVE",
  "principalAmount": 100000.00,
  "outstandingBalance": 78549.23,
  "totalAmount": 116273.44,
  "monthlyPayment": 3230.37,
  "interestRate": 5.25,
  "termMonths": 36,
  "paidInstallments": 12,
  "remainingInstallments": 24,
  "nextPaymentDate": "2024-02-15T00:00:00Z",
  "nextPaymentAmount": 3230.37,
  "currency": "AED",
  "disbursementDate": "2024-01-20T00:00:00Z",
  "maturityDate": "2024-01-20T00:00:00Z",
  "isOverdue": false,
  "overdueInstallments": 0,
  "totalInterest": 16273.44,
  "collateral": {
    "type": "PROPERTY",
    "value": 150000.00,
    "loanToValueRatio": 0.6667
  }
}
```

#### Make Loan Payment
```http
POST /api/v1/loans/{loanId}/payments
Content-Type: application/json
Authorization: Bearer {jwt_token}
```

**Request Body:**
```json
{
  "paymentAmount": 3230.37,
  "paymentMethod": "BANK_TRANSFER",
  "fromAccount": "ACC-11111111",
  "paymentDate": "2024-01-15T10:30:00Z",
  "description": "Monthly loan payment"
}
```

**Response (200 OK):**
```json
{
  "paymentId": "PAY-11111111",
  "loanId": "LOAN-87654321",
  "paymentAmount": 3230.37,
  "installmentsPaid": 1,
  "remainingBalance": 75318.86,
  "nextPaymentDate": "2024-02-15T00:00:00Z",
  "paymentStatus": "COMPLETED",
  "transactionId": "TXN-98765432",
  "currency": "AED",
  "processedAt": "2024-01-15T10:30:15Z"
}
```

### 3. Payment Processing API

#### Process Payment
```http
POST /api/v1/payments
Content-Type: application/json
Authorization: Bearer {jwt_token}
```

**Request Body:**
```json
{
  "customerId": "CUST-12345678",
  "fromAccount": "ACC-11111111",
  "toAccount": "ACC-22222222",
  "amount": 5000.00,
  "currency": "AED",
  "paymentType": "TRANSFER",
  "description": "Monthly transfer to savings"
}
```

**Response (200 OK):**
```json
{
  "paymentId": "PAY-33333333",
  "status": "COMPLETED",
  "transactionId": "TXN-44444444",
  "amount": 5000.00,
  "currency": "AED",
  "processedAt": "2024-01-15T10:30:15Z",
  "fromAccount": "ACC-11111111",
  "toAccount": "ACC-22222222",
  "remainingBalance": 45000.00
}
```

---

## AmanahFi Islamic Finance APIs

### 1. Murabaha Contract API

#### Create Murabaha Contract
```http
POST /api/v1/amanahfi/murabaha
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Sharia-Compliance: required
```

**Request Body:**
```json
{
  "customerId": "CUST-12345678",
  "assetType": "PROPERTY",
  "assetValue": 1000000.00,
  "currency": "AED",
  "profitRate": 4.5,
  "termMonths": 48,
  "downPayment": 200000.00,
  "assetDescription": "2-bedroom apartment in Dubai Marina",
  "shariaCompliance": {
    "certificateRequired": true,
    "boardApproval": true,
    "assetOwnership": "BANK_OWNED"
  }
}
```

**Response (201 Created):**
```json
{
  "contractId": "MUR-12345678",
  "contractReference": "AMF-2024-001234",
  "status": "SHARIA_REVIEW",
  "customerId": "CUST-12345678",
  "assetValue": 1000000.00,
  "financedAmount": 800000.00,
  "totalAmount": 944000.00,
  "profitAmount": 144000.00,
  "profitRate": 4.5,
  "monthlyPayment": 19666.67,
  "termMonths": 48,
  "currency": "AED",
  "expectedApprovalDate": "2024-01-20T00:00:00Z",
  "shariaCompliance": {
    "status": "UNDER_REVIEW",
    "boardMember": "Sheikh Abdullah Al-Mansoori",
    "reviewDate": "2024-01-16T00:00:00Z"
  },
  "links": {
    "self": "/api/v1/amanahfi/murabaha/MUR-12345678",
    "payments": "/api/v1/amanahfi/murabaha/MUR-12345678/payments",
    "sharia-certificate": "/api/v1/amanahfi/murabaha/MUR-12345678/sharia-certificate"
  }
}
```

#### Get Sharia Compliance Certificate
```http
GET /api/v1/amanahfi/murabaha/{contractId}/sharia-certificate
Authorization: Bearer {jwt_token}
Accept: application/pdf
```

**Response (200 OK):**
```json
{
  "certificateId": "SHC-87654321",
  "contractId": "MUR-12345678",
  "shariaBoard": "AmanahFi Sharia Supervisory Board",
  "issuedDate": "2024-01-18T00:00:00Z",
  "expiryDate": "2025-01-18T00:00:00Z",
  "boardMembers": [
    "Sheikh Abdullah Al-Mansoori (Chairman)",
    "Dr. Fatima Al-Zahra (Member)",
    "Sheikh Omar Al-Qasimi (Member)"
  ],
  "complianceStatus": "FULLY_COMPLIANT",
  "certificateUrl": "/api/v1/documents/sharia-certificates/SHC-87654321.pdf",
  "digitalSignature": "sha256:a1b2c3d4...",
  "blockchainHash": "0x1234567890abcdef..."
}
```

### 2. Ijarah (Lease) API

#### Create Ijarah Contract
```http
POST /api/v1/amanahfi/ijarah
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Sharia-Compliance: required
```

**Request Body:**
```json
{
  "customerId": "CUST-12345678",
  "assetType": "EQUIPMENT",
  "assetValue": 500000.00,
  "currency": "AED",
  "leaseTerm": 60,
  "monthlyRental": 10000.00,
  "purchaseOption": true,
  "residualValue": 50000.00,
  "assetDescription": "Industrial manufacturing equipment",
  "maintenanceResponsibility": "LESSEE"
}
```

**Response (201 Created):**
```json
{
  "contractId": "IJR-12345678",
  "contractReference": "AMF-IJR-2024-001234",
  "status": "SHARIA_APPROVED",
  "customerId": "CUST-12345678",
  "assetValue": 500000.00,
  "totalRental": 600000.00,
  "monthlyRental": 10000.00,
  "leaseTerm": 60,
  "currency": "AED",
  "purchaseOption": true,
  "residualValue": 50000.00,
  "startDate": "2024-02-01T00:00:00Z",
  "endDate": "2024-02-01T00:00:00Z",
  "nextPaymentDate": "2024-02-01T00:00:00Z",
  "shariaCompliance": {
    "status": "APPROVED",
    "approvedBy": "Sheikh Abdullah Al-Mansoori",
    "approvalDate": "2024-01-18T00:00:00Z",
    "certificateId": "SHC-87654322"
  }
}
```

### 3. Sukuk Investment API

#### Create Sukuk Investment
```http
POST /api/v1/amanahfi/sukuk
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Sharia-Compliance: required
```

**Request Body:**
```json
{
  "customerId": "CUST-12345678",
  "sukukType": "IJARAH",
  "investmentAmount": 100000.00,
  "currency": "AED",
  "expectedReturn": 6.5,
  "maturityPeriod": 24,
  "underlyingAsset": "Commercial real estate portfolio",
  "minimumInvestment": 10000.00,
  "distributionFrequency": "QUARTERLY"
}
```

**Response (201 Created):**
```json
{
  "sukukId": "SUK-12345678",
  "sukukReference": "AMF-SUK-2024-001234",
  "status": "ACTIVE",
  "customerId": "CUST-12345678",
  "investmentAmount": 100000.00,
  "certificateValue": 100000.00,
  "expectedReturn": 6.5,
  "maturityPeriod": 24,
  "currency": "AED",
  "issueDate": "2024-01-20T00:00:00Z",
  "maturityDate": "2024-01-20T00:00:00Z",
  "nextDistributionDate": "2024-04-20T00:00:00Z",
  "expectedDistributionAmount": 1625.00,
  "shariaCompliance": {
    "status": "APPROVED",
    "sukukStructure": "IJARAH_BASED",
    "underlyingAsset": "Commercial real estate portfolio",
    "assetOwnership": "SUKUK_HOLDERS",
    "certificateId": "SHC-87654323"
  }
}
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "principalAmount",
        "message": "Principal amount must be greater than 0",
        "code": "INVALID_AMOUNT"
      }
    ],
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req-123456789"
  }
}
```

### Common Error Codes

#### 4xx Client Errors
- **400 Bad Request**: `VALIDATION_ERROR`, `INVALID_REQUEST`
- **401 Unauthorized**: `AUTHENTICATION_REQUIRED`, `INVALID_TOKEN`
- **403 Forbidden**: `INSUFFICIENT_PERMISSIONS`, `SHARIA_NON_COMPLIANT`
- **404 Not Found**: `RESOURCE_NOT_FOUND`, `CUSTOMER_NOT_FOUND`
- **409 Conflict**: `DUPLICATE_RESOURCE`, `LOAN_ALREADY_EXISTS`
- **422 Unprocessable Entity**: `BUSINESS_RULE_VIOLATION`, `CREDIT_INSUFFICIENT`
- **429 Too Many Requests**: `RATE_LIMIT_EXCEEDED`

#### 5xx Server Errors
- **500 Internal Server Error**: `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE`
- **502 Bad Gateway**: `UPSTREAM_SERVICE_ERROR`
- **503 Service Unavailable**: `MAINTENANCE_MODE`, `SHARIA_BOARD_UNAVAILABLE`
- **504 Gateway Timeout**: `UPSTREAM_TIMEOUT`

---

## Rate Limiting

### Rate Limits by Security Level
- **Level 1**: 100 requests/minute
- **Level 2**: 500 requests/minute  
- **Level 3**: 200 requests/minute
- **Level 4**: 50 requests/minute

### Rate Limit Headers
```http
X-RateLimit-Limit: 500
X-RateLimit-Remaining: 485
X-RateLimit-Reset: 1642248600
X-RateLimit-Window: 60
```

---

## Webhooks

### Webhook Events
- `customer.created`
- `customer.updated`
- `loan.approved`
- `loan.disbursed`
- `loan.payment.completed`
- `murabaha.sharia.approved`
- `sukuk.distribution.paid`

### Webhook Payload Example
```json
{
  "eventId": "evt-123456789",
  "eventType": "loan.approved",
  "timestamp": "2024-01-15T10:30:00Z",
  "apiVersion": "v1",
  "data": {
    "loanId": "LOAN-87654321",
    "customerId": "CUST-12345678",
    "status": "APPROVED",
    "approvedAmount": 100000.00,
    "currency": "AED"
  }
}
```

---

## Environment Endpoints

### Production
- **Base URL**: `https://api.example.com`
- **AmanahFi URL**: `https://api.example.com/amanahfi`

### Sandbox
- **Base URL**: `https://sandbox-api.example.com`
- **AmanahFi URL**: `https://sandbox-api.example.com/amanahfi`

### Development
- **Base URL**: `https://dev-api.example.com`
- **AmanahFi URL**: `https://dev-api.example.com/amanahfi`

---

## SDK and Code Examples

### Node.js SDK
```javascript
const AmanahFiClient = require('@amanahfi/node-sdk');

const client = new AmanahFiClient({
  apiKey: 'your-api-key',
  environment: 'production'
});

// Create traditional loan
const loan = await client.loans.create({
  customerId: 'CUST-12345678',
  principalAmount: 100000.00,
  currency: 'AED',
  interestRate: 5.25,
  termMonths: 36
});

// Create Murabaha contract
const murabaha = await client.amanahfi.murabaha.create({
  customerId: 'CUST-12345678',
  assetValue: 1000000.00,
  currency: 'AED',
  profitRate: 4.5,
  termMonths: 48
});
```

### Python SDK
```python
from amanahfi import AmanahFiClient

client = AmanahFiClient(
    api_key='your-api-key',
    environment='production'
)

# Create traditional loan
loan = client.loans.create(
    customer_id='CUST-12345678',
    principal_amount=100000.00,
    currency='AED',
    interest_rate=5.25,
    term_months=36
)

# Create Murabaha contract
murabaha = client.amanahfi.murabaha.create(
    customer_id='CUST-12345678',
    asset_value=1000000.00,
    currency='AED',
    profit_rate=4.5,
    term_months=48
)
```

---

## Compliance & Regulatory

### UAE CBUAE Compliance
- **Regulation**: Central Bank of UAE guidelines
- **Reporting**: Real-time transaction reporting
- **KYC/AML**: Enhanced due diligence

### VARA Compliance (Virtual Assets)
- **Regulation**: Virtual Assets Regulatory Authority
- **Scope**: UAE CBDC integration
- **Reporting**: Digital asset transactions

### Sharia Compliance
- **Board**: AmanahFi Sharia Supervisory Board
- **Certification**: Real-time compliance verification
- **Audit**: Quarterly Sharia audit reports

---

*Generated with Claude Code - Last Updated: January 2024*