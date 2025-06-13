# API Specification - Microservices Architecture
## Complete REST API Documentation

### Overview

The Enterprise Loan Management System exposes RESTful APIs through a Redis-integrated API Gateway with comprehensive security, rate limiting, and circuit breaker patterns.

---

## API Gateway Configuration

**Base URL**: `http://localhost:8080`  
**Protocol**: HTTP/HTTPS  
**Authentication**: JWT Bearer Token  
**Rate Limiting**: 1000 requests/minute per IP  
**Circuit Breaker**: Resilience4j integration  

### Global Headers
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
X-Request-ID: <unique_request_id>
X-Client-Version: <client_version>
```

### Standard Response Format
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS|ERROR|WARNING",
  "message": "Human readable message",
  "data": {},
  "errors": [],
  "metadata": {
    "requestId": "req_123456",
    "processingTime": "45ms",
    "version": "v1"
  }
}
```

---

## 1. Customer Management API

**Base Path**: `/api/v1/customers`  
**Service Port**: 8081  
**Database**: customer_db  

### 1.1 Create Customer
```http
POST /api/v1/customers
```

**Request Body**:
```json
{
  "customerId": "CUST-2024-001",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@email.com",
  "phone": "+1234567890",
  "dateOfBirth": "1985-03-15",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "creditLimit": 50000.00,
  "annualIncome": 75000.00,
  "employmentStatus": "EMPLOYED",
  "creditScore": 750,
  "identificationNumber": "SSN123456789",
  "identificationType": "SSN"
}
```

**Response (201 Created)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "message": "Customer created successfully",
  "data": {
    "customerId": "CUST-2024-001",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@email.com",
    "phone": "+1234567890",
    "creditLimit": 50000.00,
    "availableCredit": 50000.00,
    "accountStatus": "ACTIVE",
    "createdAt": "2025-06-12T10:30:00.000Z",
    "updatedAt": "2025-06-12T10:30:00.000Z"
  }
}
```

### 1.2 Get Customer Details
```http
GET /api/v1/customers/{customerId}
```

**Path Parameters**:
- `customerId` (string, required): Unique customer identifier

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "data": {
    "customerId": "CUST-2024-001",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@email.com",
    "phone": "+1234567890",
    "dateOfBirth": "1985-03-15",
    "address": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "creditLimit": 50000.00,
    "availableCredit": 35000.00,
    "usedCredit": 15000.00,
    "annualIncome": 75000.00,
    "employmentStatus": "EMPLOYED",
    "creditScore": 750,
    "accountStatus": "ACTIVE",
    "kycStatus": "VERIFIED",
    "riskLevel": "LOW",
    "createdAt": "2025-06-12T10:30:00.000Z",
    "updatedAt": "2025-06-12T10:30:00.000Z",
    "lastLoginAt": "2025-06-12T09:15:00.000Z"
  }
}
```

### 1.3 Reserve Credit
```http
POST /api/v1/customers/{customerId}/credit/reserve
```

**Request Body**:
```json
{
  "amount": 10000.00,
  "purpose": "LOAN_APPLICATION",
  "reservationTimeout": 300,
  "loanApplicationId": "LOAN-APP-001",
  "notes": "Credit reservation for personal loan"
}
```

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "message": "Credit reserved successfully",
  "data": {
    "reservationId": "RES-001",
    "customerId": "CUST-2024-001",
    "reservedAmount": 10000.00,
    "availableCreditBefore": 35000.00,
    "availableCreditAfter": 25000.00,
    "reservationExpiry": "2025-06-12T10:35:00.000Z",
    "purpose": "LOAN_APPLICATION",
    "status": "ACTIVE"
  }
}
```

### 1.4 Release Credit
```http
POST /api/v1/customers/{customerId}/credit/release
```

**Request Body**:
```json
{
  "reservationId": "RES-001",
  "reason": "LOAN_APPROVED",
  "actualAmountUsed": 8000.00,
  "notes": "Partial credit utilization"
}
```

### 1.5 Update Customer
```http
PUT /api/v1/customers/{customerId}
```

### 1.6 Get Customer Credit History
```http
GET /api/v1/customers/{customerId}/credit/history
```

**Query Parameters**:
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20)
- `fromDate` (string, optional): Start date (ISO 8601)
- `toDate` (string, optional): End date (ISO 8601)

---

## 2. Loan Origination API

**Base Path**: `/api/v1/loans`  
**Service Port**: 8082  
**Database**: loan_db  

### 2.1 Create Loan Application
```http
POST /api/v1/loans
```

**Request Body**:
```json
{
  "customerId": "CUST-2024-001",
  "loanAmount": 25000.00,
  "interestRate": 0.15,
  "installmentCount": 12,
  "loanType": "PERSONAL",
  "purpose": "HOME_IMPROVEMENT",
  "applicationDate": "2025-06-12T10:30:00.000Z",
  "requestedDisbursementDate": "2025-06-15T00:00:00.000Z",
  "collateral": {
    "type": "NONE",
    "value": 0.00,
    "description": "Unsecured personal loan"
  },
  "employmentDetails": {
    "employerName": "Tech Corp Inc.",
    "position": "Software Engineer",
    "workExperience": 5,
    "monthlyIncome": 6250.00
  }
}
```

**Business Rules Validation**:
- `installmentCount`: Must be 6, 9, 12, or 24
- `interestRate`: Must be between 0.1 and 0.5 (10% to 50%)
- `loanAmount`: Must not exceed customer's available credit
- `customerId`: Must exist and be active

**Response (201 Created)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "message": "Loan application created successfully",
  "data": {
    "loanId": "LOAN-2024-001",
    "applicationId": "APP-2024-001",
    "customerId": "CUST-2024-001",
    "loanAmount": 25000.00,
    "interestRate": 0.15,
    "installmentCount": 12,
    "installmentAmount": 2245.22,
    "totalRepaymentAmount": 26942.64,
    "loanType": "PERSONAL",
    "purpose": "HOME_IMPROVEMENT",
    "status": "PENDING_APPROVAL",
    "applicationDate": "2025-06-12T10:30:00.000Z",
    "approvalDate": null,
    "disbursementDate": null,
    "firstInstallmentDate": "2025-07-15T00:00:00.000Z",
    "lastInstallmentDate": "2026-06-15T00:00:00.000Z",
    "createdAt": "2025-06-12T10:30:00.000Z"
  }
}
```

### 2.2 Get Loan Details
```http
GET /api/v1/loans/{loanId}
```

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "data": {
    "loanId": "LOAN-2024-001",
    "customerId": "CUST-2024-001",
    "loanAmount": 25000.00,
    "outstandingAmount": 18750.00,
    "interestRate": 0.15,
    "installmentCount": 12,
    "installmentAmount": 2245.22,
    "totalRepaymentAmount": 26942.64,
    "paidInstallments": 3,
    "remainingInstallments": 9,
    "loanType": "PERSONAL",
    "purpose": "HOME_IMPROVEMENT",
    "status": "ACTIVE",
    "disbursementDate": "2025-06-15T10:00:00.000Z",
    "nextInstallmentDate": "2025-10-15T00:00:00.000Z",
    "maturityDate": "2026-06-15T00:00:00.000Z",
    "paymentHistory": {
      "totalPaid": 6735.66,
      "lastPaymentDate": "2025-09-15T14:30:00.000Z",
      "lastPaymentAmount": 2245.22,
      "earlyPaymentDiscount": 45.30,
      "latePaymentPenalty": 0.00
    }
  }
}
```

### 2.3 Get Loan Installment Schedule
```http
GET /api/v1/loans/{loanId}/installments
```

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "data": {
    "loanId": "LOAN-2024-001",
    "totalInstallments": 12,
    "installments": [
      {
        "installmentNumber": 1,
        "dueDate": "2025-07-15T00:00:00.000Z",
        "principalAmount": 1954.22,
        "interestAmount": 291.00,
        "totalAmount": 2245.22,
        "status": "PAID",
        "paidDate": "2025-07-14T16:45:00.000Z",
        "paidAmount": 2230.22,
        "discountApplied": 15.00,
        "penaltyApplied": 0.00
      },
      {
        "installmentNumber": 2,
        "dueDate": "2025-08-15T00:00:00.000Z",
        "principalAmount": 1978.65,
        "interestAmount": 266.57,
        "totalAmount": 2245.22,
        "status": "PAID",
        "paidDate": "2025-08-15T12:20:00.000Z",
        "paidAmount": 2245.22,
        "discountApplied": 0.00,
        "penaltyApplied": 0.00
      },
      {
        "installmentNumber": 3,
        "dueDate": "2025-09-15T00:00:00.000Z",
        "principalAmount": 2003.40,
        "interestAmount": 241.82,
        "totalAmount": 2245.22,
        "status": "PENDING",
        "paidDate": null,
        "paidAmount": 0.00,
        "discountApplied": 0.00,
        "penaltyApplied": 0.00
      }
    ],
    "summary": {
      "totalPrincipal": 25000.00,
      "totalInterest": 1942.64,
      "totalAmount": 26942.64,
      "paidAmount": 4475.44,
      "remainingAmount": 22467.20,
      "nextDueAmount": 2245.22,
      "nextDueDate": "2025-09-15T00:00:00.000Z"
    }
  }
}
```

### 2.4 Get Customer Loans
```http
GET /api/v1/loans/customer/{customerId}
```

**Query Parameters**:
- `status` (string, optional): Filter by loan status
- `loanType` (string, optional): Filter by loan type
- `page` (integer, optional): Page number
- `size` (integer, optional): Page size

### 2.5 Approve Loan
```http
POST /api/v1/loans/{loanId}/approve
```

### 2.6 Reject Loan
```http
POST /api/v1/loans/{loanId}/reject
```

---

## 3. Payment Processing API

**Base Path**: `/api/v1/payments`  
**Service Port**: 8083  
**Database**: payment_db  

### 3.1 Process Payment
```http
POST /api/v1/payments
```

**Request Body**:
```json
{
  "loanId": "LOAN-2024-001",
  "paymentAmount": 2245.22,
  "paymentDate": "2025-06-12T10:30:00.000Z",
  "paymentMethod": "BANK_TRANSFER",
  "installmentNumbers": [3],
  "isEarlyPayment": false,
  "isPartialPayment": false,
  "paymentReference": "TXN-123456789",
  "bankDetails": {
    "bankName": "ABC Bank",
    "accountNumber": "**** **** 1234",
    "routingNumber": "123456789"
  },
  "notes": "Regular monthly installment payment"
}
```

**Business Rules**:
- Early payment discount: 0.001 per day (0.1% per day)
- Late payment penalty: 0.001 per day (0.1% per day)
- Multiple installment payment distribution
- Partial payment allocation priority: Interest → Principal → Penalties

**Response (201 Created)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "message": "Payment processed successfully",
  "data": {
    "paymentId": "PAY-2024-001",
    "loanId": "LOAN-2024-001",
    "customerId": "CUST-2024-001",
    "paymentAmount": 2245.22,
    "paymentDate": "2025-06-12T10:30:00.000Z",
    "paymentMethod": "BANK_TRANSFER",
    "paymentReference": "TXN-123456789",
    "status": "COMPLETED",
    "installmentPayments": [
      {
        "installmentNumber": 3,
        "dueAmount": 2245.22,
        "paidAmount": 2245.22,
        "principalPaid": 2003.40,
        "interestPaid": 241.82,
        "penaltyPaid": 0.00,
        "discountApplied": 0.00,
        "remainingAmount": 0.00
      }
    ],
    "calculation": {
      "baseAmount": 2245.22,
      "discountAmount": 0.00,
      "penaltyAmount": 0.00,
      "finalAmount": 2245.22,
      "earlyPaymentDays": 0,
      "latePaymentDays": 0
    },
    "balanceAfterPayment": {
      "outstandingPrincipal": 16746.60,
      "outstandingInterest": 1700.82,
      "totalOutstanding": 18447.42
    }
  }
}
```

### 3.2 Calculate Payment
```http
POST /api/v1/payments/calculate
```

**Request Body**:
```json
{
  "loanId": "LOAN-2024-001",
  "paymentAmount": 2245.22,
  "paymentDate": "2025-06-10T10:30:00.000Z",
  "installmentNumbers": [3],
  "simulateOnly": true
}
```

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "data": {
    "calculation": {
      "baseAmount": 2245.22,
      "discountAmount": 75.00,
      "penaltyAmount": 0.00,
      "finalAmount": 2170.22,
      "earlyPaymentDays": 5,
      "latePaymentDays": 0,
      "discountRate": 0.005,
      "penaltyRate": 0.000
    },
    "installmentBreakdown": [
      {
        "installmentNumber": 3,
        "originalAmount": 2245.22,
        "discountApplied": 75.00,
        "amountToPay": 2170.22,
        "principalPortion": 2003.40,
        "interestPortion": 166.82,
        "savings": 75.00
      }
    ],
    "paymentAdvice": {
      "recommendedAmount": 2170.22,
      "savingsOpportunity": 75.00,
      "paymentWindow": {
        "earlyPaymentUntil": "2025-09-15T00:00:00.000Z",
        "gracePeriodUntil": "2025-09-20T00:00:00.000Z",
        "penaltyStartsFrom": "2025-09-21T00:00:00.000Z"
      }
    }
  }
}
```

### 3.3 Get Payment Details
```http
GET /api/v1/payments/{paymentId}
```

### 3.4 Get Customer Payment History
```http
GET /api/v1/payments/customer/{customerId}
```

**Query Parameters**:
- `loanId` (string, optional): Filter by specific loan
- `fromDate` (string, optional): Start date filter
- `toDate` (string, optional): End date filter
- `status` (string, optional): Filter by payment status
- `page` (integer, optional): Page number
- `size` (integer, optional): Page size

### 3.5 Get Loan Payment History
```http
GET /api/v1/payments/loan/{loanId}
```

---

## 4. SAGA Orchestration API

**Base Path**: `/api/v1/saga`  
**Service**: API Gateway  
**Database**: banking_gateway  

### 4.1 Initiate Loan Creation SAGA
```http
POST /api/v1/saga/loan-creation
```

**Request Body**:
```json
{
  "customerId": "CUST-2024-001",
  "loanAmount": 15000.00,
  "interestRate": 0.12,
  "installmentCount": 9,
  "loanType": "PERSONAL",
  "purpose": "DEBT_CONSOLIDATION",
  "sagaTimeout": 300,
  "compensationPolicy": "AUTOMATIC",
  "notificationPreferences": {
    "email": true,
    "sms": false,
    "webhook": "https://client.example.com/webhooks/saga"
  }
}
```

**Response (202 Accepted)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "message": "SAGA initiated successfully",
  "data": {
    "sagaId": "SAGA-2024-001",
    "sagaType": "LOAN_CREATION",
    "status": "STARTED",
    "currentStep": "VALIDATE_CUSTOMER",
    "estimatedCompletion": "2025-06-12T10:35:00.000Z",
    "steps": [
      {
        "stepName": "VALIDATE_CUSTOMER",
        "status": "IN_PROGRESS",
        "estimatedDuration": "30s"
      },
      {
        "stepName": "RESERVE_CREDIT",
        "status": "PENDING",
        "estimatedDuration": "45s"
      },
      {
        "stepName": "CREATE_LOAN",
        "status": "PENDING",
        "estimatedDuration": "60s"
      },
      {
        "stepName": "GENERATE_INSTALLMENTS",
        "status": "PENDING",
        "estimatedDuration": "30s"
      }
    ],
    "trackingUrl": "/api/v1/saga/states/SAGA-2024-001"
  }
}
```

### 4.2 Get SAGA State
```http
GET /api/v1/saga/states/{sagaId}
```

**Response (200 OK)**:
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "SUCCESS",
  "data": {
    "sagaId": "SAGA-2024-001",
    "sagaType": "LOAN_CREATION",
    "status": "COMPLETED",
    "currentStep": "COMPLETED",
    "startTime": "2025-06-12T10:30:00.000Z",
    "endTime": "2025-06-12T10:32:45.000Z",
    "duration": "2m45s",
    "completedSteps": [
      {
        "stepName": "VALIDATE_CUSTOMER",
        "status": "COMPLETED",
        "startTime": "2025-06-12T10:30:00.000Z",
        "endTime": "2025-06-12T10:30:15.000Z",
        "duration": "15s",
        "result": {
          "customerId": "CUST-2024-001",
          "creditScore": 750,
          "eligibility": "APPROVED"
        }
      },
      {
        "stepName": "RESERVE_CREDIT",
        "status": "COMPLETED",
        "startTime": "2025-06-12T10:30:15.000Z",
        "endTime": "2025-06-12T10:30:45.000Z",
        "duration": "30s",
        "result": {
          "reservationId": "RES-SAGA-001",
          "reservedAmount": 15000.00
        }
      },
      {
        "stepName": "CREATE_LOAN",
        "status": "COMPLETED",
        "startTime": "2025-06-12T10:30:45.000Z",
        "endTime": "2025-06-12T10:31:30.000Z",
        "duration": "45s",
        "result": {
          "loanId": "LOAN-2024-002",
          "applicationId": "APP-2024-002"
        }
      },
      {
        "stepName": "GENERATE_INSTALLMENTS",
        "status": "COMPLETED",
        "startTime": "2025-06-12T10:31:30.000Z",
        "endTime": "2025-06-12T10:32:45.000Z",
        "duration": "1m15s",
        "result": {
          "installmentCount": 9,
          "totalAmount": 16216.50
        }
      }
    ],
    "finalResult": {
      "loanId": "LOAN-2024-002",
      "status": "APPROVED",
      "disbursementDate": "2025-06-15T10:00:00.000Z"
    }
  }
}
```

### 4.3 Get SAGA History
```http
GET /api/v1/saga/history
```

### 4.4 Cancel SAGA
```http
POST /api/v1/saga/{sagaId}/cancel
```

---

## 5. System Health and Monitoring API

**Base Path**: `/actuator`  
**Service**: All Services  

### 5.1 Health Check
```http
GET /actuator/health
```

### 5.2 Circuit Breaker Status
```http
GET /actuator/circuitbreakers
```

### 5.3 Metrics (Prometheus)
```http
GET /actuator/prometheus
```

### 5.4 Application Info
```http
GET /actuator/info
```

---

## Error Handling

### Standard Error Response
```json
{
  "timestamp": "2025-06-12T10:30:00.000Z",
  "status": "ERROR",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "interestRate",
        "message": "Interest rate must be between 0.1 and 0.5",
        "rejectedValue": 0.75
      },
      {
        "field": "installmentCount",
        "message": "Installment count must be 6, 9, 12, or 24",
        "rejectedValue": 15
      }
    ],
    "documentation": "https://docs.loan-system.com/api/errors#validation-error"
  },
  "metadata": {
    "requestId": "req_123456",
    "timestamp": "2025-06-12T10:30:00.000Z"
  }
}
```

### HTTP Status Codes

| Code | Description | Usage |
|------|-------------|--------|
| 200 | OK | Successful GET, PUT requests |
| 201 | Created | Successful POST requests |
| 202 | Accepted | Asynchronous operations (SAGA) |
| 400 | Bad Request | Validation errors, malformed requests |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource state conflict |
| 422 | Unprocessable Entity | Business rule violations |
| 429 | Too Many Requests | Rate limiting triggered |
| 500 | Internal Server Error | System errors |
| 503 | Service Unavailable | Circuit breaker open, service down |

---

## Authentication and Security

### JWT Token Structure
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user123",
    "iat": 1640995200,
    "exp": 1640998800,
    "iss": "loan-management-system",
    "aud": "api-gateway",
    "scope": ["customer:read", "loan:create", "payment:process"],
    "customerId": "CUST-2024-001",
    "role": "CUSTOMER"
  }
}
```

### Security Headers
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995260
```

---

## API Testing Examples

### Using cURL

```bash
# Create Customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{
    "customerId": "CUST-TEST-001",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@email.com",
    "creditLimit": 30000.00
  }'

# Create Loan
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{
    "customerId": "CUST-TEST-001",
    "loanAmount": 15000.00,
    "interestRate": 0.12,
    "installmentCount": 12
  }'

# Process Payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt_token>" \
  -d '{
    "loanId": "LOAN-TEST-001",
    "paymentAmount": 1350.50,
    "paymentMethod": "BANK_TRANSFER",
    "installmentNumbers": [1]
  }'
```

This comprehensive API specification provides complete documentation for all microservice endpoints with detailed request/response examples, business rule validation, error handling, and security requirements.