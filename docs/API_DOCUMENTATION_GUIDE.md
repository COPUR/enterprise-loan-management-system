# Enterprise Banking API Documentation Guide

## Overview

This guide provides comprehensive documentation for the Enterprise Banking Platform API, including setup instructions, usage examples, and testing procedures.

## API Architecture

### Core Features
- **RESTful Design**: Following REST principles with proper HTTP methods and status codes
- **OpenAPI 3.1**: Complete specification with interactive documentation
- **HATEOAS**: Hypermedia-driven API navigation
- **Versioning**: URL-based and header-based versioning support
- **Security**: OAuth 2.1 with DPoP, mTLS, and FAPI 2.0 compliance
- **Rate Limiting**: Sophisticated rate limiting with multiple strategies
- **Real-time Events**: Server-Sent Events for live updates
- **Idempotency**: Safe retry mechanisms for all operations

### Base URL
```
Production: https://api.banking.example.com
Staging: https://api-staging.banking.example.com
Development: https://api-dev.banking.example.com
```

## Authentication

### OAuth 2.1 with DPoP (Recommended)

#### 1. Obtain Access Token
```bash
curl -X POST https://auth.banking.example.com/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjgzT1JfLCJlIjoiIn0sInktU1BiWS...}" \
  -d "grant_type=client_credentials" \
  -d "client_id=your_client_id" \
  -d "client_secret=your_client_secret" \
  -d "scope=banking:read banking:write"
```

#### 2. Use Access Token
```bash
curl -X GET https://api.banking.example.com/api/v1/customers/123 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2NjAyMzVlLTMwMDAtNGM3NS05MzFkLWQ2MzVlNzNkMzRmZCJ9..." \
  -H "DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiZjgzT1JfLCJlIjoiIn0sInktU1BiWS...}"
```

### API Key Authentication (Service-to-Service)
```bash
curl -X GET https://api.banking.example.com/api/v1/customers/123 \
  -H "X-API-Key: your_api_key_here"
```

## API Endpoints

### Customer Management

#### Create Customer
```bash
curl -X POST https://api.banking.example.com/api/v1/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-123" \
  -d '{
    "firstName": "Ahmad",
    "lastName": "Al-Rashid",
    "email": "ahmad.rashid@email.com",
    "phone": "+971501234567",
    "initialCreditLimit": {
      "amount": 50000,
      "currency": "AED"
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "customerId": "cust_1234567890",
    "firstName": "Ahmad",
    "lastName": "Al-Rashid",
    "email": "ahmad.rashid@email.com",
    "phone": "+971501234567",
    "creditLimit": {
      "amount": 50000,
      "currency": "AED"
    },
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00Z",
    "_links": {
      "self": {"href": "/api/v1/customers/cust_1234567890"},
      "loans": {"href": "/api/v1/customers/cust_1234567890/loans"},
      "payments": {"href": "/api/v1/customers/cust_1234567890/payments"}
    }
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_abc123def456"
}
```

#### Get Customer
```bash
curl -X GET https://api.banking.example.com/api/v1/customers/cust_1234567890 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

#### Update Customer Credit Limit
```bash
curl -X PUT https://api.banking.example.com/api/v1/customers/cust_1234567890/credit-limit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-456" \
  -d '{
    "newCreditLimit": {
      "amount": 75000,
      "currency": "AED"
    },
    "reason": "Increased income verification"
  }'
```

### Loan Management

#### Create Loan Application
```bash
curl -X POST https://api.banking.example.com/api/v1/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-789" \
  -d '{
    "customerId": "cust_1234567890",
    "amount": {
      "amount": 100000,
      "currency": "AED"
    },
    "term": {
      "months": 60
    },
    "purpose": "Home purchase",
    "requestedInterestRate": 0.055
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "loanId": "loan_0987654321",
    "customerId": "cust_1234567890",
    "amount": {
      "amount": 100000,
      "currency": "AED"
    },
    "term": {
      "months": 60
    },
    "status": "PENDING_APPROVAL",
    "applicationDate": "2024-01-15T10:35:00Z",
    "_links": {
      "self": {"href": "/api/v1/loans/loan_0987654321"},
      "approve": {"href": "/api/v1/loans/loan_0987654321/approve"},
      "documents": {"href": "/api/v1/loans/loan_0987654321/documents"}
    }
  },
  "timestamp": "2024-01-15T10:35:00Z",
  "requestId": "req_def456ghi789"
}
```

#### Approve Loan
```bash
curl -X PUT https://api.banking.example.com/api/v1/loans/loan_0987654321/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-101" \
  -d '{
    "approvedAmount": {
      "amount": 100000,
      "currency": "AED"
    },
    "interestRate": 0.055,
    "approvalNotes": "Standard approval based on credit score"
  }'
```

#### Get Loan Details
```bash
curl -X GET https://api.banking.example.com/api/v1/loans/loan_0987654321 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

#### Get Amortization Schedule
```bash
curl -X GET https://api.banking.example.com/api/v1/loans/loan_0987654321/amortization-schedule \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

### Payment Processing

#### Make Payment
```bash
curl -X POST https://api.banking.example.com/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-202" \
  -d '{
    "loanId": "loan_0987654321",
    "amount": {
      "amount": 2500,
      "currency": "AED"
    },
    "paymentMethod": "BANK_TRANSFER",
    "reference": "Monthly payment January 2024"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "paymentId": "pay_1122334455",
    "loanId": "loan_0987654321",
    "amount": {
      "amount": 2500,
      "currency": "AED"
    },
    "status": "PROCESSING",
    "paymentMethod": "BANK_TRANSFER",
    "paymentDate": "2024-01-15T10:40:00Z",
    "_links": {
      "self": {"href": "/api/v1/payments/pay_1122334455"},
      "receipt": {"href": "/api/v1/payments/pay_1122334455/receipt"},
      "loan": {"href": "/api/v1/loans/loan_0987654321"}
    }
  },
  "timestamp": "2024-01-15T10:40:00Z",
  "requestId": "req_ghi789jkl012"
}
```

#### Get Payment Status
```bash
curl -X GET https://api.banking.example.com/api/v1/payments/pay_1122334455 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

## Islamic Banking APIs

### Murabaha Contract Creation
```bash
curl -X POST https://api.banking.example.com/api/v1/islamic/murabaha \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-303" \
  -d '{
    "customerId": "cust_1234567890",
    "assetDetails": {
      "type": "REAL_ESTATE",
      "description": "Residential property in Dubai",
      "value": {
        "amount": 1000000,
        "currency": "AED"
      }
    },
    "financing": {
      "amount": 800000,
      "currency": "AED"
    },
    "profitRate": 0.04,
    "term": {
      "months": 120
    }
  }'
```

## Error Handling

### Standard Error Response Format (RFC 9457)
```json
{
  "type": "https://banking.example.com/problems/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 400,
  "detail": "The account does not have sufficient funds for this transaction",
  "instance": "/api/v1/payments/pay_1122334455",
  "timestamp": "2024-01-15T10:45:00Z",
  "requestId": "req_error123",
  "correlationId": "corr_error456",
  "errors": [
    {
      "field": "amount",
      "message": "Amount exceeds available balance",
      "code": "INSUFFICIENT_FUNDS"
    }
  ]
}
```

### Common Error Codes

| Status Code | Error Type | Description |
|-------------|------------|-------------|
| 400 | Bad Request | Invalid request format or parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate customer) |
| 422 | Unprocessable Entity | Validation errors |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Service temporarily unavailable |

## Rate Limiting

### Rate Limit Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 85
X-RateLimit-Reset: 1642248000
```

### Rate Limit Tiers

| Tier | Global Limit | Endpoint Limit | Sliding Window |
|------|-------------|---------------|----------------|
| Basic | 100/min | 20/min | 500/hour |
| Premium | 500/min | 100/min | 2000/hour |
| Enterprise | 2000/min | 500/min | 10000/hour |

## Real-time Events (SSE)

### Subscribe to Customer Events
```bash
curl -X GET https://api.banking.example.com/api/v1/customers/cust_1234567890/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: text/event-stream" \
  -H "Cache-Control: no-cache"
```

**Event Stream Response:**
```
event: customer.credit.updated
data: {"customerId": "cust_1234567890", "newCreditLimit": {"amount": 75000, "currency": "AED"}, "timestamp": "2024-01-15T10:50:00Z"}

event: loan.approved
data: {"loanId": "loan_0987654321", "customerId": "cust_1234567890", "amount": {"amount": 100000, "currency": "AED"}, "timestamp": "2024-01-15T10:55:00Z"}
```

## Pagination

### Request with Pagination
```bash
curl -X GET "https://api.banking.example.com/api/v1/customers?page=1&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"
```

### Paginated Response
```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  },
  "_links": {
    "self": {"href": "/api/v1/customers?page=1&size=20"},
    "first": {"href": "/api/v1/customers?page=1&size=20"},
    "next": {"href": "/api/v1/customers?page=2&size=20"},
    "last": {"href": "/api/v1/customers?page=8&size=20"}
  }
}
```

## Testing Tools

### Postman Collection
Import the Postman collection for comprehensive API testing:
```bash
# Download collection
curl -o banking-api.postman_collection.json \
  https://api.banking.example.com/docs/postman/collection.json

# Environment variables
curl -o banking-api.postman_environment.json \
  https://api.banking.example.com/docs/postman/environment.json
```

### OpenAPI Specification
Access the complete OpenAPI specification:
```bash
# JSON format
curl https://api.banking.example.com/v3/api-docs

# YAML format
curl https://api.banking.example.com/v3/api-docs.yaml
```

### Interactive API Documentation
Visit the Swagger UI interface:
```
https://api.banking.example.com/swagger-ui/index.html
```

## SDK Generation

### Generate Client SDKs
```bash
# JavaScript/TypeScript
npm install @openapi-generator-cli/cli -g
openapi-generator-cli generate \
  -i https://api.banking.example.com/v3/api-docs \
  -g typescript-axios \
  -o ./banking-api-client

# Python
pip install openapi-generator-cli
openapi-generator-cli generate \
  -i https://api.banking.example.com/v3/api-docs \
  -g python \
  -o ./banking-api-client-python

# Java
openapi-generator-cli generate \
  -i https://api.banking.example.com/v3/api-docs \
  -g java \
  -o ./banking-api-client-java
```

## Health Checks

### System Health
```bash
curl -X GET https://api.banking.example.com/actuator/health \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "responseTime": "15ms",
        "connectionTest": "PASS"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "responseTime": "3ms",
        "ping": "PONG"
      }
    },
    "business": {
      "status": "UP",
      "details": {
        "customerService": "UP",
        "loanService": "UP",
        "paymentService": "UP"
      }
    }
  }
}
```

## Monitoring and Metrics

### API Metrics
```bash
# Prometheus metrics
curl https://api.banking.example.com/actuator/prometheus

# Application metrics
curl https://api.banking.example.com/actuator/metrics
```

### Performance Monitoring
```bash
# Response time metrics
curl https://api.banking.example.com/actuator/metrics/http.server.requests

# Database metrics
curl https://api.banking.example.com/actuator/metrics/banking.database.query.time
```

## Security Best Practices

### Request Signing (JWS)
```bash
# Generate JWS signature
PAYLOAD='{"customerId": "cust_1234567890", "amount": {"amount": 100000, "currency": "AED"}}'
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -sign private.pem | base64)

curl -X POST https://api.banking.example.com/api/v1/loans \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-JWS-Signature: $SIGNATURE" \
  -H "X-Idempotency-Key: unique-key-404" \
  -d "$PAYLOAD"
```

### FAPI Headers
```bash
curl -X POST https://api.banking.example.com/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-FAPI-Interaction-ID: $(uuidgen)" \
  -H "X-FAPI-Auth-Date: $(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  -H "X-FAPI-Customer-IP-Address: 192.168.1.100" \
  -d '{...}'
```

## Troubleshooting

### Common Issues

#### 1. Authentication Failures
```bash
# Check token validity
curl -X GET https://auth.banking.example.com/oauth/introspect \
  -H "Authorization: Bearer $TOKEN"
```

#### 2. Rate Limit Exceeded
```bash
# Check rate limit status
curl -I https://api.banking.example.com/api/v1/customers/cust_1234567890 \
  -H "Authorization: Bearer $TOKEN"
```

#### 3. Validation Errors
- Ensure all required fields are provided
- Check data types and formats
- Verify currency codes and amounts
- Validate UUIDs and reference formats

### Support and Documentation

- **API Documentation**: https://api.banking.example.com/docs
- **Developer Portal**: https://developer.banking.example.com
- **Support Email**: api-support@banking.example.com
- **Status Page**: https://status.banking.example.com

## Changelog

### Version 1.0.0 (Current)
- Initial production release
- Full OAuth 2.1 with DPoP support
- FAPI 2.0 compliance
- Complete CRUD operations for all resources
- Real-time event streaming
- Comprehensive error handling
- Rate limiting and security features

### Upcoming Features
- GraphQL endpoint support
- Enhanced Islamic banking products
- Machine learning-based risk assessment
- Multi-currency support expansion
- Advanced analytics and reporting APIs