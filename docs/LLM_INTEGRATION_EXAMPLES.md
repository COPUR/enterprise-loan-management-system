# LLM Integration Examples
## Enterprise Loan Management System - Practical Implementation Guide

### Overview

The Enterprise Loan Management System provides comprehensive GraphQL and MCP integration designed specifically for Large Language Model interactions, enabling AI systems to access real banking data, perform complex queries, and execute transactions through natural language interfaces.

---

## Integration Architecture

### GraphQL Endpoint
- **URL**: `http://localhost:5000/graphql`
- **Protocol**: HTTP POST with JSON payloads
- **Authentication**: JWT Bearer tokens (optional for demo)
- **Features**: Query, Mutation, Subscription support with real-time updates

### MCP Server
- **URL**: `ws://localhost:5000/mcp` 
- **Protocol**: WebSocket with JSON-RPC 2.0
- **Tools**: 11 specialized banking operations
- **Resources**: 4 data sources (customers, loans, payments, analytics)
- **Prompts**: 3 LLM-optimized prompt templates

---

## GraphQL Integration Examples

### 1. Customer Data Analysis
```javascript
// LLM can query comprehensive customer information
const customerQuery = `
  query CustomerAnalysis($customerId: ID!) {
    customer(id: $customerId) {
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
        daysOverdue
        installments {
          installmentNumber
          dueDate
          totalAmount
          status
        }
      }
      riskProfile {
        overallRisk
        paymentHistory
        creditUtilization
        incomeStability
      }
    }
  }
`;

// Execute with variables
fetch('http://localhost:5000/graphql', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    query: customerQuery,
    variables: { customerId: "1" }
  })
}).then(response => response.json());
```

### 2. Natural Language Query Processing
```javascript
// LLM can process natural language banking queries
const nlQuery = `
  query NaturalLanguageQuery($query: String!, $context: NLContext) {
    nlQuery(query: $query, context: $context) {
      query
      intent
      entities {
        type
        value
        confidence
        position
      }
      result
      confidence
      suggestions
      executionTime
    }
  }
`;

// Example usage
const queryData = {
  query: nlQuery,
  variables: {
    query: "Show me all overdue loans for high-risk customers",
    context: {
      domain: "RISK_MANAGEMENT",
      language: "en"
    }
  }
};
```

### 3. Real-time Analytics Dashboard
```javascript
// LLM can access comprehensive banking analytics
const analyticsQuery = `
  query BankingAnalytics {
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
    
    paymentAnalytics(period: LAST_30_DAYS) {
      totalPayments
      totalPaymentAmount
      onTimePaymentRate
      earlyPaymentRate
      latePaymentRate
      paymentMethodDistribution {
        method
        count
        totalAmount
        percentage
      }
    }
    
    systemHealth {
      status
      timestamp
      services {
        serviceName
        status
        responseTime
        errorRate
      }
      metrics {
        cpuUsage
        memoryUsage
        activeConnections
        requestsPerSecond
      }
    }
  }
`;
```

### 4. Payment Processing and Calculations
```javascript
// LLM can calculate and process payments
const paymentCalculation = `
  query PaymentCalculation($input: PaymentCalculationInput!) {
    paymentCalculation(input: $input) {
      baseAmount
      discountAmount
      penaltyAmount
      finalAmount
      earlyPaymentDays
      latePaymentDays
      installmentBreakdown {
        installmentNumber
        originalAmount
        discountApplied
        penaltyApplied
        amountToPay
      }
    }
  }
`;

// Process actual payment
const processPayment = `
  mutation ProcessPayment($input: ProcessPaymentInput!) {
    processPayment(input: $input) {
      ... on PaymentSuccess {
        payment {
          paymentId
          paymentAmount
          status
          processingFee
        }
        message
      }
      ... on PaymentError {
        message
        code
        field
      }
    }
  }
`;
```

---

## MCP Server Integration Examples

### 1. WebSocket Connection Setup
```javascript
// LLM establishes MCP connection
const ws = new WebSocket('ws://localhost:5000/mcp');

ws.onopen = function() {
  // Send MCP initialization
  const initMessage = {
    jsonrpc: "2.0",
    id: "init",
    method: "initialize",
    params: {
      protocolVersion: "1.0.0",
      capabilities: {
        tools: true,
        resources: true,
        prompts: true
      }
    }
  };
  ws.send(JSON.stringify(initMessage));
};

ws.onmessage = function(event) {
  const response = JSON.parse(event.data);
  console.log('MCP Response:', response);
};
```

### 2. Tool Execution Examples
```javascript
// Search customers with complex criteria
const searchCustomers = {
  jsonrpc: "2.0",
  id: "search_1",
  method: "tools/call",
  params: {
    name: "search_customers",
    arguments: {
      query: "high credit score customers",
      filters: {
        creditScore: { min: 750, max: 850 },
        accountStatus: "ACTIVE",
        riskLevel: "LOW"
      }
    }
  }
};

// Get comprehensive customer details
const getCustomerDetails = {
  jsonrpc: "2.0",
  id: "customer_1",
  method: "tools/call",
  params: {
    name: "get_customer_details",
    arguments: {
      customerId: "CUST-001"
    }
  }
};

// Process natural language queries
const nlQueryTool = {
  jsonrpc: "2.0",
  id: "nl_1",
  method: "tools/call",
  params: {
    name: "natural_language_query",
    arguments: {
      query: "What customers have overdue payments greater than $5000?",
      context: {
        domain: "RISK_MANAGEMENT",
        language: "en"
      }
    }
  }
};
```

### 3. Resource Access Examples
```javascript
// Access banking databases
const getCustomersDatabase = {
  jsonrpc: "2.0",
  id: "resource_1",
  method: "resources/read",
  params: {
    uri: "banking://customers"
  }
};

const getLoansDatabase = {
  jsonrpc: "2.0",
  id: "resource_2", 
  method: "resources/read",
  params: {
    uri: "banking://loans"
  }
};

const getAnalytics = {
  jsonrpc: "2.0",
  id: "resource_3",
  method: "resources/read",
  params: {
    uri: "banking://analytics"
  }
};
```

### 4. LLM Prompt Generation
```javascript
// Generate customer analysis prompt
const customerAnalysisPrompt = {
  jsonrpc: "2.0",
  id: "prompt_1",
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

// Generate risk assessment prompt
const riskAssessmentPrompt = {
  jsonrpc: "2.0",
  id: "prompt_2",
  method: "prompts/get",
  params: {
    name: "risk_assessment",
    arguments: {
      customerId: "CUST-001",
      loanAmount: 50000.00
    }
  }
};
```

---

## Real-time Subscriptions for LLMs

### 1. Loan Status Monitoring
```javascript
// GraphQL subscription for real-time loan updates
const loanSubscription = `
  subscription LoanUpdates($customerId: ID) {
    loanStatusUpdates(customerId: $customerId) {
      loanId
      customerId
      oldStatus
      newStatus
      timestamp
      reason
    }
  }
`;

// WebSocket subscription setup
const subscriptionWs = new WebSocket('ws://localhost:5000/graphql', 'graphql-ws');

subscriptionWs.onopen = function() {
  const initMessage = {
    type: 'connection_init',
    payload: {}
  };
  subscriptionWs.send(JSON.stringify(initMessage));
};
```

### 2. Payment Notifications
```javascript
const paymentSubscription = `
  subscription PaymentNotifications($customerId: ID) {
    paymentNotifications(customerId: $customerId) {
      paymentId
      loanId
      customerId
      amount
      status
      timestamp
    }
  }
`;
```

### 3. System Alerts
```javascript
const systemAlertsSubscription = `
  subscription SystemAlerts($severity: AlertSeverity) {
    systemAlerts(severity: $severity) {
      id
      severity
      message
      component
      timestamp
      resolved
    }
  }
`;
```

---

## Practical LLM Implementation Examples

### 1. Customer Service Chatbot
```python
import asyncio
import websockets
import json
import requests

class BankingChatbot:
    def __init__(self):
        self.graphql_url = "http://localhost:5000/graphql"
        self.mcp_url = "ws://localhost:5000/mcp"
        
    async def process_customer_query(self, query, customer_id=None):
        # Use MCP for natural language processing
        async with websockets.connect(self.mcp_url) as websocket:
            nl_request = {
                "jsonrpc": "2.0",
                "id": "chatbot_query",
                "method": "tools/call",
                "params": {
                    "name": "natural_language_query",
                    "arguments": {
                        "query": query,
                        "context": {
                            "domain": "CUSTOMER_SERVICE",
                            "language": "en"
                        }
                    }
                }
            }
            
            await websocket.send(json.dumps(nl_request))
            response = await websocket.recv()
            return json.loads(response)
    
    def get_customer_data(self, customer_id):
        # Use GraphQL for detailed customer information
        query = """
        query CustomerDetails($id: ID!) {
            customer(id: $id) {
                fullName
                accountStatus
                creditLimit
                availableCredit
                loans {
                    loanId
                    outstandingAmount
                    status
                    nextInstallment { dueDate totalAmount }
                }
            }
        }
        """
        
        response = requests.post(
            self.graphql_url,
            json={"query": query, "variables": {"id": customer_id}}
        )
        return response.json()

# Usage example
chatbot = BankingChatbot()
customer_data = chatbot.get_customer_data("1")
print(f"Customer: {customer_data['data']['customer']['fullName']}")
```

### 2. Risk Analysis Assistant
```python
class RiskAnalysisAssistant:
    def __init__(self):
        self.graphql_url = "http://localhost:5000/graphql"
        
    def analyze_customer_risk(self, customer_id):
        query = """
        query RiskAnalysis($customerId: ID!) {
            customer(id: $customerId) {
                creditScore
                riskLevel
                riskProfile {
                    overallRisk
                    paymentHistory
                    creditUtilization
                }
            }
            
            riskAssessment(customerId: $customerId) {
                overallRiskScore
                creditRisk
                incomeRisk
                behavioralRisk
                riskFactors {
                    factor
                    impact
                    description
                }
                recommendations {
                    action
                    description
                    urgency
                }
            }
        }
        """
        
        response = requests.post(
            self.graphql_url,
            json={"query": query, "variables": {"customerId": customer_id}}
        )
        return response.json()
    
    def generate_risk_report(self, customer_id):
        data = self.analyze_customer_risk(customer_id)
        customer = data['data']['customer']
        risk_assessment = data['data']['riskAssessment']
        
        report = f"""
        Risk Analysis Report
        Customer: {customer_id}
        Credit Score: {customer['creditScore']}
        Overall Risk Score: {risk_assessment['overallRiskScore']:.2f}
        
        Risk Breakdown:
        - Credit Risk: {risk_assessment['creditRisk']:.2f}
        - Income Risk: {risk_assessment['incomeRisk']:.2f}
        - Behavioral Risk: {risk_assessment['behavioralRisk']:.2f}
        
        Key Risk Factors:
        """
        
        for factor in risk_assessment['riskFactors']:
            report += f"- {factor['factor']}: {factor['description']}\n"
        
        return report

# Usage
risk_assistant = RiskAnalysisAssistant()
report = risk_assistant.generate_risk_report("1")
print(report)
```

### 3. Payment Optimization Bot
```python
class PaymentOptimizationBot:
    def __init__(self):
        self.graphql_url = "http://localhost:5000/graphql"
    
    def optimize_payment_schedule(self, loan_id):
        # Get loan details and calculate optimal payment strategy
        query = """
        query PaymentOptimization($loanId: ID!) {
            loan(id: $loanId) {
                loanAmount
                outstandingAmount
                interestRate
                installments {
                    installmentNumber
                    dueDate
                    totalAmount
                    status
                }
            }
            
            paymentCalculation(input: {
                loanId: $loanId
                paymentAmount: 5000.00
                paymentDate: "2025-06-15T00:00:00Z"
                installmentNumbers: [1, 2, 3]
                simulateOnly: true
            }) {
                baseAmount
                discountAmount
                finalAmount
                installmentBreakdown {
                    installmentNumber
                    originalAmount
                    discountApplied
                    amountToPay
                }
            }
        }
        """
        
        response = requests.post(
            self.graphql_url,
            json={"query": query, "variables": {"loanId": loan_id}}
        )
        return response.json()
    
    def suggest_payment_strategy(self, loan_id):
        data = self.optimize_payment_schedule(loan_id)
        loan = data['data']['loan']
        calculation = data['data']['paymentCalculation']
        
        savings = calculation['discountAmount']
        
        strategy = f"""
        Payment Optimization Strategy for Loan {loan_id}
        
        Current Outstanding: ${loan['outstandingAmount']}
        Potential Savings: ${savings}
        
        Recommended Action:
        """
        
        if savings > 0:
            strategy += f"Early payment could save ${savings}. Consider paying multiple installments."
        else:
            strategy += "Continue with regular payment schedule."
        
        return strategy

# Usage
payment_bot = PaymentOptimizationBot()
strategy = payment_bot.suggest_payment_strategy("LOAN-001")
print(strategy)
```

---

## Authentication and Security

### JWT Token Integration
```javascript
// Include JWT token for authenticated requests
const authenticatedRequest = {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify({
    query: secureQuery,
    variables: queryVariables
  })
};

fetch('http://localhost:5000/graphql', authenticatedRequest);
```

### Rate Limiting Considerations
- GraphQL: 1000 requests per minute per IP
- MCP WebSocket: 1000 concurrent connections
- Complex queries: Automatic depth and complexity analysis
- Authentication: Optional JWT for demo, required for production

---

## Error Handling

### GraphQL Error Responses
```javascript
// Handle GraphQL errors appropriately
function handleGraphQLResponse(response) {
  if (response.errors) {
    console.error('GraphQL Errors:', response.errors);
    // Handle specific error types
    response.errors.forEach(error => {
      switch(error.extensions?.code) {
        case 'NOT_FOUND':
          console.log('Resource not found:', error.message);
          break;
        case 'VALIDATION_ERROR':
          console.log('Validation failed:', error.message);
          break;
        default:
          console.log('Unknown error:', error.message);
      }
    });
  }
  
  return response.data;
}
```

### MCP Error Handling
```javascript
// Handle MCP protocol errors
function handleMCPResponse(response) {
  if (response.error) {
    console.error('MCP Error:', response.error);
    switch(response.error.code) {
      case 'METHOD_NOT_FOUND':
        console.log('Tool or method not available');
        break;
      case 'INVALID_PARAMS':
        console.log('Invalid parameters provided');
        break;
      default:
        console.log('MCP protocol error');
    }
    return null;
  }
  
  return response.result;
}
```

This comprehensive integration enables LLMs to access real banking data, perform complex financial calculations, execute transactions, and provide intelligent insights through both GraphQL queries and MCP protocol interactions.