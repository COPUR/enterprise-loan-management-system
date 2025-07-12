# 🚀 API Catalogue - AmanahFi Platform

## 🎯 Overview

This document provides a comprehensive catalogue of all REST and GraphQL APIs available in the AmanahFi Platform, including authentication requirements, request/response formats, and usage examples for Islamic finance operations, CBDC integration, and regulatory compliance.

## 📊 Document Information

| Attribute | Value |
|-----------|-------|
| **Document Version** | 1.0.0 |
| **Last Updated** | December 2024 |
| **Owner** | API Architecture Team |
| **Reviewers** | Development Team, Security Team, DevSecOps |
| **Approval** | Technical Leadership |
| **Classification** | Internal |

## 🔐 Authentication & Security

### Authentication Methods

All APIs require authentication using one of the following methods:

1. **OAuth 2.1 with DPoP** (Recommended)
2. **Mutual TLS (mTLS)**
3. **Bearer Token (JWT)**

### Security Headers

```http
Authorization: DPoP <access_token>
DPoP: <dpop_proof_jwt>
X-API-Version: 1.0
Content-Type: application/json
Accept: application/json
```

### Base URLs

| Environment | Base URL |
|-------------|----------|
| **Production** | `https://api.amanahfi.ae` |
| **Staging** | `https://api-staging.amanahfi.ae` |
| **Development** | `https://api-dev.amanahfi.ae` |

---

## 🕌 Islamic Finance APIs

### 💰 Murabaha Finance API

#### Create Murabaha Contract

**Endpoint**: `POST /api/v1/islamic-finance/murabaha`

**Authentication**: OAuth 2.1 with DPoP

**Description**: Creates a new Sharia-compliant Murabaha cost-plus financing contract.

**Request Body**:
```json
{
  "customerProfile": {
    "customerId": "CUST-001",
    "customerName": "Ahmed Al-Rashid",
    "customerType": "INDIVIDUAL",
    "nationalId": "784-1234-1234567-1",
    "phoneNumber": "+971501234567",
    "email": "ahmed.alrashid@email.ae"
  },
  "assetDetails": {
    "assetDescription": "Toyota Camry 2024",
    "assetCategory": "VEHICLE",
    "assetCost": {
      "amount": "80000.00",
      "currency": "AED"
    },
    "supplier": {
      "name": "Toyota Dealer UAE",
      "registrationNumber": "CN-1234567",
      "contactDetails": "+971441234567"
    },
    "isHalal": true,
    "assetSpecifications": {
      "make": "Toyota",
      "model": "Camry",
      "year": 2024,
      "color": "White"
    }
  },
  "financingTerms": {
    "profitMargin": 0.15,
    "maturityDate": "2027-12-31",
    "paymentSchedule": "MONTHLY",
    "downPayment": {
      "amount": "20000.00",
      "currency": "AED"
    }
  },
  "shariaCompliance": {
    "shariaBoard": "UAE_HIGHER_SHARIA_AUTHORITY",
    "complianceChecks": [
      "RIBA_FREE",
      "GHARAR_FREE",
      "ASSET_BACKING",
      "HALAL_ASSET"
    ]
  }
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "timestamp": "2024-12-11T10:30:00Z",
  "data": {
    "financingId": "MURABAHA-20241211-001",
    "contractReference": "AmanahFi-MUR-001234567",
    "status": "APPROVED",
    "totalFinancingAmount": {
      "amount": "92000.00",
      "currency": "AED"
    },
    "profitAmount": {
      "amount": "12000.00",
      "currency": "AED"
    },
    "shariaComplianceStatus": "COMPLIANT",
    "approvalDate": "2024-12-11T10:30:00Z",
    "maturityDate": "2027-12-31T23:59:59Z",
    "nextPaymentDate": "2025-01-11T00:00:00Z",
    "monthlyInstallment": {
      "amount": "2555.56",
      "currency": "AED"
    }
  },
  "links": {
    "self": "/api/v1/islamic-finance/murabaha/MURABAHA-20241211-001",
    "contract": "/api/v1/contracts/AmanahFi-MUR-001234567",
    "payments": "/api/v1/payments/MURABAHA-20241211-001"
  }
}
```

#### Get Murabaha Contract Details

**Endpoint**: `GET /api/v1/islamic-finance/murabaha/{financingId}`

**Authentication**: OAuth 2.1 with DPoP

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "financingId": "MURABAHA-20241211-001",
    "contractDetails": {
      "contractReference": "AmanahFi-MUR-001234567",
      "customerProfile": {
        "customerId": "CUST-001",
        "customerName": "Ahmed Al-Rashid"
      },
      "assetDetails": {
        "description": "Toyota Camry 2024",
        "currentValue": {
          "amount": "80000.00",
          "currency": "AED"
        }
      },
      "financingStatus": "ACTIVE",
      "remainingBalance": {
        "amount": "68000.00",
        "currency": "AED"
      },
      "shariaComplianceStatus": "COMPLIANT"
    }
  }
}
```

### 🤝 Musharakah Partnership API

#### Create Musharakah Partnership

**Endpoint**: `POST /api/v1/islamic-finance/musharakah`

**Request Body**:
```json
{
  "partnershipDetails": {
    "projectName": "Real Estate Development - Downtown Dubai",
    "projectType": "REAL_ESTATE",
    "totalInvestment": {
      "amount": "5000000.00",
      "currency": "AED"
    }
  },
  "partnershipStructure": {
    "bankContribution": {
      "amount": "3000000.00",
      "percentage": 60.0
    },
    "customerContribution": {
      "amount": "2000000.00",
      "percentage": 40.0
    },
    "profitSharingRatio": {
      "bankShare": 60.0,
      "customerShare": 40.0
    },
    "lossSharingRatio": {
      "bankShare": 60.0,
      "customerShare": 40.0
    }
  },
  "managementStructure": {
    "managingPartner": "CUSTOMER",
    "managementFee": {
      "amount": "50000.00",
      "currency": "AED"
    },
    "decisionMaking": "JOINT"
  }
}
```

### 🏠 Ijarah Lease API

#### Create Ijarah Lease Agreement

**Endpoint**: `POST /api/v1/islamic-finance/ijarah`

**Request Body**:
```json
{
  "leaseDetails": {
    "assetDescription": "Commercial Office Building",
    "assetType": "REAL_ESTATE",
    "assetValue": {
      "amount": "2000000.00",
      "currency": "AED"
    },
    "leaseType": "IJARAH_WA_IQTINA",
    "leasePeriod": {
      "startDate": "2024-12-15",
      "endDate": "2034-12-15",
      "durationYears": 10
    }
  },
  "rentalTerms": {
    "monthlyRental": {
      "amount": "25000.00",
      "currency": "AED"
    },
    "securityDeposit": {
      "amount": "100000.00",
      "currency": "AED"
    },
    "maintenanceResponsibility": "LESSEE",
    "insuranceResponsibility": "LESSOR"
  },
  "ownershipTransfer": {
    "enabled": true,
    "transferCondition": "END_OF_LEASE",
    "nominalPrice": {
      "amount": "1.00",
      "currency": "AED"
    }
  }
}
```

---

## 💎 CBDC Integration APIs

### 🇦🇪 Digital Dirham API

#### Create CBDC Transaction

**Endpoint**: `POST /api/v1/cbdc/digital-dirham/transactions`

**Authentication**: mTLS + OAuth 2.1

**Description**: Process a transaction using UAE's Central Bank Digital Currency.

**Request Body**:
```json
{
  "transactionDetails": {
    "transactionType": "PAYMENT",
    "amount": {
      "value": "1000.00",
      "currency": "AED-CBDC"
    },
    "purpose": "MURABAHA_PAYMENT",
    "reference": "MURABAHA-20241211-001-PAY-001"
  },
  "sender": {
    "walletId": "CBDC-WALLET-BANK-001",
    "accountId": "AMANAHFI-MAIN-001",
    "institutionCode": "AMANAHFI"
  },
  "receiver": {
    "walletId": "CBDC-WALLET-CUST-001",
    "accountId": "CUST-001-MAIN",
    "institutionCode": "EXTERNAL"
  },
  "compliance": {
    "amlChecks": true,
    "kycVerified": true,
    "sanctionsCheck": true,
    "regulatoryReporting": true
  },
  "blockchain": {
    "network": "UAE_CBDC_MAINNET",
    "consensusRequired": true,
    "smartContractAddress": "0x1234567890abcdef"
  }
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "timestamp": "2024-12-11T10:35:00Z",
  "data": {
    "transactionId": "CBDC-TX-20241211-001",
    "blockchainHash": "0xabcdef1234567890abcdef1234567890abcdef12",
    "blockNumber": 1234567,
    "confirmations": 6,
    "status": "CONFIRMED",
    "processedAmount": {
      "value": "1000.00",
      "currency": "AED-CBDC"
    },
    "fees": {
      "networkFee": {
        "value": "0.01",
        "currency": "AED-CBDC"
      },
      "processingFee": {
        "value": "0.50",
        "currency": "AED-CBDC"
      }
    },
    "settlement": {
      "settledAt": "2024-12-11T10:35:02Z",
      "settlementTime": "2.1 seconds",
      "finality": "IMMEDIATE"
    }
  }
}
```

#### Query CBDC Transaction Status

**Endpoint**: `GET /api/v1/cbdc/digital-dirham/transactions/{transactionId}`

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "transactionId": "CBDC-TX-20241211-001",
    "currentStatus": "SETTLED",
    "blockchain": {
      "hash": "0xabcdef1234567890",
      "blockNumber": 1234567,
      "confirmations": 12,
      "networkStatus": "FINALIZED"
    },
    "timeline": [
      {
        "status": "INITIATED",
        "timestamp": "2024-12-11T10:35:00.000Z"
      },
      {
        "status": "VALIDATED",
        "timestamp": "2024-12-11T10:35:00.500Z"
      },
      {
        "status": "CONFIRMED",
        "timestamp": "2024-12-11T10:35:02.100Z"
      },
      {
        "status": "SETTLED",
        "timestamp": "2024-12-11T10:35:02.100Z"
      }
    ]
  }
}
```

### 💱 Multi-Currency CBDC Support

#### Convert Between CBDC Currencies

**Endpoint**: `POST /api/v1/cbdc/currency-exchange`

**Request Body**:
```json
{
  "exchange": {
    "fromCurrency": "AED-CBDC",
    "toCurrency": "SAR-CBDC",
    "amount": {
      "value": "1000.00",
      "currency": "AED-CBDC"
    }
  },
  "exchangeRate": {
    "rate": 1.02,
    "rateSource": "CENTRAL_BANK_FEED",
    "rateTimestamp": "2024-12-11T10:30:00Z"
  },
  "compliance": {
    "crossBorderReporting": true,
    "jurisdictionalApproval": true
  }
}
```

---

## 🏛️ Regulatory Compliance APIs

### 📋 CBUAE Reporting API

#### Submit Regulatory Report

**Endpoint**: `POST /api/v1/compliance/cbuae/reports`

**Authentication**: mTLS (Required for regulatory APIs)

**Request Body**:
```json
{
  "reportDetails": {
    "reportType": "MONTHLY_ISLAMIC_FINANCE",
    "reportingPeriod": {
      "startDate": "2024-11-01",
      "endDate": "2024-11-30"
    },
    "submissionDeadline": "2024-12-15",
    "reportFormat": "CBUAE_XML_V2.1"
  },
  "reportData": {
    "islamicFinancePortfolio": {
      "totalOutstanding": {
        "amount": "50000000.00",
        "currency": "AED"
      },
      "newFinancing": {
        "amount": "5000000.00",
        "currency": "AED"
      },
      "productBreakdown": {
        "murabaha": "60%",
        "musharakah": "25%",
        "ijarah": "15%"
      }
    },
    "shariaCompliance": {
      "complianceRate": "100%",
      "nonCompliantTransactions": 0,
      "shariaBoard": "UAE_HIGHER_SHARIA_AUTHORITY"
    }
  }
}
```

### 🔍 VARA Digital Asset Compliance

#### Register Digital Asset Transaction

**Endpoint**: `POST /api/v1/compliance/vara/digital-assets`

**Request Body**:
```json
{
  "assetTransaction": {
    "transactionType": "CBDC_TRANSFER",
    "digitalAssetType": "CENTRAL_BANK_DIGITAL_CURRENCY",
    "amount": {
      "value": "1000.00",
      "currency": "AED-CBDC"
    },
    "counterpartyType": "REGULATED_INSTITUTION"
  },
  "compliance": {
    "varaLicense": "VASP-001-2024",
    "riskAssessment": "LOW",
    "amlStatus": "CLEARED",
    "sanctionsCheck": "PASSED"
  }
}
```

### 🕌 HSA Sharia Compliance

#### Validate Sharia Compliance

**Endpoint**: `POST /api/v1/compliance/hsa/validate`

**Request Body**:
```json
{
  "transaction": {
    "transactionId": "MURABAHA-20241211-001",
    "transactionType": "MURABAHA_CREATION",
    "amount": {
      "principal": "80000.00",
      "profit": "12000.00",
      "currency": "AED"
    },
    "assetBacking": {
      "assetType": "VEHICLE",
      "assetValue": "80000.00",
      "isHalal": true,
      "ownershipTransfer": true
    }
  },
  "shariaChecks": [
    "RIBA_PROHIBITION",
    "GHARAR_ELIMINATION",
    "ASSET_BACKING_VALIDATION",
    "PROFIT_MARGIN_COMPLIANCE"
  ]
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "complianceStatus": "COMPLIANT",
    "shariaBoard": "UAE_HIGHER_SHARIA_AUTHORITY",
    "complianceChecks": [
      {
        "check": "RIBA_PROHIBITION",
        "status": "PASSED",
        "details": "No interest-based elements detected"
      },
      {
        "check": "GHARAR_ELIMINATION",
        "status": "PASSED",
        "details": "Asset specifications clearly defined"
      },
      {
        "check": "ASSET_BACKING_VALIDATION",
        "status": "PASSED",
        "details": "Physical asset backing verified"
      },
      {
        "check": "PROFIT_MARGIN_COMPLIANCE",
        "status": "PASSED",
        "details": "Profit margin within HSA guidelines (15% < 30%)"
      }
    ],
    "certificateNumber": "HSA-CERT-20241211-001",
    "validUntil": "2024-12-31T23:59:59Z"
  }
}
```

---

## 👤 Customer Management APIs

### 📝 Customer Onboarding

#### Create Customer Profile

**Endpoint**: `POST /api/v1/customers`

**Request Body**:
```json
{
  "personalDetails": {
    "firstName": "Ahmed",
    "lastName": "Al-Rashid",
    "dateOfBirth": "1985-05-15",
    "nationality": "UAE",
    "gender": "MALE",
    "maritalStatus": "MARRIED"
  },
  "identification": {
    "emiratesId": "784-1234-1234567-1",
    "passportNumber": "A12345678",
    "visaStatus": "RESIDENT"
  },
  "contactInformation": {
    "phoneNumber": "+971501234567",
    "email": "ahmed.alrashid@email.ae",
    "address": {
      "street": "Sheikh Zayed Road",
      "city": "Dubai",
      "emirate": "DUBAI",
      "postalCode": "12345",
      "country": "UAE"
    }
  },
  "employment": {
    "employerName": "Emirates Airlines",
    "jobTitle": "Senior Manager",
    "monthlyIncome": {
      "amount": "25000.00",
      "currency": "AED"
    },
    "employmentType": "PERMANENT"
  },
  "kycDocuments": [
    {
      "documentType": "EMIRATES_ID",
      "documentId": "DOC-20241211-001",
      "verificationStatus": "VERIFIED"
    },
    {
      "documentType": "SALARY_CERTIFICATE",
      "documentId": "DOC-20241211-002",
      "verificationStatus": "PENDING"
    }
  ]
}
```

#### Customer Risk Assessment

**Endpoint**: `POST /api/v1/customers/{customerId}/risk-assessment`

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "customerId": "CUST-001",
    "riskProfile": {
      "overallRisk": "LOW",
      "riskScore": 25,
      "riskFactors": [
        {
          "factor": "INCOME_STABILITY",
          "score": 10,
          "weight": 0.3
        },
        {
          "factor": "CREDIT_HISTORY",
          "score": 15,
          "weight": 0.4
        },
        {
          "factor": "EMPLOYMENT_TYPE",
          "score": 5,
          "weight": 0.3
        }
      ]
    },
    "eligibility": {
      "islamicFinanceProducts": true,
      "maxFinancingAmount": {
        "amount": "500000.00",
        "currency": "AED"
      },
      "approvedProducts": [
        "MURABAHA",
        "IJARAH",
        "MUSHARAKAH"
      ]
    }
  }
}
```

---

## 💳 Payment Processing APIs

### 💰 Real-Time Payments

#### Process Instant Payment

**Endpoint**: `POST /api/v1/payments/instant`

**Request Body**:
```json
{
  "paymentDetails": {
    "amount": {
      "value": "2555.56",
      "currency": "AED"
    },
    "paymentType": "INSTALLMENT_PAYMENT",
    "reference": "MURABAHA-20241211-001-INST-001",
    "description": "Monthly installment payment for Murabaha financing"
  },
  "payer": {
    "customerId": "CUST-001",
    "accountNumber": "1234567890",
    "bankCode": "AMANAHFI"
  },
  "payee": {
    "accountNumber": "9876543210",
    "bankCode": "AMANAHFI",
    "accountType": "ISLAMIC_FINANCE_POOL"
  },
  "paymentMethod": {
    "method": "BANK_TRANSFER",
    "channel": "MOBILE_APP",
    "authenticationMethod": "BIOMETRIC"
  }
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "timestamp": "2024-12-11T10:40:00Z",
  "data": {
    "paymentId": "PAY-20241211-001",
    "transactionReference": "TXN-AmanahFi-20241211-001",
    "status": "COMPLETED",
    "processedAmount": {
      "value": "2555.56",
      "currency": "AED"
    },
    "fees": {
      "processingFee": {
        "value": "0.00",
        "currency": "AED"
      }
    },
    "settlement": {
      "settledAt": "2024-12-11T10:40:02Z",
      "settlementTime": "2.1 seconds"
    },
    "receipt": {
      "receiptNumber": "RCP-20241211-001",
      "downloadUrl": "/api/v1/payments/PAY-20241211-001/receipt"
    }
  }
}
```

### 🌍 Cross-Border Payments

#### International Transfer

**Endpoint**: `POST /api/v1/payments/international`

**Request Body**:
```json
{
  "transferDetails": {
    "amount": {
      "value": "10000.00",
      "currency": "AED"
    },
    "targetCurrency": "USD",
    "exchangeRate": {
      "rate": 3.6725,
      "rateSource": "CENTRAL_BANK",
      "rateTimestamp": "2024-12-11T10:35:00Z"
    }
  },
  "sender": {
    "customerId": "CUST-001",
    "country": "UAE",
    "purpose": "FAMILY_SUPPORT"
  },
  "beneficiary": {
    "name": "John Smith",
    "accountNumber": "1234567890",
    "bankCode": "CHASUS33",
    "country": "USA",
    "address": "123 Main Street, New York, NY 10001"
  },
  "compliance": {
    "amlChecks": true,
    "sanctionsScreening": true,
    "regulatoryReporting": true,
    "sourceOfFunds": "SALARY"
  }
}
```

---

## 📊 Analytics & Reporting APIs

### 📈 Business Intelligence

#### Get Portfolio Analytics

**Endpoint**: `GET /api/v1/analytics/portfolio`

**Query Parameters**:
- `period`: DAILY | WEEKLY | MONTHLY | QUARTERLY | YEARLY
- `fromDate`: 2024-01-01
- `toDate`: 2024-12-31
- `productType`: MURABAHA | MUSHARAKAH | IJARAH | ALL

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "portfolioSummary": {
      "totalOutstanding": {
        "amount": "150000000.00",
        "currency": "AED"
      },
      "numberOfAccounts": 1250,
      "averageFinancingAmount": {
        "amount": "120000.00",
        "currency": "AED"
      }
    },
    "productBreakdown": [
      {
        "productType": "MURABAHA",
        "percentage": 65.0,
        "amount": {
          "amount": "97500000.00",
          "currency": "AED"
        },
        "accountCount": 812
      },
      {
        "productType": "MUSHARAKAH",
        "percentage": 20.0,
        "amount": {
          "amount": "30000000.00",
          "currency": "AED"
        },
        "accountCount": 250
      },
      {
        "productType": "IJARAH",
        "percentage": 15.0,
        "amount": {
          "amount": "22500000.00",
          "currency": "AED"
        },
        "accountCount": 188
      }
    ],
    "performanceMetrics": {
      "portfolioGrowth": "12.5%",
      "averageReturn": "8.2%",
      "riskRating": "LOW",
      "shariaComplianceRate": "100%"
    }
  }
}
```

### 📋 Regulatory Reports

#### Generate Compliance Report

**Endpoint**: `POST /api/v1/reports/compliance`

**Request Body**:
```json
{
  "reportConfiguration": {
    "reportType": "MONTHLY_REGULATORY",
    "period": {
      "startDate": "2024-11-01",
      "endDate": "2024-11-30"
    },
    "regulators": ["CBUAE", "VARA", "HSA"],
    "format": "PDF",
    "includeDetails": true
  },
  "dataScope": {
    "includeTransactions": true,
    "includeCustomers": true,
    "includeCompliance": true,
    "includeShariaValidation": true
  }
}
```

---

## 🔒 Security & Authentication APIs

### 🔐 OAuth 2.1 with DPoP

#### Token Exchange

**Endpoint**: `POST /oauth/token`

**Request Headers**:
```http
Content-Type: application/x-www-form-urlencoded
DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwieCI6Imw4dEZyaHgtMzR0VjNoUklDUkRZOWpuV2pHUllQV1ZSN3lYRVJfUktfSU0iLCJ5IjoiLV9KZkthZ0xEa3pkbF9jM2J6dTU4c2JQRVhEU0Q5RURDTkgwa1RINlQwayIsImNydiI6IlAtMjU2In19
```

**Request Body**:
```
grant_type=authorization_code&
code=SplxlOBeZQQYbYS6WxSbIA&
redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb&
client_id=client123
```

**Response**:
```json
{
  "access_token": "2YotnFZFEjr1zCsicMWpAA",
  "token_type": "DPoP",
  "expires_in": 3600,
  "refresh_token": "tGzv3JOkF0XG5Qx2TlKWIA",
  "scope": "islamic-finance cbdc-payments customer-management"
}
```

### 🔑 mTLS Certificate Validation

#### Validate Client Certificate

**Endpoint**: `POST /api/v1/security/mtls/validate`

**Request Body**:
```json
{
  "certificate": {
    "pemFormat": "-----BEGIN CERTIFICATE-----\nMIIE...\n-----END CERTIFICATE-----",
    "serialNumber": "123456789",
    "issuer": "CN=AmanahFi CA,OU=Security,O=AmanahFi,C=AE",
    "subject": "CN=client.amanahfi.ae,OU=API Client,O=AmanahFi,C=AE",
    "validFrom": "2024-01-01T00:00:00Z",
    "validTo": "2025-01-01T00:00:00Z"
  },
  "validationChecks": [
    "CERTIFICATE_CHAIN",
    "EXPIRY_DATE",
    "REVOCATION_STATUS",
    "HOSTNAME_VERIFICATION"
  ]
}
```

---

## 📡 Integration APIs

### 🔄 External System Integration

#### Banking Core System Sync

**Endpoint**: `POST /api/v1/integrations/core-banking/sync`

**Request Body**:
```json
{
  "syncOperation": {
    "operationType": "CUSTOMER_DATA_SYNC",
    "customerId": "CUST-001",
    "dataElements": [
      "PERSONAL_DETAILS",
      "ACCOUNT_BALANCES",
      "TRANSACTION_HISTORY",
      "RISK_PROFILE"
    ]
  },
  "targetSystem": {
    "systemId": "CORE_BANKING_V2",
    "endpoint": "https://core.amanahfi.ae/api/sync",
    "authenticationMethod": "MUTUAL_TLS"
  }
}
```

### 🌐 Open Banking APIs

#### Account Information Service (AIS)

**Endpoint**: `GET /open-banking/v1/accounts/{accountId}`

**Authentication**: OAuth 2.1 + TPP Certificate

**Response**:
```json
{
  "data": {
    "accountId": "ACC-001",
    "currency": "AED",
    "accountType": "ISLAMIC_CURRENT",
    "accountSubType": "CurrentAccount",
    "nickname": "My Halal Account",
    "openingDate": "2024-01-15",
    "maturityDate": null,
    "status": "ACTIVE"
  },
  "links": {
    "self": "/open-banking/v1/accounts/ACC-001"
  },
  "meta": {
    "totalPages": 1,
    "firstAvailableDateTime": "2024-01-15T00:00:00Z",
    "lastAvailableDateTime": "2024-12-11T23:59:59Z"
  }
}
```

#### Payment Initiation Service (PIS)

**Endpoint**: `POST /open-banking/v1/payments`

**Request Body**:
```json
{
  "data": {
    "consentId": "CONSENT-001",
    "initiation": {
      "instructionIdentification": "INSTR-001",
      "instructedAmount": {
        "amount": "100.00",
        "currency": "AED"
      },
      "creditorAccount": {
        "schemeName": "IBAN",
        "identification": "AE070331234567890123456",
        "name": "Recipient Name"
      },
      "remittanceInformation": {
        "unstructured": "Payment for Murabaha installment"
      }
    }
  }
}
```

---

## 📱 Multi-Channel APIs

### 📲 Mobile Banking APIs

#### Mobile App Authentication

**Endpoint**: `POST /api/v1/mobile/authenticate`

**Request Body**:
```json
{
  "authentication": {
    "method": "BIOMETRIC",
    "biometricType": "FINGERPRINT",
    "deviceId": "DEVICE-ABC123",
    "appVersion": "2.1.0",
    "platform": "iOS"
  },
  "credentials": {
    "customerId": "CUST-001",
    "biometricHash": "sha256_hash_value",
    "deviceTrust": "TRUSTED"
  }
}
```

### 💬 Chatbot APIs

#### Process Natural Language Query

**Endpoint**: `POST /api/v1/chatbot/query`

**Request Body**:
```json
{
  "query": {
    "message": "What is my remaining balance on my Murabaha financing?",
    "language": "en",
    "customerId": "CUST-001",
    "sessionId": "CHAT-SESSION-001"
  },
  "context": {
    "channel": "MOBILE_APP",
    "previousInteractions": [],
    "intentConfidence": 0.95
  }
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "response": {
      "message": "Your current remaining balance on Murabaha financing MURABAHA-20241211-001 is AED 68,000.00. Your next payment of AED 2,555.56 is due on January 11, 2025.",
      "intent": "BALANCE_INQUIRY",
      "confidence": 0.97,
      "language": "en"
    },
    "actions": [
      {
        "type": "DISPLAY_BALANCE",
        "data": {
          "financingId": "MURABAHA-20241211-001",
          "remainingBalance": "68000.00",
          "currency": "AED"
        }
      }
    ],
    "suggestedQueries": [
      "When is my next payment due?",
      "Can I make an early payment?",
      "Show me my payment history"
    ]
  }
}
```

---

## 🌍 Localization APIs

### 🌐 Multi-Language Support

#### Get Localized Content

**Endpoint**: `GET /api/v1/localization/content`

**Query Parameters**:
- `language`: ar | en | fr
- `region`: UAE | SAU | QAT | KWT | BHR | OMN
- `contentType`: LEGAL_TERMS | UI_LABELS | ERROR_MESSAGES

**Response**:
```json
{
  "status": "SUCCESS",
  "data": {
    "language": "ar",
    "region": "UAE",
    "content": {
      "islamicFinance": {
        "murabaha": "مرابحة",
        "musharakah": "مشاركة",
        "ijarah": "إجارة",
        "profitMargin": "هامش الربح",
        "shariaCompliant": "متوافق مع الشريعة"
      },
      "legalTerms": {
        "termsAndConditions": "الشروط والأحكام",
        "privacyPolicy": "سياسة الخصوصية",
        "shariaCompliance": "الامتثال للشريعة الإسلامية"
      }
    }
  }
}
```

---

## 🚨 Error Handling

### Standard Error Response Format

All APIs return errors in a standardized format:

```json
{
  "status": "ERROR",
  "timestamp": "2024-12-11T10:45:00Z",
  "error": {
    "code": "INVALID_MURABAHA_TERMS",
    "message": "Profit margin exceeds HSA maximum limit of 30%",
    "details": {
      "requestedMargin": 35.0,
      "maximumAllowed": 30.0,
      "regulation": "UAE_HIGHER_SHARIA_AUTHORITY"
    },
    "traceId": "trace-abc123",
    "supportReference": "SUP-20241211-001"
  },
  "validationErrors": [
    {
      "field": "financingTerms.profitMargin",
      "code": "EXCEEDS_MAXIMUM",
      "message": "Profit margin must not exceed 30% as per HSA guidelines"
    }
  ]
}
```

### HTTP Status Codes

| Status Code | Description | Use Case |
|-------------|-------------|----------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request format or parameters |
| 401 | Unauthorized | Authentication required or failed |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Business rule violation or conflict |
| 422 | Unprocessable Entity | Sharia compliance violation |
| 429 | Too Many Requests | Rate limiting exceeded |
| 500 | Internal Server Error | System error |
| 503 | Service Unavailable | System maintenance or overload |

---

## 📊 Rate Limiting

### API Rate Limits

| Endpoint Category | Requests per Minute | Burst Limit |
|------------------|-------------------|-------------|
| **Authentication** | 30 | 10 |
| **Islamic Finance** | 100 | 20 |
| **CBDC Transactions** | 50 | 15 |
| **Customer Management** | 200 | 50 |
| **Reporting** | 20 | 5 |
| **Open Banking** | 300 | 100 |

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 85
X-RateLimit-Reset: 1639223400
X-RateLimit-Retry-After: 60
```

---

## 📈 API Versioning

### Version Strategy

- **Current Version**: v1
- **Supported Versions**: v1
- **Deprecation Policy**: 12 months notice
- **Version Header**: `X-API-Version: 1.0`

### Version-Specific Changes

#### v1.0 (Current)
- Initial release with full Islamic finance capabilities
- CBDC integration with Digital Dirham
- Comprehensive regulatory compliance

#### v1.1 (Planned - Q1 2025)
- Enhanced cross-border CBDC support
- Additional Islamic finance products
- Improved analytics APIs

---

## 🔍 API Testing

### Testing Environment

**Base URL**: `https://api-sandbox.amanahfi.ae`

### Test Data

Use the following test credentials for sandbox testing:

```json
{
  "testCustomer": {
    "customerId": "TEST-CUST-001",
    "name": "Ahmed Test Customer",
    "emiratesId": "784-0000-0000000-0"
  },
  "testFinancing": {
    "financingId": "TEST-MURABAHA-001",
    "amount": "10000.00",
    "currency": "AED"
  }
}
```

### Postman Collection

Download our comprehensive Postman collection:
[AmanahFi API Collection](https://api.amanahfi.ae/docs/postman/collection.json)

---

**📞 API Support**

- **Developer Portal**: [https://developers.amanahfi.ae](https://developers.amanahfi.ae)
- **API Documentation**: [https://docs.amanahfi.ae/api](https://docs.amanahfi.ae/api)
- **Technical Support**: [api-support@amanahfi.ae](mailto:api-support@amanahfi.ae)
- **Emergency Support**: +971-4-XXX-XXXX (24/7)

---

*This API catalogue is maintained by the API Architecture Team and updated with each platform release.*