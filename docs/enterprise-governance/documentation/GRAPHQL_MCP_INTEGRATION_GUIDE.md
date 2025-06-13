# GraphQL and MCP Integration Guide
## Enterprise Loan Management System - LLM and External System Integration

### Overview

The Enterprise Loan Management System now includes comprehensive GraphQL API support with specialized MCP (Model Context Protocol) server integration, enabling seamless interaction with Large Language Models and external AI systems.

---

## GraphQL API Endpoints

### Primary GraphQL Endpoint
- **URL**: `http://localhost:5000/graphql`
- **Method**: POST
- **Content-Type**: `application/json`
- **WebSocket**: `ws://localhost:5000/graphql` (for subscriptions)

### GraphQL Playground
- **URL**: `http://localhost:5000/graphql/playground`
- **Interactive Interface**: Full-featured GraphQL IDE with schema exploration
- **Authentication**: JWT Bearer tokens supported

---

## Core GraphQL Capabilities

### 1. Customer Operations
```graphql
# Get customer with complete profile
query GetCustomer {
  customer(id: "1") {
    customerId
    fullName
    email
    creditLimit
    availableCredit
    creditScore
    accountStatus
    riskLevel
    loans {
      loanId
      loanAmount
      outstandingAmount
      status
      nextInstallment {
        dueDate
        totalAmount
      }
    }
    riskProfile {
      overallRisk
      paymentHistory
      creditUtilization
    }
  }
}

# Search customers with filters
query SearchCustomers {
  customers(
    filter: {
      creditScore: { min: 700, max: 850 }
      accountStatus: ACTIVE
      riskLevel: LOW
    }
    page: { page: 0, size: 20 }
  ) {
    nodes {
      customerId
      fullName
      creditScore
    }
    totalCount
    pageInfo {
      hasNextPage
    }
  }
}
```

### 2. Loan Management
```graphql
# Get comprehensive loan details
query GetLoanDetails {
  loan(id: "LOAN-001") {
    loanId
    customer {
      fullName
      creditScore
    }
    loanAmount
    outstandingAmount
    interestRate
    installmentCount
    status
    installments {
      installmentNumber
      dueDate
      totalAmount
      status
      paidAmount
    }
    paymentHistory {
      totalPaid
      remainingAmount
      nextDueDate
    }
    overdueAmount
    daysOverdue
  }
}

# Create new loan
mutation CreateLoan {
  createLoan(input: {
    customerId: "CUST-001"
    loanAmount: 25000.00
    interestRate: 0.15
    installmentCount: 12
    loanType: PERSONAL
    purpose: "Home improvement"
  }) {
    ... on LoanSuccess {
      loan {
        loanId
        installmentAmount
        totalRepaymentAmount
      }
      message
    }
    ... on LoanError {
      message
      code
      field
    }
  }
}
```

### 3. Payment Processing
```graphql
# Calculate payment with discounts/penalties
query CalculatePayment {
  paymentCalculation(input: {
    loanId: "LOAN-001"
    paymentAmount: 2245.22
    paymentDate: "2025-06-10T10:30:00Z"
    installmentNumbers: [3]
    simulateOnly: true
  }) {
    baseAmount
    discountAmount
    penaltyAmount
    finalAmount
    earlyPaymentDays
    installmentBreakdown {
      installmentNumber
      originalAmount
      discountApplied
      amountToPay
    }
  }
}

# Process payment
mutation ProcessPayment {
  processPayment(input: {
    loanId: "LOAN-001"
    paymentAmount: 2245.22
    paymentMethod: BANK_TRANSFER
    paymentReference: "TXN-123456"
    installmentNumbers: [3]
  }) {
    ... on PaymentSuccess {
      payment {
        paymentId
        status
        processingFee
      }
      message
    }
    ... on PaymentError {
      message
      code
    }
  }
}
```

### 4. Analytics and Reporting
```graphql
# Customer analytics
query CustomerAnalytics {
  customerAnalytics(customerId: "CUST-001") {
    totalLoans
    activeLoans
    totalBorrowed
    totalRepaid
    outstandingAmount
    creditUtilization
    riskScore
    paymentReliability {
      score
      onTimePercentage
      averageDelayDays
    }
    recommendations {
      type
      description
      priority
    }
  }
}

# System-wide loan analytics
query LoanAnalytics {
  loanAnalytics(period: LAST_30_DAYS) {
    totalLoansCreated
    totalLoanAmount
    averageLoanAmount
    approvalRate
    defaultRate
    loanTypeDistribution {
      loanType
      count
      totalAmount
      percentage
    }
  }
}
```

### 5. Natural Language Processing
```graphql
# Natural language query for LLM integration
query NaturalLanguageQuery {
  nlQuery(
    query: "Show me all overdue loans for high-risk customers in the last 30 days"
    context: {
      domain: RISK_MANAGEMENT
      language: "en"
    }
  ) {
    query
    intent
    entities {
      type
      value
      confidence
    }
    result
    confidence
    suggestions
    executionTime
  }
}

# Get AI recommendations
query GetRecommendations {
  recommendations(
    customerId: "CUST-001"
    type: CREDIT_INCREASE
  ) {
    type
    title
    description
    priority
    impact
    actionRequired
    estimatedBenefit
    implementationEffort
  }
}
```

### 6. Real-time Subscriptions
```graphql
# Subscribe to loan status updates
subscription LoanUpdates {
  loanStatusUpdates(customerId: "CUST-001") {
    loanId
    customerId
    oldStatus
    newStatus
    timestamp
    reason
  }
}

# Subscribe to payment notifications
subscription PaymentNotifications {
  paymentNotifications(customerId: "CUST-001") {
    paymentId
    loanId
    amount
    status
    timestamp
  }
}

# Subscribe to system alerts
subscription SystemAlerts {
  systemAlerts(severity: HIGH) {
    id
    severity
    message
    component
    timestamp
    resolved
  }
}
```

---

## MCP Server Integration

### MCP WebSocket Endpoint
- **URL**: `ws://localhost:5000/mcp`
- **Protocol**: JSON-RPC 2.0 over WebSocket
- **Authentication**: Optional JWT token in connection headers

### MCP Capabilities

#### 1. Tools Available
```json
{
  "tools": [
    {
      "name": "search_customers",
      "description": "Search for customers by various criteria",
      "inputSchema": {
        "type": "object",
        "properties": {
          "query": {"type": "string"},
          "filters": {
            "type": "object",
            "properties": {
              "creditScore": {"type": "object"},
              "accountStatus": {"type": "string"},
              "riskLevel": {"type": "string"}
            }
          }
        }
      }
    },
    {
      "name": "natural_language_query",
      "description": "Process natural language queries about banking data",
      "inputSchema": {
        "type": "object",
        "properties": {
          "query": {"type": "string"},
          "context": {
            "type": "object",
            "properties": {
              "domain": {"type": "string"},
              "language": {"type": "string"}
            }
          }
        }
      }
    }
  ]
}
```

#### 2. Resources Available
```json
{
  "resources": [
    {
      "uri": "banking://customers",
      "name": "Customers Database",
      "description": "Access to customer information and credit profiles",
      "mimeType": "application/json"
    },
    {
      "uri": "banking://loans",
      "name": "Loans Database", 
      "description": "Complete loan portfolio with installment schedules",
      "mimeType": "application/json"
    },
    {
      "uri": "banking://analytics",
      "name": "Analytics Engine",
      "description": "Real-time banking analytics and reporting",
      "mimeType": "application/json"
    }
  ]
}
```

#### 3. Prompts for LLM Integration
```json
{
  "prompts": [
    {
      "name": "customer_analysis",
      "description": "Comprehensive customer analysis prompt for LLMs",
      "arguments": [
        {"name": "customerId", "required": true},
        {"name": "includeLoans", "required": false},
        {"name": "includePayments", "required": false}
      ]
    },
    {
      "name": "risk_assessment", 
      "description": "Risk assessment prompt for credit decisions",
      "arguments": [
        {"name": "customerId", "required": true},
        {"name": "loanAmount", "required": false}
      ]
    }
  ]
}
```

---

## LLM Integration Examples

### 1. Customer Analysis with ChatGPT/Claude
```javascript
// Connect to MCP server
const ws = new WebSocket('ws://localhost:5000/mcp');

// Request customer analysis prompt
const promptRequest = {
  jsonrpc: "2.0",
  id: "1",
  method: "prompts/get",
  params: {
    name: "customer_analysis",
    arguments: {
      customerId: "CUST-001",
      includeLoans: true,
      includePayments: true
    }
  }
};

ws.send(JSON.stringify(promptRequest));

// The response provides a structured prompt for LLM analysis
```

### 2. Natural Language Query Processing
```javascript
// Process natural language banking queries
const nlQuery = {
  jsonrpc: "2.0",
  id: "2", 
  method: "tools/call",
  params: {
    name: "natural_language_query",
    arguments: {
      query: "Find all customers with overdue payments in the last 30 days",
      context: {
        domain: "RISK_MANAGEMENT",
        language: "en"
      }
    }
  }
};

ws.send(JSON.stringify(nlQuery));
```

### 3. Real-time Analytics Integration
```javascript
// Get comprehensive system analytics
const analyticsQuery = {
  jsonrpc: "2.0",
  id: "3",
  method: "tools/call", 
  params: {
    name: "get_analytics",
    arguments: {
      type: "loan",
      period: "LAST_30_DAYS"
    }
  }
};

ws.send(JSON.stringify(analyticsQuery));
```

---

## Authentication and Security

### JWT Token Integration
```javascript
// GraphQL with authentication
const graphqlQuery = {
  query: `
    query SecureCustomerData {
      customer(id: "CUST-001") {
        fullName
        creditLimit
        loans { loanAmount status }
      }
    }
  `
};

fetch('http://localhost:5000/graphql', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify(graphqlQuery)
});
```

### WebSocket Authentication
```javascript
// MCP WebSocket with authentication
const ws = new WebSocket('ws://localhost:5000/mcp', [], {
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  }
});
```

---

## Error Handling

### GraphQL Error Responses
```json
{
  "data": null,
  "errors": [
    {
      "message": "Customer not found",
      "locations": [{"line": 2, "column": 3}],
      "path": ["customer"],
      "extensions": {
        "code": "NOT_FOUND",
        "customerId": "INVALID-ID"
      }
    }
  ]
}
```

### MCP Error Responses
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "error": {
    "code": "INVALID_PARAMS",
    "message": "Missing required parameter: customerId"
  }
}
```

---

## Performance Considerations

### Query Optimization
- **DataLoader Pattern**: Implemented for efficient batch loading
- **Field-level Caching**: Redis integration for frequently accessed data
- **Pagination**: Cursor-based pagination for large datasets
- **Query Complexity Analysis**: Automatic query depth and complexity limits

### Connection Limits
- **GraphQL**: No connection limit (stateless HTTP)
- **WebSocket/MCP**: 1000 concurrent connections per server instance
- **Rate Limiting**: 1000 requests/minute per IP for GraphQL

---

## Monitoring and Observability

### GraphQL Metrics
```graphql
# Available through /actuator/prometheus
query SystemMetrics {
  systemHealth {
    status
    timestamp
    services {
      serviceName
      status
      responseTime
    }
    metrics {
      cpuUsage
      memoryUsage
      requestsPerSecond
    }
  }
}
```

### MCP Health Monitoring
```json
{
  "jsonrpc": "2.0",
  "id": "health",
  "method": "tools/call",
  "params": {
    "name": "system_health",
    "arguments": {}
  }
}
```

---

## Integration Testing

### GraphQL Testing
```bash
# Test GraphQL endpoint
curl -X POST http://localhost:5000/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { systemHealth { status timestamp } }"
  }'
```

### MCP Testing
```bash
# Test MCP WebSocket connection
wscat -c ws://localhost:5000/mcp

# Send initialization message
{"jsonrpc":"2.0","id":"init","method":"initialize","params":{"protocolVersion":"1.0.0"}}
```

This comprehensive GraphQL and MCP integration provides powerful APIs for LLM systems, external applications, and real-time banking operations with enterprise-grade security and performance.