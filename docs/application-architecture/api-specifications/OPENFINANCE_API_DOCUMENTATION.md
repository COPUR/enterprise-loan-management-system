# OpenFinance & OpenBanking FAPI-Compliant API Gateway
## Enterprise Loan Management System - Advanced Banking Integration

**API Version**: FAPI 1.0 Advanced  
**Compliance**: OpenBanking UK 3.1.10, OpenFinance Brazil 2.0  
**Security**: OAuth2 PKCE, JWS Signing, MTLS Authentication  
**Base URL**: `https://api.enterprise-loans.com/fapi/v1`  

---

## Authentication and Security

### FAPI Security Requirements

All API requests must include FAPI-compliant security headers:

```http
POST /fapi/v1/domestic-payment-consents
Authorization: Bearer {access_token}
x-fapi-auth-date: 2025-06-12T10:30:00Z
x-fapi-customer-ip-address: 192.168.1.100
x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000
x-jws-signature: eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

### OAuth2 Scopes

| Scope | Description | Required For |
|-------|-------------|--------------|
| `accounts` | Read account information | Account Information APIs |
| `payments` | Initiate payments | Payment Initiation APIs |
| `openbanking_aisp` | Account Information Service Provider | UK OpenBanking |
| `openbanking_pisp` | Payment Initiation Service Provider | UK OpenBanking |
| `openfinance` | OpenFinance Brazil compliance | Brazilian market |
| `credit_offers` | Access credit product information | Loan marketplace |

---

## Account Information Service Provider (AISP) APIs

### Get Account Details

Retrieve comprehensive account information for loan management.

```http
GET /fapi/v1/accounts/{AccountId}
```

**Parameters:**
- `AccountId` (path): Customer account identifier

**Response:**
```json
{
  "Data": {
    "Account": {
      "AccountId": "12345",
      "Currency": "USD",
      "AccountType": "Loan",
      "AccountSubType": "CreditCard",
      "Nickname": "John Doe Loan Account",
      "OpeningDate": "2024-01-15"
    }
  },
  "Links": {
    "Self": "/fapi/v1/accounts/12345"
  },
  "Meta": {
    "TotalPages": 1
  }
}
```

### Get Account Balances

Retrieve current balance and available credit information.

```http
GET /fapi/v1/accounts/{AccountId}/balances
```

**Response:**
```json
{
  "Data": {
    "Balance": [
      {
        "AccountId": "12345",
        "CreditDebitIndicator": "Debit",
        "Type": "OpeningAvailable",
        "DateTime": "2025-06-12T10:30:00Z",
        "Amount": {
          "Amount": "25000.00",
          "Currency": "USD"
        }
      },
      {
        "AccountId": "12345",
        "CreditDebitIndicator": "Credit",
        "Type": "Available",
        "DateTime": "2025-06-12T10:30:00Z",
        "Amount": {
          "Amount": "75000.00",
          "Currency": "USD"
        }
      }
    ]
  },
  "Links": {
    "Self": "/fapi/v1/accounts/12345/balances"
  },
  "Meta": {
    "TotalPages": 1
  }
}
```

### Get Account Transactions

Retrieve transaction history with optional date filtering.

```http
GET /fapi/v1/accounts/{AccountId}/transactions?fromBookingDateTime=2024-01-01T00:00:00Z&toBookingDateTime=2025-06-12T23:59:59Z
```

**Response:**
```json
{
  "Data": {
    "Transaction": [
      {
        "AccountId": "12345",
        "TransactionId": "txn-001",
        "TransactionReference": "TXN-2024-001",
        "Amount": {
          "Amount": "1200.50",
          "Currency": "USD"
        },
        "CreditDebitIndicator": "Debit",
        "Status": "Booked",
        "BookingDateTime": "2025-06-01T14:30:00Z",
        "ValueDateTime": "2025-06-01T14:30:00Z",
        "TransactionInformation": "Loan Payment - BANK_TRANSFER",
        "MerchantDetails": {
          "MerchantName": "Enterprise Loan Management",
          "MerchantCategoryCode": "6012"
        }
      }
    ]
  },
  "Links": {
    "Self": "/fapi/v1/accounts/12345/transactions"
  },
  "Meta": {
    "TotalPages": 1,
    "FirstAvailableDateTime": "2024-01-01T00:00:00Z",
    "LastAvailableDateTime": "2025-06-12T23:59:59Z"
  }
}
```

---

## Payment Initiation Service Provider (PISP) APIs

### Create Payment Consent

Create payment consent before initiating payment.

```http
POST /fapi/v1/domestic-payment-consents
```

**Request:**
```json
{
  "Data": {
    "Initiation": {
      "InstructionIdentification": "INSTR-001",
      "EndToEndIdentification": "E2E-001",
      "InstructedAmount": {
        "Amount": "1200.50",
        "Currency": "USD"
      },
      "DebtorAccount": {
        "SchemeName": "SortCodeAccountNumber",
        "Identification": "12345678901234",
        "Name": "John Doe"
      },
      "CreditorAccount": {
        "SchemeName": "SortCodeAccountNumber", 
        "Identification": "98765432109876",
        "Name": "Enterprise Loan Management"
      },
      "RemittanceInformation": {
        "Reference": "Loan-001",
        "Unstructured": "Monthly loan payment"
      }
    }
  },
  "Risk": {
    "PaymentContextCode": "BillPayment",
    "MerchantCategoryCode": "6012",
    "MerchantCustomerIdentification": "12345",
    "DeliveryAddress": {
      "AddressLine": ["123 Main Street"],
      "PostCode": "12345",
      "Country": "US"
    }
  }
}
```

**Response:**
```json
{
  "Data": {
    "ConsentId": "consent-123-456",
    "Status": "AwaitingAuthorisation",
    "CreationDateTime": "2025-06-12T10:30:00Z",
    "StatusUpdateDateTime": "2025-06-12T10:30:00Z",
    "Initiation": {
      "InstructionIdentification": "INSTR-001",
      "EndToEndIdentification": "E2E-001",
      "InstructedAmount": {
        "Amount": "1200.50",
        "Currency": "USD"
      }
    },
    "Authorisation": {
      "AuthorisationType": "Single",
      "CompletionDateTime": "2025-06-12T10:45:00Z"
    }
  },
  "Risk": {
    "PaymentContextCode": "BillPayment"
  },
  "Links": {
    "Self": "/fapi/v1/domestic-payment-consents/consent-123-456"
  },
  "Meta": {
    "TotalPages": 1
  }
}
```

### Submit Payment

Execute payment based on approved consent.

```http
POST /fapi/v1/domestic-payments
```

**Request:**
```json
{
  "Data": {
    "ConsentId": "consent-123-456",
    "Initiation": {
      "InstructionIdentification": "INSTR-001",
      "EndToEndIdentification": "E2E-001",
      "InstructedAmount": {
        "Amount": "1200.50",
        "Currency": "USD"
      },
      "DebtorAccount": {
        "SchemeName": "SortCodeAccountNumber",
        "Identification": "12345678901234"
      },
      "CreditorAccount": {
        "SchemeName": "SortCodeAccountNumber",
        "Identification": "98765432109876"
      },
      "RemittanceInformation": {
        "Reference": "Loan-001"
      }
    }
  }
}
```

**Response:**
```json
{
  "Data": {
    "DomesticPaymentId": "payment-789-012",
    "ConsentId": "consent-123-456",
    "Status": "AcceptedSettlementCompleted",
    "CreationDateTime": "2025-06-12T10:30:00Z",
    "StatusUpdateDateTime": "2025-06-12T10:31:00Z",
    "ExpectedExecutionDateTime": "2025-06-12T10:31:00Z",
    "ExpectedSettlementDateTime": "2025-06-12T10:31:00Z",
    "Initiation": {
      "InstructionIdentification": "INSTR-001",
      "EndToEndIdentification": "E2E-001",
      "InstructedAmount": {
        "Amount": "1200.50",
        "Currency": "USD"
      }
    }
  },
  "Links": {
    "Self": "/fapi/v1/domestic-payments/payment-789-012"
  },
  "Meta": {
    "TotalPages": 1
  }
}
```

---

## OpenFinance Credit Information APIs

### Get Credit Offers

Retrieve personalized credit offers based on customer profile.

```http
GET /fapi/v1/credit-offers?customerId=12345
```

**Response:**
```json
{
  "Data": {
    "Offers": [
      {
        "OfferId": "offer-abc-123",
        "ProductType": "PERSONAL_LOAN",
        "InterestRate": 0.12,
        "MaxAmount": 75000.00,
        "TermMonths": [12, 24, 36, 48],
        "ValidUntil": "2025-07-12T10:30:00Z",
        "Features": [
          "No prepayment penalty",
          "Flexible payment dates",
          "Digital loan management"
        ],
        "Eligibility": {
          "MinCreditScore": 650,
          "MinIncome": 50000,
          "EmploymentRequired": true
        }
      },
      {
        "OfferId": "offer-def-456",
        "ProductType": "BUSINESS_LOAN",
        "InterestRate": 0.14,
        "MaxAmount": 150000.00,
        "TermMonths": [24, 36, 48, 60],
        "ValidUntil": "2025-07-12T10:30:00Z",
        "Features": [
          "Business equipment financing",
          "Working capital support",
          "Revenue-based underwriting"
        ]
      }
    ]
  },
  "Links": {
    "Self": "/fapi/v1/credit-offers?customerId=12345"
  },
  "Meta": {
    "TotalPages": 1
  }
}
```

---

## Model Context Protocol (MCP) for LLM Integration

### MCP Server Capabilities

The MCP server provides structured banking data for LLM consumption.

```http
GET /mcp/v1/resources
```

**Response:**
```json
{
  "resources": [
    {
      "uri": "banking://customers",
      "name": "Customer Management",
      "description": "Access customer profiles, credit scores, and account information",
      "mimeType": "application/json",
      "capabilities": ["read", "query", "analytics"]
    },
    {
      "uri": "banking://loans",
      "name": "Loan Portfolio",
      "description": "Loan applications, approvals, calculations, and status tracking",
      "mimeType": "application/json",
      "capabilities": ["read", "create", "update", "calculate"]
    },
    {
      "uri": "banking://payments",
      "name": "Payment Processing",
      "description": "Payment history, processing, and transaction analytics",
      "mimeType": "application/json",
      "capabilities": ["read", "create", "analytics"]
    }
  ]
}
```

### MCP Tools for Banking Operations

```http
GET /mcp/v1/tools
```

**Response:**
```json
{
  "tools": [
    {
      "name": "get_customer_profile",
      "description": "Retrieve complete customer profile including credit score and loan history",
      "inputSchema": {
        "type": "object",
        "properties": {
          "customerId": {
            "type": "string",
            "description": "Customer ID or email"
          },
          "includeHistory": {
            "type": "boolean",
            "description": "Include loan and payment history"
          }
        },
        "required": ["customerId"]
      }
    },
    {
      "name": "calculate_loan_eligibility",
      "description": "Calculate loan eligibility and personalized rates",
      "inputSchema": {
        "type": "object",
        "properties": {
          "customerId": {"type": "string"},
          "requestedAmount": {"type": "number"},
          "termMonths": {"type": "number"}
        },
        "required": ["customerId", "requestedAmount", "termMonths"]
      }
    }
  ]
}
```

### Execute MCP Tool - Customer Profile

```http
POST /mcp/v1/tools/get_customer_profile
```

**Request:**
```json
{
  "name": "get_customer_profile",
  "arguments": {
    "customerId": "12345",
    "includeHistory": true
  }
}
```

**Response:**
```json
{
  "isError": false,
  "content": [
    {
      "type": "application/json",
      "data": {
        "customerId": 12345,
        "name": "John Doe",
        "email": "john.doe@example.com",
        "creditScore": 750,
        "accountStatus": "ACTIVE",
        "memberSince": "2024-01-15T09:00:00Z",
        "loanHistory": [
          {
            "loanId": 1,
            "amount": 25000.00,
            "interestRate": 0.12,
            "termMonths": 24,
            "status": "APPROVED",
            "purpose": "Home improvement"
          }
        ],
        "totalLoansAmount": 25000.00,
        "totalPaymentsAmount": 2400.50
      }
    }
  ]
}
```

---

## LLM Chatbot Interface

### Chat with Banking Assistant

Natural language interface for banking operations.

```http
POST /llm/v1/chat
```

**Request:**
```json
{
  "message": "I want to apply for a $50,000 loan for 36 months",
  "customerId": "12345",
  "conversationId": "conv-abc-123",
  "sessionId": "session-def-456"
}
```

**Response:**
```json
{
  "message": "Great news! You're eligible for this loan.\n\n Requested Amount: $50,000.00\n Approved Amount: $50,000.00\n Interest Rate: 12.0% per annum\n📅 Term: 36 months\n Monthly Payment: $1,662.76\n\nReason: Approved based on credit score 750 and loan amount $50,000.00 within limits\n\nWould you like to proceed with the application?",
  "responseType": "loan_eligibility",
  "data": {
    "eligible": true,
    "requestedAmount": 50000.00,
    "approvedAmount": 50000.00,
    "interestRate": 0.12,
    "termMonths": 36,
    "monthlyPayment": 1662.76,
    "creditScore": 750,
    "riskLevel": "LOW"
  },
  "suggestions": [
    "Proceed with application",
    "Explore different terms",
    "Set up automatic payments",
    "Download loan documents"
  ],
  "conversationId": "conv-abc-123",
  "timestamp": "2025-06-12T10:30:00Z"
}
```

### Get Banking Context for LLMs

```http
GET /llm/v1/context?customerId=12345
```

**Response:**
```json
{
  "systemName": "Enterprise Loan Management System",
  "capabilities": [
    "Account balance inquiries",
    "Loan application processing",
    "Payment processing and history",
    "EMI calculations",
    "Credit score information",
    "Loan eligibility assessment"
  ],
  "supportedOperations": [
    "get_customer_profile",
    "calculate_loan_eligibility",
    "process_payment",
    "get_portfolio_analytics"
  ],
  "complianceLevel": "FAPI 1.0 Advanced",
  "securityFeatures": [
    "OAuth2 with PKCE",
    "Request/Response signing",
    "MTLS authentication",
    "Audit logging"
  ],
  "customerContext": {
    "customerId": 12345,
    "name": "John Doe",
    "creditScore": 750,
    "accountStatus": "ACTIVE"
  }
}
```

---

## Error Handling

### FAPI Error Response Format

All errors follow FAPI-compliant structured format:

```json
{
  "Code": "400",
  "Id": "550e8400-e29b-41d4-a716-446655440000",
  "Message": "Bad Request",
  "Errors": [
    {
      "ErrorCode": "UK.OBIE.Field.Invalid",
      "Message": "The field received is invalid",
      "Path": "/Data/Initiation/InstructedAmount/Amount",
      "Url": "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/..."
    }
  ]
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `UK.OBIE.Field.Missing` | 400 | Required field missing |
| `UK.OBIE.Field.Invalid` | 400 | Field format invalid |
| `UK.OBIE.Signature.Invalid` | 401 | JWS signature validation failed |
| `UK.OBIE.Header.Missing` | 400 | Required FAPI header missing |
| `UK.OBIE.UnexpectedError` | 500 | Internal server error |

---

## Testing and Integration

### Postman Collection

Import the provided Postman collection for comprehensive API testing:

```bash
# Download collection
curl -O https://api.enterprise-loans.com/postman/openfinance-collection.json

# Import into Postman and configure environment variables:
# - base_url: https://api.enterprise-loans.com
# - access_token: your_oauth2_token
# - customer_id: test_customer_id
```

### Sample Test Scenarios

1. **Account Information Flow**
   - Authenticate with OAuth2
   - Retrieve account details
   - Check account balances
   - Get transaction history

2. **Payment Initiation Flow**
   - Create payment consent
   - Authorize consent (simulate SCA)
   - Submit payment
   - Check payment status

3. **Credit Offers Flow**
   - Request credit offers
   - Apply for selected loan
   - Check application status

4. **LLM Integration Flow**
   - Start chat conversation
   - Process natural language queries
   - Execute banking operations
   - Maintain conversation context

### Compliance Validation

All APIs are validated against:
- OpenBanking UK 3.1.10 specification
- OpenFinance Brazil 2.0 standards
- FAPI 1.0 Advanced security requirements
- OAuth2 PKCE implementation
- JWS signing and verification

The system maintains comprehensive audit logs for regulatory compliance and supports real-time monitoring of all API interactions.