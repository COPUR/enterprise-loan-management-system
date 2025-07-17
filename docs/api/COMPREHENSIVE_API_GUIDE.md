# Enterprise Loan Management System - Comprehensive API Guide

## 🏛️ API Overview

This guide provides comprehensive documentation for the Enterprise Loan Management System API, featuring advanced security, Islamic finance compliance, and enterprise-grade scalability.

### 🔗 Base URLs

| Environment | Base URL | Description |
|-------------|----------|-------------|
| Production | `https://api.banking.com/v2` | Production environment |
| Staging | `https://api-staging.banking.com/v2` | Staging environment |
| Sandbox | `https://api-sandbox.banking.com/v2` | Islamic finance testing |
| Local | `http://localhost:8080/v2` | Local development |

### 🔐 Authentication

#### OAuth 2.1 with PKCE

**Authorization Code Flow:**

1. **Generate PKCE Challenge**
```bash
# Generate code verifier (43-128 characters)
CODE_VERIFIER=$(openssl rand -base64 96 | tr -d "=+/" | cut -c1-128)

# Generate code challenge
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -sha256 -binary | base64 | tr -d "=+/" | cut -c1-43)
```

2. **Authorization Request**
```http
GET /oauth/authorize?response_type=code&client_id=banking-app&redirect_uri=https://app.banking.com/callback&scope=customer:read customer:write loan:read loan:write&code_challenge=CODE_CHALLENGE&code_challenge_method=S256&state=random-state-value HTTP/1.1
Host: api.banking.com
```

3. **Token Exchange**
```http
POST /oauth/token HTTP/1.1
Host: api.banking.com
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&client_id=banking-app&redirect_uri=https://app.banking.com/callback&code_verifier=CODE_VERIFIER
```

#### DPoP (Demonstration of Proof of Possession)

**Generate DPoP Proof:**

```javascript
// Example DPoP proof generation
const dpopProof = {
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": {
    "kty": "EC",
    "crv": "P-256",
    "x": "...",
    "y": "..."
  }
};

const dpopClaims = {
  "jti": "unique-identifier",
  "htm": "POST",
  "htu": "https://api.banking.com/v2/customers",
  "iat": Math.floor(Date.now() / 1000),
  "exp": Math.floor(Date.now() / 1000) + 60
};
```

**Usage:**
```http
POST /v2/customers HTTP/1.1
Host: api.banking.com
Authorization: DPoP eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2In0...
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2In0...
Content-Type: application/json
```

## 📝 API Endpoints

### 👥 Customer Management

#### Create Customer

**Endpoint:** `POST /v2/customers`

**Request:**
```json
{
  "firstName": "Ahmed",
  "lastName": "Al-Rashid",
  "email": "ahmed.alrashid@example.com",
  "phone": "+971501234567",
  "dateOfBirth": "1985-03-15",
  "nationality": "AE",
  "customerType": "INDIVIDUAL",
  "address": {
    "street": "Sheikh Zayed Road",
    "city": "Dubai",
    "country": "AE",
    "postalCode": "12345"
  },
  "identityDocument": {
    "type": "UAE_EMIRATES_ID",
    "number": "784-1985-1234567-8",
    "issueDate": "2020-01-01",
    "expiryDate": "2025-01-01"
  },
  "islamicBankingPreferences": {
    "preferredProducts": ["MURABAHA", "IJARAH"],
    "shariaCompliantOnly": true,
    "preferredCurrency": "AED"
  }
}
```

**Response:**
```json
{
  "customerId": "CUST-20241216-001",
  "customerNumber": "1000000001",
  "status": "PENDING_KYC",
  "createdAt": "2024-12-16T10:30:00Z",
  "kycStatus": "PENDING",
  "creditScore": null,
  "creditLimit": {
    "amount": 0,
    "currency": "AED"
  },
  "_links": {
    "self": {
      "href": "/v2/customers/CUST-20241216-001"
    },
    "kyc": {
      "href": "/v2/customers/CUST-20241216-001/kyc"
    },
    "credit-assessment": {
      "href": "/v2/customers/CUST-20241216-001/credit-assessment"
    }
  }
}
```

#### Get Customer

**Endpoint:** `GET /v2/customers/{customerId}`

**Response:**
```json
{
  "customerId": "CUST-20241216-001",
  "customerNumber": "1000000001",
  "personalInfo": {
    "firstName": "Ahmed",
    "lastName": "Al-Rashid",
    "email": "ahmed.alrashid@example.com",
    "phone": "+971501234567",
    "dateOfBirth": "1985-03-15",
    "nationality": "AE"
  },
  "status": "ACTIVE",
  "kycStatus": "COMPLETED",
  "creditScore": 750,
  "creditLimit": {
    "amount": 250000,
    "currency": "AED",
    "availableCredit": 250000,
    "reservedCredit": 0
  },
  "islamicBankingProfile": {
    "preferredProducts": ["MURABAHA", "IJARAH"],
    "shariaCompliantOnly": true,
    "preferredCurrency": "AED",
    "personalizedRates": {
      "murabaha": 0.08,
      "ijarah": 0.07,
      "musharakah": 0.09
    }
  },
  "accountSummary": {
    "totalLoans": 2,
    "activeLoans": 1,
    "totalOutstanding": {
      "amount": 150000,
      "currency": "AED"
    }
  },
  "_links": {
    "self": {
      "href": "/v2/customers/CUST-20241216-001"
    },
    "loans": {
      "href": "/v2/customers/CUST-20241216-001/loans"
    },
    "payments": {
      "href": "/v2/customers/CUST-20241216-001/payments"
    },
    "islamic-products": {
      "href": "/v2/customers/CUST-20241216-001/islamic-products"
    }
  }
}
```

### 🏦 Loan Management

#### Create Loan Application

**Endpoint:** `POST /v2/loans`

**Conventional Loan Request:**
```json
{
  "customerId": "CUST-20241216-001",
  "loanType": "PERSONAL",
  "amount": {
    "amount": 100000,
    "currency": "AED"
  },
  "term": {
    "value": 36,
    "unit": "MONTHS"
  },
  "purpose": "HOME_RENOVATION",
  "requestedRate": 0.12,
  "collateral": {
    "type": "REAL_ESTATE",
    "value": {
      "amount": 150000,
      "currency": "AED"
    },
    "description": "Apartment in Dubai Marina"
  }
}
```

**Islamic Finance Request (Murabaha):**
```json
{
  "customerId": "CUST-20241216-001",
  "loanType": "MURABAHA",
  "amount": {
    "amount": 100000,
    "currency": "AED"
  },
  "term": {
    "value": 36,
    "unit": "MONTHS"
  },
  "purpose": "CAR_PURCHASE",
  "assetDetails": {
    "type": "VEHICLE",
    "description": "Toyota Camry 2024",
    "supplier": "Toyota UAE",
    "assetValue": {
      "amount": 95000,
      "currency": "AED"
    }
  },
  "islamicFinanceDetails": {
    "productType": "MURABAHA",
    "profitMargin": 0.15,
    "shariaCompliant": true,
    "supervisoryBoard": "UAE_HIGHER_SHARIA_AUTHORITY",
    "assetOwnership": true
  }
}
```

**Response:**
```json
{
  "loanId": "LOAN-20241216-001",
  "applicationNumber": "APP-2024-001",
  "customerId": "CUST-20241216-001",
  "loanType": "MURABAHA",
  "status": "PENDING_APPROVAL",
  "requestedAmount": {
    "amount": 100000,
    "currency": "AED"
  },
  "term": {
    "value": 36,
    "unit": "MONTHS"
  },
  "calculatedRate": 0.12,
  "monthlyPayment": {
    "amount": 3321.43,
    "currency": "AED"
  },
  "totalInterest": {
    "amount": 19571.48,
    "currency": "AED"
  },
  "islamicFinanceDetails": {
    "productType": "MURABAHA",
    "profitMargin": 0.15,
    "shariaCompliant": true,
    "complianceStatus": "VALIDATED",
    "supervisoryBoard": "UAE_HIGHER_SHARIA_AUTHORITY"
  },
  "applicationDate": "2024-12-16T10:30:00Z",
  "expectedDecisionDate": "2024-12-18T10:30:00Z",
  "_links": {
    "self": {
      "href": "/v2/loans/LOAN-20241216-001"
    },
    "customer": {
      "href": "/v2/customers/CUST-20241216-001"
    },
    "documents": {
      "href": "/v2/loans/LOAN-20241216-001/documents"
    },
    "approval": {
      "href": "/v2/loans/LOAN-20241216-001/approval"
    }
  }
}
```

#### Get Loan Details

**Endpoint:** `GET /v2/loans/{loanId}`

**Response:**
```json
{
  "loanId": "LOAN-20241216-001",
  "customerId": "CUST-20241216-001",
  "loanType": "MURABAHA",
  "status": "ACTIVE",
  "currentBalance": {
    "amount": 85000,
    "currency": "AED"
  },
  "originalAmount": {
    "amount": 100000,
    "currency": "AED"
  },
  "interestRate": 0.12,
  "term": {
    "value": 36,
    "unit": "MONTHS"
  },
  "remainingTerm": {
    "value": 30,
    "unit": "MONTHS"
  },
  "monthlyPayment": {
    "amount": 3321.43,
    "currency": "AED"
  },
  "nextPaymentDate": "2024-12-25T00:00:00Z",
  "paymentHistory": {
    "totalPayments": 6,
    "onTimePayments": 5,
    "latePayments": 1,
    "missedPayments": 0
  },
  "islamicFinanceDetails": {
    "productType": "MURABAHA",
    "profitMargin": 0.15,
    "assetDetails": {
      "type": "VEHICLE",
      "description": "Toyota Camry 2024",
      "currentValue": {
        "amount": 90000,
        "currency": "AED"
      },
      "ownershipTransferred": true
    },
    "shariaCompliance": {
      "status": "COMPLIANT",
      "lastReviewDate": "2024-12-01T00:00:00Z",
      "reviewedBy": "UAE_HIGHER_SHARIA_AUTHORITY"
    }
  },
  "_links": {
    "self": {
      "href": "/v2/loans/LOAN-20241216-001"
    },
    "customer": {
      "href": "/v2/customers/CUST-20241216-001"
    },
    "payments": {
      "href": "/v2/loans/LOAN-20241216-001/payments"
    },
    "schedule": {
      "href": "/v2/loans/LOAN-20241216-001/schedule"
    },
    "early-settlement": {
      "href": "/v2/loans/LOAN-20241216-001/early-settlement"
    }
  }
}
```

### 💳 Payment Processing

#### Create Payment

**Endpoint:** `POST /v2/payments`

**Request:**
```json
{
  "loanId": "LOAN-20241216-001",
  "amount": {
    "amount": 3321.43,
    "currency": "AED"
  },
  "paymentMethod": {
    "type": "BANK_TRANSFER",
    "bankAccount": {
      "accountNumber": "1234567890",
      "bankCode": "ADBAE",
      "accountHolderName": "Ahmed Al-Rashid"
    }
  },
  "paymentDate": "2024-12-16T10:30:00Z",
  "reference": "Monthly Payment - December 2024",
  "islamicFinanceDetails": {
    "shariaCompliant": true,
    "paymentType": "REGULAR_INSTALLMENT"
  }
}
```

**Response:**
```json
{
  "paymentId": "PAY-20241216-001",
  "loanId": "LOAN-20241216-001",
  "amount": {
    "amount": 3321.43,
    "currency": "AED"
  },
  "paymentBreakdown": {
    "principalAmount": {
      "amount": 2821.43,
      "currency": "AED"
    },
    "interestAmount": {
      "amount": 500.00,
      "currency": "AED"
    },
    "penaltyAmount": {
      "amount": 0.00,
      "currency": "AED"
    }
  },
  "status": "PROCESSING",
  "paymentDate": "2024-12-16T10:30:00Z",
  "processedDate": null,
  "reference": "Monthly Payment - December 2024",
  "transactionId": "TXN-20241216-001",
  "islamicFinanceDetails": {
    "shariaCompliant": true,
    "paymentType": "REGULAR_INSTALLMENT",
    "complianceValidated": true
  },
  "_links": {
    "self": {
      "href": "/v2/payments/PAY-20241216-001"
    },
    "loan": {
      "href": "/v2/loans/LOAN-20241216-001"
    },
    "receipt": {
      "href": "/v2/payments/PAY-20241216-001/receipt"
    }
  }
}
```

### 🕌 Islamic Finance Products

#### Get Islamic Products

**Endpoint:** `GET /v2/islamic-finance/products`

**Response:**
```json
{
  "products": [
    {
      "productId": "MURABAHA-001",
      "name": "Murabaha Car Finance",
      "type": "MURABAHA",
      "description": "Asset-backed financing for vehicle purchases",
      "features": {
        "minAmount": {
          "amount": 10000,
          "currency": "AED"
        },
        "maxAmount": {
          "amount": 500000,
          "currency": "AED"
        },
        "minTerm": {
          "value": 12,
          "unit": "MONTHS"
        },
        "maxTerm": {
          "value": 84,
          "unit": "MONTHS"
        },
        "profitRateRange": {
          "min": 0.08,
          "max": 0.25
        }
      },
      "shariaCompliance": {
        "compliant": true,
        "supervisoryBoard": "UAE_HIGHER_SHARIA_AUTHORITY",
        "certificateNumber": "SHARIA-CERT-001"
      },
      "eligibility": {
        "minCreditScore": 650,
        "minIncome": {
          "amount": 15000,
          "currency": "AED"
        },
        "maxDebtToIncome": 0.5
      }
    },
    {
      "productId": "IJARAH-001",
      "name": "Ijarah Equipment Lease",
      "type": "IJARAH",
      "description": "Lease-to-own financing for equipment",
      "features": {
        "minAmount": {
          "amount": 25000,
          "currency": "AED"
        },
        "maxAmount": {
          "amount": 1000000,
          "currency": "AED"
        },
        "minTerm": {
          "value": 24,
          "unit": "MONTHS"
        },
        "maxTerm": {
          "value": 60,
          "unit": "MONTHS"
        },
        "rentalRateRange": {
          "min": 0.07,
          "max": 0.20
        }
      },
      "shariaCompliance": {
        "compliant": true,
        "supervisoryBoard": "UAE_HIGHER_SHARIA_AUTHORITY",
        "certificateNumber": "SHARIA-CERT-002"
      }
    }
  ],
  "totalProducts": 2,
  "_links": {
    "self": {
      "href": "/v2/islamic-finance/products"
    },
    "application": {
      "href": "/v2/islamic-finance/applications"
    }
  }
}
```

### 📊 Risk Analytics

#### Get Risk Dashboard

**Endpoint:** `GET /v2/analytics/risk/dashboard`

**Response:**
```json
{
  "overview": {
    "totalLoans": 1250,
    "totalOutstanding": {
      "amount": 125000000,
      "currency": "AED"
    },
    "averageRiskScore": 7.2,
    "defaultRate": 2.3
  },
  "riskDistribution": {
    "lowRisk": 45,
    "mediumRisk": 35,
    "highRisk": 20
  },
  "islamicFinanceMetrics": {
    "totalMurabahaContracts": 450,
    "totalIjarahContracts": 120,
    "totalMusharakahContracts": 80,
    "shariaComplianceRate": 99.2,
    "assetBackingRatio": 98.5
  },
  "alerts": {
    "critical": 2,
    "high": 5,
    "medium": 12,
    "low": 28
  },
  "recentAlerts": [
    {
      "alertId": "ALERT-001",
      "type": "SHARIA_COMPLIANCE",
      "severity": "HIGH",
      "message": "Murabaha contract requires Sharia board review",
      "timestamp": "2024-12-16T09:30:00Z"
    }
  ],
  "_links": {
    "self": {
      "href": "/v2/analytics/risk/dashboard"
    },
    "detailed-report": {
      "href": "/v2/analytics/risk/detailed-report"
    },
    "alerts": {
      "href": "/v2/analytics/risk/alerts"
    }
  }
}
```

## 🔄 Webhooks

### Webhook Events

The API supports webhooks for real-time event notifications:

#### Customer Events
- `customer.created`
- `customer.updated`
- `customer.kyc.completed`
- `customer.credit.updated`

#### Loan Events
- `loan.application.created`
- `loan.approved`
- `loan.rejected`
- `loan.activated`
- `loan.closed`

#### Payment Events
- `payment.created`
- `payment.completed`
- `payment.failed`
- `payment.refunded`

#### Islamic Finance Events
- `islamic.product.created`
- `islamic.compliance.validated`
- `islamic.compliance.violated`
- `islamic.review.required`

### Webhook Payload Example

```json
{
  "eventId": "EVT-20241216-001",
  "eventType": "loan.approved",
  "timestamp": "2024-12-16T10:30:00Z",
  "data": {
    "loanId": "LOAN-20241216-001",
    "customerId": "CUST-20241216-001",
    "amount": {
      "amount": 100000,
      "currency": "AED"
    },
    "approvalDetails": {
      "approvedBy": "SYSTEM",
      "approvalDate": "2024-12-16T10:30:00Z",
      "conditions": []
    }
  },
  "metadata": {
    "requestId": "REQ-20241216-001",
    "correlationId": "CORR-20241216-001",
    "environment": "production"
  }
}
```

## 📋 Error Handling

### Standard Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid data",
    "details": [
      {
        "field": "amount",
        "message": "Amount must be greater than 1000",
        "code": "MIN_VALUE_ERROR"
      }
    ],
    "requestId": "REQ-20241216-001",
    "timestamp": "2024-12-16T10:30:00Z"
  }
}
```

### Common Error Codes

| Code | Description | HTTP Status |
|------|-------------|-------------|
| `VALIDATION_ERROR` | Request validation failed | 400 |
| `UNAUTHORIZED` | Authentication required | 401 |
| `FORBIDDEN` | Insufficient permissions | 403 |
| `NOT_FOUND` | Resource not found | 404 |
| `CONFLICT` | Resource already exists | 409 |
| `RATE_LIMIT_EXCEEDED` | Too many requests | 429 |
| `INTERNAL_ERROR` | Server error | 500 |

## 🚀 Rate Limiting

### Rate Limit Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640000000
```

### Rate Limits by Endpoint

| Endpoint | Limit | Window |
|----------|-------|--------|
| `/v2/customers` | 100 requests | 1 hour |
| `/v2/loans` | 50 requests | 1 hour |
| `/v2/payments` | 200 requests | 1 hour |
| `/v2/analytics` | 500 requests | 1 hour |

## 📚 SDK Examples

### JavaScript/Node.js

```javascript
const BankingAPI = require('@banking/enterprise-api');

const client = new BankingAPI({
  baseURL: 'https://api.banking.com/v2',
  clientId: 'your-client-id',
  clientSecret: 'your-client-secret'
});

// Create customer
const customer = await client.customers.create({
  firstName: 'Ahmed',
  lastName: 'Al-Rashid',
  email: 'ahmed@example.com',
  islamicBankingPreferences: {
    shariaCompliantOnly: true
  }
});

// Apply for Islamic loan
const loan = await client.loans.create({
  customerId: customer.customerId,
  loanType: 'MURABAHA',
  amount: { amount: 100000, currency: 'AED' },
  term: { value: 36, unit: 'MONTHS' }
});
```

### Python

```python
from banking_api import BankingClient

client = BankingClient(
    base_url='https://api.banking.com/v2',
    client_id='your-client-id',
    client_secret='your-client-secret'
)

# Create customer
customer = client.customers.create({
    'firstName': 'Ahmed',
    'lastName': 'Al-Rashid',
    'email': 'ahmed@example.com',
    'islamicBankingPreferences': {
        'shariaCompliantOnly': True
    }
})

# Apply for Islamic loan
loan = client.loans.create({
    'customerId': customer['customerId'],
    'loanType': 'MURABAHA',
    'amount': {'amount': 100000, 'currency': 'AED'},
    'term': {'value': 36, 'unit': 'MONTHS'}
})
```

## 🆘 Support & Resources

### Documentation
- [API Reference](https://docs.banking.com/api)
- [Developer Guide](https://docs.banking.com/guides)
- [Islamic Finance Guide](https://docs.banking.com/islamic-finance)

### Support
- **Email**: [api-support@banking.com](mailto:api-support@banking.com)
- **Status Page**: [status.banking.com](https://status.banking.com)
- **Community**: [community.banking.com](https://community.banking.com)

### Testing
- **Postman Collection**: [Download](https://api.banking.com/postman)
- **OpenAPI Spec**: [Download](https://api.banking.com/openapi.yaml)
- **Sandbox Environment**: [https://api-sandbox.banking.com](https://api-sandbox.banking.com)

---

*This API documentation is maintained by the Enterprise Banking API Team. For updates and announcements, subscribe to our [developer newsletter](https://banking.com/newsletter).*